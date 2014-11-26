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

import at.nonblocking.maven.nonsnapshot.ProcessedUpstreamDependency;
import at.nonblocking.maven.nonsnapshot.UpstreamDependencyHandler;
import at.nonblocking.maven.nonsnapshot.exception.NonSnapshotDependencyResolverException;
import at.nonblocking.maven.nonsnapshot.exception.NonSnapshotPluginException;
import at.nonblocking.maven.nonsnapshot.model.MavenArtifact;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.version.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Default implementation of UpstreamDependencyHandler
 *
 * @author Juergen Kofler
 */
@Component(role = UpstreamDependencyHandler.class, hint = "default")
public class UpstreamDependencyHandlerDefaultImpl implements UpstreamDependencyHandler {

  private static final Logger LOG = LoggerFactory.getLogger(UpstreamDependencyHandlerDefaultImpl.class);

  @Override
  public List<ProcessedUpstreamDependency> processDependencyList(List<String> upstreamDependencyStrings) {
    if (upstreamDependencyStrings == null || upstreamDependencyStrings.isEmpty()) {
      return null;
    }

    List<ProcessedUpstreamDependency> upstreamDependencies = new ArrayList<>(upstreamDependencyStrings.size());

    for (String upstreamDependencyString : upstreamDependencyStrings) {
      if (upstreamDependencyString.trim().isEmpty()) {
        throw new NonSnapshotPluginException("Illegal upstreamDependency: " + upstreamDependencyString);
      }

      Pattern groupPattern;
      Pattern artifactPattern = null;
      Integer versionMajor = null;
      Integer versionMinor = null;
      Integer versionIncrement = null;

      String[] parts = upstreamDependencyString.split(":");
      groupPattern = createPattern(parts[0]);
      if (parts.length > 1) {
        artifactPattern = createPattern(parts[1]);
      }
      if (parts.length > 2) {
        String version = parts[2].trim();
        if (!version.isEmpty() && !"LATEST".equalsIgnoreCase(version)) {
          String[] versionParts = version.split("\\.");
          try {
            versionMajor = Integer.parseInt(versionParts[0]);
            if (versionParts.length > 1) {
              versionMinor = Integer.parseInt(versionParts[1]);
            }
            if (versionParts.length > 2) {
              versionIncrement = Integer.parseInt(versionParts[2]);
            }
          } catch (NumberFormatException e) {
            throw new NonSnapshotPluginException("Illegal upstreamDependency: " + upstreamDependencyString);
          }
        }
      }

      ProcessedUpstreamDependency upstreamDependency = new ProcessedUpstreamDependency(groupPattern, artifactPattern, versionMajor, versionMinor, versionIncrement);
      LOG.debug("Upstream dependency: {}", upstreamDependency);
      upstreamDependencies.add(upstreamDependency);
    }

    return upstreamDependencies;
  }

  private Pattern createPattern(String regex) {
    regex = regex.replaceAll("\\.", "\\\\.");
    regex = regex.replaceAll("\\*", ".*");
    return Pattern.compile(regex);
  }

  @Override
  public ProcessedUpstreamDependency findMatch(MavenArtifact mavenArtifact, List<ProcessedUpstreamDependency> upstreamDependencies) {
    if (upstreamDependencies == null) {
      return null;
    }

    for (ProcessedUpstreamDependency upstreamDependency : upstreamDependencies) {
      if (upstreamDependency.getGroupPattern().matcher(mavenArtifact.getGroupId()).matches()
          && upstreamDependency.getArtifactPattern().matcher(mavenArtifact.getArtifactId()).matches()) {
        return upstreamDependency;
      }
    }

    return null;
  }

  @Override
  public String resolveLatestVersion(MavenArtifact mavenArtifact, ProcessedUpstreamDependency upstreamDependency,
                                     RepositorySystem repositorySystem, RepositorySystemSession repositorySystemSession,
                                     List<RemoteRepository> remoteRepositories) throws NonSnapshotDependencyResolverException {

    String currentVersion = mavenArtifact.getVersion();
    if (currentVersion.contains("$")) {
      currentVersion = "0.0.0";
    } else if (currentVersion.endsWith("-SNAPSHOT")) {
      currentVersion = currentVersion.split("-")[0];
    }

    String versionPrefix;
    String versionQuery;

    if (upstreamDependency.getVersionIncrement() != null) {
      versionPrefix = upstreamDependency.getVersionMajor() + "." + upstreamDependency.getVersionMinor() + "." + upstreamDependency.getVersionIncrement();
      String nextIncrement = upstreamDependency.getVersionMajor() + "." + upstreamDependency.getVersionMinor() + "." + (upstreamDependency.getVersionIncrement() + 1);
      versionQuery = mavenArtifact.getGroupId() + ":" + mavenArtifact.getArtifactId() + ":(" + currentVersion + "," + nextIncrement + ")";
    } else if (upstreamDependency.getVersionMinor() != null) {
      versionPrefix = upstreamDependency.getVersionMajor() + "." + upstreamDependency.getVersionMinor();
      String nextMinor = upstreamDependency.getVersionMajor() + "." + (upstreamDependency.getVersionMinor() + 1) + ".0";
      versionQuery = mavenArtifact.getGroupId() + ":" + mavenArtifact.getArtifactId() + ":(" + currentVersion + "," + nextMinor + ")";
    } else if (upstreamDependency.getVersionMajor() != null) {
      versionPrefix = String.valueOf(upstreamDependency.getVersionMajor());
      String nextMajor = (upstreamDependency.getVersionMajor() + 1) + ".0.0";
      versionQuery = mavenArtifact.getGroupId() + ":" + mavenArtifact.getArtifactId() + ":(" + currentVersion + "," + nextMajor + ")";
    } else {
      versionPrefix = "";
      versionQuery = mavenArtifact.getGroupId() + ":" + mavenArtifact.getArtifactId() + ":(" + currentVersion + ",)";
    }

    Artifact aetherArtifact = new DefaultArtifact(versionQuery);

    VersionRangeRequest rangeRequest = new VersionRangeRequest();
    rangeRequest.setArtifact(aetherArtifact);
    rangeRequest.setRepositories(remoteRepositories);

    try {
      LOG.debug("Resolving versions for {}", versionQuery);
      VersionRangeResult result = repositorySystem.resolveVersionRange(repositorySystemSession, rangeRequest);
      LOG.debug("Found versions for {}: {}", versionQuery, result);

      List<Version> versions = result.getVersions();
      Collections.reverse(versions);

      for (Version version : versions) {
        String versionStr = version.toString();
        if (!versionStr.endsWith("-SNAPSHOT") && versionStr.startsWith(versionPrefix)) {
          return versionStr;
        }
      }

      return null;

    } catch (VersionRangeResolutionException e) {
      throw new NonSnapshotDependencyResolverException("Couldn't resolve latest upstream version for: " + versionQuery + ". Keeping current version " + currentVersion, e);
    }
  }


}
