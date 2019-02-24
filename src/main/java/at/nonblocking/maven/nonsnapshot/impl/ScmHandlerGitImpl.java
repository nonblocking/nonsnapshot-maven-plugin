/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.nonblocking.maven.nonsnapshot.impl;

import at.nonblocking.maven.nonsnapshot.PathUtil;
import at.nonblocking.maven.nonsnapshot.ScmHandler;
import at.nonblocking.maven.nonsnapshot.exception.NonSnapshotPluginException;
import com.jcraft.jsch.Session;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * GIT implementation of {@link at.nonblocking.maven.nonsnapshot.ScmHandler} based on JGit.
 *
 * @author Juergen Kofler
 */
@Component(role = ScmHandler.class, hint = "GIT")
public class ScmHandlerGitImpl implements ScmHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ScmHandlerGitImpl.class);

    private File baseDir;
    private Git git;
    private CredentialsProvider credentialsProvider;
    private boolean doPush = true;

    static {
        SshSessionFactory.setInstance(new JschConfigSessionFactory() {
            @Override
            protected void configure(OpenSshConfig.Host hc, Session session) {
                session.setConfig("StrictHostKeyChecking", "no");
            }
        });
    }

    @Override
    public boolean isWorkingCopy(File path) {
        return this.git != null;
    }

    @Override
    public Date getLastCommitDate(File path) {

        try {
            LogCommand logCommand = this.git
                    .log()
                    .setMaxCount(100);

            String modulePath = PathUtil.relativePath(this.baseDir, path);
            if (!modulePath.isEmpty()) {
                logCommand.addPath(modulePath);
            }

            RevCommit lastCommit = logCommand.call().iterator().next();
            return new Date(lastCommit.getCommitTime() * 1000L);

        } catch (Exception e) {
            throw new NonSnapshotPluginException("Failed to determine last commit date!", e);
        }
    }

    @Override
    public boolean checkChangesSinceRevision(final File moduleDirectory, long sinceRevision, long workspaceRevision) {
        throw new RuntimeException("Operation checkChangesSinceRevision() not supported by the GIT handler");
    }

    @Override
    public boolean checkChangesSinceDate(File moduleDirectory, final Date sinceDate, final Date workspaceLastCommitDate) {
        if (this.git == null) {
            return false;
        }

        try {
            String modulePath = PathUtil.relativePath(this.baseDir, moduleDirectory);

            LogCommand logCommand = this.git
                    .log()
                    .setMaxCount(100);

            if (!modulePath.isEmpty()) {
                logCommand.addPath(modulePath);
            }

            for (RevCommit commit : logCommand.call()) {
                Date commitTime = new Date(commit.getCommitTime() * 1000L);
                if (commitTime.after(sinceDate)) {
                    if (!commit.getFullMessage().startsWith(NONSNAPSHOT_COMMIT_MESSAGE_PREFIX)) {
                        LOG.debug("Module folder {}: Change since last commit: rev{} @ {} ({})",
                                moduleDirectory.getAbsolutePath(), commit.getId(), commitTime, commit.getFullMessage());
                        return true;
                    }
                } else {
                    break;
                }
            }

        } catch (Exception e) {
            LOG.warn("Failed to check changes for path: {}" + moduleDirectory.getAbsolutePath(), e);
            return true;
        }

        return false;
    }

    @Override
    public long getCurrentRevisionId(File path) {
        throw new RuntimeException("Operation getCurrentRevisionId() not supported by the GIT handler");
    }

    @Override
    public void commitFiles(List<File> files, String commitMessage) {
        LOG.debug("Committing files: {}", files);

        try {
            for (File file : files) {
                String filePath = PathUtil.relativePath(this.baseDir, file);

                LOG.debug("Git: Adding file: {}", filePath);
                this.git
                        .add()
                        .addFilepattern(filePath)
                        .call();
            }

            LOG.debug("Git: Committing changes");
            this.git
                    .commit()
                    .setMessage(commitMessage)
                    .call();

            if (this.doPush) {
                LOG.debug("Git: Pushing changes");
                this.git
                        .push()
                        .setCredentialsProvider(this.credentialsProvider)
                        .call();
            }

        } catch (Exception e) {
            throw new NonSnapshotPluginException("Failed to commit files!", e);
        }
    }


    @Override
    public void init(File baseDir, String scmUser, String scmPassword, Properties properties) {
        this.baseDir = findGitRepo(baseDir);
        if (this.baseDir == null) {
            LOG.error("Project seems not be within a GIT repository!");
            return;
        }

        LOG.info("Using GIT repository: {}", this.baseDir.getAbsolutePath());

        try {
            FileRepository localRepo = new FileRepository(this.baseDir + "/.git");
            this.git = new Git(localRepo);
            if (scmPassword != null && !scmPassword.trim().isEmpty()) {
                this.credentialsProvider = new UsernamePasswordAndPassphraseCredentialProvider(scmUser, scmPassword);
            }
            if (properties != null && "false".equals(properties.getProperty("gitDoPush"))) {
                this.doPush = false;
                LOG.info("GIT push is disabled");
            }

        } catch (Exception e) {
            LOG.error("Project seems not be within a GIT repository!", e);
        }
    }

    private File findGitRepo(File baseDir) {
        File dir = baseDir;
        do {
            if (new File(dir, ".git").exists()) {
                return dir;
            }
            dir = dir.getParentFile();
        } while (dir != null);

        return null;
    }

    private static class UsernamePasswordAndPassphraseCredentialProvider extends CredentialsProvider {

        private String username;
        private String password;

        private UsernamePasswordAndPassphraseCredentialProvider(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public boolean isInteractive() {
            return false;
        }

        @Override
        public boolean supports(CredentialItem... items) {
            return true;
        }

        @Override
        public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
            for (CredentialItem i : items) {
                if (i instanceof CredentialItem.Username) {
                    ((CredentialItem.Username) i).setValue(username);
                    continue;
                }
                if (i instanceof CredentialItem.Password) {
                    ((CredentialItem.Password) i).setValue(this.password.toCharArray());
                    continue;
                }
                if (i instanceof CredentialItem.StringType) {
                    if (i.getPromptText().equals("Password: ") || i.getPromptText().startsWith("Passphrase ")) {
                        ((CredentialItem.StringType) i).setValue(this.password);
                        continue;
                    }
                }
                throw new UnsupportedCredentialItem(uri, i.getClass().getName() + ":" + i.getPromptText());
            }
            return true;
        }
    }

}
