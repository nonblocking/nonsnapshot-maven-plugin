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

import at.nonblocking.maven.nonsnapshot.exception.NonSnapshotDependencyResolverException;
import at.nonblocking.maven.nonsnapshot.model.MavenArtifact;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import java.util.List;

public interface UpstreamDependencyHandler {

  List<ProcessedUpstreamDependency> processDependencyList(List<String> upstreamDependencyStrings);

  ProcessedUpstreamDependency findMatch(MavenArtifact mavenArtifact, List<ProcessedUpstreamDependency> upstreamDependencies);

  /**
   * Try to find a newer version for given upstream dependency. Return null if no newer exits.
   *
   * @param mavenArtifact MavenArtifact
   * @param upstreamDependency UpstreamDependency
   * @param repositorySystem RepositorySystem
   * @param repositorySystemSession RepositorySystemSession
   * @param remoteRepositories List<RemoteRepository>
   * @return String
   * @throws NonSnapshotDependencyResolverException
   */
  String resolveLatestVersion(MavenArtifact mavenArtifact, ProcessedUpstreamDependency upstreamDependency,
                              RepositorySystem repositorySystem, RepositorySystemSession repositorySystemSession,
                              List<RemoteRepository> remoteRepositories) throws NonSnapshotDependencyResolverException;

}
