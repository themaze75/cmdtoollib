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
package com.maziade.cmdtool.utils.parsers;

import java.util.HashMap;
import java.util.Map;

import com.maziade.cmdtool.data.VersionInfo;
import com.maziade.cmdtool.utils.SystemUtility.LineProcessor;

abstract class VersionParser implements LineProcessor
{
	String version;
	Map<String, String> context = new HashMap<>();

	/**
	 * Split the given line
	 * @param line the given line
	 */
	protected void splitThis(String line)
	{
		int mark = 0;
		boolean openQuote = false;
		String varName = null;
		int i = 0;
		for (; i < line.length(); i++)
		{
			char c = line.charAt(i);
			switch (c)
			{
				case ':':
					if (openQuote)
						continue;
					varName = line.substring(mark, i).trim();
					mark = i + 1;
					break;

				case '"':
					openQuote = !openQuote;
					break;

				case ',':
					getContext().put(varName, line.substring(mark, i).trim());
					mark = i + 1;
					break;
				default:
					// keep moving
			}
		}

		if (mark > 0 && mark < i && !openQuote)
		{
			getContext().put(varName, line.substring(mark, i).trim());
		}
	}

	/**
	 * @return Version Info
	 */
	public VersionInfo toVersionInfo()
	{
		return new VersionInfo(version, context);
	}

	/**
	 * @return version string
	 */
	public String getVersion()
	{
		return version;
	}

	/**
	 * @return context information
	 */
	public Map<String, String> getContext()
	{
		return context;
	}
}
