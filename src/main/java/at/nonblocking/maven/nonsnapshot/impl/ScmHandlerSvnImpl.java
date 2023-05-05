/*
 * Copyright 2012-2019 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import at.nonblocking.maven.nonsnapshot.ScmHandler;
import at.nonblocking.maven.nonsnapshot.exception.NonSnapshotPluginException;

/**
 * SVN implementation of {@link ScmHandler} based on SvnKit.
 *
 * @author Juergen Kofler
 */
@Component(role = ScmHandler.class, hint = "SVN")
public class ScmHandlerSvnImpl implements ScmHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ScmHandlerSvnImpl.class);

    private SVNClientManager svnClientManager = SVNClientManager.newInstance();

    public ScmHandlerSvnImpl() {
    }

    @Override
    public boolean isWorkingCopy(File path) {
        return SVNWCUtil.isVersionedDirectory(toCanonicalPath(path));
    }

    @Override
    public boolean checkChangesSinceRevision(final File moduleDirectory, final long sinceRevision, final long workspaceRevision) {
        final Boolean[] changes = new Boolean[1];
        changes[0] = false;

        try {
            this.svnClientManager.getLogClient().doLog(new File[]{moduleDirectory},
                    SVNRevision.WORKING,
                    SVNRevision.create(sinceRevision + 1),
                    SVNRevision.create(workspaceRevision),
                    false, false,
                    100L,
                    svnLogEntry -> {
                        if (!svnLogEntry.getMessage().startsWith(NONSNAPSHOT_COMMIT_MESSAGE_PREFIX)) {
                            LOG.debug("Module folder {}: Change since last commit: rev{} @ {} ({})",
                                    moduleDirectory.getAbsolutePath(), svnLogEntry.getRevision(), svnLogEntry.getDate(), svnLogEntry.getMessage());
                            changes[0] = true;
                        }
                    });

        } catch (SVNException e) {
            LOG.warn("Failed to check changes for path: {}" + moduleDirectory.getAbsolutePath(), e);
            return true;
        }

        return changes[0];
    }

    @Override
    public boolean checkChangesSinceDate(final File moduleDirectory, final Date sinceDate, final Date workspaceLastCommitDate) {
        final Boolean[] changes = new Boolean[1];
        changes[0] = false;

        try {
            this.svnClientManager.getLogClient().doLog(new File[]{moduleDirectory},
                    SVNRevision.WORKING,
                    SVNRevision.create(sinceDate),
                    SVNRevision.create(workspaceLastCommitDate),
                    false, true,
                    100L,
                    svnLogEntry -> {
                        if (!svnLogEntry.getMessage().startsWith(NONSNAPSHOT_COMMIT_MESSAGE_PREFIX)) {
                            LOG.debug("Module folder {}: Change since last commit: rev{} @ {} ({})",
                                    moduleDirectory.getAbsolutePath(), svnLogEntry.getRevision(), svnLogEntry.getDate(), svnLogEntry.getMessage());
                            changes[0] = true;
                        }
                    });

        } catch (SVNException e) {
            LOG.warn("Failed to check changes for path: {}" + moduleDirectory.getAbsolutePath(), e);
            return true;
        }

        return changes[0];
    }

    @Override
    public Date getLastCommitDate(File path) {
        try {
            SVNInfo info = this.svnClientManager.getWCClient().doInfo(path, null);
            return info.getCommittedDate();
        } catch (SVNException e) {
            throw new NonSnapshotPluginException("Failed to obtain current revision number for path: " + path.getAbsolutePath(), e);
        }
    }

    @Override
    public long getCurrentRevisionId(File path) {
        try {
            SVNInfo info = this.svnClientManager.getWCClient().doInfo(path, null);
            return info.getRevision().getNumber();
        } catch (SVNException e) {
            throw new NonSnapshotPluginException("Failed to obtain current revision number for path: " + path.getAbsolutePath(), e);
        }
    }

    @Override
    public void commitFiles(List<File> files, String commitMessage) {
        LOG.debug("Committing files: {}", files);

        try {
            SVNCommitInfo info = this.svnClientManager.getCommitClient().doCommit(files.toArray(new File[0]), false, commitMessage,
                    null, null, false, false, SVNDepth.FILES);

            if (info.getErrorMessage() != null) {
                throw new NonSnapshotPluginException("Failed to commit files. Message: " + info.getErrorMessage().getMessage());
            }

            LOG.debug("Files committed. New revision: {}", info.getNewRevision());

        } catch (SVNException e) {
            throw new NonSnapshotPluginException("Failed to commit files!", e);
        }
    }

    @Override
    public void init(File localRepoPath, String scmUser, String scmPassword, Properties properties) {
        if (StringUtils.isEmpty(scmUser) || StringUtils.isEmpty(scmPassword)) {
            throw new NonSnapshotPluginException("Parameters 'scmUser' and 'scmPassword' are required!");
        }

        ISVNAuthenticationManager authManager = new BasicAuthenticationManager(scmUser, scmPassword);
        this.svnClientManager.setAuthenticationManager(authManager);
    }

    private File toCanonicalPath(File path) {
        try {
            return path.getCanonicalFile();
        } catch (IOException e) {
            throw new NonSnapshotPluginException(e.getMessage(), e);
        }
    }

}
