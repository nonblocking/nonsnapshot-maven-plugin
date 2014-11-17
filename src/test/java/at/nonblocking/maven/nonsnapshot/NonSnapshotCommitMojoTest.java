package at.nonblocking.maven.nonsnapshot;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;

import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.impl.StaticLoggerBinder;

public class NonSnapshotCommitMojoTest {

  private NonSnapshotCommitMojo nonSnapshotMojo = new NonSnapshotCommitMojo();
  private ModuleTraverser mockModuleTraverser = mock(ModuleTraverser.class);
  private DependencyTreeProcessor mockDependencyTreeProcessor = mock(DependencyTreeProcessor.class);
  private MavenPomHandler mockMavenPomHandler = mock(MavenPomHandler.class);
  private ScmHandler mockScmHandler = mock(ScmHandler.class);

  @BeforeClass
  public static void setupLog() {
    StaticLoggerBinder.getSingleton().setLog(new DebugSystemStreamLog());
  }

  @Before
  public void setupMojo() {
    MavenProject mavenProject = new MavenProject();
    mavenProject.setFile(new File("target/pom.xml"));
    this.nonSnapshotMojo.setMavenProject(mavenProject);

    this.nonSnapshotMojo.setScmUser("foo");
    this.nonSnapshotMojo.setScmPassword("bar");
    this.nonSnapshotMojo.setDeferPomCommit(false);
    this.nonSnapshotMojo.setModuleTraverser(this.mockModuleTraverser);
    this.nonSnapshotMojo.setDependencyTreeProcessor(this.mockDependencyTreeProcessor);
    this.nonSnapshotMojo.setMavenPomHandler(this.mockMavenPomHandler);
    this.nonSnapshotMojo.setScmHandler(this.mockScmHandler);
  }

  @Test
  public void testCommit() throws Exception {
    File pomFilesToCommit = new File("target/nonSnapshotDirtyModules.txt");
    File pom1 = new File("target/test1/pom.xml");
    File pom2 = new File("target/test2/pom.xml");
    File pom3 = new File("target/test3/pom.xml");

    PrintWriter writer = new PrintWriter(pomFilesToCommit);
    writer.write("test1" + System.getProperty("line.separator"));
    writer.write("test2" + System.getProperty("line.separator"));
    writer.write("test3" + System.getProperty("line.separator"));
    writer.close();

    this.nonSnapshotMojo.execute();

    verify(this.mockScmHandler).commitFiles(Arrays.asList(pom1, pom2, pom3), "Nonsnapshot Plugin: Version of 3 modules updated");

    assertFalse(pomFilesToCommit.exists());
  }

  @Test
  public void testCommitIgnoreDuplicateEntries() throws Exception {
    File pomFilesToCommit = new File("target/nonSnapshotDirtyModules.txt");
    File pom1 = new File("target/test1/pom.xml");
    File pom2 = new File("target/test2/pom.xml");
    File pom3 = new File("target/test3/pom.xml");

    PrintWriter writer = new PrintWriter(pomFilesToCommit);
    writer.write("test1" + System.getProperty("line.separator"));
    writer.write("test2" + System.getProperty("line.separator"));
    writer.write("test3" + System.getProperty("line.separator"));
    writer.write("test2" + System.getProperty("line.separator"));
    writer.close();

    this.nonSnapshotMojo.execute();

    verify(this.mockScmHandler).commitFiles(Arrays.asList(pom1, pom2, pom3), "Nonsnapshot Plugin: Version of 3 modules updated");

    assertFalse(pomFilesToCommit.exists());
  }

  @Test
  public void testDontFailOnCommitTrue() throws Exception {
    File pomFilesToCommit = new File("target/nonSnapshotDirtyModules.txt");
    File pom1 = new File("target/test1/pom.xml");

    PrintWriter writer = new PrintWriter(pomFilesToCommit);
    writer.write(pom1.getPath() + System.getProperty("line.separator"));
    writer.close();

    doThrow(new RuntimeException("test")).when(this.mockScmHandler).commitFiles(anyList(), eq("Nonsnapshot Plugin: Version of 1 artifacts updated"));

    this.nonSnapshotMojo.setDontFailOnCommit(true);

    this.nonSnapshotMojo.execute();
  }

  @Test(expected = RuntimeException.class)
  public void testDontFailOnCommitFalse() throws Exception {
    File pomFilesToCommit = new File("target/nonSnapshotDirtyModules.txt");
    File pom1 = new File("target/test1/pom.xml");

    PrintWriter writer = new PrintWriter(pomFilesToCommit);
    writer.write(pom1.getPath() + System.getProperty("line.separator"));
    writer.close();

    doThrow(new RuntimeException("test")).when(this.mockScmHandler).commitFiles(anyList(), eq("Nonsnapshot Plugin: Version of 1 modules updated"));

    this.nonSnapshotMojo.setDontFailOnCommit(false);

    this.nonSnapshotMojo.execute();
  }

}
