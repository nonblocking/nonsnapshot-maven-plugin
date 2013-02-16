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
package at.nonblocking.maven.nonsnapshot;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

import at.nonblocking.maven.nonsnapshot.exception.NonSnapshotPluginException;

/**
 * Base class for NonSnapshot Plugin Mojos.
 * <br/>
 * Does all the parameter handling.
 * 
 * @author Juergen Kofler
 */
abstract class NonSnapshotBaseMojo extends AbstractMojo implements Contextualizable {

    private static final Logger LOG = LoggerFactory.getLogger(NonSnapshotBaseMojo.class);

    protected static final String POMS_TO_COMMIT_TEXT_FILE = "nonSnapshotPomsToCommit.txt";
    
    /**
     * The SCM (Source Code Management System) type
     * 
     * @parameter default-value="svn"
     */
    private String scmType;
    
    /**
     * SCM Username
     * 
     * @parameter
     */
    private String scmUser;

    /**
     * SCM Password
     * 
     * @parameter
     */
    private String scmPassword;

    /**
     * Defer the actual commit until nonsnapshot:commit is called.
     * 
     * @parameter default-value="false"
     */
    private boolean deferPomCommit;
    
    /**
     * Don't let the build fail if the commit of the POM files fails.
     * <br/>
     * Useful if you run this plugin on a CI Server and don't want to let the build fail when
     * a concurrent POM update occurred.
     * 
     * @parameter default-value="false"
     */
    private boolean dontFailOnCommit;
    
    /**
     * The workspace base directory
     * 
     * @parameter
     * @required
     */
    private File workspaceDir;

    /**
     * Folders in the workspace directory to exclude
     * 
     * @parameter
     */
    private List<String> excludeFolders;

    /**
     * Base versions for workspace artifacts. <br/>
     * E.g.:
     * 
     * <pre>
     *   &lt;configuration&gt;
     *     &lt;baseVersions&gt;
     *       &lt;baseVersion&gt;
     *         &lt;groupId&gt;at.nonblocking&lt;/groupId&gt;
     *         &lt;artifactId&gt;test&lt;/artifactId&gt;
     *         &lt;version&gt;1.0.0&lt;/version&gt;
     *       &lt;/baseVersion&gt;
     *     &lt;/baseVersions&gt;
     *   &lt;/configuration&gt;
     * </pre>
     * 
     * @parameter
     * @required
     */
    private List<BaseVersion> baseVersions;

    /**
     * When to update dependency versions. 
     * <br/>
     * E.g. if the strategy is SAME_MAJOR and an artifact from the dependency list is in the workspace but has another
     * major version, the dependency version isn't updated.
     * 
     * @parameter default-value="ALWAYS"
     */
    private DEPENDENCY_UPDATE_STRATEGY dependencyUpdateStrategy;

    /**
     * Disable this plugin
     * 
     * @parameter default-value="false"
     */
    private boolean skip;

    /** 
     * @component role="at.nonblocking.maven.nonsnapshot.MavenPomHandler" role-hint="default" 
     */
    private MavenPomHandler mavenPomHandler;

    /** 
     * @component role="at.nonblocking.maven.nonsnapshot.WorkspaceTraverser" role-hint="default" 
     */
    private WorkspaceTraverser workspaceTraverser;

    /** 
     * @component role="at.nonblocking.maven.nonsnapshot.DependencyTreeProcessor" role-hint="default" 
     */
    private DependencyTreeProcessor dependencyTreeProcessor;

    private ScmHandler scmHandler;
    
    private PlexusContainer plexusContainer;

    public void execute() throws MojoExecutionException, MojoFailureException {
        StaticLoggerBinder.getSingleton().setLog(this.getLog());

        if (this.skip) {
            LOG.info("NonSnapshot Plugin has been disabled for this project.");
            return;
        }

        LOG.info("Executing NonSnapshot Plugin for project path: {}", this.workspaceDir.getAbsoluteFile());

        postProcessParameters();

        internalExecute();
    }

    protected abstract void internalExecute();

    private void postProcessParameters() {
        if (this.scmHandler == null) {
            LOG.debug("Lookup for ScmHandler implementation of type: {}", this.scmType);
            
            try {
                this.scmHandler = this.plexusContainer.lookup(ScmHandler.class, this.scmType.toLowerCase());
            } catch (ComponentLookupException e) {
                throw new NonSnapshotPluginException("Unable to instantiate ScmHandler class for type: " + this.scmType, e);
            }
            
            if (this.scmHandler == null) {
                throw new NonSnapshotPluginException("Unable to instantiate ScmHandler class for type: " + this.scmType);
            }
            
            LOG.debug("Found ScmHandler: {}", this.scmHandler.getClass());
        }

        this.scmHandler.setCredentials(this.scmUser, this.scmPassword);
    }
    
    @Override
    public void contextualize(Context context) throws ContextException {  
        this.plexusContainer = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    public void setScmUser(String scmUser) {
        this.scmUser = scmUser;
    }

    public void setScmPassword(String scmPassword) {
        this.scmPassword = scmPassword;
    }

    public void setMavenPomHandler(MavenPomHandler mavenPomHandler) {
        this.mavenPomHandler = mavenPomHandler;
    }

    public void setDeferPomCommit(boolean deferPomCommit) {
        this.deferPomCommit = deferPomCommit;
    }

    public void setWorkspaceDir(File workspaceDir) {
        this.workspaceDir = workspaceDir;
    }

    public void setExcludeFolders(List<String> excludeFolders) {
        this.excludeFolders = excludeFolders;
    }

    public void setBaseVersions(List<BaseVersion> baseVersions) {
        this.baseVersions = baseVersions;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public ScmHandler getScmHandler() {
        return scmHandler;
    }

    public void setScmHandler(ScmHandler scmHandler) {
        this.scmHandler = scmHandler;
    }

    public MavenPomHandler getMavenPomHandler() {
        return mavenPomHandler;
    }

    public DependencyTreeProcessor getDependencyTreeProcessor() {
        return dependencyTreeProcessor;
    }

    public void setDependencyTreeProcessor(DependencyTreeProcessor dependencyTreeProcessor) {
        this.dependencyTreeProcessor = dependencyTreeProcessor;
    }

    public void setWorkspaceTraverser(WorkspaceTraverser workspaceTraverser) {
        this.workspaceTraverser = workspaceTraverser;
    }

    public boolean isDeferPomCommit() {
        return deferPomCommit;
    }

    public boolean isDontFailOnCommit() {
        return dontFailOnCommit;
    }

    public void setDontFailOnCommit(boolean dontFailOnCommit) {
        this.dontFailOnCommit = dontFailOnCommit;
    }

    public List<BaseVersion> getBaseVersions() {
        return baseVersions;
    }

    public void setDependencyUpdateStrategy(DEPENDENCY_UPDATE_STRATEGY dependencyUpdateStrategy) {
        this.dependencyUpdateStrategy = dependencyUpdateStrategy;
    }

    public DEPENDENCY_UPDATE_STRATEGY getDependencyUpdateStrategy() {
        return dependencyUpdateStrategy;
    }

    public File getWorkspaceDir() {
        return workspaceDir;
    }

    public WorkspaceTraverser getWorkspaceTraverser() {
        return workspaceTraverser;
    }

    public List<String> getExcludeFolders() {
        return excludeFolders;
    }

}
