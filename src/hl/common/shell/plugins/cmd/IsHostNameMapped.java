package hl.common.shell.plugins.cmd;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IsHostNameMapped {
	
    
	public static void main(String args[])
	{
		boolean isSyntaxErr = true;
		
		if(args.length==1)
		{
			isSyntaxErr = false;
			
			String sIP = null;
			String sHostname = args[0];
			
			try {
				try {
					InetAddress ipAddr =  InetAddress.getByName(sHostname);
					
					if(ipAddr!=null)
					{
						sIP = ipAddr.getHostAddress();
					}
				}
				catch(UnknownHostException e)
				{
				}
				
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
				isSyntaxErr = true;
			}	
			
			
			if(sIP!=null && sIP.length()>=4)
			{
				System.out.println("[IsHostNameMapped-OK] "+sHostname+" - "+sIP);
			}
			else
			{
				System.out.println("[IsHostNameMapped-ERR] "+sHostname+" NOT mapped to any IP addresses.");
			}
			
			
		}
		
		if(isSyntaxErr)
		{
			System.out.println("Syntax  : IsHostNameMapped <hostname>");
			System.out.println("Example : IsHostNameMapped myhostname");
		}
	}
	
}