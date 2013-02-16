package at.nonblocking.maven.nonsnapshot;

import static junit.framework.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.impl.StaticLoggerBinder;

import at.nonblocking.maven.nonsnapshot.impl.WorkspaceTraverserDefaultImpl;

public class WorkspaceTraverserDefaultImplTest {

    @BeforeClass
    public static void setupLog() {
        StaticLoggerBinder.getSingleton().setLog(new DebugSystemStreamLog());
    }
    
    @Test
    public void testFindAllPomFiles() {        
        WorkspaceTraverser traverser = new WorkspaceTraverserDefaultImpl();
        
        List<File> pomFiles = traverser.findAllPomFiles(new File("src/test/resources/testworkspace"), null);
        
        assertEquals(4, pomFiles.size());
        assertTrue(pomFiles.get(0).exists());
        assertTrue(pomFiles.get(1).exists());
        assertTrue(pomFiles.get(2).exists());
        assertTrue(pomFiles.get(3).exists());
        assertEquals("pom.xml", pomFiles.get(0).getName());
        assertEquals("pom.xml", pomFiles.get(1).getName());
        assertEquals("pom.xml", pomFiles.get(2).getName());
        assertEquals("pom.xml", pomFiles.get(3).getName());
    }
    
    @Test
    public void testFindAllPomFilesWithExcludeFolders() {        
        WorkspaceTraverser traverser = new WorkspaceTraverserDefaultImpl();
        
        List<File> pomFiles = traverser.findAllPomFiles(new File("src/test/resources/testworkspace"), 
                Arrays.asList( "project1" ));
        
        assertEquals(1, pomFiles.size());
        assertTrue(pomFiles.get(0).exists());
        assertTrue(pomFiles.get(0).getAbsolutePath().contains("project2"));
        assertEquals("pom.xml", pomFiles.get(0).getName());
    }
    
}
