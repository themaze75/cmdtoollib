package com.maziade.cmdtool.utils.lineprocessors;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.maziade.cmdtool.utils.SystemUtility.LineProcessor;

/**
 * Just accumulates the lines
 */
public class StringListLineProcessor implements LineProcessor
{
	final List<String> strings = new ArrayList<>();
	final boolean keepEmptyLines;
	
	public StringListLineProcessor() {
		keepEmptyLines = false;
	}
	
	public StringListLineProcessor(boolean keepEmptyLines) {
		this.keepEmptyLines = keepEmptyLines;
	}
	
	/**
	 * @return accumulated strings
	 */
	public List<String> strings() {
		return strings;
	}

	@Override
	public void process(String line, int lineIdx) {
		if (keepEmptyLines || StringUtils.isNotEmpty(StringUtils.trim(line)))
			strings.add(line);
	}
}
