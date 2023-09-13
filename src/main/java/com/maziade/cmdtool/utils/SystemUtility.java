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
package com.maziade.cmdtool.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

/**
 * Helps to run system commands, trying to be a system-agnostic as we can.
 */
@Component
public class SystemUtility
{
	static final String[] CMD_EXT = ((Supplier<List<String>>) () -> {

		// For windows, use Environment Variables[PATHEXT] : .COM;.EXE;.BAT;.CMD;.VBS;.VBE;.JS;.JSE;.WSF;.WSH;.MSC
		final String ext = System.getenv("PATHEXT");
		final List<String> exts = new ArrayList<>();
		if (!Strings.isBlank(ext))
			exts.addAll(List.of(ext.split(";")));

		// For linux, extensions are meaningless... we're just trying to find the thing without you telling us... we could do `.` and `.sh`
		exts.add(".");
		exts.add(".sh");

		return exts;
	}).get().toArray(new String[] {});

	/**
	 * Run a command on the operating system
	 * @param cmd command
	 * @param cmdArgs arguments
	 * @param processor processor that will parse the output
	 */
	public void runCommand(Path cmd, String cmdArgs, LineProcessor processor)
	{
		runCommand(buildCommand(cmd, cmdArgs), processor, false);
	}

	/**
	 * Finds the absolute path to the command (works out extensions for Windows)
	 * @param home home folder
	 * @param binPath path to binary under the home folder
	 * @param cmdBaseName name of the command (without extension for Windows)
	 * @return absolute path, if the command was found
	 */
	public Optional<Path> firstPathOf(File home, String binPath, String cmdBaseName)
	{
		final String homePath = home.getAbsolutePath();

		for (String ext : CMD_EXT)
		{
			Path cmd = Path.of(homePath, binPath, cmdBaseName + ext);
			if (Files.exists(cmd))
				return Optional.of(cmd.toAbsolutePath());
		}

		return Optional.empty();
	}
	
	/**
	 * Run a command on the operating system
	 * @param cmd command
	 * @param cmdArgs arguments
	 * @param processor processor that will parse the output
	 * @param readFromErrorOut if true, monitor error stream.
	 * @throws RuntimeException with error stream output
	 */
	public void runCommand(Path cmd, String cmdArgs, LineProcessor processor, boolean readFromErrorOut)
	{
		runCommand(buildCommand(cmd, cmdArgs), processor, readFromErrorOut);
	}

	/**
	 * Run a command on the operating system
	 * @param command command
	 * @param processor processor that will parse the output
	 */
	public void runCommand(String command, LineProcessor processor)
	{
		runCommand(command, processor, false);
	}
	
	/**
	 * Run a command on the operating system
	 * @param command command
	 * @param processor processor that will parse the output
	 * @param readFromErrorOut if true, monitor error stream.
	 * @throws RunCommandException if the commands ends with a non-zero error code or if there is captured output on the error stream
	 */
	public void runCommand(String command, LineProcessor processor, boolean readFromErrorOut)
	{
		try
		{
			final Runtime rt = Runtime.getRuntime();
			final Process proc = rt.exec(command);
		
			try(BufferedReader stdInput = new BufferedReader(new InputStreamReader(getResultStream(proc, readFromErrorOut)));
					InputStream errOut = getErrorStream(proc, readFromErrorOut))
			{
				int lineIdx = 1;
				String line = null;
				while ((line = stdInput.readLine()) != null) 
				{
					processor.process(line, lineIdx);
					lineIdx++;
				}

				final String err = IOUtils.toString(errOut, StandardCharsets.UTF_8);

				if (StringUtils.isNotEmpty(err))
					throw new RunCommandException(err).exitValue(proc.exitValue());
			}

			if (proc.exitValue() != 0)
			{
				throw new RunCommandException("Failed running: " + command).exitValue(proc.exitValue());
			}
		}
		catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private String buildCommand(final Path cmd, String cmdArgs)
	{
		return cmd.toString() + " " + cmdArgs;
	}

	InputStream getResultStream(Process proc, boolean readFromErrorOutput)
	{
		if (readFromErrorOutput)
			return proc.getErrorStream();

		return proc.getInputStream();
	}
	
	InputStream getErrorStream(Process proc, boolean readFromErrorOutput)
	{
		// If we're reading from output, there is no real error stream, we'll return a fake one.
		if (readFromErrorOutput)
			return new InputStream()
			{
				@Override
				public int read() throws IOException
				{
					return -1;
				}
			};

		return proc.getErrorStream();
	}

	//--------------------------------------------------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------------------------------------------------
	/**
	 * Processes output lines when running commands
	 */
	@FunctionalInterface
	public interface LineProcessor
	{
		/**
		 * @param line line to process
		 * @param lineIdx line index
		 */
		public void process(String line, int lineIdx);
	}
	
	/**
	 * Exception thrown when trying to run a command
	 */
	@SuppressWarnings("serial")
	public class RunCommandException extends RuntimeException
	{
		/**
		 * Exit value
		 */
		private OptionalInt exitValue = OptionalInt.empty();

		/**
		 * @param  cause the cause (which is saved for later retrieval by the
		 *         {@link #getCause()} method).  (A {@code null} value is
		 *         permitted, and indicates that the cause is nonexistent or
		 *         unknown.)
		 */
		public RunCommandException(RuntimeException cause)
		{
			super(cause);
		}

		/**
		 * @param  message the detail message (which is saved for later retrieval
		 *         by the {@link #getMessage()} method).
		 * @param  cause the cause (which is saved for later retrieval by the
		 *         {@link #getCause()} method).  (A {@code null} value is
		 *         permitted, and indicates that the cause is nonexistent or
		 *         unknown.)
		 */		
		public RunCommandException(String message, RuntimeException cause)
		{
			super(message, cause);
		}

		/**
		 * @param   message   the detail message. The detail message is saved for
		 *          later retrieval by the {@link #getMessage()} method.
		 */
		public RunCommandException(String message)
		{
			super(message);
		}
		
		/**
		 * @param value exit value returned by the command execution
		 * @return exception for chaining
		 */
		public RunCommandException exitValue(int value)
		{
			exitValue = OptionalInt.of(value);
			return this;
		}

		/**
		 * @return exit value, if we captured one
		 */
		public OptionalInt exitValue()
		{
			return exitValue;
		}
	}
}
