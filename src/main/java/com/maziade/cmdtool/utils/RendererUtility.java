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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ansi.AnsiBackground;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiOutput.Enabled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Core output render utility, handles some basic formatting and color functionality.
 * 
 * Use {@link RendererUtility#start()} to get an appender and build your output string.
 */
@Component
public class RendererUtility
{
	@Value( "${spring.output.ansi.enabled:DETECT}") Enabled ansiEnabled;

	@PostConstruct
	protected void init()
	{
		AnsiOutput.setEnabled(ansiEnabled);
	}

	/**
	 * @return New Appender object
	 */
	public Appender start()
	{
		return new Appender();
	}

	/**
	 * Render properties
	 * @param out appender
	 * @param properties properties to render
	 */
	public void renderOut(Appender out, Properties properties)
	{
		out.indent(() -> 
			renderOut(out, properties.entrySet())
		);
	}

	/**
	 * Render a set of entries as a grid.  Keys and values will be turned to strings using Object:toStirng
	 * @param <K> Key class
	 * @param <V> Value class
	 * @param out appender
	 * @param entries entries to render
	 */
	public <K, V> void renderOut(Appender out, Set<Map.Entry<K, V>> entries)
	{
		var set = entries.stream().sorted((a, b) -> a.getKey().toString().compareTo(b.getKey().toString())).toList();
		renderOut(
				out, 
				set.stream().map(e -> e.getKey().toString()).toList(), 
				set.stream().map(e -> e.getValue().toString()).toList()
			);
	}

	/**
	 * Render a grid
	 * @param out appender
	 * @param withTitles true to use first row as titles
	 * @param columns list of columns
	 */
	public final void renderOut(Appender out, boolean withTitles, List<List<String>> columns)
	{
		final int[] colWidths = columns.size() > 1 ? new int[columns.size() -1] : null;
		int totalPad = calculateColumnWidths(colWidths, columns);

		final List<Iterator<String>> iters = columns.stream().map(l -> l.iterator()).toList();
		boolean firstLine = true;
		
		while (iters.get(0).hasNext())
		{
			if (firstLine && withTitles)
				out.setColor(ColorSetting.TITLE);
			else
				out.setColor(AnsiColor.DEFAULT);

			for (int i = 0; i < columns.size(); i++)
			{
				String val = iters.get(i).next();
				out.append(val);

				if (i < columns.size() - 1 && colWidths != null)
				{
					out.pad(colWidths[i] - val.length() + 1);
					out.append(ColorSetting.SYMBOL, " | ");
				}
			}
			out.endLine();

			if (firstLine && withTitles)
			{
				out.setColor(ColorSetting.SYMBOL).pad(totalPad,'-').setColor(AnsiColor.DEFAULT);
				out.endLine();
			}

			firstLine = false;
		}
	}

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
	 * Render a grid of keys/values
	 * @param out appender
	 * @param keys list of keys
	 * @param values list of strings
	 */
	public void renderOut(Appender out, List<String> keys, List<String> values)
	{
		OptionalInt colSize = keys.stream().mapToInt(String::length).max();
		if (colSize.isEmpty())
			return;
		
		var valueIt = values.iterator();
		for(String key : keys)
		{
			out.appendKeyword(key);
			out.pad(colSize.getAsInt() - key.length() + 1);
			out.append(": ");
			out.append(valueIt.next());
			
			out.endLine();
		}
	}

	//--------------------------------------------------------------------------------------------------------------------------------
	/**
	 * Builds the output string.  Built with chaining in mind.
	 * 
	 * Use {@link Appender#toString()} to acquire the resulting string.
	 */
	public class Appender
	{
		private StringBuilder out = new StringBuilder();
		private int indentation = 0;
		private AnsiColor lastColor = AnsiColor.DEFAULT;
		private AnsiBackground lastBackground = AnsiBackground.DEFAULT;
		private boolean lineStarted = false;

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

		/**
		 * Change the color
		 * @param color new color
		 * @return this for chainining
		 */
		public Appender setColor(AnsiColor color)
		{
			lastColor = color;
			out.append(AnsiOutput.encode(color));
			return this;
		}
		
		/**
		 * Change the color
		 * @param color new color
		 * @return this for chainining
		 */
		public Appender setColor(ColorSetting color)
		{
			return setColor(color.color);
		}

		/**
		 * Sets the background color
		 * @param color new color
		 * @return this for chainining
		 */
		public Appender setBackground(AnsiBackground color)
		{
			lastBackground = color;
			out.append(AnsiOutput.encode(color));
			return this;
		}

		/**
		 * Indents (2 spaces) the content that will be appended by the passed lambda
		 * @param doIt function that will output more data.
		 * @return this for chainining
		 */
		public Appender indent(Runnable doIt)
		{
			indentation += 2;
			doIt.run();
			indentation -= 2;
			return this;
		}

		/**
		 * Write a line and ends it
		 * @param line line to output
		 * @return this for chainining
		 */
		public Appender writeln(String line)
		{
			append(line);
			endLine();
			return this;
		}

		/**
		 * Writes a title using title formatting
		 * @param title title 
		 * @return this for chainining
		 */
		public Appender writeTitle(String title)
		{
			append(ColorSetting.TITLE, title);
			endLine();
			return this;
		}

		/**
		 * Writes a keyword using keyword formatting
		 * @param in keyword
		 * @return this for chainining
		 */
		public Appender appendKeyword(String in)
		{
			append(ColorSetting.KEYWORD, in);
			return this;
		}

		/**
		 * Writes content in a given color and brings back the previous color
		 * @param color new color
		 * @param in content to append
		 * @return this for chainining
		 */
		public Appender append(AnsiColor color, String in)
		{
			AnsiColor c = lastColor;
			if (c != color)
				setColor(color);

			append(in);

			if (c != color)
				setColor(c);
			
			return this;
		}
		
		/**
		 * Writes content in a given color and brings back the previous color
		 * @param color new color
		 * @param in content to append
		 * @return this for chainining
		 */
		public Appender append(ColorSetting color, String in)
		{
			return append(color.color, in);
		}

		/**
		 * Writes content using error styling
		 * @param in content to append
		 * @return this for chainining
		 */
		public Appender error(String in)
		{
			return append(ColorSetting.ERROR, in);
		}

		/**
		 * Writes content to the output 
		 * @param in content to append
		 * @return this for chainining
		 */
		public Appender append(String in)
		{
			if (!lineStarted)
			{
				lineStarted = true;
				pad(indentation);
			}

			out.append(in);
			
			return this;
		}

		/**
		 * Writes a certain number of spaces for padding
		 * @param length number of spaces
		 * @return this for chainining
		 */
		public Appender pad(int length)
		{
			return pad(length, ' ');
		}
		
		/**
		 * Writes a certain number of a given character for padding
		 * @param length number of characters
		 * @param c character
		 * @return this for chainining
		 */
		public Appender pad(int length, char c)
		{
			for (int i = 0; i <= length; i++)
				out.append(c);
			
			return this;
		}

		/**
		 * Ends the current line
		 * @return this for chainining
		 */
		public Appender endLine()
		{
			out.append("\n");
			lineStarted = false;
			return this;
		}
	}
	
	public enum ColorSetting {

		TITLE(AnsiColor.BLUE), 
		INFO(AnsiColor.GREEN), 
		ERROR(AnsiColor.RED), 
		WARN(AnsiColor.YELLOW),
		KEYWORD(AnsiColor.CYAN), 
		SYMBOL(AnsiColor.BRIGHT_CYAN);
		
		private AnsiColor color;
		
		private ColorSetting(AnsiColor color)
		{
			setColor(color);
		}
		
		protected void setColor(AnsiColor color)
		{
			this.color = color;
		}
	}
}