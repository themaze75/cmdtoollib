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

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ansi.*;
import org.springframework.boot.ansi.AnsiOutput.Enabled;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
public class RendererUtility
{
	public static final AnsiColor COLOR_TITLE = AnsiColor.BLUE;
	public static final AnsiColor COLOR_INFO = AnsiColor.GREEN;
	public static final AnsiColor COLOR_ERROR = AnsiColor.RED;
	public static final AnsiColor COLOR_INFO_KEYWORD = AnsiColor.CYAN;
	public static final AnsiColor COLOR_SYMBOL = AnsiColor.BRIGHT_CYAN;

	@Value( "${spring.output.ansi.enabled:DETECT}") Enabled ansiEnabled;

	@PostConstruct
	protected void init()
	{
		AnsiOutput.setEnabled(ansiEnabled);
	}

	public Appender start()
	{
		return new Appender();
	}

	public void renderOut(Appender out, Properties properties)
	{
		out.indent(() -> 
			renderOut(out, properties.entrySet())
		);
	}

	public <K, V> void renderOut(Appender out, Set<Map.Entry<K, V>> entries)
	{
		var set = entries.stream().sorted((a, b) -> a.getKey().toString().compareTo(b.getKey().toString())).toList();
		renderOut(
				out, 
				set.stream().map(e -> e.getKey().toString()).toList(), 
				set.stream().map(e -> e.getValue().toString()).toList()
			);
	}

	public final void renderOut(Appender out, boolean withTitles, List<List<String>> columns)
	{
		final int[] colWidths = columns.size() > 1 ? new int[columns.size() -1] : null;
		int totalPad = calculateColumnWidths(colWidths, columns);

		final List<Iterator<String>> iters = columns.stream().map(l -> l.iterator()).toList();
		boolean firstLine = true;
		
		while (iters.get(0).hasNext())
		{
			if (firstLine && withTitles)
				out.setColor(COLOR_TITLE);
			else
				out.setColor(AnsiColor.DEFAULT);

			out.startLine();

			for (int i = 0; i < columns.size(); i++)
			{
				String val = iters.get(i).next();
				out.append(val);

				if (i < columns.size() - 1 && colWidths != null)
				{
					out.pad(colWidths[i] - val.length() + 1);
					out.append(COLOR_SYMBOL, " | ");
				}
			}
			out.endLine();

			if (firstLine && withTitles)
			{
				out.startLine();
				out.setColor(COLOR_SYMBOL).pad(totalPad,'-').setColor(AnsiColor.DEFAULT);
				out.endLine();
			}

			firstLine = false;
		}
	}
	
	//--------------------------------------------------------------------------------------------------------------------------------
	private int calculateColumnWidths(int[] colWidths, List<List<String>> columns)
	{
		int totalPad = 0;
		if (colWidths != null)
		{
			var iter = columns.iterator();
			for (int i = 0; i < colWidths.length; i++)
			{
				colWidths[i] = iter.next().stream().mapToInt(String::length).max().orElse(0);
				totalPad += colWidths[i];
			}

			// We want the last column in the total, though
			totalPad += iter.next().stream().mapToInt(String::length).max().orElse(0);
		}

		return totalPad;
	}

	/**
	 * Render a grid of keys/values and aligns the key column
	 */
	public void renderOut(Appender out, List<String> keys, List<String> values)
	{
		OptionalInt colSize = keys.stream().mapToInt(String::length).max();
		if (colSize.isEmpty())
			return;
		
		var valueIt = values.iterator();
		for(String key : keys)
		{
			out.startLine();

			out.appendKeyword(key);
			out.pad(colSize.getAsInt() - key.length() + 1);
			out.append(": ");
			out.append(valueIt.next());
			
			out.endLine();
		}
	}

	//--------------------------------------------------------------------------------------------------------------------------------
	public class Appender
	{
		private StringBuilder out = new StringBuilder();
		private int indentation = 0;
		private AnsiColor lastColor = AnsiColor.DEFAULT;
		private AnsiBackground lastBackground = AnsiBackground.DEFAULT;

		@Override
		public String toString()
		{
			// Reset to default color after.
			if (lastColor != AnsiColor.DEFAULT)
				setColor(AnsiColor.DEFAULT);

			if (lastBackground != AnsiBackground.DEFAULT)
				setBackground(AnsiBackground.DEFAULT);

			return out.toString();
		}

		public Appender setColor(AnsiColor color)
		{
			lastColor = color;
			out.append(AnsiOutput.encode(color));
			return this;
		}

		public Appender setBackground(AnsiBackground color)
		{
			lastBackground = color;
			out.append(AnsiOutput.encode(color));
			return this;
		}

		public Appender indent(Runnable doIt)
		{
			indentation += 2;
			doIt.run();
			indentation -= 2;
			return this;
		}

		public Appender writeln(String line)
		{
			startLine();
			out.append(line);
			endLine();
			return this;
		}

		public Appender writeTitle(String line)
		{
			append(COLOR_TITLE, line);
			endLine();
			return this;
		}

		public Appender appendKeyword(String in)
		{
			append(COLOR_INFO_KEYWORD, in);
			return this;
		}

		public Appender append(AnsiColor color, String in)
		{
			AnsiColor c = lastColor;
			if (c != color)
				setColor(color);

			out.append(in);

			if (c != color)
				setColor(c);
			
			return this;
		}
		
		public Appender error(String in)
		{
			return append(COLOR_ERROR, in);
		}

		public Appender append(String in)
		{
			out.append(in);
			
			return this;
		}

		public Appender pad(int length)
		{
			return pad(length, ' ');
		}
		
		public Appender pad(int length, char c)
		{
			for (int i = 0; i <= length; i++)
				out.append(c);
			
			return this;
		}

		public Appender startLine()
		{
			for (int i = 0; i < indentation; i++)
				out.append(' ');
			
			return this;
		}

		public Appender endLine()
		{
			out.append("\n");
			return this;
		}
		
	}
}