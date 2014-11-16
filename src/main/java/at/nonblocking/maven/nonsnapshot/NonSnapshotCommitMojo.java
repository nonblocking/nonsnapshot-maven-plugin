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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugins.annotations.Mojo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.nonblocking.maven.nonsnapshot.exception.NonSnapshotPluginException;

/**
 * Goal to commit changed POM files if the updateVersions goal was called with deferPomCommit = true 
 * 
 * @author Juergen Kofler
 */
@Mojo(name = "commitVersions", aggregator = true)
public class NonSnapshotCommitMojo extends NonSnapshotBaseMojo {

    private static Logger LOG = LoggerFactory.getLogger(NonSnapshotCommitMojo.class);

    @Override
    protected void internalExecute() {
        File dirtyModulesRegistryFile = getDirtyModulesRegistryFile();
        LOG.debug("Reading POM files to commit from: {}", dirtyModulesRegistryFile.getAbsolutePath());
        
        if (!dirtyModulesRegistryFile.exists()) {
            LOG.info("File {} does not exist. Doing nothing.", dirtyModulesRegistryFile.getAbsolutePath());
            return;
        }

        List<File> pomsToCommit = readPomFileList(dirtyModulesRegistryFile);
        if (pomsToCommit.size() == 0) {
            return;
        }
        
        try {
            LOG.info("Commiting {} POM files", pomsToCommit.size());
            getScmHandler().commitFiles(pomsToCommit, "Nonsnapshot Plugin: Version of " + pomsToCommit.size() + " artifacts updated");
        } catch (RuntimeException e) {
            if (isDontFailOnCommit()) {
                LOG.warn("Error occurred during commit, ignoring it since dontFailOnCommit=true.", e);
            } else {
                throw e;
            }
        }
    }

    private List<File> readPomFileList(File inputFile) {
        List<File> pomFileList = new ArrayList<File>();
        File baseDir = getMavenProject().getBasedir();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));

            String line;
            while ((line = reader.readLine()) != null) {
                File pom = new File(baseDir, line + "/pom.xml");
                if (!pomFileList.contains(pom)) {
                    pomFileList.add(pom);
                }
            }

            reader.close();

            LOG.info("Deleting {}", inputFile.getAbsolutePath());
            inputFile.delete();
            
            return pomFileList;

        } catch (IOException e) {
            throw new NonSnapshotPluginException("Failed to read dirty modules registry file!", e);
        }
    }
}
