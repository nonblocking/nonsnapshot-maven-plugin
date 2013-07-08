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
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.nonblocking.maven.nonsnapshot.exception.NonSnapshotPluginException;
import at.nonblocking.maven.nonsnapshot.model.WorkspaceArtifact;

/**
 * Main Goal of this Plugin. <br/>
 * <br/>
 * Checks the version number and manipulates it if necessary (if changes were found). 
 * <br/>
 * Updates the versions in the dependent projects accordingly. All dependent
 * projects must be in the same folder (workspace). <br/>
 * <br/>
 * Commits the POM files if deferPomCommit is false.
 * 
 * @author Juergen Kofler
 * 
 * @goal updateVersions
 */
public class NonSnapshotUpdateVersionsMojo extends NonSnapshotBaseMojo {

    private static Logger LOG = LoggerFactory.getLogger(NonSnapshotUpdateVersionsMojo.class);

    private static String LINE_SEPARATOR = System.getProperty("line.separator");

    @Override
    protected void internalExecute() {
        List<File> pomFiles = getWorkspaceTraverser().findAllPomFiles(getWorkspaceDir(), getExcludeFolders());
        List<WorkspaceArtifact> workspaceArtifacts = new ArrayList<WorkspaceArtifact>();

        for (File pomFile : pomFiles) {
            WorkspaceArtifact artifact = getMavenPomHandler().readArtifact(pomFile);
            workspaceArtifacts.add(artifact);
        }

        List<WorkspaceArtifact> rootArtifacts = getDependencyTreeProcessor().buildDependencyTree(workspaceArtifacts);

        getDependencyTreeProcessor().applyBaseVersions(rootArtifacts, getBaseVersions());

        markDirtyWhenRevisionChangedOrInvalidQualifier(workspaceArtifacts);

        //Recursively mark artifacts dirty
        boolean changes = getDependencyTreeProcessor().markAllArtifactsDirtyWithDirtyDependencies(workspaceArtifacts);
        while (changes) {
            changes = getDependencyTreeProcessor().markAllArtifactsDirtyWithDirtyDependencies(workspaceArtifacts);
        }

        setNextRevisionOnDirtyArtifacts(workspaceArtifacts);

        dumpArtifactTreeToLog(rootArtifacts);
        
        writeAndCommitArtifacts(workspaceArtifacts);
    }

    protected void writeAndCommitArtifacts(List<WorkspaceArtifact> workspaceArtifacts) {
        List<File> pomsToCommit = new ArrayList<File>();

        for (WorkspaceArtifact workspaceArtifact : workspaceArtifacts) {
            if (workspaceArtifact.isDirty() && workspaceArtifact.getNewVersion() != null) {
                getMavenPomHandler().updateArtifact(workspaceArtifact, getDependencyUpdateStrategy());
                LOG.info("Add POM file to commit list: ", workspaceArtifact.getPomFile().getAbsolutePath());
                pomsToCommit.add(workspaceArtifact.getPomFile());
            }
        }

        if (pomsToCommit.size() > 0) {
            if (!isDeferPomCommit()) {
                LOG.info("Commiting {} POM files", pomsToCommit.size());
                getScmHandler().commitFiles(pomsToCommit, "Nonsnapshot Plugin: Version of " + pomsToCommit.size() + " artifacts updated");
            } else {               
                File pomsToCommitFile = new File(getWorkspaceDir(), POMS_TO_COMMIT_TEXT_FILE);
                LOG.info("Deferring the POM commit. Execute nonsnapshot:commit to actually commit the changes.");
                
                LOG.debug("Writing POM files to commit to: {}", pomsToCommitFile.getAbsolutePath());
                writeFilesToPomsToCommitList(pomsToCommit, pomsToCommitFile);
            }
        } else {
            LOG.info("Workspace is up-to-date. No versions udpated.");
        }
    }

    private void markDirtyWhenRevisionChangedOrInvalidQualifier(List<WorkspaceArtifact> workspaceArtifacts) {
        for (WorkspaceArtifact artifact : workspaceArtifacts) {
            if (artifact.getVersion() == null) {
                LOG.info("No version found for artifact {}:{}. Assigning a new version.", artifact.getGroupId(), artifact.getArtifactId());
                artifact.setDirty(true);

            } else if (artifact.getVersion().startsWith("${")) {
                LOG.info("Version property found for artifact {}:{}. Assigning a new version.", artifact.getGroupId(), artifact.getArtifactId());
                artifact.setDirty(true);

            } else {
                String[] versionParts = artifact.getVersion().split("-");
                String qualifierString = null;
                if (versionParts.length > 1) {
                    qualifierString = versionParts[versionParts.length - 1];
                }

                if (qualifierString == null) {
                    LOG.info("Invalid qualifier string found for artifact {}:{}: {}. Assigning a new version.",
                            new Object[] { artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion() });
                    artifact.setDirty(true);

                } else if (qualifierString.equals("SNAPSHOT")) {
                    LOG.info("Snapshot version found for artifact {}:{}. Assigning a new version.", artifact.getGroupId(), artifact.getArtifactId());
                    artifact.setDirty(true);

                } else {
                    String currentRevision = getScmHandler().getRevisionId(artifact.getPomFile().getParentFile());
                    LOG.debug("Current project revision id: {}; revision id in the version qualifier: {}", currentRevision, qualifierString);

                    if (!currentRevision.equals(qualifierString)) {
                        LOG.info("Current project revision id is different from the revision id in the version qualifier of artifact {}:{}. Assigning a new version.",
                                artifact.getGroupId(), artifact.getArtifactId());
                        artifact.setDirty(true);

                    }
                }
            }
        }
    }

    private void setNextRevisionOnDirtyArtifacts(List<WorkspaceArtifact> workspaceArtifacts) {        
        for (WorkspaceArtifact artifact : workspaceArtifacts) {
            File artifactPath = artifact.getPomFile().getParentFile();  
                    
            if (artifact.isDirty() && getScmHandler().isWorkingCopy(artifactPath)) {
                artifact.setNextRevisionId(getScmHandler().getNextRevisionId(artifactPath));
            }
        }
    }

    private void writeFilesToPomsToCommitList(List<File> fileList, File outputFile) {
        try {
            PrintWriter writer = new PrintWriter(new FileOutputStream(outputFile, false));

            for (File file : fileList) {
                writer.write(file.getAbsolutePath() + LINE_SEPARATOR);
            }

            writer.close();
        } catch (IOException e) {
            throw new NonSnapshotPluginException("Failed to write text file with POMs to commit!", e);
        }
    }
    
    private void dumpArtifactTreeToLog(List<WorkspaceArtifact> rootArtifacts) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        getDependencyTreeProcessor().printWorkspaceArtifactTree(rootArtifacts, new PrintStream(baos));
        LOG.info("\n" + baos.toString());
    }
}
