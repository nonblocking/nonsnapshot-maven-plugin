package at.nonblocking.maven.nonsnapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import at.nonblocking.maven.nonsnapshot.model.MavenArtifact;
import at.nonblocking.maven.nonsnapshot.model.MavenModule;
import at.nonblocking.maven.nonsnapshot.model.MavenModuleDependency;

public class NonSnapshotUpdateVersionsMojoTest {

    private NonSnapshotUpdateVersionsMojo nonSnapshotMojo = new NonSnapshotUpdateVersionsMojo();
    private ModuleTraverser mockModuleTraverser = mock(ModuleTraverser.class);
    private DependencyTreeProcessor mockDependencyTreeProcessor = mock(DependencyTreeProcessor.class);
    private MavenPomHandler mockMavenPomHandler = mock(MavenPomHandler.class);
    private ScmHandler mockScmHandler = mock(ScmHandler.class);
    private UpstreamDependencyHandler mockUpstreamDependencyHandler = mock(UpstreamDependencyHandler.class);

    @Before
    public void setupMojo() {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setFile(new File("target/pom.xml"));
        this.nonSnapshotMojo.setMavenProject(mavenProject);

        this.nonSnapshotMojo.setBaseVersion("1.0.13");
        this.nonSnapshotMojo.setScmUser("foo");
        this.nonSnapshotMojo.setScmPassword("bar");
        this.nonSnapshotMojo.setDeferPomCommit(false);
        this.nonSnapshotMojo.setModuleTraverser(mockModuleTraverser);
        this.nonSnapshotMojo.setDependencyTreeProcessor(this.mockDependencyTreeProcessor);
        this.nonSnapshotMojo.setMavenPomHandler(this.mockMavenPomHandler);
        this.nonSnapshotMojo.setScmHandler(this.mockScmHandler);
        this.nonSnapshotMojo.setUpstreamDependencyHandler(this.mockUpstreamDependencyHandler);
    }

