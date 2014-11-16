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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import at.nonblocking.maven.nonsnapshot.model.UpstreamMavenArtifact;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.nonblocking.maven.nonsnapshot.DependencyTreeProcessor;
import at.nonblocking.maven.nonsnapshot.model.MavenArtifact;
import at.nonblocking.maven.nonsnapshot.model.MavenModule;
import at.nonblocking.maven.nonsnapshot.model.MavenModuleDependency;

/**
 * Default {@link DependencyTreeProcessor} implementation.
 * 
 * @author Juergen Kofler
 */
@Component(role = DependencyTreeProcessor.class, hint = "default")
public class DependencyTreeProcessorDefaultImpl implements DependencyTreeProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(DependencyTreeProcessorDefaultImpl.class);
    
    @Override
    public List<MavenModule> buildDependencyTree(List<MavenModule> artifacts) {
        List<MavenModule> rootArtifacts = new ArrayList<>();

        for (MavenModule wsArtifact : artifacts) {
            if (wsArtifact.getParent() != null) {
                MavenModule parentWsArtifact = findArtifact(artifacts, wsArtifact.getParent().getGroupId(), wsArtifact.getParent().getArtifactId());
                if (parentWsArtifact != null) {
                    parentWsArtifact.getChildren().add(wsArtifact);
                    wsArtifact.setParent(parentWsArtifact);
                }
            } else {
                rootArtifacts.add(wsArtifact);
            }

            for (MavenModuleDependency dependency : wsArtifact.getDependencies()) {
                MavenModule dependencyWsArtifact = findArtifact(artifacts, dependency.getArtifact().getGroupId(), dependency.getArtifact()
                        .getArtifactId());
                if (dependencyWsArtifact != null) {
                    dependency.setArtifact(dependencyWsArtifact);
                }
            }
        }

        return rootArtifacts;
    }

    private MavenModule findArtifact(List<MavenModule> artifacts, String groupId, String artifactId) {
        for (MavenModule artifact : artifacts) {
            if (groupId.equals(artifact.getGroupId()) && artifactId.equals(artifact.getArtifactId())) {
                return artifact;
            }
        }

        return null;
    }

    @Override
    public boolean markAllArtifactsDirtyWithDirtyDependencies(List<MavenModule> artifacts) {
        boolean changes = false;
        
        for (MavenModule artifact : artifacts) {
            if (artifact.isDirty()) {
                continue;
            }
            
            if (artifact.getParent() != null && artifact.getParent() instanceof MavenModule) {
                MavenModule parentWsArtifact = (MavenModule) artifact.getParent();
                if (parentWsArtifact.isDirty()) {
                    LOG.debug("Marking artifact {}:{} dirty because parent is dirty.", artifact.getGroupId(), artifact.getArtifactId());
                    artifact.setDirty(true);
                    changes = true;
                    continue;
                }
            }
            
            for (MavenModuleDependency dependency : artifact.getDependencies()) {
                if (dependency.getArtifact() instanceof MavenModule) {
                    MavenModule dependencyWsArtifact = (MavenModule) dependency.getArtifact();
                    if (dependencyWsArtifact.isDirty()) {
                        LOG.debug("Marking artifact {}:{} dirty because dependency is dirty: {}:{}", 
                                new Object[] { artifact.getGroupId(), artifact.getArtifactId(), dependencyWsArtifact.getGroupId(), dependencyWsArtifact.getArtifactId() });
                        artifact.setDirty(true);
                        changes = true;
                        break;
                    }
                } else if (dependency.getArtifact() instanceof UpstreamMavenArtifact) {
                    UpstreamMavenArtifact upstreamDependency = (UpstreamMavenArtifact) dependency.getArtifact();
                    if (upstreamDependency.isDirty()) {
                        LOG.debug("Marking artifact {}:{} dirty because upstream dependency is dirty: {}:{}",
                            new Object[] { artifact.getGroupId(), artifact.getArtifactId(), upstreamDependency.getGroupId(), upstreamDependency.getArtifactId() });
                        artifact.setDirty(true);
                        changes = true;
                        break;
                    }
                }
            }
        }
        
        return changes;
    }

    @Override
    public void printMavenModuleTree(List<MavenModule> rootArtifacts, PrintStream printStream) {
        for (MavenModule rootArtifact : rootArtifacts) {
            printTree(rootArtifact, printStream, 0);
        }
    }

    private void printTree(MavenArtifact artifact, PrintStream printStream, int level) {
        printStream.print(StringUtils.leftPad(" ", level * 3));

        if (artifact instanceof MavenModule) {
            MavenModule wsArtifact = (MavenModule) artifact;

            printStream.print(wsArtifact.getGroupId() + ":" + wsArtifact.getArtifactId() + ":" + wsArtifact.getVersion());
            if (wsArtifact.isDirty()) {
                if (wsArtifact.getNewVersion() != null) {
                    printStream.println(" -> " + wsArtifact.getNewVersion());
                } else {
                    printStream.println(" -> (Dirty, but new version couldn't be determined!)");
                }
            } else{
                printStream.println(" ");   
            }

            for (MavenModule child : wsArtifact.getChildren()) {
                printTree(child, printStream, level + 1);
            }

        } else {
            printStream.println("(" + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion() + ")");
        }
    }
}
