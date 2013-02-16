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

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.nonblocking.maven.nonsnapshot.BaseVersion;
import at.nonblocking.maven.nonsnapshot.DependencyTreeProcessor;
import at.nonblocking.maven.nonsnapshot.model.MavenArtifact;
import at.nonblocking.maven.nonsnapshot.model.WorkspaceArtifact;
import at.nonblocking.maven.nonsnapshot.model.WorkspaceArtifactDependency;

/**
 * Default {@link DependencyTreeProcessor} implementation.
 * 
 * @author Juergen Kofler
 */
@Component(role = DependencyTreeProcessor.class, hint = "default")
public class DependencyTreeProcessorDefaultImpl implements DependencyTreeProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(DependencyTreeProcessorDefaultImpl.class);
    
    @Override
    public List<WorkspaceArtifact> buildDependencyTree(List<WorkspaceArtifact> artifacts) {
        List<WorkspaceArtifact> rootArtifacts = new ArrayList<WorkspaceArtifact>();

        for (WorkspaceArtifact wsArtifact : artifacts) {
            if (wsArtifact.getParent() != null) {
                WorkspaceArtifact parentWsArtifact = findArtifact(artifacts, wsArtifact.getParent().getGroupId(), wsArtifact.getParent().getArtifactId());
                if (parentWsArtifact != null) {
                    parentWsArtifact.getChildren().add(wsArtifact);
                    wsArtifact.setParent(parentWsArtifact);
                }
            } else {
                rootArtifacts.add(wsArtifact);
            }

            for (WorkspaceArtifactDependency dependency : wsArtifact.getDependencies()) {
                WorkspaceArtifact dependencyWsArtifact = findArtifact(artifacts, dependency.getArtifact().getGroupId(), dependency.getArtifact()
                        .getArtifactId());
                if (dependencyWsArtifact != null) {
                    dependency.setArtifact(dependencyWsArtifact);
                }
            }
        }

        return rootArtifacts;
    }

    private WorkspaceArtifact findArtifact(List<WorkspaceArtifact> artifacts, String groupId, String artifactId) {
        for (WorkspaceArtifact artifact : artifacts) {
            if (groupId.equals(artifact.getGroupId()) && artifactId.equals(artifact.getArtifactId())) {
                return artifact;
            }
        }

        return null;
    }

    @Override
    public void applyBaseVersions(List<WorkspaceArtifact> rootArtifacts, List<BaseVersion> baseVersions) {
        for (WorkspaceArtifact rootArtifact : rootArtifacts) {
            applyBaseVersions(rootArtifact, baseVersions);
        }       
    }
    
    private void applyBaseVersions(WorkspaceArtifact artifact, List<BaseVersion> baseVersions) {
        String baseVersion = findBaseVersion(baseVersions, artifact.getGroupId(), artifact.getArtifactId());
        
        if (baseVersion == null && artifact.getParent() != null && artifact.getParent() instanceof WorkspaceArtifact) {
            baseVersion = ((WorkspaceArtifact) artifact.getParent()).getBaseVersion();
            if (baseVersion != null) {
                baseVersions.add(new BaseVersion(artifact.getGroupId(), artifact.getArtifactId(), baseVersion));
            } else {
                LOG.warn("No base version found for workspace artifact {}:{}", artifact.getGroupId(), artifact.getArtifactId());
                return;                
            }
        }
        
        LOG.debug("Applying base version '{}' to artifact {}:{}", new Object[] { baseVersion, artifact.getGroupId(), artifact.getArtifactId() });
        artifact.setBaseVersion(baseVersion);
        
        for (WorkspaceArtifact child : artifact.getChildren()) {
            applyBaseVersions(child, baseVersions);
        }
    }
    
    private String findBaseVersion(List<BaseVersion> baseVersions, String groupId, String artifactId) {
        for (BaseVersion baseVersion : baseVersions) {
            if (groupId.equals(baseVersion.getGroupId()) && artifactId.equals(baseVersion.getArtifactId())) {
                return baseVersion.getVersion();
            }
        }
        
        return null;
    }

    @Override
    public boolean markAllArtifactsDirtyWithDirtyDependencies(List<WorkspaceArtifact> artifacts) {
        boolean changes = false;
        
        for (WorkspaceArtifact artifact : artifacts) {
            if (artifact.isDirty()) {
                continue;
            }
            
            if (artifact.getParent() != null && artifact.getParent() instanceof WorkspaceArtifact) {
                WorkspaceArtifact parentWsArtifact = (WorkspaceArtifact) artifact.getParent();
                if (parentWsArtifact.isDirty()) {
                    LOG.debug("Marking artifact {}:{} dirty because parent is dirty.", artifact.getGroupId(), artifact.getArtifactId());
                    artifact.setDirty(true);
                    changes = true;
                    continue;
                }
            }
            
            for (WorkspaceArtifactDependency dependency : artifact.getDependencies()) {
                if (dependency.getArtifact() instanceof WorkspaceArtifact) {
                    WorkspaceArtifact dependencyWsArtifact = (WorkspaceArtifact) dependency.getArtifact();
                    if (dependencyWsArtifact.isDirty()) {
                        LOG.debug("Marking artifact {}:{} dirty because dependency is dirty: {}:{}", 
                                new Object[] { artifact.getGroupId(), artifact.getArtifactId(), dependencyWsArtifact.getGroupId(), dependencyWsArtifact.getArtifactId() });
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
    public void printWorkspaceArtifactTree(List<WorkspaceArtifact> rootArtifacts, PrintStream printStream) {
        for (WorkspaceArtifact rootArtifact : rootArtifacts) {
            printTree(rootArtifact, printStream, 0);
        }
    }

    private void printTree(MavenArtifact artifact, PrintStream printStream, int level) {
        printStream.print(StringUtils.leftPad(" ", level * 3));

        if (artifact instanceof WorkspaceArtifact) {
            WorkspaceArtifact wsArtifact = (WorkspaceArtifact) artifact;

            printStream.print(wsArtifact.getGroupId() + ":" + wsArtifact.getArtifactId() + ":" + wsArtifact.getVersion());
            if (wsArtifact.isDirty()) {
                if (wsArtifact.getNewVersion() != null) {
                    printStream.println(" -> " + wsArtifact.getNewVersion());
                } else {
                    printStream.println(" -> (Dirty, but no baseVersion definied!)");
                }
            } else{
                printStream.println(" ");   
            }

            for (WorkspaceArtifact child : wsArtifact.getChildren()) {
                printTree(child, printStream, level + 1);
            }

        } else {
            printStream.println("(" + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion() + ")");
        }
    }
}