    @Test
    public void testUpdateSvnRevisionQualifiers() throws Exception {
        Model model1 = new Model();
        Model model2 = new Model();
        Model model3 = new Model();
        Model model4 = new Model();
        Model model5 = new Model();

        File pom1 = new File("test1/pom.xm");
        File pom2 = new File("test2/pom.xm");
        File pom3 = new File("test3/pom.xm");
        File pom4 = new File("test4/pom.xm");
        File pom5 = new File("test5/pom.xm");

        MavenModule wsArtifact1 = new MavenModule(pom1, "nonblocking.at", "test1", "1.0.0-SNAPSHOT"); // Invalid version
        MavenModule wsArtifact2 = new MavenModule(pom2, "nonblocking.at", "test2", "1.1.0-1234");
        MavenModule wsArtifact3 = new MavenModule(pom3, "nonblocking.at", "test3", null); // Invalid version
        MavenModule wsArtifact4 = new MavenModule(pom4, "nonblocking.at", "test3", "2.1.0-FIX1-1234");
        MavenModule wsArtifact5 = new MavenModule(pom5, "nonblocking.at", "test3", "${test.version}"); // Invalid version

        List<MavenModule> artifactList = new ArrayList<>();
        artifactList.add(wsArtifact1);
        artifactList.add(wsArtifact2);
        artifactList.add(wsArtifact3);
        artifactList.add(wsArtifact4);
        artifactList.add(wsArtifact5);

        MavenProject mavenProject = new MavenProject();
        mavenProject.setFile(new File("target"));

        when(this.mockModuleTraverser.findAllModules(mavenProject, Collections.<Profile>emptyList())).thenReturn(Arrays.asList(model1, model2, model3, model4, model5));
        when(this.mockMavenPomHandler.readArtifact(model1)).thenReturn(wsArtifact1);
        when(this.mockMavenPomHandler.readArtifact(model2)).thenReturn(wsArtifact2);
        when(this.mockMavenPomHandler.readArtifact(model3)).thenReturn(wsArtifact3);
        when(this.mockMavenPomHandler.readArtifact(model4)).thenReturn(wsArtifact4);
        when(this.mockMavenPomHandler.readArtifact(model5)).thenReturn(wsArtifact5);

        when(this.mockScmHandler.isWorkingCopy(any(File.class))).thenReturn(true);
        when(this.mockScmHandler.getCurrentRevisionId(pom1.getParentFile())).thenReturn(1355L);
        when(this.mockScmHandler.getCurrentRevisionId(pom4.getParentFile())).thenReturn(1356L);
        when(this.mockScmHandler.getCurrentRevisionId(pom5.getParentFile())).thenReturn(1357L);
        when(this.mockScmHandler.checkChangesSinceRevision(pom4.getParentFile(), 1234, 1356)).thenReturn(true);

        this.nonSnapshotMojo.setScmType(SCM_TYPE.SVN);
        this.nonSnapshotMojo.setUseSvnRevisionQualifier(true);

        this.nonSnapshotMojo.execute();

        assertEquals("1.0.13-1355", wsArtifact1.getNewVersion());
        assertNull(wsArtifact2.getNewVersion());
        assertNotNull(wsArtifact3.getNewVersion());
        assertEquals("1.0.13-1356", wsArtifact4.getNewVersion());
        assertEquals("1.0.13-1357", wsArtifact5.getNewVersion());

        InOrder inOrder = inOrder(this.mockDependencyTreeProcessor, this.mockMavenPomHandler, this.mockScmHandler, this.mockModuleTraverser);

        inOrder.verify(this.mockMavenPomHandler).readArtifact(model1);
        inOrder.verify(this.mockMavenPomHandler).readArtifact(model2);
        inOrder.verify(this.mockMavenPomHandler).readArtifact(model3);
        inOrder.verify(this.mockMavenPomHandler).readArtifact(model4);
        inOrder.verify(this.mockMavenPomHandler).readArtifact(model5);

        inOrder.verify(this.mockDependencyTreeProcessor).buildDependencyTree(artifactList);

        inOrder.verify(this.mockScmHandler, times(1)).checkChangesSinceRevision(pom4.getParentFile(), 1234, 1356);

        inOrder.verify(this.mockMavenPomHandler, times(1)).updateArtifact(wsArtifact1);
        verify(this.mockMavenPomHandler, never()).updateArtifact(wsArtifact2);
        verify(this.mockMavenPomHandler, times(1)).updateArtifact(wsArtifact3);
        inOrder.verify(this.mockMavenPomHandler, times(1)).updateArtifact(wsArtifact4);
        inOrder.verify(this.mockMavenPomHandler, times(1)).updateArtifact(wsArtifact5);

        inOrder.verify(this.mockScmHandler).commitFiles(Arrays.asList(pom1, pom3, pom4, pom5), ScmHandler.NONSNAPSHOT_COMMIT_MESSAGE_PREFIX + " Version of 4 artifacts updated");
    }

