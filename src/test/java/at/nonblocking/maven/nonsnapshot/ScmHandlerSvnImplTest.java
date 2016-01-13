package at.nonblocking.maven.nonsnapshot;

import static junit.framework.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.Date;

import org.junit.Ignore;
import org.junit.Test;

import at.nonblocking.maven.nonsnapshot.impl.ScmHandlerSvnImpl;

@Ignore
public class ScmHandlerSvnImplTest {

  private static final String SVN_USER = "???";
  private static final String SVN_PASSWORD = "???";
  private static final String SVN_TEST_FOLDER = "???";

  @Test
  public void testCheckChangesSinceRevision() throws Exception {
    File path = new File(SVN_TEST_FOLDER);

    ScmHandler scmService = new ScmHandlerSvnImpl();
    scmService.init(null, SVN_USER, SVN_PASSWORD, null);

    boolean changes = scmService.checkChangesSinceRevision(path, 26505);

    assertTrue(changes);
  }

  @Test
  public void testCheckChangesSinceDate() throws Exception {
    File path = new File(SVN_TEST_FOLDER);

    ScmHandler scmService = new ScmHandlerSvnImpl();
    scmService.init(null, SVN_USER, SVN_PASSWORD, null);

    boolean changes = scmService.checkChangesSinceDate(path, new Date(114, 9, 26));

    assertTrue(changes);
  }

  @Test
  public void testNoWorkingCopy() throws Exception {
    File path = new File("target");

    ScmHandler scmService = new ScmHandlerSvnImpl();
    scmService.init(null, SVN_USER, SVN_PASSWORD, null);

    assertFalse(scmService.isWorkingCopy(path));
  }

  @Test
  public void testGetCurrentRevisionId() throws Exception {
    File path = new File(SVN_TEST_FOLDER);

    ScmHandler scmService = new ScmHandlerSvnImpl();
    scmService.init(null, SVN_USER, SVN_PASSWORD, null);

    long currentRevision = scmService.getCurrentRevisionId(path);

    assertTrue(currentRevision > 0);
  }

  @Test
  public void testGetLastCommitDate() throws Exception {
    File path = new File(SVN_TEST_FOLDER);

    ScmHandler scmService = new ScmHandlerSvnImpl();
    scmService.init(null, SVN_USER, SVN_PASSWORD, null);

    Date lastCommitDate = scmService.getLastCommitDate(path);

    assertNotNull(lastCommitDate);
  }

  @Test
  public void testCommit() throws Exception {
    ScmHandler scmService = new ScmHandlerSvnImpl();
    scmService.init(null, SVN_USER, SVN_PASSWORD, null);

    scmService.commitFiles(Arrays.asList(
        new File("/Users/jkofler/development/ws-nonsnapshot/build-test-one/pom.xml"),
        new File("/Users/jkofler/development/ws-nonsnapshot/build-test-two/pom.xml")), "test");
  }


}
