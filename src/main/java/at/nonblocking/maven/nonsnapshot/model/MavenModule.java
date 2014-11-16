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
package at.nonblocking.maven.nonsnapshot.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Maven module in the build tree
 * 
 * @author Juergen Kofler
 */
public class MavenModule extends MavenArtifact {

    private File pomFile;

    private boolean insertVersionTag;
    private int versionLocation;
        
    private MavenArtifact parent;
    private int parentVersionLocation;

    private List<MavenModule> children = new ArrayList<MavenModule>();

    private List<MavenModuleDependency> dependencies = new ArrayList<MavenModuleDependency>();

    private String newVersion;

    private boolean dirty;

    public MavenModule(File pomFile, String groupId, String artifactId, String version) {
        super(groupId, artifactId, version);
        this.pomFile = pomFile;
    }

    public File getPomFile() {
        return pomFile;
    }

    public void setPomFile(File pomFile) {
        this.pomFile = pomFile;
    }

    public boolean isInsertVersionTag() {
        return insertVersionTag;
    }

    public void setInsertVersionTag(boolean insertVersionTag) {
        this.insertVersionTag = insertVersionTag;
    }

    public int getVersionLocation() {
        return versionLocation;
    }

    public void setVersionLocation(int versionLocation) {
        this.versionLocation = versionLocation;
    }

    public int getParentVersionLocation() {
        return parentVersionLocation;
    }

    public void setParentVersionLocation(int parentVersionLocation) {
        this.parentVersionLocation = parentVersionLocation;
    }

    public MavenArtifact getParent() {
        return parent;
    }

    public void setParent(MavenArtifact parent) {
        this.parent = parent;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public List<MavenModule> getChildren() {
        return children;
    }

    public List<MavenModuleDependency> getDependencies() {
        return dependencies;
    }

    public String getNewVersion() {
        return newVersion;
    }

    public void setNewVersion(String newVersion) {
        this.newVersion = newVersion;
    }
}
