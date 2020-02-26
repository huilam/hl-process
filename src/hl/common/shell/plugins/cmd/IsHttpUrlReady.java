package hl.common.shell.plugins.cmd;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

import hl.common.shell.utils.TimeUtil;

public class IsHttpUrlReady {
	
	private static SSLContext anyHostSSLContext = null;
	private static Pattern pattNotIP = Pattern.compile("[a-zA-Z]+\\.");
	
	public static boolean isUrlReady(String aURL, int iTimeoutSecs, int iCheckIntervalSecs)
	{
		boolean isOK = false;
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss.SSS");
		HttpURLConnection conn 	= null;
		
		if(iCheckIntervalSecs>1000)
		{
			iCheckIntervalSecs -= 10;
		}
		
		if(iCheckIntervalSecs<=0)
		{
			iCheckIntervalSecs = 50;
		}
		
		try {
			long lStartTimeMs = System.currentTimeMillis();
			int iRespCode  = 404;
			String sIP = null;
			
			while(!isOK)
			{
				URL url = new URL(aURL);
				conn = (HttpURLConnection) url.openConnection();
				
				Matcher m = pattNotIP.matcher(url.getHost());
				if(m.find())
				{
					try {
						//Hostname
						InetSocketAddress sock = new InetSocketAddress(url.getHost(), 0);
						InetAddress addr = sock.getAddress();
						if(addr!=null)
						{
							sIP = addr.getHostAddress();
						}
					}catch(Exception ex)
					{
						//ignore
					}
					
				}
				

				if(url.getProtocol().equalsIgnoreCase("https"))
				{
					HttpsURLConnection httpsconn = (HttpsURLConnection)conn;
					
					System.out.println("httpsconn="+httpsconn);
					if(httpsconn!=null)
					{
						try {
							SSLSocketFactory sc = getTrustAnyHostSSLSocketFactory();
							
							System.out.println("sc="+sc);
							
							httpsconn.setSSLSocketFactory(sc);
						} catch (KeyManagementException e) {
							e.printStackTrace();
						} catch (NoSuchAlgorithmException e) {
							e.printStackTrace();
						}
					}
				}
				
				String sBasicAuth = url.getUserInfo();
				if(conn!=null && sBasicAuth!=null)
				{
			   		String sEncodedBasicAuth = "Basic " + new String(Base64.getEncoder().encode(sBasicAuth.getBytes()));
			   		conn.setRequestProperty ("Authorization", sEncodedBasicAuth);
				}
				
				if(iTimeoutSecs<=0)
				{
					iTimeoutSecs = 5;
				}

				conn.setConnectTimeout(iTimeoutSecs*1000);
				
				try {
					iRespCode = conn.getResponseCode();
				}
				catch(Exception ex)
				{
				}
				isOK = iRespCode>=200 && iRespCode<300;
				
				String sOutput = df.format(System.currentTimeMillis())+"  "+aURL+(sIP!=null ? " (ip:"+sIP+")" : "")+" : "+iRespCode;
				if(isOK)
				{
					System.out.println("[IsHttpUrlReady-OK] "+sOutput);
				}
				else
				{
					System.out.println(sOutput);
				}
				
				if(iTimeoutSecs>0)
				{
					if(TimeUtil.isTimeout(lStartTimeMs, iTimeoutSecs*1000))
					{
						return false;
					}
				}
				Thread.sleep(iCheckIntervalSecs);
			}
			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		
		return isOK;
	}
	

    private static SSLSocketFactory getTrustAnyHostSSLSocketFactory() throws NoSuchAlgorithmException, KeyManagementException
    {
    	SSLContext sc = anyHostSSLContext;

    	if(sc==null)
    	{
	    	TrustManager trustmgr = new X509ExtendedTrustManager() 
	    	{
	
				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
	
				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType)
						throws CertificateException {}
	
				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType)
						throws CertificateException {}
				
				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
						throws CertificateException {}
	
				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
						throws CertificateException {}
	
				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
						throws CertificateException {}
	
				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
						throws CertificateException {}
			};
			
			sc = SSLContext.getInstance("SSL");
			sc.init(null, new TrustManager[]{trustmgr}, new SecureRandom());
			anyHostSSLContext = sc;
    	}
		return sc.getSocketFactory();
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