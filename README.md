Maven Nonsnapshot Plugin
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
The algorithm does the following:

 1. Scans the whole workspace directory for Maven artifacts (they might also be in sub-modules)
 2. Checks for all artifacts if there were changes (commits) since the last version update
 3. Marks all artifacts with changes (commits) as dirty
 4. Marks all artifacts as dirty which depend on dirty artifacts
 5. Rewrites the pom.xml for all dirty artifacts with the new versions
 6. Commits all changed pom.xml files

The generated artifact versions consist of a "base version", which can be configured, and the revision id as a qualifier.
For SVN the versions look like *1.1.1-7432*, where 7432 is the revision number. In step 2 of the algorithm is simply tested
if the current revision id differs from the revision id in the version.

Configuration
-------------

The plugin can be added to a separate (POM-) project or the aggregator project. Like this:

```xml
<plugin>
	<groupId>at.nonblocking</groupId>
	<artifactId>maven-nonsnapshot-plugin</artifactId>
	<version>1.3</version>
	<configuration>
		<workspaceDir>${basedir}/..</workspaceDir>
		<scmUser>${svnUser}</scmUser>
		<scmPassword>${svnPassword}</scmPassword>		
		<dependencyUpdateStrategy>ALWAYS</dependencyUpdateStrategy>
		<baseVersions>
			<baseVersion>
				<groupId>my.domain</groupId>
				<artifactId>my-parent-artifact</artifactId>
				<version>1.0.12</version>
			</baseVersion>
		</baseVersions>
		<excludeFolders>
			<excludeFolder>not-part-of-the-project</excludeFolder>						
		</excludeFolders>
	</configuration>
</plugin>

```

The *workspaceDir* is the root directory of all Maven artifacts which are part of the project.

The *baseVersions* state the base artifact versions the plugin shall assign. In the example above:
For the artifact *my-parent-artifact* and all its child modules newly assigned versions would start with *1.0.12*, 
e.g. *1.0.12-4567*.

The *dependencyUpdateStrategy* determines under which conditions a dependency to an artifact,
which is present in the workspace, shall be updated. For example, if the strategy is *SAME_MAJOR* and the artifact in the
workspace has a major version different from the one in the dependencies section of the POM, the dependency version
isn't updated. Valid strategies are:

 * ALWAYS (default)
 * SAME_MAJOR
 * SAME_MAJOR_MINOR
 * SAME_BASE_VERSION

Usage
-----

### Goals

The following goals are available:

 * *nonsnapshot:pretent*: Just shows how the versions would going to be changed. Does no actual POM rewrite or commit.
 * *nonsnapshot:updateVersions*: Rewrite all versions and commit. As soon the configuration parameter *deferPomCommit* is not set to true. In that case the commit is deferred.
 * *nonsnapshot:commitVersions*: Commits the POM files rewritten with the *updateVersions* goal. Makes only sense when *deferPomCommit" is set to true.   

### Using it on a CI Server

A fully automatized auto-release is of course only possible if you use this plugin within a CI server (e.g. *Jenkins*).

First add the following optional configuration parameters:

```xml
<deferPomCommit>true</deferPomCommit>
<dontFailOnCommit>true</dontFailOnCommit>
```

Then configure the job on the CI server like this:

 1. Revert and update the workspace
 2. Execute the goal *nonsnapshot:updateVersions*
 3. Build the whole project
 4. Deploy all generated artifacts to your remote Maven repository (e.g. *Artifactory*)
 5. Execute the goal *nonsnapshot:commitVersions*  

On *Jenkins* you can use pre-build and post-build steps for #2 and #5.

This configuration guarantees that the artifact versions in the dependencies of a POM file are always available from the remote Maven repository. 
(And so the developers no longer need all the Java projects in their workspace.)

### Making auto-deployments simple

If you've once setup the auto-release as described above, auto-deployment is pretty simple:

Just create a new release (POM-) artifact which includes all your deployment artifacts in the dependencies section. 
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

Limitations/TODO
----------------

Works currently **only with Subversion**. But support for GIT is planned.

Licence
-------

Licensed under the Apache License, Version 2.0.


