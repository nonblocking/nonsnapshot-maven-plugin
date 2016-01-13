package at.nonblocking.maven.nonsnapshot;

import static junit.framework.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import at.nonblocking.maven.nonsnapshot.impl.DependencyTreeProcessorDefaultImpl;
import at.nonblocking.maven.nonsnapshot.model.MavenArtifact;
import at.nonblocking.maven.nonsnapshot.model.MavenModule;
import at.nonblocking.maven.nonsnapshot.model.MavenModuleDependency;

public class DependencyTreeProcessorDefaultImplTest {

  @Test
  public void testBuildDependencyTree() {
    MavenModule wsArtifact1 = new MavenModule(null, "at.nonblocking.at", "parent", "1.0.0");
    MavenModule wsArtifact2 = new MavenModule(null, "at.nonblocking.at", "test1", "1.0.0");
    MavenModule wsArtifact3 = new MavenModule(null, "at.nonblocking.at", "test1-module1", "1.0.0");
    MavenModule wsArtifact4 = new MavenModule(null, "at.nonblocking.at", "test1-module2", "1.0.0");
    MavenModule wsArtifact5 = new MavenModule(null, "at.nonblocking.at", "test2", "1.0.0");
    MavenModule wsArtifact6 = new MavenModule(null, "at.nonblocking.at", "plugin1", "1.0.0");

    wsArtifact1.getDependencies().add(new MavenModuleDependency(0, new MavenArtifact("at.nonblocking.at", "plugin1", "1.0.0")));
    wsArtifact1.getDependencies().add(new MavenModuleDependency(0, new MavenArtifact("junit", "junit", "4.7")));
    wsArtifact2.setParent(new MavenArtifact("at.nonblocking.at", "parent", "1.0.0"));
    wsArtifact2.getDependencies().add(new MavenModuleDependency(0, new MavenArtifact("at.nonblocking.at", "test2", "1.0.0")));
    wsArtifact3.setParent(new MavenArtifact("at.nonblocking.at", "test1", "1.0.0"));
    wsArtifact4.setParent(new MavenArtifact("at.nonblocking.at", "test1", "1.0.0"));
    wsArtifact5.setParent(new MavenArtifact("at.nonblocking.at", "parent", "1.0.0"));

    List<MavenModule> artifacts = new ArrayList<MavenModule>();
    artifacts.add(wsArtifact1);
    artifacts.add(wsArtifact2);
    artifacts.add(wsArtifact3);
    artifacts.add(wsArtifact4);
    artifacts.add(wsArtifact5);
    artifacts.add(wsArtifact6);

    DependencyTreeProcessor dependencyTreeProcessor = new DependencyTreeProcessorDefaultImpl();

    dependencyTreeProcessor.buildDependencyTree(artifacts);

    dependencyTreeProcessor.printMavenModuleTree(wsArtifact1, System.out);
  }

  @Test
  public void testMarkAllArtifactsDirtyWithDirtyDependencies() {
    MavenModule wsArtifact1 = new MavenModule(null, "at.nonblocking.at", "test1", "1.0.0");
    MavenModule wsArtifact2 = new MavenModule(null, "at.nonblocking.at", "test2", "1.0.0");
    MavenModule wsArtifact3 = new MavenModule(null, "at.nonblocking.at", "plugin1", "1.0.0");

    wsArtifact2.getDependencies().add(new MavenModuleDependency(0, wsArtifact3));
    wsArtifact3.setDirty(true);

    List<MavenModule> artifacts = new ArrayList<MavenModule>();
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

    dependencyTreeProcessor.printMavenModuleTree(wsArtifact1, System.out);
  }

  @Test
  public void testMarkAllArtifactsDirtyWithDirtyDependenciesRecursive() {
    MavenModule wsArtifact1 = new MavenModule(null, "at.nonblocking.at", "test1", "1.0.0");
    MavenModule wsArtifact2 = new MavenModule(null, "at.nonblocking.at", "test2", "1.0.0");
    MavenModule wsArtifact3 = new MavenModule(null, "at.nonblocking.at", "plugin1", "1.0.0");
    MavenModule wsArtifact4 = new MavenModule(null, "at.nonblocking.at", "test", "1.0.0");

    //4 -> 2 -> 3 (dirty)
    wsArtifact4.getDependencies().add(new MavenModuleDependency(0, wsArtifact2));
    wsArtifact2.getDependencies().add(new MavenModuleDependency(0, wsArtifact3));
    wsArtifact3.setDirty(true);

    List<MavenModule> artifacts = new ArrayList<MavenModule>();
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

    dependencyTreeProcessor.printMavenModuleTree(wsArtifact1, System.out);
  }
}
