/*
 * Copyright 2023 Eric Maziade
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.maziade.cmdtool.commands;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.maziade.cmdtool.utils.RendererUtility;
import com.maziade.cmdtool.utils.RendererUtility.Appender;

import jakarta.annotation.PostConstruct;

// --------------------------------------------------------------------------------------------------------------------------------
@ShellComponent
@ShellCommandGroup("Tool Context")
public class ToolContextCommands implements CommandLineRunner
{
	private static final String PROPERTY_CURRENT_PATH = "tool.currentPath";
	
	@Autowired RendererUtility rendererUtility;

	Path currentPath = Paths.get("").toAbsolutePath();
	Path propFile = Paths.get("tool.status");
	Properties status = new Properties();

	//--------------------------------------------------------------------------------------------------------------------------------
	@PostConstruct
	public void init()
	{
		if (Files.exists(propFile))
		{
			try (InputStream in = new FileInputStream(propFile.toFile()))
			{
				status.load(in);
				
				String propPath = status.getProperty(PROPERTY_CURRENT_PATH);
				if (propPath != null)
				{
					Path path = Path.of(propPath);
					if (Files.exists(path) && Files.isDirectory(path))
						currentPath = path;
				}
			}
			catch(IOException e)
			{
				LoggerFactory.getLogger(getClass()).error("Could not load {}", propFile);
			}
		}
		 
	}

	//--------------------------------------------------------------------------------------------------------------------------------
	/**
	 * Get the current path set for the tool
	 * @return current path
	 */
	public Path currentPath()
	{
		return currentPath;
	}

	//--------------------------------------------------------------------------------------------------------------------------------
	/**
	 * Get a resolved path from a path received in argument, taking into consideration the current path
	 * @param path path
	 * @return resolved path (might not exist)
	 */
	public Path getPath(String path)
	{
		Path a = Path.of(path);
		if (a.isAbsolute())
			return a;

		return Path.of(currentPath.toString(), path);
	}

	//--------------------------------------------------------------------------------------------------------------------------------
	@ShellMethod(key = "pwd", value = "Show current working path for test tool")
	public String pwd()
	{
		Appender out = rendererUtility.start();

		appendCurrentPath(out);

		return out.toString();
	}

	//--------------------------------------------------------------------------------------------------------------------------------
	void appendCurrentPath(Appender out)
	{
		out.append("Current path: ");
		out.append(RendererUtility.COLOR_SYMBOL, currentPath.toString());
	}
	
	void saveCurrentStatus()
	{
		try (Writer out = new FileWriter(propFile.toFile()))
		{
			status.store(out, PROPERTY_CURRENT_PATH);
		}
		catch(IOException e)
		{
			LoggerFactory.getLogger(getClass()).error("Could not save {}", propFile);
		}
	}

	//--------------------------------------------------------------------------------------------------------------------------------
	@ShellMethod(key = "cd", value = "Change current working path")
	public String cd(String path)
	{
		Appender out = rendererUtility.start();

		Path newPath = getPath(path);

		if (!Files.exists(newPath))
			out.error(String.format("%s does not exist", newPath.toString()));
		else if (!Files.isDirectory(newPath))
			out.error(String.format("%s is not a directory", newPath.toString()));
		else
		{
			currentPath = newPath;
			status.setProperty(PROPERTY_CURRENT_PATH, currentPath.toString());
			saveCurrentStatus();
			appendCurrentPath(out);
		}

		return out.toString();
	}
	
	//--------------------------------------------------------------------------------------------------------------------------------
	@ShellMethod(key = "ls", value = "List files in current director")
	public String ls(@ShellOption(defaultValue = "")String arg)
	{
		Appender out = rendererUtility.start();

		appendCurrentPath(out);
		out.endLine();

		out.indent( () -> {

			for (var file : currentPath.toFile().listFiles())
			{
				out.startLine();

				if (file.isDirectory())
					out.setColor(AnsiColor.BRIGHT_BLUE);
				else
					out.setColor(AnsiColor.BRIGHT_GREEN);
				
				out.append(file.getName());
				out.endLine();
			}
		});

		return out.toString();
	}

	//--------------------------------------------------------------------------------------------------------------------------------
	@Override
	public void run(String... args) throws Exception
	{
		// Done running.
	}
}