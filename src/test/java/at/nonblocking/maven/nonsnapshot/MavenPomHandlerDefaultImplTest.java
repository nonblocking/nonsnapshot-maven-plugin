package at.nonblocking.maven.nonsnapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Test;

import at.nonblocking.maven.nonsnapshot.impl.MavenPomHandlerDefaultImpl;
import at.nonblocking.maven.nonsnapshot.model.MavenModule;

public class MavenPomHandlerDefaultImplTest {

    @Test
    public void testReadArtifact() throws Exception {
        File pomFile = new File("target/test-pom.xml");
        IOUtil.copy(new FileReader("src/test/resources/test-pom.xml"), new FileOutputStream(pomFile));

        MavenPomHandler pomHandler = new MavenPomHandlerDefaultImpl();

        MavenModule wsArtifact = pomHandler.readArtifact(pomFile);

        assertEquals("at.nonblocking", wsArtifact.getGroupId());
        assertEquals("test1", wsArtifact.getArtifactId());
        assertEquals("1.0.0-SNAPSHOT", wsArtifact.getVersion());
        assertEquals(8, wsArtifact.getVersionLocation());
        assertFalse(wsArtifact.isInsertVersionTag());
        assertNull(wsArtifact.getParent());

        assertEquals(6, wsArtifact.getDependencies().size());

        assertEquals(14, wsArtifact.getDependencies().get(0).getVersionLocation());
        assertEquals(20, wsArtifact.getDependencies().get(1).getVersionLocation());
        assertEquals(26, wsArtifact.getDependencies().get(2).getVersionLocation());
        assertEquals(35, wsArtifact.getDependencies().get(3).getVersionLocation());
        assertEquals(40, wsArtifact.getDependencies().get(4).getVersionLocation());
        assertEquals(46, wsArtifact.getDependencies().get(5).getVersionLocation());
    }

    @Test
    public void testReadArtifactWithParent() throws Exception {
        File pomFile = new File("target/test-pom-parent.xml");
        IOUtil.copy(new FileReader("src/test/resources/test-pom-parent.xml"), new FileOutputStream(pomFile));

        MavenPomHandler pomHandler = new MavenPomHandlerDefaultImpl();

        MavenModule wsArtifact = pomHandler.readArtifact(pomFile);

        assertEquals("at.nonblocking", wsArtifact.getGroupId());
        assertEquals("test1", wsArtifact.getArtifactId());
        assertEquals("1.0.0-SNAPSHOT", wsArtifact.getVersion());
        assertEquals(12, wsArtifact.getVersionLocation());

        assertNotNull(wsArtifact.getParent());

        assertEquals(9, wsArtifact.getParentVersionLocation());
    }

    @Test
    public void testReadAndUpdateArtifact() throws Exception {
        File pomFile = new File("target/test-pom.xml");
        IOUtil.copy(new FileReader("src/test/resources/test-pom.xml"), new FileOutputStream(pomFile));

        MavenPomHandler pomHandler = new MavenPomHandlerDefaultImpl();

        MavenModule wsArtifact = pomHandler.readArtifact(pomFile);

        wsArtifact.setDirty(true);
        wsArtifact.setNewVersion("1.1.1-12345");

        MavenModule dependentArtifact = new MavenModule(null, "at.nonblocking.at", "test2", "2.0.5-123");
        dependentArtifact.setDirty(true);
        dependentArtifact.setNewVersion("5.0.1-555");

        wsArtifact.getDependencies().get(1).setArtifact(dependentArtifact);

        pomHandler.updateArtifact(wsArtifact);

        Model pom = new MavenXpp3Reader().read(new FileInputStream(pomFile));

        assertEquals("1.1.1-12345", pom.getVersion());
        assertEquals("5.0.1-555", pom.getDependencies().get(1).getVersion());
    }

    @Test
    public void testReadAndUpdateArtifactWithParent() throws Exception {
        File pomFile = new File("target/test-pom-parent.xml");
        IOUtil.copy(new FileReader("src/test/resources/test-pom-parent.xml"), new FileOutputStream(pomFile));

        MavenPomHandler pomHandler = new MavenPomHandlerDefaultImpl();

        MavenModule wsArtifact = pomHandler.readArtifact(pomFile);

        wsArtifact.setDirty(true);
        wsArtifact.setNewVersion("1.1.1-12345");

        MavenModule parentArtifact = new MavenModule(null, "at.nonblocking.at", "parent-test", "1.4.5-123");
        parentArtifact.setDirty(true);
        parentArtifact.setNewVersion("3.3.3-456");

        wsArtifact.setParent(parentArtifact);

        pomHandler.updateArtifact(wsArtifact);

        Model pom = new MavenXpp3Reader().read(new FileInputStream(pomFile));

        assertEquals("1.1.1-12345", pom.getVersion());
        assertEquals("3.3.3-456", pom.getParent().getVersion());
    }

    @Test
    public void testReadAndUpdateArtifactWithNoVersion() throws Exception {
        File pomFile = new File("target/test-pom-noversion.xml");
        IOUtil.copy(new FileReader("src/test/resources/test-pom-noversion.xml"), new FileOutputStream(pomFile));

        MavenPomHandler pomHandler = new MavenPomHandlerDefaultImpl();

        MavenModule wsArtifact = pomHandler.readArtifact(pomFile);

        assertNotNull(wsArtifact.getVersion());
        assertTrue(wsArtifact.isInsertVersionTag());

        wsArtifact.setDirty(true);
        wsArtifact.setNewVersion("1.1.1-12345");

        pomHandler.updateArtifact(wsArtifact);

        Model pom = new MavenXpp3Reader().read(new FileInputStream(pomFile));

        assertEquals("1.1.1-12345", pom.getVersion());
    }

    @Test
    public void testReadAndUpdateArtifactInsertVersionTag() throws Exception {
        File pomFile = new File("target/test-pom.xml");
        IOUtil.copy(new FileReader("src/test/resources/test-pom.xml"), new FileOutputStream(pomFile));

        MavenPomHandler pomHandler = new MavenPomHandlerDefaultImpl();

        MavenModule wsArtifact = pomHandler.readArtifact(pomFile);

        wsArtifact.setDirty(true);
        wsArtifact.setNewVersion("1.1.1-12345");

        MavenModule dependentArtifact = new MavenModule(null, "at.nonblocking.at", "test2", "2.0.5-123");
        dependentArtifact.setDirty(true);
        dependentArtifact.setNewVersion("5.0.1-555");
        dependentArtifact.setInsertVersionTag(true);

        wsArtifact.getDependencies().get(1).setArtifact(dependentArtifact);

        pomHandler.updateArtifact(wsArtifact);

        Model pom = new MavenXpp3Reader().read(new FileInputStream(pomFile));

        assertEquals("1.1.1-12345", pom.getVersion());
        assertEquals("5.0.1-555", pom.getDependencies().get(1).getVersion());
    }
}
