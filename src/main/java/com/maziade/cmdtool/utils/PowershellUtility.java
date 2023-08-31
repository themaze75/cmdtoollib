package com.maziade.cmdtool.utils;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.maziade.cmdtool.utils.SystemUtility.LineProcessor;
import com.maziade.cmdtool.utils.SystemUtility.RunCommandException;
import com.maziade.cmdtool.utils.lineprocessors.StringListLineProcessor;

@Service
public class PowershellUtility // TODO Not sure what to call this yet? I might want to make this cross-OS in the future... 
{
	@Autowired SystemUtility systemUtility;

	public List<String> resolveHost(String host)
	{
		// https://learn.microsoft.com/en-us/powershell/module/dnsclient/resolve-dnsname?view=windowsserver2022-ps
		String cmd = new StringBuilder("Resolve-DnsName -Name ")
			.append(host)
			.append(" -Type A | Select-Object IpAddress | Format-Table -HideTableHeaders")
			.toString();

		StringListLineProcessor proc = new StringListLineProcessor();
		runPowerShell(cmd, proc);

		return proc.strings();
	}
	
	public boolean isLocalIp(String ip)
	{
		// https://learn.microsoft.com/en-us/powershell/module/nettcpip/get-netipaddress?view=windowsserver2022-ps
		String cmd = new StringBuilder("Get-NetIPAddress -IpAddress ")
			.append(ip)
			.append(" | Select-Object IpAddress | Format-Table -HideTableHeaders")
			.toString();

		StringListLineProcessor proc = new StringListLineProcessor();
		try
		{
			runPowerShell(cmd, proc);
		}
		catch (RunCommandException e)
		{
			// Command fails if IP is not found.
			return false;
		}

		return true;
	}

	private void runPowerShell(String command, LineProcessor processor)
	{
		systemUtility.runCommand(buildPowershellCommand(command), processor);
	}

	private String buildPowershellCommand(String psCommand)
	{
		final StringBuilder out = new StringBuilder("powershell.exe ");
		out.append(psCommand);
		return out.toString();
	}


	// https://learn.microsoft.com/en-us/powershell/module/nettcpip/get-netipaddress?view=windowsserver2022-ps
	// Get-NetIPAddress -IpAddress 127.0.0.1 | Select-Object IpAddress | Format-Table -HideTableHeaders
	
	
	// TODO is windows?
	
	// https://learn.microsoft.com/en-us/powershell/module/dnsclient/resolve-dnsname?view=windowsserver2022-ps
	// Resolve-DnsName -Name localhost -Type A | Select-Object IpAddress | Format-Table -HideTableHeaders


}
