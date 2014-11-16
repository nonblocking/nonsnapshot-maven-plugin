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

import java.io.File;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import at.nonblocking.maven.nonsnapshot.ScmHandler;
import at.nonblocking.maven.nonsnapshot.exception.NonSnapshotPluginException;

/**
 * SVN implementation for {@link ScmHandler} based on SvnKit.
 * 
 * @author Juergen Kofler
 */
@Component(role = ScmHandler.class, hint = "svn")
public class ScmHandlerSvnImpl implements ScmHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ScmHandlerSvnImpl.class);

    private SVNClientManager svnClientManager = SVNClientManager.newInstance();

    private String cachedNextRevision;

    public ScmHandlerSvnImpl() {
        // Avoid a bad_record_mac error when using SSL with Java 6
        // See http://issues.tmatesoft.com/issue/SVNKIT-176?projectKey=SVNKIT&query=bad_record_mac
        System.setProperty("svnkit.http.sslProtocols", "SSLv3");
    }

    @Override
    public boolean isWorkingCopy(File path) {
        return SVNWCUtil.isVersionedDirectory(path);
    }

    @Override
    public String getRevisionId(File path) {
        LOG.debug("Try to obtain revision for path: {}", path.getAbsolutePath());

        try {
            SVNInfo info = this.svnClientManager.getWCClient().doInfo(path, null);
            return String.valueOf(info.getCommittedRevision().getNumber());
        } catch (SVNException e) {
            throw new NonSnapshotPluginException("Failed to obtain SVN revision for path: " + path.getAbsolutePath(), e);
        }
    }

    @Override
    public String getNextRevisionId(File path) {
        if (this.cachedNextRevision == null) {
            try {
                SVNInfo info = this.svnClientManager.getWCClient().doInfo(path, null);

                LOG.debug("Try to obtain next revision of repository: {}", info.getRepositoryRootURL());

                SVNRepository repository = this.svnClientManager.createRepository(info.getRepositoryRootURL(), false);

                this.cachedNextRevision = String.valueOf(repository.getLatestRevision() + 1);
            } catch (SVNException e) {
                throw new NonSnapshotPluginException("Failed to obtain next revision number for path: " + path.getAbsolutePath(), e);
            }
        }

        return this.cachedNextRevision;
    }

    @Override
    public void commitFiles(List<File> files, String commitMessage) {
        LOG.debug("Commiting files: {}", files);

        this.cachedNextRevision = null;

        try {
            SVNCommitInfo info = this.svnClientManager.getCommitClient().doCommit(files.toArray(new File[files.size()]), false, commitMessage,
                    null, null, false, false, SVNDepth.FILES);

            if (info.getErrorMessage() != null) {
                throw new NonSnapshotPluginException("Failed to commit files. Message: " + info.getErrorMessage().getMessage());
            }

            LOG.debug("Files commited. New revision: {}", info.getNewRevision());

        } catch (SVNException e) {
            throw new NonSnapshotPluginException("Failed to commit files!", e);
        }
    }

    @Override
    public void setCredentials(String scmUser, String scmPassword) {
        if (StringUtils.isEmpty(scmUser) || StringUtils.isEmpty(scmPassword)) {
            throw new NonSnapshotPluginException("Parameters 'scmUser' and 'scmPassword' are required!");
        }

        ISVNAuthenticationManager authManager = new BasicAuthenticationManager(scmUser, scmPassword);
        this.svnClientManager.setAuthenticationManager(authManager);
    }

}
