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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import com.maziade.cmdtool.utils.PowershellUtility;
import com.maziade.cmdtool.utils.RendererUtility;
import com.maziade.cmdtool.utils.RendererUtility.Appender;
import com.maziade.cmdtool.utils.RendererUtility.ColorSetting;

@ShellComponent
@ShellCommandGroup("Generic Web Commands")
public class WebCommands implements CommandLineRunner
{
	@Autowired RendererUtility rendererUtility;
	@Autowired PowershellUtility psUtility;


	@ShellMethod(key = "host", value = "Resolve host name")
	public String resolve(String hostName)
	{
		Appender out = rendererUtility.start();

		var hosts = psUtility.resolveHost(hostName);
		if (hosts.isEmpty())
		{
			out.append(ColorSetting.WARN, "Could not resolve " + hostName);
		}
		else
		{
			out.indent(() -> hosts.stream().forEach(out::writeln));
		}

		return out.toString();
	}
	
	@ShellMethod(key = "isLocalIp", value = "Check if given IP is bound to the local machine")
	public String isLocalIP(String ip)
	{
		Appender out = rendererUtility.start();

		boolean isLocal = psUtility.isLocalIp(ip);

		out
			.append(ip)
			.append(" ");

		if (isLocal)
			out.append(ColorSetting.INFO, "is on this machine");
		else
			out.append(ColorSetting.WARN, "is NOT on this machine");

		return out.toString();
	}

	@Override
	public void run(String... args) throws Exception
	{
		// Done running.
	}
}