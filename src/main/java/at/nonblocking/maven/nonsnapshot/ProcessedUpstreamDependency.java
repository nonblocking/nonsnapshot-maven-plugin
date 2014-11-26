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

import java.util.regex.Pattern;

public class ProcessedUpstreamDependency {

  private Pattern groupPattern;
  private Pattern artifactPattern;
  private Integer versionMajor;
  private Integer versionMinor;
  private Integer versionIncrement;

  public ProcessedUpstreamDependency(Pattern groupPattern, Pattern artifactPattern, Integer versionMajor, Integer versionMinor, Integer versionIncrement) {
    this.groupPattern = groupPattern;
    this.artifactPattern = artifactPattern;
    this.versionMajor = versionMajor;
    this.versionMinor = versionMinor;
    this.versionIncrement = versionIncrement;
  }

  public Pattern getGroupPattern() {
    return groupPattern;
  }

  public void setGroupPattern(Pattern groupPattern) {
    this.groupPattern = groupPattern;
  }

  public Pattern getArtifactPattern() {
    return artifactPattern;
  }

  public void setArtifactPattern(Pattern artifactPattern) {
    this.artifactPattern = artifactPattern;
  }

  public Integer getVersionMajor() {
    return versionMajor;
  }

  public void setVersionMajor(Integer versionMajor) {
    this.versionMajor = versionMajor;
  }

  public Integer getVersionMinor() {
    return versionMinor;
  }

  public void setVersionMinor(Integer versionMinor) {
    this.versionMinor = versionMinor;
  }

  public Integer getVersionIncrement() {
    return versionIncrement;
  }

  public void setVersionIncrement(Integer versionIncrement) {
    this.versionIncrement = versionIncrement;
  }

  @Override
  public String toString() {
    return "UpstreamDependency{" +
        "groupPattern=" + groupPattern +
        ", artifactPattern=" + artifactPattern +
        ", versionMajor=" + versionMajor +
        ", versionMinor=" + versionMinor +
        ", versionIncrement=" + versionIncrement +
        '}';
  }
}
