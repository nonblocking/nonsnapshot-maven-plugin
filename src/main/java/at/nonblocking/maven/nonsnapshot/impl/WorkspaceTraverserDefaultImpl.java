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

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.nonblocking.maven.nonsnapshot.WorkspaceTraverser;

/**
 * Default implementation of {@link WorkspaceTraverser}.
 * 
 * @author Juergen Kofler
 */
@Component(role = WorkspaceTraverser.class, hint = "default")
public class WorkspaceTraverserDefaultImpl implements WorkspaceTraverser {

    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceTraverserDefaultImpl.class);
    
    private static final String[] IGNORE_SUBFOLDERS = { "bin", "target" };
    
    @Override
    public List<File> findAllPomFiles(File workspaceDir, List<String> excludeFolders) {
        List<File> pomFiles = new ArrayList<File>();
        
        for (File dir : workspaceDir.listFiles(new DirectoryFilter(excludeFolders))) {
            gatherPomFiles(dir, pomFiles);
        }
        
        return pomFiles;
    }

    private void gatherPomFiles(File dir, List<File> pomFiles) {
        File pomFile = new File(dir, "pom.xml");
        if (pomFile.exists()) {
            LOG.debug("Found pom: {}", pomFile.getAbsolutePath());
            pomFiles.add(pomFile);
            
            for (File subdir : dir.listFiles(new DirectoryFilter(null))) {
                gatherPomFiles(subdir, pomFiles);
            }
        }
    }
    
    private static class DirectoryFilter implements FileFilter {

        private List<String> excludes = new ArrayList<String>();
        
        DirectoryFilter(List<String> excludeFolders) {
            if (excludeFolders != null) {
                this.excludes.addAll(excludeFolders);
            }
            
            this.excludes.addAll(Arrays.asList(IGNORE_SUBFOLDERS));
        }

        @Override
        public boolean accept(File file) {
            return file.isDirectory() && !this.excludes.contains(file.getName());
        }                      
    }
    
}
