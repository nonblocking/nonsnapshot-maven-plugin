package at.nonblocking.maven.nonsnapshot;
/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility to compare paths
 *
 * @author Juergen Kofler
 */
public final class PathUtil {

    private PathUtil() {
    }

    public static String relativePath(File baseDirectory, File file) throws IOException {
        Path basePath = Paths.get(baseDirectory.getCanonicalPath());
        Path filePath = Paths.get(file.getCanonicalPath());
        String relativeModuleDir = basePath.relativize(filePath).toString();
        return relativeModuleDir.replaceAll("\\\\", "/");
    }

}