    @Test
    public void testUpdateTimestampQualifiers() throws Exception {
        String pattern = "yyyyMMdd";
        Date currentTime = new Date();
        String currentTimestamp = new SimpleDateFormat(pattern).format(currentTime);

        Model model1 = new Model();
        Model model2 = new Model();
        Model model3 = new Model();
        Model model4 = new Model();
        Model model5 = new Model();

        File pom1 = new File("test1/pom.xm");
        File pom2 = new File("test2/pom.xm");
        File pom3 = new File("test3/pom.xm");
        File pom4 = new File("test4/pom.xm");
        File pom5 = new File("test5/pom.xm");

        MavenModule wsArtifact1 = new MavenModule(pom1, "nonblocking.at", "test1", "1.0.0-SNAPSHOT"); // Invalid version
        MavenModule wsArtifact2 = new MavenModule(pom2, "nonblocking.at", "test2", "1.1.0-" + currentTimestamp);
        MavenModule wsArtifact3 = new MavenModule(pom3, "nonblocking.at", "test3", null); // Invalid version
        MavenModule wsArtifact4 = new MavenModule(pom4, "nonblocking.at", "test3", "2.1.0-FIX1-1234");
        MavenModule wsArtifact5 = new MavenModule(pom5, "nonblocking.at", "test3", "${test.version}"); // Invalid version

        List<MavenModule> artifactList = new ArrayList<>();
        artifactList.add(wsArtifact1);
        artifactList.add(wsArtifact2);
        artifactList.add(wsArtifact3);
        artifactList.add(wsArtifact4);
        artifactList.add(wsArtifact5);

        MavenProject mavenProject = new MavenProject();
        mavenProject.setFile(new File("target"));

        when(this.mockModuleTraverser.findAllModules(mavenProject, Collections.<Profile>emptyList())).thenReturn(Arrays.asList(model1, model2, model3, model4, model5));
        when(this.mockMavenPomHandler.readArtifact(model1)).thenReturn(wsArtifact1);
        when(this.mockMavenPomHandler.readArtifact(model2)).thenReturn(wsArtifact2);
        when(this.mockMavenPomHandler.readArtifact(model3)).thenReturn(wsArtifact3);
        when(this.mockMavenPomHandler.readArtifact(model4)).thenReturn(wsArtifact4);
        when(this.mockMavenPomHandler.readArtifact(model5)).thenReturn(wsArtifact5);

        when(this.mockScmHandler.getLastCommitDate(pom1.getParentFile())).thenReturn(currentTime);
        when(this.mockScmHandler.getLastCommitDate(pom2.getParentFile())).thenReturn(currentTime);
        when(this.mockScmHandler.getLastCommitDate(pom3.getParentFile())).thenReturn(currentTime);
        when(this.mockScmHandler.getLastCommitDate(pom4.getParentFile())).thenReturn(currentTime);
        when(this.mockScmHandler.getLastCommitDate(pom5.getParentFile())).thenReturn(currentTime);

        when(this.mockScmHandler.checkChangesSinceRevision(pom2.getParentFile(), 1234, 10000)).thenReturn(false);
        when(this.mockScmHandler.checkChangesSinceRevision(pom4.getParentFile(), 1234, 10000)).thenReturn(true);

        when(this.mockScmHandler.isWorkingCopy(any(File.class))).thenReturn(true);

        this.nonSnapshotMojo.setUseSvnRevisionQualifier(false);
        this.nonSnapshotMojo.setTimestampQualifierPattern(pattern);
        this.nonSnapshotMojo.execute();

        assertEquals("1.0.13-" + currentTimestamp, wsArtifact1.getNewVersion());
        assertNull(wsArtifact2.getNewVersion());
        assertNotNull(wsArtifact3.getNewVersion());
        assertEquals("1.0.13-" + currentTimestamp, wsArtifact4.getNewVersion());
        assertEquals("1.0.13-" + currentTimestamp, wsArtifact5.getNewVersion());

        InOrder inOrder = inOrder(this.mockDependencyTreeProcessor, this.mockMavenPomHandler, this.mockScmHandler, this.mockModuleTraverser);

        inOrder.verify(this.mockMavenPomHandler).readArtifact(model1);
        inOrder.verify(this.mockMavenPomHandler).readArtifact(model2);
        inOrder.verify(this.mockMavenPomHandler).readArtifact(model3);
        inOrder.verify(this.mockMavenPomHandler).readArtifact(model4);
        inOrder.verify(this.mockMavenPomHandler).readArtifact(model5);

        inOrder.verify(this.mockDependencyTreeProcessor).buildDependencyTree(artifactList);

        when(this.mockScmHandler.checkChangesSinceRevision(pom2.getParentFile(), 1234, 10000)).thenReturn(false);
        when(this.mockScmHandler.checkChangesSinceRevision(pom4.getParentFile(), 1234, 10000)).thenReturn(true);

        inOrder.verify(this.mockMavenPomHandler, times(1)).updateArtifact(wsArtifact1);
        verify(this.mockMavenPomHandler, never()).updateArtifact(wsArtifact2);
        verify(this.mockMavenPomHandler, times(1)).updateArtifact(wsArtifact3);
        inOrder.verify(this.mockMavenPomHandler, times(1)).updateArtifact(wsArtifact4);
        inOrder.verify(this.mockMavenPomHandler, times(1)).updateArtifact(wsArtifact5);

        inOrder.verify(this.mockScmHandler).commitFiles(Arrays.asList(pom1, pom3, pom4, pom5), ScmHandler.NONSNAPSHOT_COMMIT_MESSAGE_PREFIX + " Version of 4 artifacts updated");
    }

    @Test
    public void testIncrementalBuildScript() throws Exception {

        File scriptFile;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            scriptFile = new File("target/nonsnapshotBuildIncremental.bat");
        } else {
            scriptFile = new File("target/nonsnapshotBuildIncremental.sh");
        }

        scriptFile.delete();

