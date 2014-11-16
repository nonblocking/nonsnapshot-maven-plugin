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

import java.util.List;

import org.apache.maven.plugins.annotations.Mojo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.nonblocking.maven.nonsnapshot.model.MavenModule;

/**
 * Test Plugin Goal which not actually updates the POMs.
 * 
 * @author Juergen Kofler
 */
@Mojo(name = "pretend", aggregator = true)
public class NonSnapshotPretendMojo extends NonSnapshotUpdateVersionsMojo {

    private static Logger LOG = LoggerFactory.getLogger(NonSnapshotPretendMojo.class);

    @Override
    protected void writeAndCommitArtifacts(List<MavenModule> workspaceArtifacts) {
       int dirtyCount = 0;
       for (MavenModule artifact : workspaceArtifacts) {
           if (artifact.isDirty()) {
               dirtyCount ++;
           }
       }
        
       LOG.info("Artifacts in Workspace: {}, thereof about to be updated: {}", workspaceArtifacts.size(), dirtyCount);
       LOG.info("NonSnapshot Plugin is in pretend mode. Doing nothing.");
    }
  
}
