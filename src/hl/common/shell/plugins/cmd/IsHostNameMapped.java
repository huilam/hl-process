package hl.common.shell.plugins.cmd;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import hl.common.shell.utils.FileUtil;

public class IsHostNameMapped {
	
    
	public static void main(String args[])
	{
		boolean isSyntaxErr = true;
		
		if(args.length>=1)
		{
			isSyntaxErr = false;
			
			String sIP = null;
			
			boolean isFileMode = false;
			List<String> listParamName = new ArrayList<String>();
			
			for(String sArg : args)
			{
				if(sArg.startsWith("-"))
				{
					if(!isFileMode)
					{
						isFileMode = "-f".equalsIgnoreCase(sArg);
					}
				}
				else 
				{
					listParamName.add(sArg);
				}
			}
			
			
			List<String> listHostName = new ArrayList<String>();
			
			if(isFileMode)
			{
				for(String sParamName : listParamName)
				{
				
					File file = new File(sParamName);
					
					if(file.isFile())
					{
						System.out.println("Reading "+file.getName()+" ...");
						String sContent = FileUtil.loadContent(file.getAbsolutePath());
						
						StringTokenizer tk1 = new StringTokenizer(sContent, "\n");
						while(tk1.hasMoreTokens())
						{
							String sStr1 = tk1.nextToken().trim();
							StringTokenizer tk2 = new StringTokenizer(sStr1, ",");
							while(tk2.hasMoreTokens())
							{
								String sStr2 = tk2.nextToken().trim();
								listHostName.add(sStr2);
								System.out.println("  - Adding "+sStr2);
							}
							
						}
					}
				}
			}
			else
			{
				listHostName = listParamName;
			}
			
			System.out.println("[IsHostNameMapped] Checking hostname mapping ...");
			List<String> listMapErr = new ArrayList<String>();
			for(String sHostname : listHostName)
			{
				if(sHostname!=null && sHostname.trim().length()>0)
				{
					try {
						InetAddress ipAddr =  InetAddress.getByName(sHostname);
						
						if(ipAddr!=null)
						{
							sIP = ipAddr.getHostAddress();
							
							System.out.println("  - [OK] '"+sHostname+"' mapped to "+sIP);
						}
					}
					catch(Throwable t)
					{
						listMapErr.add(sHostname);
						sIP = null;
						System.err.println("  - [ERR] '"+sHostname+"' NOT mapped to any IP addresses.");
					}
				}
			}
			
			
			String[] sErrMapHost = listMapErr.toArray(new String[listMapErr.size()]);
			if(sErrMapHost.length==0)
			{
				System.out.println("[IsHostNameMapped-OK] All Required hostnames are mapped.");
			}
			else
			{
				System.err.println("[IsHostNameMapped-ERR] NOT all required hostnames are mapped.");
			}
			
			
		}
		
		if(isSyntaxErr)
		{
			System.out.println("Syntax  : IsHostNameMapped [-f] <hostname>");
			System.out.println("Example : IsHostNameMapped myhostname");
			System.out.println("        : IsHostNameMapped -f C:/hostsname.txt");
		}
	}
	
}