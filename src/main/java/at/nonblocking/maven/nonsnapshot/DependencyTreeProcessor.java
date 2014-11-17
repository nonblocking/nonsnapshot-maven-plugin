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

import java.io.PrintStream;
import java.util.List;

import at.nonblocking.maven.nonsnapshot.model.MavenModule;

/**
 * Methods to process the Maven depedency tree found in the Workspace.
 *
 * @author Juergen Kofler
 */
public interface DependencyTreeProcessor {

  /**
   * Build an dependency tree from given artifact list.
   * <br/><br/>
   * Replace all parent, children and dependency references through references of actual
   * WorkspaceArtifacts from the given list.
   * <br/><br/>
   * Returns a list of root artifacts, with parent == null.
   *
   * @param artifacts List<MavenModule - List of all workspace artifacts
   * @return List<WorkspaceArtifact>
   */
  List<MavenModule> buildDependencyTree(List<MavenModule> artifacts);

  /**
   * Mark all artifacts with dirty dependencies dirty.
   *
   * @param artifacts List<MavenModule>
   * @return boolean True if any new artifacts have been marked dirty
   */
  boolean markAllArtifactsDirtyWithDirtyDependencies(List<MavenModule> artifacts);

  /**
   * Print the artifact tree found in the workspace for debug purposes.
   *
   * @param rootArtifacts List<MavenModule>
   * @param printStream   PrintStream
   */
  void printMavenModuleTree(List<MavenModule> rootArtifacts, PrintStream printStream);

}
