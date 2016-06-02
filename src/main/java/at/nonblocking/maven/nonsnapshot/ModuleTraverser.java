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

import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;

import java.util.List;

/**
 * Traverses all nested Maven modules
 *
 * @author Juergen Kofler
 */
public interface ModuleTraverser {

  /**
   * Find (recursively) all Maven modules
   *
   * @param baseProject MavenProject
   * @param activeProfiles List&lt;Profile&gt;
   * @return List&lt;Model&gt;
   */
  List<Model> findAllModules(MavenProject baseProject, List<Profile> activeProfiles);

}
