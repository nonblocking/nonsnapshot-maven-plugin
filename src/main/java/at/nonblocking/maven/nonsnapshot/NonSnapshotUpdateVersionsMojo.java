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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import at.nonblocking.maven.nonsnapshot.model.MavenArtifact;
import at.nonblocking.maven.nonsnapshot.model.MavenModuleDependency;
import at.nonblocking.maven.nonsnapshot.model.UpstreamMavenArtifact;
import org.apache.maven.model.Model;
import org.apache.maven.plugins.annotations.Mojo;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.version.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.nonblocking.maven.nonsnapshot.exception.NonSnapshotPluginException;
import at.nonblocking.maven.nonsnapshot.model.MavenModule;

/**
 * Main Goal of this Plugin. <br/>
 * <br/>
 * Checks the version number and manipulates it if necessary (if changes were found). 
 * <br/>
 * Updates the version of upstream modules.
 * <br/>
 * Commits the POM files if deferPomCommit is false.
 * 
 * @author Juergen Kofler
 */
@Mojo(name = "updateVersions", aggregator = true)
public class NonSnapshotUpdateVersionsMojo extends NonSnapshotBaseMojo {

    private static Logger LOG = LoggerFactory.getLogger(NonSnapshotUpdateVersionsMojo.class);

    private static String LINE_SEPARATOR = System.getProperty("line.separator");

    private String timestamp;

    @Override
    protected void internalExecute() {
        setTimestamp();

        List<Model> mavenModels = getModuleTraverser().findAllModules(getMavenProject());

        List<MavenModule> mavenModules = new ArrayList<MavenModule>();

        for (Model model : mavenModels) {
            MavenModule artifact = getMavenPomHandler().readArtifact(model);
            mavenModules.add(artifact);
        }

        List<MavenModule> rootModules = getDependencyTreeProcessor().buildDependencyTree(mavenModules);

        markDirtyWhenRevisionChangedOrInvalidQualifier(mavenModules);

        if (getUpstreamGroupIds() != null) {
            updateUpstreamPlugins(mavenModules);
        }

        //Recursively mark artifacts dirty
        boolean changes = getDependencyTreeProcessor().markAllArtifactsDirtyWithDirtyDependencies(mavenModules);
        while (changes) {
            changes = getDependencyTreeProcessor().markAllArtifactsDirtyWithDirtyDependencies(mavenModules);
        }

        setNextRevisionOnDirtyArtifacts(mavenModules);

        dumpArtifactTreeToLog(rootModules);
        
        writeAndCommitArtifacts(mavenModules);
    }

    protected void writeAndCommitArtifacts(List<MavenModule> mavenModules) {
        List<File> pomsToCommit = new ArrayList<File>();

        for (MavenModule mavenModule : mavenModules) {
            if (mavenModule.isDirty() && mavenModule.getNewVersion() != null) {
                getMavenPomHandler().updateArtifact(mavenModule);
                LOG.debug("Add module to dirty registry list: {}", mavenModule.getPomFile().getAbsolutePath());
                pomsToCommit.add(mavenModule.getPomFile());
            }
        }

        if (pomsToCommit.size() > 0) {
            File dirtyModulesRegistryFile = getDirtyModulesRegistryFile();
            LOG.debug("Writing dirty modules registry to: {}", dirtyModulesRegistryFile.getAbsolutePath());
            writeDirtyModulesRegistry(pomsToCommit, dirtyModulesRegistryFile);

            if (!isDeferPomCommit()) {
                LOG.info("Committing {} POM files", pomsToCommit.size());
                getScmHandler().commitFiles(pomsToCommit, "Version of " + pomsToCommit.size() + " artifacts updated");
            } else {
                LOG.info("Deferring the POM commit. Execute nonsnapshot:commit to actually commit the changes.");
            }
        } else {
            LOG.info("Workspace is up-to-date. No versions updated.");
        }
    }

    private void markDirtyWhenRevisionChangedOrInvalidQualifier(List<MavenModule> mavenModules) {
        for (MavenModule mavenModule : mavenModules) {
            if (mavenModule.getVersion() == null) {
                LOG.info("No version found for artifact {}:{}. Assigning a new version.", mavenModule.getGroupId(), mavenModule.getArtifactId());
                mavenModule.setDirty(true);

            } else if (mavenModule.getVersion().startsWith("${")) {
                LOG.info("Version property found for artifact {}:{}. Assigning a new version.", mavenModule.getGroupId(), mavenModule.getArtifactId());
                mavenModule.setDirty(true);

            } else {
                String[] versionParts = mavenModule.getVersion().split("-");
                String qualifierString = null;
                if (versionParts.length > 1) {
                    qualifierString = versionParts[versionParts.length - 1];
                }

                if (qualifierString == null) {
                    LOG.info("Invalid qualifier string found for artifact {}:{}: {}. Assigning a new version.",
                            new Object[] { mavenModule.getGroupId(), mavenModule.getArtifactId(), mavenModule.getVersion() });
                    mavenModule.setDirty(true);

                } else if (qualifierString.equals("SNAPSHOT")) {
                    LOG.info("Snapshot version found for artifact {}:{}. Assigning a new version.", mavenModule.getGroupId(), mavenModule.getArtifactId());
                    mavenModule.setDirty(true);

                } else {
                    String currentRevision = getScmHandler().getRevisionId(mavenModule.getPomFile().getParentFile());
                    LOG.debug("Current project revision id: {}; revision id in the version qualifier: {}", currentRevision, qualifierString);

                    if (!currentRevision.equals(qualifierString)) {
                        LOG.info("Current project revision id is different from the revision id in the version qualifier of artifact {}:{}. Assigning a new version.",
                                mavenModule.getGroupId(), mavenModule.getArtifactId());
                        mavenModule.setDirty(true);

                    }
                }
            }
        }
    }

