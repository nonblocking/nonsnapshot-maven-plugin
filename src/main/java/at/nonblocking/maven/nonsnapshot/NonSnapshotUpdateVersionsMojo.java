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
import java.text.ParseException;
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

    List<MavenModule> mavenModules = new ArrayList<>();

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
    List<File> pomsToCommit = new ArrayList<>();

    for (MavenModule mavenModule : mavenModules) {
      if (mavenModule.isDirty() && mavenModule.getNewVersion() != null) {
        getMavenPomHandler().updateArtifact(mavenModule);
        LOG.debug("Add module to dirty registry list: {}", mavenModule.getPomFile().getAbsolutePath());
        pomsToCommit.add(mavenModule.getPomFile());
      }
    }

    if (pomsToCommit.size() > 0) {
      writeDirtyModulesRegistry(pomsToCommit);
      if (isGenerateIncrementalBuildScripts()) {
        generateIncrementalBuildScripts(pomsToCommit);
      }

      if (!isDeferPomCommit()) {
        LOG.info("Committing {} POM files", pomsToCommit.size());
        getScmHandler().commitFiles(pomsToCommit, ScmHandler.NONSNAPSHOT_COMMIT_MESSAGE_PREFIX + " Version of " + pomsToCommit.size() + " artifacts updated");
      } else {
        LOG.info("Deferring the POM commit. Execute nonsnapshot:commit to actually commit the changes.");
      }
    } else {
      LOG.info("Modules are up-to-date. No versions updated.");
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
              new Object[]{mavenModule.getGroupId(), mavenModule.getArtifactId(), mavenModule.getVersion()});
          mavenModule.setDirty(true);

        } else if (qualifierString.equals("SNAPSHOT")) {
          LOG.info("Snapshot version found for artifact {}:{}. Assigning a new version.", mavenModule.getGroupId(), mavenModule.getArtifactId());
          mavenModule.setDirty(true);

        } else {
          if (getScmType() == SCM_TYPE.SVN && isUseSvnRevisionQualifier()) {
            boolean changes = getScmHandler().checkChangesSinceRevision(mavenModule.getPomFile().getParentFile(), qualifierString);
            if (changes) {
              LOG.info("Module {}:{}: Revision nr is different from the revision nr in the version qualifier. Assigning a new version.", mavenModule.getGroupId(), mavenModule.getArtifactId());
              mavenModule.setDirty(true);
            }
          } else {

            //Default: compare timestamps

            boolean changes;

            try {
              Date versionTimestamp = new SimpleDateFormat(getTimestampQualifierPattern()).parse(qualifierString);
              changes = getScmHandler().checkChangesSinceDate(mavenModule.getPomFile().getParentFile(), versionTimestamp);
            } catch (ParseException e) {
              LOG.debug("Module {}:{}: Invalid timestamp qualifier: {}",
                  new Object[]{mavenModule.getGroupId(), mavenModule.getArtifactId(), qualifierString});
              changes = true;
            }

            if (changes) {
              LOG.info("Module {}:{}: Last committed timestamp is different from the timestamp in the version qualifier. Assigning a new version.", mavenModule.getGroupId(), mavenModule.getArtifactId());
              mavenModule.setDirty(true);
            }
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
            LOG.info("Found new upstream version for module {}:{}: {}", new Object[]{dependency.getGroupId(), dependency.getArtifactId(), latestVersion});

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
        throw new NonSnapshotPluginException("Failed to resolve latest upstream version for: " + versionQuery, e);
      } else {
        LOG.warn("Couldn't resolve latest upstream version for: " + versionQuery + ". Keeping current version " + currentVersion, e);
        return currentVersion;
      }
    }
  }

  private void setNextRevisionOnDirtyArtifacts(List<MavenModule> mavenModules) {
    for (MavenModule mavenModule : mavenModules) {
      File artifactPath = mavenModule.getPomFile().getParentFile();

      if (mavenModule.isDirty() && getScmHandler().isWorkingCopy(artifactPath)) {
        if (isUseSvnRevisionQualifier()) {
          mavenModule.setNewVersion(getBaseVersion() + "-" + getScmHandler().getNextRevisionId(artifactPath));
        } else {
          mavenModule.setNewVersion(getBaseVersion() + "-" + this.timestamp);
        }
      }
    }
  }

  private void writeDirtyModulesRegistry(List<File> pomFileList) {
    File dirtyModulesRegistryFile = getDirtyModulesRegistryFile();
    LOG.info("Writing dirty modules registry to: {}", dirtyModulesRegistryFile.getAbsolutePath());

    try (PrintWriter writer = new PrintWriter(new FileOutputStream(dirtyModulesRegistryFile, false))) {

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

  private void generateIncrementalBuildScripts(List<File> pomFileList) {
    StringBuilder projectPaths = new StringBuilder();

    try {
      Path basePath = Paths.get(getMavenProject().getBasedir().getCanonicalPath());
      for (File pomFile : pomFileList) {
        Path modulePath = Paths.get(pomFile.getParentFile().getCanonicalPath());
        String relativeModuleDir = basePath.relativize(modulePath).toString();
        relativeModuleDir = relativeModuleDir.replaceAll("\\\\", "/");
        if (projectPaths.length() != 0) {
          relativeModuleDir = "," + relativeModuleDir;
        }
        projectPaths.append(relativeModuleDir);
      }
    } catch (IOException e) {
      throw new NonSnapshotPluginException("Failed to write incremental build scripts!", e);
    }

    if (isWindows()) {
      File batFile = new File(getMavenProject().getBasedir(), "nonsnapshotBuildIncremental.bat");
      LOG.info("Writing windows batch script for incremental build to: {}", batFile.getAbsolutePath());

      try (PrintWriter writer = new PrintWriter(batFile)) {
        writer.write("REM Incremental build script generated by nonsnapshot-maven-plugin\n");
        writer.write("REM To install all modified modules call:\n");
        writer.write("REM nonsnapshotBuildIncremental.bat install\n\n");
        writer.write("mvn --projects " + projectPaths.toString() + " %*");
        writer.close();

      } catch (IOException e) {
        LOG.error("Failed to write windows batch script for incremental build to: {}", batFile.getAbsolutePath());
      }
    } else {

      File shellFile = new File(getMavenProject().getBasedir(), "nonsnapshotBuildIncremental.sh");
      LOG.info("Writing unix shell script for incremental build to: {}", shellFile.getAbsolutePath());

      try (PrintWriter writer = new PrintWriter(shellFile)) {
        writer.write("#!/bin/sh\n");
        writer.write("# Incremental build script generated by nonsnapshot-maven-plugin\n");
        writer.write("# To install all modified modules call:\n");
        writer.write("# ./nonsnapshotBuildIncremental.sh install\n\n");
        writer.write("mvn --projects " + projectPaths.toString() + " $@");
        writer.close();

        Runtime.getRuntime().exec("chmod u+x " + shellFile.getAbsolutePath());

      } catch (IOException e) {
        LOG.error("Failed to write windows batch script for incremental build to: {}", shellFile.getAbsolutePath());
      }
    }
  }

  private boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
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
