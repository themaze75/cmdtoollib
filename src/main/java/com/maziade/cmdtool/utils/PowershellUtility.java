package com.maziade.cmdtool.utils;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.maziade.cmdtool.utils.SystemUtility.LineProcessor;
import com.maziade.cmdtool.utils.SystemUtility.RunCommandException;
import com.maziade.cmdtool.utils.lineprocessors.StringListLineProcessor;

/**
 * Windows-based commands running through PowerShell.
 */
@Service
public class PowershellUtility 
{
	@Autowired SystemUtility systemUtility;

	/**
	 * Resolve host.  Will query DNS to get type A records and identify IP addresses.
	 * @param host host to resolve
	 * @return list of IPs the host resolves to
	 */
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
	
	/**
	 * Determines if an IP is local to this machine
	 * @param ip IP to look for
	 * @return true if IP is bound to this machine
	 */
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
}
