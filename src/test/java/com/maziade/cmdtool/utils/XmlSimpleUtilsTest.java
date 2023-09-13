package com.maziade.cmdtool.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

public class XmlSimpleUtilsTest {

	@Test
	public void testPathEndsWith()
	{
		final XmlSimpleUtils instance = new XmlSimpleUtils();

		var trailDeep = List.of("configuration", "system.webServer", "httpErrors");
		var trailMatch = List.of("system.webServer", "httpErrors");
		var trailMisMatch = List.of("yolo", "httpErrors");
		
		var pathEnd = "system.webServer/httpErrors";

		assertTrue(instance.pathEndsWith(trailDeep, pathEnd));
		assertTrue(instance.pathEndsWith(trailMatch, pathEnd));
		assertFalse(instance.pathEndsWith(trailMisMatch, pathEnd));
	}

}
