Nonsnapshot Maven Plugin
========================

Motivation
----------

When working on a huge project with hundreds of Maven artifacts depending on each other, 
dealing with SNAPSHOT versions really becomes unhandy. The main reasons are:

1. The developers need to have all projects in the workspace to make sure the dependency resolution of the IDE works
2. Manually versioning 100+ projects means a lot of effort
3. It is not possible to reproduce the exact state of a project to any given time, when you depend on SNAPSHOT versions
4. It makes a fully automatized deployment complicated, since you need a manual versioning step instead just pushing the latest build onto your servers

How this plugin works
---------------------

The aim of this plugin is to get rid of SNAPSHOT versions and periodically auto-release the whole project with all artifacts.

The algorithm works as follows:

1. Collect all modules recursively.
2. Check all modules for changes (commits) since the last version update. If commits exist mark the module as dirty.
3. Resolve the latest upstream dependencies versions. If any newer version are available update the version accordingly
    and mark the module as dirty.
4. Mark all modules as dirty which have dirty dependencies.
5. Bump the version of all dirty modules.
6. Rewrite the pom.xml of all modules with a new version.
7. Optional: Generate a script for an incremental build of all dirty modules (Maven > 3.2.1 required).
8. Commit all changed pom.xml files to SCM. This step can be deferred and done by a second goal.

The generated artifact versions consist of a "base version", which can be configured,
and the build timestamp or revision id (SVN only) as a qualifier. Examples:

* 1.2.3-20141125
* 1.2.3-123456

Configuration
-------------

The plugin can be added to a separate (POM-) project or your main aggregator project. Like this:

```xml
<plugin>
	<groupId>at.nonblocking</groupId>
	<artifactId>nonsnapshot-maven-plugin</artifactId>
	<version>2.0</version>
	<configuration>
		<baseVersion>1.2.3</baseVersion>
		<scmType>SVN</scmType>
		<scmUser>${scmUser}</scmUser>
		<scmPassword>${scmPassword}</scmPassword>
		<useSvnRevisionQualifier>true</useSvnRevisionQualifier>
		<deferPomCommit>true</deferPomCommit>
		<generateIncrementalBuildScripts>true</generateIncrementalBuildScripts>
		<dontFailOnCommit>false</dontFailOnCommit>
		<dontFailOnUpstreamVersionResolution>false</dontFailOnUpstreamVersionResolution>
		<upstreamDependencies>
			<upstreamDependency>at.nonblocking:*:LATEST</upstreamDependency>
			<!-- Examples -->
			<!-- <upstreamDependency>at.nonblocking:*:2.10</upstreamDependency> -->
			<!-- <upstreamDependency>at.nonblocking:test-test2:2.10.3</upstreamDependency>-->
			<!-- <upstreamDependency>at.nonblocking:test-*:LATEST</upstreamDependency>-->
		</upstreamDependencies>
	</configuration>
</plugin>

```

### Notes

* Supported SCM systems are SVN and GIT, default is SVN.
* By the default timestamps are used as qualifiers
* An upstream dependency is defined as *groupId:artifactId:baseVersion*. Whereas *groupId* and *artifactId* can contain
   wildcards. The *baseVersion* is the "prefix" of allowed versions. Examples:
    * 2.8 -> Look for the latest version that starts with 2.8, e.g. 2.8.1-20140203
    * 2.8.3 -> Look for the latest version that starts with 2.8.3
    * LATEST -> Always look for the latest (non snapshot!) version
* The upstream dependency list is processed in order of their definition and the first match is taken. That allows
  it to define an exceptions from a wildcard rule like this:

  ```xml
  	<upstreamDependency>at.nonblocking:test:2.3.4</upstreamDependency>
  	<upstreamDependency>at.nonblocking:*:LATEST</upstreamDependency>
  ```

Usage
-----

### Goals

The following goals are available:

* *nonsnapshot:pretent*: Just shows how the versions would going to be changed. Does no actual POM rewrite or commit.
* *nonsnapshot:updateVersions*: Rewrite all versions and commit. As soon the configuration parameter *deferPomCommit* is not set to true. In that case the commit is deferred.
* *nonsnapshot:commitVersions*: Commits the POM files rewritten by the *updateVersions* goal. Makes only sense when *deferPomCommit" is set to true.

### Using it on a CI Server

A fully automatized auto-release is of course only possible if you use this plugin within a CI server (e.g. *Jenkins*).

First add the following optional configuration parameter:

```xml
<deferPomCommit>true</deferPomCommit>
```

Then configure the job on your CI server like this:

1. Revert and update the workspace
2. Execute the goal *nonsnapshot:updateVersions*
3. Build the whole project
    * Option 1: Jenkins Maven Build
    * Option 2: Execute script: *./nonsnapshotBuildIncremental.sh install*
4. Deploy all generated artifacts to your remote Maven repository (e.g. *Artifactory*)
5. Execute the goal *nonsnapshot:commitVersions*

On *Jenkins* you can use pre-build and post-build steps for #2 and #5.

This configuration guarantees that the module versions in the dependencies section of a POM file are always available from the remote Maven repository.
(And so the developers no longer need all the Java projects in their workspace.)

### Continuous Deployment

For Continuous Deployment just create a new release (POM-) module which includes
all your deployment modules in the dependencies section.

For example:

```xml
<dependencies>
		<dependency>
			<groupId>my.domain</groupId>
			<artifactId>webapp1</artifactId>
			<version>1.0.12-4567</version>
			<type>war</type>
		</dependency>
		<dependency>
			<groupId>my.domain</groupId>
			<artifactId>webapp2</artifactId>
			<version>1.0.12-4555</version>
			<type>war</type>
		</dependency>
</dependencies>
```

Since the dependency versions are rewritten with each plugin execution, they will always point to the latest version.
You can now use the *maven-dependency-plugin* or the *maven-assembly-plugin* together with *ant* to copy all your deployment
artifact to the server.

Licence
-------

Licensed under the Apache License, Version 2.0.


