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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputLocationTracker;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3ReaderEx;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.nonblocking.maven.nonsnapshot.DEPENDENCY_UPDATE_STRATEGY;
import at.nonblocking.maven.nonsnapshot.MavenPomHandler;
import at.nonblocking.maven.nonsnapshot.exception.NonSnapshotPluginException;
import at.nonblocking.maven.nonsnapshot.model.MavenArtifact;
import at.nonblocking.maven.nonsnapshot.model.WorkspaceArtifact;
import at.nonblocking.maven.nonsnapshot.model.WorkspaceArtifactDependency;

/**
 * Default implementation of {@link MavenPomHandler}
 * 
 * @author Juergen Kofler
 */
@Component(role = MavenPomHandler.class, hint = "default")
public class MavenPomHandlerDefaultImpl implements MavenPomHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MavenPomHandlerDefaultImpl.class);

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    @Override
    public WorkspaceArtifact readArtifact(File pomFile) {
        LOG.debug("Loading POM file: {}", pomFile.getAbsolutePath());

        InputSource is = new InputSource();
        MavenXpp3ReaderEx reader = new MavenXpp3ReaderEx();

        Model model = null;
        
        try {
            model = reader.read(ReaderFactory.newXmlReader(pomFile), false, is);
        } catch (IOException e) {
            throw new NonSnapshotPluginException("Failed to load POM: " + pomFile.getAbsolutePath(), e);
        } catch (XmlPullParserException e) {
            throw new NonSnapshotPluginException("Failed to load POM: " + pomFile.getAbsolutePath(), e);
        }
        
        String groupId = model.getGroupId();
        if (groupId == null) {
            if (model.getParent() != null) {
                groupId = model.getParent().getGroupId();
            } else {
                throw new NonSnapshotPluginException("Invalid POM file: groupId is not set and no parent either: " + pomFile.getAbsolutePath());
            }
        }
        
        boolean insertVersionTag = false;
        String version = model.getVersion();
        if (version == null) {
            if (model.getParent() != null) {
                version = model.getParent().getVersion();
                insertVersionTag = true;
            } else {
                throw new NonSnapshotPluginException("Invalid POM file: Version is not set and no parent either: " + pomFile.getAbsolutePath());
            }
        }
        
        //TODO: method to long
        
        WorkspaceArtifact wsArtifact = new WorkspaceArtifact(pomFile, groupId, model.getArtifactId(), version);
        wsArtifact.setInsertVersionTag(insertVersionTag);
        wsArtifact.setVersionLocation(getVersionLocation(model));
        
        // Parent
        if (model.getParent() != null) {            
            wsArtifact.setParent(new MavenArtifact(model.getParent().getGroupId(), 
                    model.getParent().getArtifactId(), model.getParent().getVersion()));            
            wsArtifact.setParentVersionLocation(getVersionLocation(model.getParent()));
        }

        // Dependencies
        for (Dependency dependency : model.getDependencies()) {            
            wsArtifact.getDependencies().add(new WorkspaceArtifactDependency(
                    getVersionLocation(dependency), 
                    new MavenArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion())));
        }

        // Plugins
        if (model.getBuild() != null) {
            for (Plugin plugin : model.getBuild().getPlugins()) {                
                wsArtifact.getDependencies().add(new WorkspaceArtifactDependency(
                        getVersionLocation(plugin), 
                        new MavenArtifact(plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion())));
                
                for (Dependency dependency : plugin.getDependencies()) {                    
                    wsArtifact.getDependencies().add(new WorkspaceArtifactDependency(
                            getVersionLocation(dependency), 
                            new MavenArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion())));
                }
            }
        }

        // Profile Dependencies
        for (Profile profile : model.getProfiles()) {
            for (Dependency dependency : profile.getDependencies()) {                                 
                wsArtifact.getDependencies().add(new WorkspaceArtifactDependency(
                        getVersionLocation(dependency),  
                        new MavenArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion())));
            }
        }

        // Profile Plugin with Dependecies
        for (Profile profile : model.getProfiles()) {
            if (profile.getBuild() != null) {
                for (Plugin plugin : profile.getBuild().getPlugins()) {                
                    wsArtifact.getDependencies().add(new WorkspaceArtifactDependency(
                            getVersionLocation(plugin), 
                            new MavenArtifact(plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion())));
                         

                    for (Dependency dependency : plugin.getDependencies()) {                        
                        wsArtifact.getDependencies().add(new WorkspaceArtifactDependency(
                                getVersionLocation(dependency),  
                                new MavenArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion())));
                    }
                }
            }
        }

        return wsArtifact;
    }
    
    private int getVersionLocation(InputLocationTracker tracker) {
        InputLocation location = tracker.getLocation("version");
        if (location == null) {
            location = tracker.getLocation("artifactId");
        }
        
        return location.getLineNumber();
    }

    @Override
    public void updateArtifact(WorkspaceArtifact workspaceArtifact, DEPENDENCY_UPDATE_STRATEGY dependencyUpdateStrategy) {
        if (!workspaceArtifact.isDirty()) {
            return;
        }
        
        List<PomUpdateCommand> commands = new ArrayList<MavenPomHandlerDefaultImpl.PomUpdateCommand>();
                
        addUpdateCommand(workspaceArtifact, workspaceArtifact.getVersionLocation(), false, dependencyUpdateStrategy, commands);
        
        if (workspaceArtifact.getParent() != null && workspaceArtifact.getParent() instanceof WorkspaceArtifact) {
            addUpdateCommand((WorkspaceArtifact) workspaceArtifact.getParent(), workspaceArtifact.getParentVersionLocation(), true, dependencyUpdateStrategy, commands);           
        }
        
        for (WorkspaceArtifactDependency dependency : workspaceArtifact.getDependencies()) {
            if (dependency.getArtifact() instanceof WorkspaceArtifact) {
                addUpdateCommand((WorkspaceArtifact) dependency.getArtifact(), dependency.getVersionLocation(), true, dependencyUpdateStrategy, commands);
            }
        }
        
        executeUpdateCommands(commands, workspaceArtifact.getPomFile());
    }
    
    private void addUpdateCommand(WorkspaceArtifact wsArtifact, Integer lineNumber, boolean dependency, DEPENDENCY_UPDATE_STRATEGY dependencyUpdateStrategy, List<PomUpdateCommand> commands) {
        if (!wsArtifact.isDirty()) {
            return;
        }
        if (wsArtifact.getNewVersion() == null) {
            LOG.warn("New version for artifact {}:{} not set, cannot update version!", wsArtifact.getGroupId(), wsArtifact.getArtifactId());
            return;
        }
        
        if (dependency && !checkDependecyShallBeUpdated(wsArtifact, dependencyUpdateStrategy)) {
            LOG.info("Don't update dependency {}:{} because update strategy is not satisfied: Current version: {}, new version: {}",
                    new Object[] { wsArtifact.getGroupId(), wsArtifact.getArtifactId(), wsArtifact.getVersion(), wsArtifact.getNewVersion() });
            return;
        }
        
        if (!dependency && wsArtifact.isInsertVersionTag()) {
            commands.add(new PomUpdateCommand(lineNumber, UPDATE_COMMAND_TYPE.INSERT, "<version>" + wsArtifact.getNewVersion() + "</version>", null));            
        } else {            
            commands.add(new PomUpdateCommand(lineNumber, UPDATE_COMMAND_TYPE.REPLACE, "<version>.*?</version>", "<version>" + wsArtifact.getNewVersion() + "</version>"));
        }
    }
    
    private boolean checkDependecyShallBeUpdated(WorkspaceArtifact wsArtifact, DEPENDENCY_UPDATE_STRATEGY dependencyUpdateStrategy) {
        String[] versionParts = wsArtifact.getVersion().split("[\\.-]");
        String[] newVersionParts = wsArtifact.getNewVersion().split("[\\.-]");
        
        switch (dependencyUpdateStrategy) {
        case SAME_MAJOR:
            if (versionParts.length > 0 && versionParts[0].equals(newVersionParts[0])) {
                return true;
            }
            return false;
        case SAME_MAJOR_MINOR:
            if (versionParts.length > 1 && versionParts[0].equals(newVersionParts[0]) && versionParts[1].equals(newVersionParts[1])) {
                return true;
            }
            return false;
        case SAME_BASE_VERSION:
            return wsArtifact.getVersion().startsWith(wsArtifact.getBaseVersion());
        default:
        case ALWAYS: 
            return true;
        }        
    }

    private void executeUpdateCommands(List<PomUpdateCommand> commands, File pomFile) {
        Map<Integer, PomUpdateCommand> commandMap = new HashMap<Integer, PomUpdateCommand>();

        for (PomUpdateCommand command : commands) {
            commandMap.put(command.lineNumber, command);
        }

        try {
            LineNumberReader reader = new LineNumberReader(new FileReader(pomFile));
            File tempTarget = File.createTempFile("pom", ".xml");
            PrintWriter writer = new PrintWriter(tempTarget);

            LOG.debug("Writing temporary POM file to: {}", tempTarget.getAbsoluteFile());

            String line = null;
            while ((line = reader.readLine()) != null) {
                PomUpdateCommand command = commandMap.get(reader.getLineNumber());
                if (command != null) {
                    line = executeCommand(line, command);
                }

                writer.write(line + LINE_SEPARATOR);
            }

            reader.close();
            writer.close();

            LOG.debug("Copy temporary POM file to: {}", pomFile.getAbsoluteFile());
            IOUtil.copy(new FileReader(tempTarget), new FileOutputStream(pomFile));
            tempTarget.delete();

        } catch (IOException e) {
            throw new NonSnapshotPluginException("Failed to updated POM file: " + pomFile.getAbsolutePath(), e);
        }
    }

    private String executeCommand(String line, PomUpdateCommand command) {
        switch (command.commandType) {
        case REPLACE:
            LOG.debug("Replacing '{}' with '{}' in line number: {}", new Object[] { command.text1, command.text2, command.lineNumber });
            return line.replaceAll(command.text1, command.text2);
        case INSERT:
            LOG.debug("Inserting '{}' in line number: {}", new Object[] { command.text1, command.lineNumber });
            return line + LINE_SEPARATOR + command.text1;
        }

        return line;
    }

    private enum UPDATE_COMMAND_TYPE {
        INSERT, REPLACE
    }

    private static class PomUpdateCommand {

        int lineNumber;
        UPDATE_COMMAND_TYPE commandType;
        String text1;
        String text2;

        public PomUpdateCommand(int lineNumber, UPDATE_COMMAND_TYPE commandType, String text1, String text2) {
            this.lineNumber = lineNumber;
            this.commandType = commandType;
            this.text1 = text1;
            this.text2 = text2;
        }

    }

}
