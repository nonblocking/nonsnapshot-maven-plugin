package at.nonblocking.maven.nonsnapshot;

import static junit.framework.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.impl.StaticLoggerBinder;

import at.nonblocking.maven.nonsnapshot.impl.DependencyTreeProcessorDefaultImpl;
import at.nonblocking.maven.nonsnapshot.model.MavenArtifact;
import at.nonblocking.maven.nonsnapshot.model.WorkspaceArtifact;
import at.nonblocking.maven.nonsnapshot.model.WorkspaceArtifactDependency;

public class DependencyTreeProcessorDefaultImplTest {

    @BeforeClass
    public static void setupLog() {
        StaticLoggerBinder.getSingleton().setLog(new DebugSystemStreamLog());
    }
    
    @Test
    public void testBuildDependencyTree() {       
       WorkspaceArtifact wsArtifact1 = new WorkspaceArtifact(null, "at.nonblocking.at", "parent", "1.0.0");
       WorkspaceArtifact wsArtifact2 = new WorkspaceArtifact(null, "at.nonblocking.at", "test1", "1.0.0");
       WorkspaceArtifact wsArtifact3 = new WorkspaceArtifact(null, "at.nonblocking.at", "test1-module1", "1.0.0");
       WorkspaceArtifact wsArtifact4 = new WorkspaceArtifact(null, "at.nonblocking.at", "test1-module2", "1.0.0");
       WorkspaceArtifact wsArtifact5 = new WorkspaceArtifact(null, "at.nonblocking.at", "test2", "1.0.0");
       WorkspaceArtifact wsArtifact6 = new WorkspaceArtifact(null, "at.nonblocking.at", "plugin1", "1.0.0");
        
       wsArtifact1.getDependencies().add(new WorkspaceArtifactDependency(0, new MavenArtifact("at.nonblocking.at", "plugin1", "1.0.0")));
       wsArtifact1.getDependencies().add(new WorkspaceArtifactDependency(0, new MavenArtifact("junit", "junit", "4.7")));
       wsArtifact2.setParent(new MavenArtifact("at.nonblocking.at", "parent", "1.0.0"));
       wsArtifact2.getDependencies().add(new WorkspaceArtifactDependency(0, new MavenArtifact("at.nonblocking.at", "test2", "1.0.0")));
       wsArtifact3.setParent(new MavenArtifact("at.nonblocking.at", "test1", "1.0.0"));
       wsArtifact4.setParent(new MavenArtifact("at.nonblocking.at", "test1", "1.0.0"));
       wsArtifact5.setParent(new MavenArtifact("at.nonblocking.at", "parent", "1.0.0"));       

       List<WorkspaceArtifact> artifacts = new ArrayList<WorkspaceArtifact>();
       artifacts.add(wsArtifact1);
       artifacts.add(wsArtifact2);
       artifacts.add(wsArtifact3);
       artifacts.add(wsArtifact4);
       artifacts.add(wsArtifact5);
       artifacts.add(wsArtifact6);
       
       DependencyTreeProcessor dependencyTreeProcessor = new DependencyTreeProcessorDefaultImpl();
       
       List<WorkspaceArtifact> rootArtifacts = dependencyTreeProcessor.buildDependencyTree(artifacts);

       assertNotNull(rootArtifacts);
       assertEquals(2, rootArtifacts.size());
       assertEquals(2, rootArtifacts.get(0).getChildren().size());
       assertEquals(0, rootArtifacts.get(1).getChildren().size());
       
       dependencyTreeProcessor.printWorkspaceArtifactTree(rootArtifacts, System.out);
    }
    
    @Test
    public void testApplyBaseVersions() {
        WorkspaceArtifact wsArtifact1 = new WorkspaceArtifact(null, "at.nonblocking.at", "parent", "1.0.0");
        WorkspaceArtifact wsArtifact2 = new WorkspaceArtifact(null, "at.nonblocking.at", "test1", "1.0.0");
        WorkspaceArtifact wsArtifact3 = new WorkspaceArtifact(null, "at.nonblocking.at", "test1-module1", "1.0.0");
        WorkspaceArtifact wsArtifact4 = new WorkspaceArtifact(null, "at.nonblocking.at", "test1-module2", "1.0.0");
        WorkspaceArtifact wsArtifact5 = new WorkspaceArtifact(null, "at.nonblocking.at", "test2", "1.0.0");
        WorkspaceArtifact wsArtifact6 = new WorkspaceArtifact(null, "at.nonblocking.at", "plugin1", "1.0.0");
         
        wsArtifact1.getDependencies().add(new WorkspaceArtifactDependency(0, wsArtifact6));
        wsArtifact1.getChildren().add(wsArtifact2);
        wsArtifact1.getChildren().add(wsArtifact5);
        wsArtifact1.setDirty(true);
        wsArtifact1.setNextRevisionId("0");
        wsArtifact2.setParent(wsArtifact1);
        wsArtifact2.getChildren().add(wsArtifact3);
        wsArtifact2.getChildren().add(wsArtifact4);        
        wsArtifact2.getDependencies().add(new WorkspaceArtifactDependency(0, wsArtifact5));
        wsArtifact2.setDirty(true);
        wsArtifact2.setNextRevisionId("0");
        wsArtifact3.setParent(wsArtifact2);
        wsArtifact3.setDirty(true);
        wsArtifact3.setNextRevisionId("0");
        wsArtifact4.setParent(wsArtifact2);
        wsArtifact4.setDirty(true);
        wsArtifact4.setNextRevisionId("0");
        wsArtifact5.setParent(wsArtifact1);
        wsArtifact5.setDirty(true);
        wsArtifact5.setNextRevisionId("0");
        wsArtifact6.setDirty(true);
        wsArtifact6.setNextRevisionId("0");
        
        List<WorkspaceArtifact> artifacts = new ArrayList<WorkspaceArtifact>();
        artifacts.add(wsArtifact1);
        artifacts.add(wsArtifact2);
        artifacts.add(wsArtifact3);
        artifacts.add(wsArtifact4);
        artifacts.add(wsArtifact5);
        artifacts.add(wsArtifact6);

        List<WorkspaceArtifact> rootArtifacts = new ArrayList<WorkspaceArtifact>();
        rootArtifacts.add(wsArtifact1);
        rootArtifacts.add(wsArtifact6);
        
        List<BaseVersion> baseVersions = new ArrayList<BaseVersion>();
        baseVersions.add(new BaseVersion("at.nonblocking.at", "parent", "2.1.2"));
        baseVersions.add(new BaseVersion("at.nonblocking.at", "test1-module2", "1.2.2"));
        baseVersions.add(new BaseVersion("at.nonblocking.at", "plugin1", "3.0.1"));
              
        DependencyTreeProcessor dependencyTreeProcessor = new DependencyTreeProcessorDefaultImpl();
                
        dependencyTreeProcessor.applyBaseVersions(rootArtifacts, baseVersions);
        
        assertEquals("2.1.2", wsArtifact1.getBaseVersion());
        assertEquals("2.1.2", wsArtifact2.getBaseVersion());
        assertEquals("2.1.2", wsArtifact3.getBaseVersion());
        assertEquals("1.2.2", wsArtifact4.getBaseVersion());
        assertEquals("2.1.2", wsArtifact5.getBaseVersion());
        assertEquals("3.0.1", wsArtifact6.getBaseVersion());
        
        dependencyTreeProcessor.printWorkspaceArtifactTree(rootArtifacts, System.out);        
    }
    
