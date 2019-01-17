package hl.common.shell.plugins.cmd;

import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;

import hl.common.shell.utils.TimeUtil;

public class IsHttpUrlReady {

	public static boolean isUrlReady(String aURL, long lTimeoutSecs, long lCheckIntervalSecs)
	{
		boolean isOK = false;
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss.SSS");
		HttpURLConnection conn 	= null;
		
		if(lCheckIntervalSecs>1000)
		{
			lCheckIntervalSecs -= 5;
		}
		
		try {
			long lStartTimeMs = System.currentTimeMillis();
			int iRespCode  = 404;
			while(!isOK)
			{
				URL url = new URL(aURL);
				conn = (HttpURLConnection) url.openConnection();
				try {
					iRespCode = conn.getResponseCode();
				}
				catch(Exception ex)
				{
				}
				isOK = iRespCode>=200 && iRespCode<300;
				
				String sOutput = df.format(System.currentTimeMillis())+"  "+aURL+" : "+iRespCode;
				if(isOK)
				{
					System.out.println("[IsHttpUrlReady-OK] "+sOutput);
				}
				else
				{
					System.out.println(sOutput);
				}
				
				if(lTimeoutSecs>0)
				{
					if(TimeUtil.isTimeout(lStartTimeMs, lTimeoutSecs*1000))
					{
						return false;
					}
				}
				Thread.sleep(lCheckIntervalSecs);
			}
			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		
		return isOK;
	}
	
	public static void main(String args[])
	{
		boolean isSyntaxErr = true;
		
		if(args.length==2 || args.length==3)
		{
			String sUrl = args[0];
			int iTimeoutSecs = Integer.parseInt(args[1]);
			int iCheckIntervalSecs = 1000;
			
			if(args.length==3)
			{
				iCheckIntervalSecs = Integer.parseInt(args[2])*1000;
			}
			
			if(!sUrl.startsWith("http"))
			{
				sUrl = "http://"+sUrl;
			}
			
			try {
				isSyntaxErr = false;
				isUrlReady(sUrl, iTimeoutSecs, iCheckIntervalSecs);
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
				isSyntaxErr = true;
			}			
		}
		
		if(isSyntaxErr)
		{
			System.out.println("Syntax  : IsHttpUrlReady <url> <timeout-secs> [optional-check-interval-secs]");
			System.out.println("Example : IsHttpUrlReady www.nec.com 30");
			System.out.println("        : IsHttpUrlReady http://www.nec.com/index.html 40");
			System.out.println("        : IsHttpUrlReady www.nec.com/index.html 60 3");
		}
	}
	
}