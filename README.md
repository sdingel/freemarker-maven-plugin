# FreeMarker Maven Plugin
[![GitHub Workflow Status (branch)](https://img.shields.io/github/workflow/status/sdingel/freemarker-maven-plugin/Build/main?label=Build)](https://github.com/sdingel/freemarker-maven-plugin/actions?query=workflow%3ABuild)
[![Maven Central](https://img.shields.io/maven-central/v/de.steffendingel/freemarker-maven-plugin?label=Maven%20Central)](https://search.maven.org/artifact/de.steffendingel/freemarker-maven-plugin)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/de.steffendingel/freemarker-maven-plugin?label=Snapshot%20at%20Sonatype%20OSSRH&server=https%3A%2F%2Foss.sonatype.org)](https://oss.sonatype.org/#nexus-search;gav~de.steffendingel~freemarker-maven-plugin~~~)
[![GitHub](https://img.shields.io/github/license/sdingel/freemarker-maven-plugin?label=License)](https://github.com/sdingel/freemarker-maven-plugin/blob/main/LICENSE)

[Maven](https://maven.apache.org/) Plugin for using the [FreeMarker](https://freemarker.apache.org/) template engine in builds

## Usage

To use the plugin, place the following `plugin` element into your `pom.xml` under `project/build/plugins` (adjust the `configuration` element to your needs, see below):

```
<project>

	<build>
	
		<plugins>
    
			<plugin>
				<groupId>de.steffendingel</groupId>
				<artifactId>freemarker-maven-plugin</artifactId>
				<version>1.0.0</version>
				<executions>
					<execution>
						<phase>generate-resources</phase>
						<goals>
							<goal>freemarker</goal>
						</goals>
						<configuration>
							<templateDirectory>${project.basedir}/src/main/freemarker</templateDirectory>
							<templateName>simple-template.ftl</templateName>
							<inputFiles>
								<directory>${project.basedir}/src/main/model</directory>
							</inputFiles>
							<outputDirectory>${project.build.directory}/html</outputDirectory>
							<outputExtension>html</outputExtension>
						</configuration>
					</execution>
				</executions>
			</plugin>

		</plugins>

	</build>

</project>
```

Currently, the plugin supports only one goal `freemarker:freemarker` applies a FreeMarker template to a set of JSON input files, writing one output file for each input file. By default, this goal binds to the Maven lifecylce phase `generate-resources`.

The goal supports the following configuration parameters:
* `inputFiles` (required): Set of JSON input files (models) for FreeMarker. The path of each file relative to the base directory of the file set is used to determine the output file path.
* `outputDirectory` (required): Directory to write the output files to. The output files are written in the same structure as the input files are found in the `inputFiles` file set.
* `outputExtension` (required): File extension of the output file (without the dot).
* `templateDirectory` (optional, default is `src/main/freemarker`): Directory where the FreeMarker templates are located.
* `templateName` (required): Name of the FreeMarker template file (including the extension).