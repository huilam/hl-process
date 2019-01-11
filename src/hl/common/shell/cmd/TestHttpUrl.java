package hl.common.shell.cmd;

import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;

import hl.common.shell.utils.TimeUtil;

public class TestHttpUrl {

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
					System.out.println("["+TestHttpUrl.class.getSimpleName()+".OK] "+sOutput);
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
		String sUrl = args[0];
		int sTimeoutSecs = Integer.parseInt(args[1]);
		
		if(!sUrl.startsWith("http"))
		{
			sUrl = "http://"+sUrl;
		}
		isUrlReady(sUrl, sTimeoutSecs);
	}
	
}