/*
 * Copyright 2012-2013 the original author or authors.
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
package at.nonblocking.maven.nonsnapshot;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Handle the access to the SCM (Source Code Management) system.
 *
 * @author Juergen Kofler
 */
public interface ScmHandler {

  String NONSNAPSHOT_COMMIT_MESSAGE_PREFIX = "Nonsnapshot Plugin:";

  /**
   * Check if the project directory is a even a working copy
   *
   * @param moduleDirectory File
   * @return boolean
   */
  boolean isWorkingCopy(File moduleDirectory);

  /**
   * Check if there has been changes since given revisionId
   *
   * @param moduleDirectory File
   * @param revisionId String
   * @return boolean
   */
  boolean checkChangesSinceRevision(File moduleDirectory, String revisionId);

  /**
   * Check if there has been changes since given date
   *
   * @param moduleDirectory File
   * @param date Date
   * @return boolean
   */
  boolean checkChangesSinceDate(File moduleDirectory, Date date);

  /**
   * Get the next revision number this path would get in case of a commit.
   * <br/>
   * Only supported by SVN (for all I know). Other implementations may throw NotImplementedExceptions.
   *
   * @param path File
   * @return String
   */
  String getNextRevisionId(File path);

  /**
   * Commit the given path to the remote repository.
   *
   * @param files         List<File>
   * @param commitMessage String
   */
  void commitFiles(List<File> files, String commitMessage);

  /**
   * Set the repository credentials
   *
   * @param baseDir File
   * @param scmUser     String
   * @param scmPassword String
   * @param properties Properties
   */
  void init(File baseDir, String scmUser, String scmPassword, Properties properties);

}
