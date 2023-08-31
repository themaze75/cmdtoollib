# cmd tool lib

This is to help getting a quick command-line tool to help trouble shoot or diagnose various types of projects.

While ant is nice and fine, I often ended up wanting to have a series of tools in a shell-like command.

While spring-shell is neat for that, I found that I was repeating a few steps that could be commonalized in a library.

This version is done without pretense and published as it might help someone else - including future me - to build quick and dirty tools.

## Quick And Dirty?

Yes.  This aims to quickly build a dev / diagnosis tools.  Error handling is crude and expeditive, as the tool expects to communicate with a technical resource.

## Quick Start

### App Startup

Here's a quick pom file to get you started.

You'll need to create your main app class.


```
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>3.1.2</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>
	
	<name>name of your project</name>
	<groupId>your group</groupId>
	<artifactId>your project</artifactId>
	<packaging>jar</packaging>

	<version>0.0.1-SNAPSHOT</version>

	<properties>
		<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

		<maven.compiler.release>17</maven.compiler.release>

		<org.junit.jupiter.version>5.9.2</org.junit.jupiter.version>

		<project.mainClass>Class To Your Main App</project.mainClass>
	</properties>

	<dependencies>
		<dependency>
			<groupId>com.maziade.tools</groupId>
			<artifactId>cmdtoollib</artifactId>
			<version>0.0.1</version>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<configuration>
					<source>${java.version.source}</source>
					<target>${java.version.target}</target>
				</configuration>
			</plugin>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
				<executions>
					<execution>
						<configuration>
							<mainClass>${project.mainClass}</mainClass>
						</configuration>
					</execution>
				</executions>
			</plugin>
		</plugins>

		<pluginManagement>
			<plugins>
				<plugin>
					<groupId>org.apache.maven.plugins</groupId>
					<artifactId>maven-surefire-plugin</artifactId>
					<configuration>
						<argLine>--add-opens java.base/java.util=ALL-UNNAMED
								--add-opens java.base/java.io=ALL-UNNAMED
								--add-opens java.base/sun.nio.cs=ALL-UNNAMED</argLine>
					</configuration>
					<dependencies>
						<dependency>
							<groupId>org.junit.jupiter</groupId>
							<artifactId>junit-jupiter-engine</artifactId>
							<version>${org.junit.jupiter.version}</version>
						</dependency>
					</dependencies>
				</plugin>
			</plugins>
		</pluginManagement>

	</build>
</project>
```

### Main App Class

```
package com.your.package;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"your.project.package.here", "com.maziade.cmdtool.utils", "com.maziade.cmdtool.commands"})
public class YourProject
{
	//--------------------------------------------------------------------------------------------------------------------------------
	@SuppressWarnings("resource")
	public static void main(String[] args) 
	{
		SpringApplication.run(YourProject.class, args);
	}
}

```

#### Package Scanning

To get the Utility component from this package, you'll need to have this package scanned.
You can also get the tool context package to allow for simple navigation within the file system.  
It also persists and restores and configurations from the library.

Add a component scan annotation to your main app to add the parts of this library you want: 

``` 
@ComponentScan({"com.maziade.cmdtool.utils","com.maziade.cmdtool.commands"})
``` 

## Adding Your Commands

You'll need to brush up on Spring Shell, but for a quick hello-world:

```
/// --------------------------------------------------------------------------------------------------------------------------------
@ShellComponent
public class Commands implements CommandLineRunner
{
	@Autowired RendererUtility renderer;

	//--------------------------------------------------------------------------------------------------------------------------------
	@ShellMethod(key = "hello-world")
	public String helloWorld(@ShellOption(defaultValue = "spring") String arg)
	{
		return renderer.start().append(AnsiColor.BRIGHT_BLUE, "Hello world").append(" ").append(arg).toString();
	}

	//--------------------------------------------------------------------------------------------------------------------------------
	@Override
	public void run(String... args) throws Exception
	{
		// Done running.
	}
}
``` 

## Customization

### application.properties

You can tweak a few things about Spring Shell.

For that, you'll need an `application.properties` file at the root of your resources folder.

Most configurable items, you should be able to do in code.

Here's one to get you started.

```
spring.main.banner-mode=off

# DETECT(default), ALWAYS, NEVER (Ansi output detection won't work for eclipse console.)  
spring.output.ansi.enabled=ALWAYS
```

### logback.xml

You need to configure logback if you want something in the log output.

For that, you'll need a `logback.xml` file at the root of your resources folder.

Here's a setup that might help you get started. 

```
<!DOCTYPE configuration>
<configuration>
	<logger name="com.maziade" level="WARN" />

	<!--file appender-->
	<appender name="file" class="ch.qos.logback.core.rolling.RollingFileAppender">
		<File>console.log</File>
		<encoder>
			<pattern>%d{&quot;yyyy-MM-dd HH:mm:ss,SSS&quot;} \(%t\) %-5p [%c] - %m%n</pattern>
		</encoder>
		<rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
			<fileNamePattern>console-%d{yyyy-MM-dd}.log</fileNamePattern>
		</rollingPolicy>
	</appender>

	<appender name="stdout" class="ch.qos.logback.core.ConsoleAppender">
		<encoder>
			<pattern>%d{&quot;yyyy-MM-dd HH:mm:ss,SSS&quot;} \(%t\) %-5p [%c] - %m%n</pattern>
		</encoder>
	</appender>

	<root level="WARN">
		<appender-ref ref="file" />
		<appender-ref ref="stdout" />
	</root>
</configuration>
```

## Run from command line

From the pom template, you'll build an executable jar.

From maven, you can easily start with `mvn spring-boot:run`


## TODO

- can we make this a lib so we can use it as a parent pom project insead of spring boot?