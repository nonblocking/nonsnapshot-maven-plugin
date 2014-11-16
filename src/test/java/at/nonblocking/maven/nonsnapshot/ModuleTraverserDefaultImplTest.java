package at.nonblocking.maven.nonsnapshot;

import at.nonblocking.maven.nonsnapshot.impl.ModuleTraverserDefaultImpl;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.junit.Test;
import static junit.framework.Assert.*;

import java.io.File;
import java.util.List;

public class ModuleTraverserDefaultImplTest {

  @Test
  public void readModulesTest() {
    MavenProject mavenProject = new MavenProject();
    mavenProject.setFile(new File("src/test/resources/testworkspace/project1/pom.xml"));

    ModuleTraverser moduleTraverser = new ModuleTraverserDefaultImpl();

    List<Model> mavenModels = moduleTraverser.findAllModules(mavenProject);

    assertNotNull(mavenModels);
    assertEquals(4, mavenModels.size());

    assertEquals("project1", mavenModels.get(0).getArtifactId());
    assertEquals("module1", mavenModels.get(1).getArtifactId());
    assertEquals("module2", mavenModels.get(2).getArtifactId());
    assertEquals("project2", mavenModels.get(3).getArtifactId());
  }

}
