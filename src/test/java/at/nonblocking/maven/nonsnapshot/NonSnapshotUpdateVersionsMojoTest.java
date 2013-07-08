package at.nonblocking.maven.nonsnapshot;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InOrder;
import org.slf4j.impl.StaticLoggerBinder;

import at.nonblocking.maven.nonsnapshot.model.WorkspaceArtifact;

public class NonSnapshotUpdateVersionsMojoTest {

    private NonSnapshotUpdateVersionsMojo nonSnapshotMojo = new NonSnapshotUpdateVersionsMojo();
    private WorkspaceTraverser mockWorkspaceTraverser = mock(WorkspaceTraverser.class);
    private DependencyTreeProcessor mockDependencyTreeProcessor = mock(DependencyTreeProcessor.class);
    private MavenPomHandler mockMavenPomHandler = mock(MavenPomHandler.class);
    private ScmHandler mockScmHandler = mock(ScmHandler.class);

    @BeforeClass
    public static void setupLog() {
        StaticLoggerBinder.getSingleton().setLog(new DebugSystemStreamLog());
    }

    @Before
    public void setupMojo() {
        this.nonSnapshotMojo.setWorkspaceDir(new File("target"));
        this.nonSnapshotMojo.setBaseVersions(Arrays.asList(new BaseVersion("at.nonblocking", "build-test-parent", "1.0.13")));
        this.nonSnapshotMojo.setScmUser("foo");
        this.nonSnapshotMojo.setScmPassword("bar");
        this.nonSnapshotMojo.setDeferPomCommit(false);
        this.nonSnapshotMojo.setWorkspaceTraverser(this.mockWorkspaceTraverser);
        this.nonSnapshotMojo.setDependencyTreeProcessor(this.mockDependencyTreeProcessor);
        this.nonSnapshotMojo.setMavenPomHandler(this.mockMavenPomHandler);
        this.nonSnapshotMojo.setScmHandler(this.mockScmHandler);
        this.nonSnapshotMojo.setDependencyUpdateStrategy(DEPENDENCY_UPDATE_STRATEGY.ALWAYS);
    }

    @Test
    public void testUpdate() throws Exception {
        File pom1 = new File("test1/pom.xm");
        File pom2 = new File("test2/pom.xm");
        File pom3 = new File("test3/pom.xm");
        File pom4 = new File("test4/pom.xm");
        File pom5 = new File("test5/pom.xm");

        WorkspaceArtifact wsArtifact1 = new WorkspaceArtifact(pom1, "nonblocking.at", "test1", "1.0.0-SNAPSHOT"); // Invalid version
        wsArtifact1.setBaseVersion("1.0.0");
        WorkspaceArtifact wsArtifact2 = new WorkspaceArtifact(pom2, "nonblocking.at", "test2", "1.1.0-1234");
        WorkspaceArtifact wsArtifact3 = new WorkspaceArtifact(pom3, "nonblocking.at", "test3", null); // Invalid version
        wsArtifact3.setBaseVersion("1.0.0");
        WorkspaceArtifact wsArtifact4 = new WorkspaceArtifact(pom4, "nonblocking.at", "test3", "2.1.0-FIX1-1234");
        wsArtifact4.setBaseVersion("2.1.1");
        WorkspaceArtifact wsArtifact5 = new WorkspaceArtifact(pom5, "nonblocking.at", "test3", "${test.version}"); // Invalid version
        wsArtifact5.setBaseVersion("1.1.0");

        List<WorkspaceArtifact> artifactList = new ArrayList<WorkspaceArtifact>();
        artifactList.add(wsArtifact1);
        artifactList.add(wsArtifact2);
        artifactList.add(wsArtifact3);
        artifactList.add(wsArtifact4);
        artifactList.add(wsArtifact5);

        when(this.mockWorkspaceTraverser.findAllPomFiles(new File("target"), null)).thenReturn(Arrays.asList(pom1, pom2, pom3, pom4, pom5));
        when(this.mockMavenPomHandler.readArtifact(pom1)).thenReturn(wsArtifact1);
        when(this.mockMavenPomHandler.readArtifact(pom2)).thenReturn(wsArtifact2);
        when(this.mockMavenPomHandler.readArtifact(pom3)).thenReturn(wsArtifact3);
        when(this.mockMavenPomHandler.readArtifact(pom4)).thenReturn(wsArtifact4);
        when(this.mockMavenPomHandler.readArtifact(pom5)).thenReturn(wsArtifact5);
        when(this.mockDependencyTreeProcessor.buildDependencyTree(artifactList)).thenReturn(artifactList);

        when(this.mockScmHandler.getRevisionId(pom2.getParentFile())).thenReturn("1234");
        when(this.mockScmHandler.getRevisionId(pom4.getParentFile())).thenReturn("5678");
        
        when(this.mockScmHandler.isWorkingCopy(pom1.getParentFile())).thenReturn(true);
        when(this.mockScmHandler.isWorkingCopy(pom2.getParentFile())).thenReturn(false);
        when(this.mockScmHandler.isWorkingCopy(pom3.getParentFile())).thenReturn(false);
        when(this.mockScmHandler.isWorkingCopy(pom4.getParentFile())).thenReturn(true);
        when(this.mockScmHandler.isWorkingCopy(pom5.getParentFile())).thenReturn(true);
        
        when(this.mockScmHandler.getNextRevisionId(pom1.getParentFile())).thenReturn("1234");
        when(this.mockScmHandler.getNextRevisionId(pom4.getParentFile())).thenReturn("1234");
        when(this.mockScmHandler.getNextRevisionId(pom5.getParentFile())).thenReturn("1234");
        
        this.nonSnapshotMojo.execute();

        InOrder inOrder = inOrder(this.mockDependencyTreeProcessor, this.mockMavenPomHandler, this.mockScmHandler, this.mockWorkspaceTraverser);

        inOrder.verify(this.mockMavenPomHandler).readArtifact(pom1);
        inOrder.verify(this.mockMavenPomHandler).readArtifact(pom2);
        inOrder.verify(this.mockMavenPomHandler).readArtifact(pom3);
        inOrder.verify(this.mockMavenPomHandler).readArtifact(pom4);
        inOrder.verify(this.mockMavenPomHandler).readArtifact(pom5);

        inOrder.verify(this.mockDependencyTreeProcessor).buildDependencyTree(artifactList);
        inOrder.verify(this.mockDependencyTreeProcessor).applyBaseVersions(artifactList, this.nonSnapshotMojo.getBaseVersions());

        verify(this.mockScmHandler, never()).getRevisionId(pom1.getParentFile());
        inOrder.verify(this.mockScmHandler, times(1)).getRevisionId(pom2.getParentFile());
        verify(this.mockScmHandler, never()).getRevisionId(pom3.getParentFile());
        inOrder.verify(this.mockScmHandler, times(1)).getRevisionId(pom4.getParentFile());
        verify(this.mockScmHandler, never()).getRevisionId(pom5.getParentFile());

        inOrder.verify(this.mockMavenPomHandler, times(1)).updateArtifact(wsArtifact1, DEPENDENCY_UPDATE_STRATEGY.ALWAYS);
        verify(this.mockMavenPomHandler, never()).updateArtifact(wsArtifact2, DEPENDENCY_UPDATE_STRATEGY.ALWAYS);
        verify(this.mockMavenPomHandler, never()).updateArtifact(wsArtifact3, DEPENDENCY_UPDATE_STRATEGY.ALWAYS);
        inOrder.verify(this.mockMavenPomHandler, times(1)).updateArtifact(wsArtifact4, DEPENDENCY_UPDATE_STRATEGY.ALWAYS);
        inOrder.verify(this.mockMavenPomHandler, times(1)).updateArtifact(wsArtifact5, DEPENDENCY_UPDATE_STRATEGY.ALWAYS);

        inOrder.verify(this.mockScmHandler).commitFiles(Arrays.asList(pom1, pom4, pom5), "Nonsnapshot Plugin: Version of 3 artifacts updated");
    }

