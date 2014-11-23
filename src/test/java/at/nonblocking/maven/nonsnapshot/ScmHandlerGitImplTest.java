package at.nonblocking.maven.nonsnapshot;

import at.nonblocking.maven.nonsnapshot.impl.ScmHandlerGitImpl;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.impl.StaticLoggerBinder;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Date;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@Ignore
public class ScmHandlerGitImplTest {

  private static final String SCM_PASSPHRASE = "???";
  private static final String GIT_TEST_FOLDER = "???";

  @BeforeClass
  public static void setupLog() {
    StaticLoggerBinder.getSingleton().setLog(new DebugSystemStreamLog());
  }

  @Test
  public void testCheckChangesSinceDate() throws Exception {
    File path = new File(GIT_TEST_FOLDER);

    ScmHandler scmService = new ScmHandlerGitImpl();
    scmService.init(new File(GIT_TEST_FOLDER), null, null);

    boolean changes = scmService.checkChangesSinceDate(path, new Date(114, 7, 26));

    assertTrue(changes);
  }

  @Test
  public void testNoWorkingCopy() throws Exception {
    File path = new File("target");

    ScmHandler scmService = new ScmHandlerGitImpl();
    scmService.init(new File(GIT_TEST_FOLDER), null, null);

    assertFalse(scmService.isWorkingCopy(path));
  }

  @Test
  public void testCommit() throws Exception {
    ScmHandler scmService = new ScmHandlerGitImpl();
    scmService.init(new File(GIT_TEST_FOLDER), null, SCM_PASSPHRASE);

    File file1 = new File(GIT_TEST_FOLDER + "/test1.txt");
    File file2 = new File(GIT_TEST_FOLDER + "/test2.txt");

    PrintWriter writer1 = new PrintWriter(file1);
    writer1.write(String.valueOf(System.currentTimeMillis()));
    writer1.close();

    PrintWriter writer2 = new PrintWriter(file2);
    writer2.write(String.valueOf(System.currentTimeMillis()));
    writer2.close();

    scmService.commitFiles(Arrays.asList(
        file1,
        file2
    ), "Test");
  }


}