        Model model1 = new Model();
        Model model2 = new Model();
        Model model3 = new Model();
        Model model4 = new Model();
        Model model5 = new Model();

        File pom1 = new File("test1/pom.xm");
        File pom2 = new File("test2/pom.xm");
        File pom3 = new File("test3/pom.xm");
        File pom4 = new File("test4/pom.xm");
        File pom5 = new File("test5/pom.xm");

        MavenModule wsArtifact1 = new MavenModule(pom1, "nonblocking.at", "test1", "1.0.0-SNAPSHOT"); // Invalid version
        MavenModule wsArtifact2 = new MavenModule(pom2, "nonblocking.at", "test2", "1.1.0-1234");
        MavenModule wsArtifact3 = new MavenModule(pom3, "nonblocking.at", "test3", null); // Invalid version
        MavenModule wsArtifact4 = new MavenModule(pom4, "nonblocking.at", "test3", "2.1.0-FIX1-1234");
        MavenModule wsArtifact5 = new MavenModule(pom5, "nonblocking.at", "test3", "${test.version}"); // Invalid version

        List<MavenModule> artifactList = new ArrayList<>();
        artifactList.add(wsArtifact1);
        artifactList.add(wsArtifact2);
        artifactList.add(wsArtifact3);
        artifactList.add(wsArtifact4);
        artifactList.add(wsArtifact5);

        MavenProject mavenProject = new MavenProject();
        mavenProject.setFile(new File("target"));

        when(this.mockModuleTraverser.findAllModules(mavenProject, Collections.<Profile>emptyList())).thenReturn(Arrays.asList(model1, model2, model3, model4, model5));
        when(this.mockMavenPomHandler.readArtifact(model1)).thenReturn(wsArtifact1);
        when(this.mockMavenPomHandler.readArtifact(model2)).thenReturn(wsArtifact2);
        when(this.mockMavenPomHandler.readArtifact(model3)).thenReturn(wsArtifact3);
        when(this.mockMavenPomHandler.readArtifact(model4)).thenReturn(wsArtifact4);
        when(this.mockMavenPomHandler.readArtifact(model5)).thenReturn(wsArtifact5);

        when(this.mockScmHandler.checkChangesSinceRevision(pom2.getParentFile(), 1234, 1355)).thenReturn(false);
        when(this.mockScmHandler.checkChangesSinceRevision(pom4.getParentFile(), 1234, 1355)).thenReturn(true);

        when(this.mockScmHandler.isWorkingCopy(any(File.class))).thenReturn(true);
        ;

        when(this.mockScmHandler.getCurrentRevisionId(pom1.getParentFile())).thenReturn(1355L);
        when(this.mockScmHandler.getCurrentRevisionId(pom4.getParentFile())).thenReturn(1355L);
        when(this.mockScmHandler.getCurrentRevisionId(pom5.getParentFile())).thenReturn(1355L);

        this.nonSnapshotMojo.setScmType(SCM_TYPE.SVN);
        this.nonSnapshotMojo.setUseSvnRevisionQualifier(true);
        this.nonSnapshotMojo.setGenerateIncrementalBuildScripts(true);

        this.nonSnapshotMojo.execute();

        assertTrue(scriptFile.exists());

