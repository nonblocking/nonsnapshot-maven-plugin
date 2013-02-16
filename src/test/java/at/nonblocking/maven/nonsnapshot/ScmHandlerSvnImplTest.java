package at.nonblocking.maven.nonsnapshot;

import static junit.framework.Assert.*;

import java.io.File;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.impl.StaticLoggerBinder;

import at.nonblocking.maven.nonsnapshot.impl.ScmHandlerSvnImpl;

@Ignore
public class ScmHandlerSvnImplTest {
    
    private static final String SVN_USER = "jenkins";
    private static final String SVN_PASSWORD = "???";

    @BeforeClass
    public static void setupLog() {
        StaticLoggerBinder.getSingleton().setLog(new DebugSystemStreamLog());
    }
    
    @Test
    public void testGetRevision() throws Exception {
        File path = new File("C:/Users/kofler/development/ws-nonsnapshot/build-test-one");

        ScmHandler scmService = new ScmHandlerSvnImpl();
        scmService.setCredentials(SVN_USER, SVN_PASSWORD);

        assertTrue(Long.valueOf(scmService.getRevisionId(path)) > 0);
    }

    @Test
    public void testNextRevisionNoWorkingCopy() throws Exception {
        File path = new File("C:/Users/kofler/development/ws-nonsnapshot/test");

        ScmHandler scmService = new ScmHandlerSvnImpl();
        scmService.setCredentials(SVN_USER, SVN_PASSWORD);
        
        assertFalse(scmService.isWorkingCopy(path));
    }

    @Test
    public void testNextRevision() throws Exception {
        File path = new File("C:/Users/kofler/development/ws-nonsnapshot/build-test-one");

        ScmHandler scmService = new ScmHandlerSvnImpl();
        scmService.setCredentials(SVN_USER, SVN_PASSWORD);
        
        long nextRevision = Long.valueOf(scmService.getNextRevisionId(path));

        assertTrue(nextRevision > 0);
    }
    
    @Test
    public void testCommit() throws Exception {        
        ScmHandler scmService = new ScmHandlerSvnImpl();
        scmService.setCredentials(SVN_USER, SVN_PASSWORD);
        
        scmService.commitFiles(Arrays.asList( 
                new File("C:/Users/kofler/development/ws-nonsnapshot/build-test-one/pom.xml"),
                new File("C:/Users/kofler/development/ws-nonsnapshot/build-test-two/pom.xml")), "test");
    }
    
    
}
