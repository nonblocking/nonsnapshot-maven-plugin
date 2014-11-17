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
    public List<MavenModule> buildDependencyTree(List<MavenModule> mavenModules) {
        List<MavenModule> rootArtifacts = new ArrayList<>();

        for (MavenModule mavenModule : mavenModules) {
            if (mavenModule.getParent() != null) {
                MavenModule parentModule = findArtifact(mavenModules, mavenModule.getParent().getGroupId(), mavenModule.getParent().getArtifactId());
                if (parentModule != null) {
                    parentModule.getChildren().add(mavenModule);
                    mavenModule.setParent(parentModule);
                }
            } else {
                rootArtifacts.add(mavenModule);
            }

            for (MavenModuleDependency dependency : mavenModule.getDependencies()) {
                MavenModule dependencyModule = findArtifact(mavenModules, dependency.getArtifact().getGroupId(), dependency.getArtifact()
                        .getArtifactId());
                if (dependencyModule != null) {
                    dependency.setArtifact(dependencyModule);
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
    public boolean markAllArtifactsDirtyWithDirtyDependencies(List<MavenModule> mavenModules) {
        boolean changes = false;
        
        for (MavenModule mavenModule : mavenModules) {
            if (mavenModule.isDirty()) {
                continue;
            }
            
            if (mavenModule.getParent() != null && mavenModule.getParent() instanceof MavenModule) {
                MavenModule parentModule = (MavenModule) mavenModule.getParent();
                if (parentModule.isDirty()) {
                    LOG.debug("Marking module {}:{} dirty because parent is dirty.", mavenModule.getGroupId(), mavenModule.getArtifactId());
                    mavenModule.setDirty(true);
                    changes = true;
                    continue;
                }
            }
            
            for (MavenModuleDependency dependency : mavenModule.getDependencies()) {
                if (dependency.getArtifact() instanceof MavenModule) {
                    MavenModule parentModule = (MavenModule) dependency.getArtifact();
                    if (parentModule.isDirty()) {
                        LOG.debug("Marking module {}:{} dirty because dependency is dirty: {}:{}",
                            new Object[]{mavenModule.getGroupId(), mavenModule.getArtifactId(), parentModule.getGroupId(), parentModule.getArtifactId()});
                        mavenModule.setDirty(true);
                        changes = true;
                        break;
                    }
                } else if (dependency.getArtifact() instanceof UpstreamMavenArtifact) {
                    UpstreamMavenArtifact upstreamDependency = (UpstreamMavenArtifact) dependency.getArtifact();
                    if (upstreamDependency.isDirty()) {
                        LOG.debug("Marking module {}:{} dirty because upstream dependency is dirty: {}:{}",
                            new Object[] { mavenModule.getGroupId(), mavenModule.getArtifactId(), upstreamDependency.getGroupId(), upstreamDependency.getArtifactId() });
                        mavenModule.setDirty(true);
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
            MavenModule module = (MavenModule) artifact;

            printStream.print(module.getGroupId() + ":" + module.getArtifactId() + ":" + module.getVersion());
            if (module.isDirty()) {
                if (module.getNewVersion() != null) {
                    printStream.println(" -> " + module.getNewVersion());
                } else {
                    printStream.println(" -> (Dirty, but new version couldn't be determined!)");
                }
            } else{
                printStream.println(" ");   
            }

            for (MavenModule child : module.getChildren()) {
                printTree(child, printStream, level + 1);
            }

        } else {
            printStream.println("(" + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion() + ")");
        }
    }
}
