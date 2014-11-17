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

/**
 * Handle the access to the SCM (Source Code Management) system.
 *
 * @author Juergen Kofler
 */
public interface ScmHandler {

  /**
   * Check if the project directory is a even a working copy
   *
   * @param projectDirectory File
   * @return boolean
   */
  boolean isWorkingCopy(File projectDirectory);

  /**
   * Get the revision number for given model directory.
   * <br/>
   * If nothing has changed since the last build, this MUST return the same revision id as
   * getNextRevisionId() in the previous build.
   * <br/><br/>
   * For SVN this could just be the current revision number.
   * For GIT this could be the last (abbreviated) commit hash not caused by this maven plugin.
   *
   * @param moduleDirectory File
   * @return String
   */
  String getRevisionId(File moduleDirectory);

  /**
   * Get the timestamp of the last commit in given directory.
   *
   * @param moduleDirectory File
   * @return Date
   */
  Date getLastCommitTimestamp(File moduleDirectory);

  /**
   * Get the next revision number this path would get in case of a commit.
   * <br/>
   * The return string will be the qualifier of the next POM version.
   * <br/><br/>
   * For SVN this could be the current root revision number + 1.
   * For GIT this could be just the last (abbreviated) commit hash.
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
   * @param scmUser     String
   * @param scmPassword String
   */
  void setCredentials(String scmUser, String scmPassword);

}