    private void updateUpstreamPlugins(List<MavenModule> mavenModules) {
        for (MavenModule mavenModule : mavenModules) {
            for (MavenModuleDependency moduleDependency : mavenModule.getDependencies()) {
                MavenArtifact dependency = moduleDependency.getArtifact();
                if (!(dependency instanceof MavenModule)
                    && getUpstreamGroupIds().contains(dependency.getGroupId())) {

                    LOG.debug("Upstream dependency found: {}:{}", dependency.getGroupId(), dependency.getArtifactId());

                    String latestVersion = determineLatestNonSnapshotVersionInRepo(dependency);
                    if (latestVersion == null || !latestVersion.equals(dependency.getVersion())) {
                        LOG.info("Found new upstream version for artifact {}: {}", dependency.getArtifactId(), latestVersion);

                        UpstreamMavenArtifact upstreamMavenArtifact =
                            new UpstreamMavenArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), latestVersion);
                        moduleDependency.setArtifact(upstreamMavenArtifact);
                    }

                }
            }
        }
    }

    private String determineLatestNonSnapshotVersionInRepo(MavenArtifact mavenArtifact) {
        String currentVersion = mavenArtifact.getVersion();
        if (currentVersion.contains("$")) {
            currentVersion = "1.0";
        } else if (currentVersion.contains("-")) {
            currentVersion = currentVersion.split("-")[0];
        }

        String versionQuery = mavenArtifact.getGroupId() + ":" + mavenArtifact.getArtifactId() + ":(" + currentVersion + ",)";
        Artifact aetherArtifact = new DefaultArtifact(versionQuery);

        VersionRangeRequest rangeRequest = new VersionRangeRequest();
        rangeRequest.setArtifact(aetherArtifact);
        rangeRequest.setRepositories(getRemoteRepositories());

        try {
            LOG.debug("Resolving versions for {}", versionQuery);
            VersionRangeResult result = getRepositorySystem().resolveVersionRange(getRepositorySystemSession(), rangeRequest);
            LOG.debug("Found versions for {}: {}", versionQuery, result);

            List<Version> versions = result.getVersions();
            Collections.reverse(versions);

            for (Version version : versions) {
                if (!version.toString().endsWith("-SNAPSHOT")) {
                    return version.toString();
                }
            }

            //No newer non-snapshot dependency found
            return currentVersion;

        } catch (VersionRangeResolutionException e) {
            if (!isDontFailOnUpstreamVersionResolution()) {
                throw new NonSnapshotPluginException("Failed to resolve latest upstream version for: " + versionQuery , e);
            } else {
                LOG.warn("Couldn't resolve latest upstream version for: " + versionQuery + ". Keeping current version " + currentVersion, e);
                return currentVersion;
            }
        }
    }

    private void setNextRevisionOnDirtyArtifacts(List<MavenModule> workspaceArtifacts) {
        for (MavenModule artifact : workspaceArtifacts) {
            File artifactPath = artifact.getPomFile().getParentFile();  
                    
            if (artifact.isDirty() && getScmHandler().isWorkingCopy(artifactPath)) {
                if (!isUseTimestampQualifier()) {
                    artifact.setNewVersion(getBaseVersion() + "-" + getScmHandler().getNextRevisionId(artifactPath));
                } else {
                    artifact.setNewVersion(getBaseVersion() + "-" + this.timestamp);
                }
            }
        }
    }

    private void writeDirtyModulesRegistry(List<File> pomFileList, File outputFile) {
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(outputFile, false))) {

            Path basePath = Paths.get(getMavenProject().getBasedir().getCanonicalPath());

            for (File pomFile : pomFileList) {
                Path modulePath = Paths.get(pomFile.getParentFile().getCanonicalPath());
                String relativeModuleDir = basePath.relativize(modulePath).toString();
                relativeModuleDir = relativeModuleDir.replaceAll("\\\\", "/");
                if (relativeModuleDir.isEmpty()) {
                    relativeModuleDir = ".";
                }
                writer.write(relativeModuleDir + LINE_SEPARATOR);
            }

        } catch (IOException e) {
            throw new NonSnapshotPluginException("Failed to write text file with POMs to commit!", e);
        }
    }

    private void setTimestamp() {
        this.timestamp = new SimpleDateFormat(getTimestampQualifierPattern()).format(new Date());
    }

    private void dumpArtifactTreeToLog(List<MavenModule> rootModules) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        getDependencyTreeProcessor().printMavenModuleTree(rootModules, new PrintStream(baos));
        LOG.info("\n" + baos.toString());
    }
}
