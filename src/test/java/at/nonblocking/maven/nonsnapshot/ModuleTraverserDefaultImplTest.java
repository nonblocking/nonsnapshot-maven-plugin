package at.nonblocking.maven.nonsnapshot;

import at.nonblocking.maven.nonsnapshot.impl.ModuleTraverserDefaultImpl;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;
import org.junit.Test;

import static junit.framework.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class ModuleTraverserDefaultImplTest {

    @Test
    public void readModulesNoProfilesTest() {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setFile(new File("src/test/resources/testworkspace/project1/pom.xml"));

        ModuleTraverser moduleTraverser = new ModuleTraverserDefaultImpl();

        List<Model> mavenModels = moduleTraverser.findAllModules(mavenProject, null);

        assertNotNull(mavenModels);
        assertEquals(4, mavenModels.size());

        assertEquals("project1", mavenModels.get(0).getArtifactId());
        assertEquals("module1", mavenModels.get(1).getArtifactId());
        assertEquals("module2", mavenModels.get(2).getArtifactId());
        assertEquals("project2", mavenModels.get(3).getArtifactId());
    }

    @Test
    public void readModulesInProfilesTest() {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setFile(new File("src/test/resources/testworkspace/project1/pom.xml"));

        ModuleTraverser moduleTraverser = new ModuleTraverserDefaultImpl();

        Profile activeProfile = new Profile();
        activeProfile.setId("foo");
        List<Model> mavenModels = moduleTraverser.findAllModules(mavenProject, Arrays.asList(activeProfile));

        assertNotNull(mavenModels);
        assertEquals(5, mavenModels.size());

        assertEquals("project1", mavenModels.get(0).getArtifactId());
        assertEquals("module1", mavenModels.get(1).getArtifactId());
        assertEquals("module2", mavenModels.get(2).getArtifactId());
        assertEquals("project2", mavenModels.get(3).getArtifactId());
        assertEquals("module3", mavenModels.get(4).getArtifactId());
    }
}
