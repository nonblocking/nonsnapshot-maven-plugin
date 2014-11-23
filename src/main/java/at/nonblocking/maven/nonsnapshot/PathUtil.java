package at.nonblocking.maven.nonsnapshot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtil {

  private PathUtil() {}

  public static String relativePath(File baseDirectory, File file) throws IOException {
    Path basePath = Paths.get(baseDirectory.getCanonicalPath());
    Path filePath = Paths.get(file.getCanonicalPath());
    String relativeModuleDir = basePath.relativize(filePath).toString();
    return relativeModuleDir.replaceAll("\\\\", "/");
  }

}
