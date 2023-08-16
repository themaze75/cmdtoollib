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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

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

	//--------------------------------------------------------------------------------------------------------------------------------
	public void runCommand(Path cmd, String cmdArgs, LineProcessor processor)
	{
		runCommand(cmd, cmdArgs, processor, false);
	}

	//--------------------------------------------------------------------------------------------------------------------------------
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

	//--------------------------------------------------------------------------------------------------------------------------------
	public void runCommand(Path cmd, String cmdArgs, LineProcessor processor, boolean readFromErrorOut)
	{
		try
		{
			final Runtime rt = Runtime.getRuntime();
			final Process proc = rt.exec(cmd.toString() + " " + cmdArgs);
		
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

				// TODO handle error?
				if (StringUtils.isNotEmpty(err))
					throw new RuntimeException(err);
			}
			
			if (proc.exitValue() != 0)
			{
				// TODO error??
			}
		}
		catch(IOException e)
		{
			// TODO handle error?
			throw new RuntimeException(e);
		}
	}

	//--------------------------------------------------------------------------------------------------------------------------------
	InputStream getResultStream(Process proc, boolean readFromErrorOutput)
	{
		if (readFromErrorOutput)
			return proc.getErrorStream();

		return proc.getInputStream();
	}
	
	//--------------------------------------------------------------------------------------------------------------------------------
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
	@FunctionalInterface
	public interface LineProcessor
	{
		public void process(String line, int lineIdx);
	} 
}