    @Test
    public void testMarkAllArtifactsDirtyWithDirtyDependencies()  {
        WorkspaceArtifact wsArtifact1 = new WorkspaceArtifact(null, "at.nonblocking.at", "test1", "1.0.0");
        WorkspaceArtifact wsArtifact2 = new WorkspaceArtifact(null, "at.nonblocking.at", "test2", "1.0.0");
        WorkspaceArtifact wsArtifact3 = new WorkspaceArtifact(null, "at.nonblocking.at", "plugin1", "1.0.0");
         
        wsArtifact2.getDependencies().add(new WorkspaceArtifactDependency(0, wsArtifact3));
        wsArtifact3.setDirty(true);
        
        List<WorkspaceArtifact> artifacts = new ArrayList<WorkspaceArtifact>();
        artifacts.add(wsArtifact1);
        artifacts.add(wsArtifact2);
        artifacts.add(wsArtifact3);
        
        DependencyTreeProcessor dependencyTreeProcessor = new DependencyTreeProcessorDefaultImpl();
        
        boolean changes1 = dependencyTreeProcessor.markAllArtifactsDirtyWithDirtyDependencies(artifacts);
        boolean changes2 = dependencyTreeProcessor.markAllArtifactsDirtyWithDirtyDependencies(artifacts);
        
        assertTrue(changes1);
        assertFalse(changes2);
        
        assertFalse(wsArtifact1.isDirty());
        assertTrue(wsArtifact2.isDirty());
        assertTrue(wsArtifact3.isDirty());
        
        dependencyTreeProcessor.printWorkspaceArtifactTree(artifacts, System.out);
    }
    
    @Test
    public void testMarkAllArtifactsDirtyWithDirtyDependenciesRecursive()  {        
        WorkspaceArtifact wsArtifact1 = new WorkspaceArtifact(null, "at.nonblocking.at", "test1", "1.0.0");
        WorkspaceArtifact wsArtifact2 = new WorkspaceArtifact(null, "at.nonblocking.at", "test2", "1.0.0");
        WorkspaceArtifact wsArtifact3 = new WorkspaceArtifact(null, "at.nonblocking.at", "plugin1", "1.0.0");
        WorkspaceArtifact wsArtifact4 = new WorkspaceArtifact(null, "at.nonblocking.at", "test", "1.0.0");
        
        //4 -> 2 -> 3 (dirty)
        wsArtifact4.getDependencies().add(new WorkspaceArtifactDependency(0, wsArtifact2));
        wsArtifact2.getDependencies().add(new WorkspaceArtifactDependency(0, wsArtifact3));
        wsArtifact3.setDirty(true);
        
        List<WorkspaceArtifact> artifacts = new ArrayList<WorkspaceArtifact>();
        artifacts.add(wsArtifact1);
        artifacts.add(wsArtifact2);
        artifacts.add(wsArtifact3);
        artifacts.add(wsArtifact4);
        
        DependencyTreeProcessor dependencyTreeProcessor = new DependencyTreeProcessorDefaultImpl();
        
        boolean changes1 = dependencyTreeProcessor.markAllArtifactsDirtyWithDirtyDependencies(artifacts);
        boolean changes2 = dependencyTreeProcessor.markAllArtifactsDirtyWithDirtyDependencies(artifacts);
        
        assertTrue(changes1);
        assertFalse(changes2);
        
        assertFalse(wsArtifact1.isDirty());
        assertTrue(wsArtifact2.isDirty());
        assertTrue(wsArtifact3.isDirty());
        assertTrue(wsArtifact4.isDirty());
        
        dependencyTreeProcessor.printWorkspaceArtifactTree(artifacts, System.out);
    }
}