        assertTrue(FileUtils.fileRead(scriptFile).contains(" --projects ../test1,../test3,../test4,../test5 "));
    }

    @Test
    public void testChangedProjectsPropertyFile() throws Exception {

        File propertyFile = new File("target/nonsnapshotChangedProjects.properties");
        propertyFile.delete();

        Model model1 = new Model();
        Model model2 = new Model();
        Model model3 = new Model();
        Model model4 = new Model();
        Model model5 = new Model();

        File pom1 = new File("test1/pom.xm");
        File pom2 = new File("test2/pom.xm");
        File pom3 = new File("test3/pom.xm");
        File pom4 = new File("test4/pom.xm");
        File pom5 = new File("test5/pom.xm");

        MavenModule wsArtifact1 = new MavenModule(pom1, "nonblocking.at", "test1", "1.0.0-SNAPSHOT"); // Invalid version
        MavenModule wsArtifact2 = new MavenModule(pom2, "nonblocking.at", "test2", "1.1.0-1234");
        MavenModule wsArtifact3 = new MavenModule(pom3, "nonblocking.at", "test3", null); // Invalid version
        MavenModule wsArtifact4 = new MavenModule(pom4, "nonblocking.at", "test3", "2.1.0-FIX1-1234");
        MavenModule wsArtifact5 = new MavenModule(pom5, "nonblocking.at", "test3", "${test.version}"); // Invalid version

        List<MavenModule> artifactList = new ArrayList<>();
        artifactList.add(wsArtifact1);
        artifactList.add(wsArtifact2);
        artifactList.add(wsArtifact3);
        artifactList.add(wsArtifact4);
        artifactList.add(wsArtifact5);

        MavenProject mavenProject = new MavenProject();
        mavenProject.setFile(new File("target"));

        when(this.mockModuleTraverser.findAllModules(mavenProject, Collections.<Profile>emptyList())).thenReturn(Arrays.asList(model1, model2, model3, model4, model5));
        when(this.mockMavenPomHandler.readArtifact(model1)).thenReturn(wsArtifact1);
        when(this.mockMavenPomHandler.readArtifact(model2)).thenReturn(wsArtifact2);
        when(this.mockMavenPomHandler.readArtifact(model3)).thenReturn(wsArtifact3);
        when(this.mockMavenPomHandler.readArtifact(model4)).thenReturn(wsArtifact4);
        when(this.mockMavenPomHandler.readArtifact(model5)).thenReturn(wsArtifact5);

        when(this.mockScmHandler.checkChangesSinceRevision(pom2.getParentFile(), 1234, 1355)).thenReturn(false);
        when(this.mockScmHandler.checkChangesSinceRevision(pom4.getParentFile(), 1234, 1355)).thenReturn(true);

        when(this.mockScmHandler.isWorkingCopy(any(File.class))).thenReturn(true);
        ;

        when(this.mockScmHandler.getCurrentRevisionId(pom1.getParentFile())).thenReturn(1355L);
        when(this.mockScmHandler.getCurrentRevisionId(pom4.getParentFile())).thenReturn(1355L);
        when(this.mockScmHandler.getCurrentRevisionId(pom5.getParentFile())).thenReturn(1355L);

        this.nonSnapshotMojo.setScmType(SCM_TYPE.SVN);
        this.nonSnapshotMojo.setUseSvnRevisionQualifier(true);
        this.nonSnapshotMojo.setGenerateChangedProjectsPropertyFile(true);

        this.nonSnapshotMojo.execute();

        assertTrue(propertyFile.exists());

        assertTrue(FileUtils.fileRead(propertyFile).contains("nonsnapshot.changed.projects=../test1,../test3,../test4,../test5"));
    }

    @Test
    public void testUpdateUpstreamDependencyVersions() throws Exception {

        Model model1 = new Model();
        Model model2 = new Model();

        File pom1 = new File("test1/pom.xm");
        File pom2 = new File("test2/pom.xm");

        MavenModule wsArtifact1 = new MavenModule(pom1, "nonblocking.at", "test1", "1.1.0-1234");
        final MavenModule wsArtifact2 = new MavenModule(pom2, "nonblocking.at", "test2", "2.2.0-1234");

        MavenArtifact upstreamDep1 = new MavenArtifact("nonblocking.at", "test3", "3.3.3-1235");

        wsArtifact2.getDependencies().add(new MavenModuleDependency(-1, wsArtifact1));
        wsArtifact2.getDependencies().add(new MavenModuleDependency(-1, upstreamDep1));

        final List<MavenModule> artifactList = new ArrayList<>();
        artifactList.add(wsArtifact1);
        artifactList.add(wsArtifact2);

        MavenProject mavenProject = new MavenProject();
        mavenProject.setFile(new File("target"));

        List<String> upstreamDependencyString = Arrays.asList("at.nonblocking:test1:LATEST");

        ProcessedUpstreamDependency upstreamDependency = new ProcessedUpstreamDependency(null, null, -1, -1, -1);
        List<ProcessedUpstreamDependency> upstreamDependencies = Arrays.asList(upstreamDependency);

        when(this.mockModuleTraverser.findAllModules(mavenProject, Collections.<Profile>emptyList())).thenReturn(Arrays.asList(model1, model2));

        when(this.mockMavenPomHandler.readArtifact(model1)).thenReturn(wsArtifact1);
        when(this.mockMavenPomHandler.readArtifact(model2)).thenReturn(wsArtifact2);

        when(this.mockDependencyTreeProcessor.markAllArtifactsDirtyWithDirtyDependencies(artifactList)).then(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                wsArtifact2.setDirty(true);
                return false;
            }
        });

        when(this.mockScmHandler.checkChangesSinceRevision(pom1.getParentFile(), 1234, 10000)).thenReturn(false);
        when(this.mockScmHandler.checkChangesSinceRevision(pom2.getParentFile(), 1234, 10000)).thenReturn(false);
        when(this.mockScmHandler.isWorkingCopy(any(File.class))).thenReturn(true);
        when(this.mockScmHandler.getCurrentRevisionId(pom2.getParentFile())).thenReturn(6677L);

        when(this.mockUpstreamDependencyHandler.processDependencyList(upstreamDependencyString)).thenReturn(upstreamDependencies);
        when(this.mockUpstreamDependencyHandler.findMatch(upstreamDep1, upstreamDependencies)).thenReturn(upstreamDependency);
        when(this.mockUpstreamDependencyHandler.resolveLatestVersion(upstreamDep1, upstreamDependency, null, null, null)).thenReturn("5.0.1-1234");

        this.nonSnapshotMojo.setScmType(SCM_TYPE.SVN);
        this.nonSnapshotMojo.setUseSvnRevisionQualifier(true);
        this.nonSnapshotMojo.setUpstreamDependencies(upstreamDependencyString);

        this.nonSnapshotMojo.execute();

        verify(this.mockUpstreamDependencyHandler, times(1)).processDependencyList(upstreamDependencyString);
        verify(this.mockUpstreamDependencyHandler, times(1)).findMatch(upstreamDep1, upstreamDependencies);
        verify(this.mockUpstreamDependencyHandler, times(1)).resolveLatestVersion(upstreamDep1, upstreamDependency, null, null, null);

        assertNull(wsArtifact1.getNewVersion());
        assertEquals("1.0.13-6677", wsArtifact2.getNewVersion());
    }

    @Test
    public void testUpdateUpstreamDependencyVersionsParent() throws Exception {

        Model model1 = new Model();
        Model model2 = new Model();

        File pom1 = new File("test1/pom.xm");
        File pom2 = new File("test2/pom.xm");

        MavenModule wsArtifact1 = new MavenModule(pom1, "nonblocking.at", "test1", "1.1.0-1234");
        final MavenModule wsArtifact2 = new MavenModule(pom2, "nonblocking.at", "test2", "2.2.0-1234");

        MavenArtifact upstreamDep1 = new MavenArtifact("nonblocking.at", "test3", "3.3.3-1235");

        wsArtifact2.getDependencies().add(new MavenModuleDependency(-1, wsArtifact1));
        wsArtifact2.setParent(upstreamDep1);

        final List<MavenModule> artifactList = new ArrayList<>();
        artifactList.add(wsArtifact1);
        artifactList.add(wsArtifact2);

        MavenProject mavenProject = new MavenProject();
        mavenProject.setFile(new File("target"));

        List<String> upstreamDependencyString = Arrays.asList("at.nonblocking:test1:LATEST");

        ProcessedUpstreamDependency upstreamDependency = new ProcessedUpstreamDependency(null, null, -1, -1, -1);
        List<ProcessedUpstreamDependency> upstreamDependencies = Arrays.asList(upstreamDependency);

        when(this.mockModuleTraverser.findAllModules(mavenProject, Collections.<Profile>emptyList())).thenReturn(Arrays.asList(model1, model2));

        when(this.mockMavenPomHandler.readArtifact(model1)).thenReturn(wsArtifact1);
        when(this.mockMavenPomHandler.readArtifact(model2)).thenReturn(wsArtifact2);

        when(this.mockDependencyTreeProcessor.markAllArtifactsDirtyWithDirtyDependencies(artifactList)).then(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                wsArtifact2.setDirty(true);
                return false;
            }
        });

        when(this.mockScmHandler.checkChangesSinceRevision(pom1.getParentFile(), 1234, 10000)).thenReturn(false);
        when(this.mockScmHandler.checkChangesSinceRevision(pom2.getParentFile(), 1234, 10000)).thenReturn(false);
        when(this.mockScmHandler.isWorkingCopy(any(File.class))).thenReturn(true);
        when(this.mockScmHandler.getCurrentRevisionId(pom2.getParentFile())).thenReturn(6677L);

        when(this.mockUpstreamDependencyHandler.processDependencyList(upstreamDependencyString)).thenReturn(upstreamDependencies);
        when(this.mockUpstreamDependencyHandler.findMatch(upstreamDep1, upstreamDependencies)).thenReturn(upstreamDependency);
        when(this.mockUpstreamDependencyHandler.resolveLatestVersion(upstreamDep1, upstreamDependency, null, null, null)).thenReturn("5.0.1-1234");

        this.nonSnapshotMojo.setScmType(SCM_TYPE.SVN);
        this.nonSnapshotMojo.setUseSvnRevisionQualifier(true);
        this.nonSnapshotMojo.setUpstreamDependencies(upstreamDependencyString);

        this.nonSnapshotMojo.execute();

        verify(this.mockUpstreamDependencyHandler, times(1)).processDependencyList(upstreamDependencyString);
        verify(this.mockUpstreamDependencyHandler, times(1)).findMatch(upstreamDep1, upstreamDependencies);
        verify(this.mockUpstreamDependencyHandler, times(1)).resolveLatestVersion(upstreamDep1, upstreamDependency, null, null, null);

        assertNull(wsArtifact1.getNewVersion());
        assertEquals("1.0.13-6677", wsArtifact2.getNewVersion());
    }


    @Test
    public void testNoUpdate() throws Exception {
        Model model1 = new Model();

        File pom1 = new File("test1/pom.xm");

        MavenModule wsArtifact1 = new MavenModule(pom1, "nonblocking.at", "test1", "1.0.0-1222");

        List<MavenModule> artifactList = new ArrayList<>();
        artifactList.add(wsArtifact1);

        MavenProject mavenProject = new MavenProject();
        mavenProject.setFile(new File("target"));

        when(this.mockModuleTraverser.findAllModules(mavenProject, Collections.<Profile>emptyList())).thenReturn(Arrays.asList(model1));
        when(this.mockMavenPomHandler.readArtifact(model1)).thenReturn(wsArtifact1);

        this.nonSnapshotMojo.setScmType(SCM_TYPE.SVN);
        this.nonSnapshotMojo.setUseSvnRevisionQualifier(true);

        this.nonSnapshotMojo.execute();

        verify(this.mockMavenPomHandler).readArtifact(model1);
        verify(this.mockDependencyTreeProcessor).buildDependencyTree(artifactList);
        verify(this.mockMavenPomHandler, times(0)).updateArtifact(wsArtifact1);
        verify(this.mockScmHandler, times(0)).commitFiles(anyList(), anyString());
    }

    @Test
    public void testUpdateDeferCommit() throws Exception {
        Model model1 = new Model();

        File pom1 = new File("test1/pom.xm");

        File dirtyModulesRegistry = new File("target/nonSnapshotDirtyModules.txt");
        if (dirtyModulesRegistry.exists()) {
            dirtyModulesRegistry.delete();
        }

        MavenModule wsArtifact1 = new MavenModule(pom1, "at.nonblocking", "test3", "1.0.0-1222");
        List<MavenModule> artifactList = new ArrayList<>();
        artifactList.add(wsArtifact1);

        MavenProject mavenProject = new MavenProject();
        mavenProject.setFile(new File("target"));

        this.nonSnapshotMojo.setDeferPomCommit(true);
        when(this.mockModuleTraverser.findAllModules(mavenProject, Collections.<Profile>emptyList())).thenReturn(Arrays.asList(model1));
        when(this.mockMavenPomHandler.readArtifact(model1)).thenReturn(wsArtifact1);

        when(this.mockScmHandler.getLastCommitDate(pom1.getParentFile())).thenReturn(new Date());
        when(this.mockScmHandler.getCurrentRevisionId(pom1.getParentFile())).thenReturn(1444L);
        when(this.mockScmHandler.isWorkingCopy(any(File.class))).thenReturn(true);

        this.nonSnapshotMojo.execute();

        assertTrue(dirtyModulesRegistry.exists());

        BufferedReader reader = new BufferedReader(new FileReader(dirtyModulesRegistry));
        assertNotNull(reader.readLine());
        assertNull(reader.readLine());
        reader.close();
    }

}