    @Test
    public void testNoUpdate() throws Exception {
        File pom1 = new File("test1/pom.xm");

        WorkspaceArtifact wsArtifact1 = new WorkspaceArtifact(pom1, "nonblocking.at", "test1", "1.0.0-1222");

        List<WorkspaceArtifact> artifactList = new ArrayList<WorkspaceArtifact>();
        artifactList.add(wsArtifact1);

        when(this.mockWorkspaceTraverser.findAllPomFiles(new File("target"), null)).thenReturn(Arrays.asList(pom1));
        when(this.mockMavenPomHandler.readArtifact(pom1)).thenReturn(wsArtifact1);
        when(this.mockDependencyTreeProcessor.buildDependencyTree(artifactList)).thenReturn(artifactList);

        when(this.mockScmHandler.getRevisionId(pom1.getParentFile())).thenReturn("1222");

        this.nonSnapshotMojo.execute();

        verify(this.mockMavenPomHandler).readArtifact(pom1);

        verify(this.mockDependencyTreeProcessor).buildDependencyTree(artifactList);
        verify(this.mockDependencyTreeProcessor).applyBaseVersions(artifactList, this.nonSnapshotMojo.getBaseVersions());

        verify(this.mockScmHandler).getRevisionId(pom1.getParentFile());

        verify(this.mockMavenPomHandler, times(0)).updateArtifact(wsArtifact1, DEPENDENCY_UPDATE_STRATEGY.ALWAYS);

        verify(this.mockScmHandler, times(0)).commitFiles(anyListOf(File.class), anyString());
    }

    @Test
    public void testUpdateDeferCommit() throws Exception {
        File pom1 = new File("test1/pom.xm");
        File pomsToCommit = new File("target/nonSnapshotPomsToCommit.txt");
        if (pomsToCommit.exists()) {
            pomsToCommit.delete();
        }

        WorkspaceArtifact wsArtifact1 = new WorkspaceArtifact(pom1, "at.nonblocking", "test3", "1.0.0-1222");
        wsArtifact1.setBaseVersion("1.0.0");
        List<WorkspaceArtifact> artifactList = new ArrayList<WorkspaceArtifact>();
        artifactList.add(wsArtifact1);

        this.nonSnapshotMojo.setDeferPomCommit(true);
        when(this.mockWorkspaceTraverser.findAllPomFiles(new File("target"), null)).thenReturn(Arrays.asList(pom1));
        when(this.mockMavenPomHandler.readArtifact(pom1)).thenReturn(wsArtifact1);
        when(this.mockDependencyTreeProcessor.buildDependencyTree(artifactList)).thenReturn(artifactList);

        when(this.mockScmHandler.getRevisionId(pom1.getParentFile())).thenReturn("1333");
        when(this.mockScmHandler.getNextRevisionId(pom1.getParentFile())).thenReturn("1444");
        when(this.mockScmHandler.isWorkingCopy(pom1.getParentFile())).thenReturn(true);

        
        this.nonSnapshotMojo.execute();

        assertTrue(pomsToCommit.exists());

        BufferedReader reader = new BufferedReader(new FileReader(pomsToCommit));
        assertNotNull(reader.readLine());
        assertNull(reader.readLine());
        reader.close();
    }

}
