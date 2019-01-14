package hl.common.shell.cmd;

import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;

import hl.common.shell.utils.TimeUtil;

public class IsHttpUrlReady {

	public static boolean isUrlReady(String aURL, long lTimeoutSecs)
	{
		boolean isOK = false;
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss.SSS");
		HttpURLConnection conn 	= null;
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
				Thread.sleep(1000);
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
		
		if(args.length==2)
		{
			String sUrl = args[0];
			int iTimeoutSecs = Integer.parseInt(args[1]);
			
			if(!sUrl.startsWith("http"))
			{
				sUrl = "http://"+sUrl;
			}
			
			try {
				isSyntaxErr = false;
				isUrlReady(sUrl, iTimeoutSecs);
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
				isSyntaxErr = true;
			}			
		}
		
		if(isSyntaxErr)
		{
			System.out.println("Syntax  : IsHttpUrlReady <url> <timeout-secs>");
			System.out.println("Example : IsHttpUrlReady www.nec.com 30");
			System.out.println("        : IsHttpUrlReady http://www.nec.com/index.html 40");
		}
	}
	
}