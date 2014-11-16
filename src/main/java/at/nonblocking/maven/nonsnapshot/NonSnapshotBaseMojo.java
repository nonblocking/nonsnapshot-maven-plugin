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
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
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
    private static final String DEFAULT_TIMESTAMP_QUALIFIER_PATTERN = "yyyyMMddHHmm";

    protected static final String DIRTY_MODULES_REGISTRY_FILE = "nonSnapshotDirtyModules.txt";

    
    /**
     * The SCM (Source Code Management System) type
     */
    @Parameter(defaultValue = "svn")
    private String scmType;
    
    /**
     * SCM Username
     */
    @Parameter
    private String scmUser;

    /**
     * SCM Password
     */
    @Parameter
    private String scmPassword;

    /**
     * Defer the actual commit until nonsnapshot:commit is called.
     */
    @Parameter(defaultValue="false")
    private boolean deferPomCommit;

    @Parameter(defaultValue = "false")
    private boolean dontFailOnUpstreamVersionResolution;

    /**
     * Don't let the build fail if the commit of the POM files fails.
     * <br/>
     * Useful if you run this plugin on a CI Server and don't want to let the build fail when
     * a concurrent POM update occurred.
     */
    @Parameter(defaultValue = "false")
    private boolean dontFailOnCommit;

    @Parameter(defaultValue = "${project}")
    private MavenProject mavenProject;

    @Parameter(required = true)
    private String baseVersion;

    @Parameter(defaultValue = "false")
    private boolean useTimestampQualifier;

    @Parameter(defaultValue = DEFAULT_TIMESTAMP_QUALIFIER_PATTERN)
    private String timestampQualifierPattern = DEFAULT_TIMESTAMP_QUALIFIER_PATTERN;

    @Parameter
    private List<String> upstreamGroupIds;

    /**
     * Generate a shell script to incrementally build only dirty artifacts (Maven > 3.2.1 only)
     */
    @Parameter(defaultValue = "false")
    private boolean generateScriptForIncrementalBuild;

    /**
     * Disable this plugin
     */
    @Parameter(defaultValue = "false")
    private boolean skip;

    @Component(role = MavenPomHandler.class, hint = "default")
    private MavenPomHandler mavenPomHandler;

    @Component(role = ModuleTraverser.class, hint = "default")
    private ModuleTraverser moduleTraverser;

    @Component(role = DependencyTreeProcessor.class, hint = "default")
    private DependencyTreeProcessor dependencyTreeProcessor;

    @Component
    private RepositorySystem repositorySystem;

    @Parameter(defaultValue = "${repositorySystemSession}")
    private RepositorySystemSession repositorySystemSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}")
    private List<RemoteRepository> remoteRepositories;

    private ScmHandler scmHandler;
    
    private PlexusContainer plexusContainer;

    public void execute() throws MojoExecutionException, MojoFailureException {
        StaticLoggerBinder.getSingleton().setLog(this.getLog());

        if (this.skip) {
            LOG.info("NonSnapshot Plugin has been disabled for this project.");
            return;
        }

        LOG.info("Executing NonSnapshot Plugin for project path: {}", this.mavenProject.getBasedir().getAbsolutePath());

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

    protected File getDirtyModulesRegistryFile() {
        return new File(this.mavenProject.getBasedir(), DIRTY_MODULES_REGISTRY_FILE);
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

    public void setModuleTraverser(ModuleTraverser moduleTraverser) {
        this.moduleTraverser = moduleTraverser;
    }

    public ModuleTraverser getModuleTraverser() {
        return moduleTraverser;
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

    public void setMavenProject(MavenProject mavenProject) {
        this.mavenProject = mavenProject;
    }

    public MavenProject getMavenProject() {
        return mavenProject;
    }

    public void setBaseVersion(String baseVersion) {
        this.baseVersion = baseVersion;
    }

    public String getBaseVersion() {
        return baseVersion;
    }

    public List<String> getUpstreamGroupIds() {
        return upstreamGroupIds;
    }

    public List<RemoteRepository> getRemoteRepositories() {
        return remoteRepositories;
    }

    public RepositorySystem getRepositorySystem() {
        return repositorySystem;
    }

    public RepositorySystemSession getRepositorySystemSession() {
        return repositorySystemSession;
    }

    public void setDontFailOnUpstreamVersionResolution(boolean dontFailOnUpstreamVersionResolution) {
        this.dontFailOnUpstreamVersionResolution = dontFailOnUpstreamVersionResolution;
    }

    public boolean isDontFailOnUpstreamVersionResolution() {
        return dontFailOnUpstreamVersionResolution;
    }

    public void setUseTimestampQualifier(boolean useTimestampQualifier) {
        this.useTimestampQualifier = useTimestampQualifier;
    }

    public boolean isUseTimestampQualifier() {
        return useTimestampQualifier;
    }

    public void setTimestampQualifierPattern(String timestampQualifierPattern) {
        this.timestampQualifierPattern = timestampQualifierPattern;
    }

    public String getTimestampQualifierPattern() {
        return timestampQualifierPattern;
    }
}

