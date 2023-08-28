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
import java.util.*;
import javax.xml.XMLConstants;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import org.springframework.stereotype.Component;

//--------------------------------------------------------------------------------------------------------------------------------
@Component
public class XmlSimpleUtils
{
	//--------------------------------------------------------------------------------------------------------------------------------
	/**
	 * @return Pre-configured XML Input factory.  NB: Not thread-safe.
	 */
	private XMLInputFactory newXmlInputFactory()
	{
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		// to be compliant, completely disable DOCTYPE declaration:
		xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
		// or completely disable external entities declarations:
		xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
		// or prohibit the use of all protocols by external entities:
		xmlInputFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		xmlInputFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		return xmlInputFactory;
	}

	//--------------------------------------------------------------------------------------------------------------------------------
	/**
	 * If the trail matches the given path of (tag1/tag2/tag3)
	 */
	public boolean isPath(List<String> trail, String path)
	{
		return Objects.equals(path,  String.join("/", trail));
	}

	//--------------------------------------------------------------------------------------------------------------------------------
	/**
	 * If the tail end of the trail matches the tag name
	 */
	public boolean isTag(List<String> trail, String tagName)
	{
		if (trail.isEmpty())
			return false;

		return Objects.equals(tagName, trail.get(trail.size() - 1));
	}

	
	//--------------------------------------------------------------------------------------------------------------------------------
	public Optional<String> getAttribute(StartElement startElement, String attribute)
	{
		var iter = startElement.getAttributes();
		while (iter.hasNext())
		{
			var a = iter.next();
			if (Objects.equals(a.getName().getLocalPart(), attribute))
				return Optional.of(a.getValue());
		}
		
		return Optional.empty();
	}
	
	//--------------------------------------------------------------------------------------------------------------------------------
	public void processXml(String xml, XmlProcessor processor)
	{
		XMLInputFactory xmlInputFactory = newXmlInputFactory();

		var stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

		try
		{
			final XMLEventReader reader = xmlInputFactory.createXMLEventReader(stream);
			readAsXml(reader, processor);
		}
		catch(XMLStreamException e)
		{
			throw new IllegalStateException("Invalid XML.", e);
		}
	}

	//--------------------------------------------------------------------------------------------------------------------------------
	public void processXmlFile(File xmlFile, XmlProcessor processor)
	{
		XMLInputFactory xmlInputFactory = newXmlInputFactory();
		
		try (FileInputStream in = new FileInputStream(xmlFile))
		{
			final XMLEventReader reader = xmlInputFactory.createXMLEventReader(in);
			readAsXml(reader, processor);
		}
		catch(FileNotFoundException e)
		{
			throw new IllegalStateException("File not found. " + xmlFile, e);
		}
		catch(IOException e)
		{
			throw new IllegalStateException("Error reading XMLfile. " + xmlFile, e);
		}
		catch(XMLStreamException e)
		{
			throw new IllegalStateException("Invalid XML file. " + xmlFile, e);
		}
	}


	//--------------------------------------------------------------------------------------------------------------------------------
	public void processXmlFile(String xmlFile, XmlProcessor processor)
	{
		XMLInputFactory xmlInputFactory = newXmlInputFactory();
		
		try (FileInputStream in = new FileInputStream(xmlFile))
		{
			final XMLEventReader reader = xmlInputFactory.createXMLEventReader(in);
			readAsXml(reader, processor);
		}
		catch(FileNotFoundException e)
		{
			throw new IllegalStateException("File not found. " + xmlFile, e);
		}
		catch(IOException e)
		{
			throw new IllegalStateException("Error reading XMLfile. " + xmlFile, e);
		}
		catch(XMLStreamException e)
		{
			throw new IllegalStateException("Invalid XML file. " + xmlFile, e);
		}
	}
		
	//--------------------------------------------------------------------------------------------------------------------------------
	private void readAsXml(XMLEventReader reader, XmlProcessor processor)
	{
		final List<String> trail = new ArrayList<>();

		while (reader.hasNext())
		{
			try
			{
				final XMLEvent nextEvent = reader.nextEvent();

				if(nextEvent.isStartElement())
				{
					final StartElement startElement = nextEvent.asStartElement();
					final String tag = startElement.getName().getLocalPart();
					trail.add(tag);

					if (!tag.isEmpty())
					{
						processor.processElementStart(trail, startElement);
					}
				}
				
				if (nextEvent.isCharacters())
				{
					var c = nextEvent.asCharacters().toString().trim();
					if (!c.isEmpty())
						processor.processCharacters(trail, c);
				}

				if (nextEvent.isEndElement())
				{
					final EndElement endElement = nextEvent.asEndElement();
					final String tag = endElement.getName().getLocalPart();

					assert trail.get(trail.size() - 1).equals(tag);

					processor.processElementEnd(trail, endElement);

					trail.remove(trail.size() - 1);
				}
			}
			catch(XMLStreamException e)
			{
				throw new IllegalStateException("Bad XML?", e);
			}
		}
	}

	//---------------------------------------------------------------------------------------------------------------------------------
	//---------------------------------------------------------------------------------------------------------------------------------
	public interface XmlProcessor
	{
		//--------------------------------------------------------------------------------------------------------------------------------
		public default void processElementStart(List<String> trail, StartElement element)
		{
			
		}
		
		//--------------------------------------------------------------------------------------------------------------------------------
		public default void processElementEnd(List<String> trail, EndElement element)
		{
			
		}
		
		//--------------------------------------------------------------------------------------------------------------------------------
		public default void processCharacters(List<String> trail, String string)
		{
			
		}
	}
}
