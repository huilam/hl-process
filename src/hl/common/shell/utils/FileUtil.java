package hl.common.shell.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class FileUtil {

	private static final int BUFFER_SIZE = 32768;
	
	public static File getJavaClassPath(Class aClass)
	{
		File folder = null;
		if(aClass!=null)
		{
			ProtectionDomain pd = aClass.getClass().getProtectionDomain();
			if(pd!=null)
			{
				CodeSource cs = pd.getCodeSource();
				if(cs!=null)
				{
					folder = new File(cs.getLocation().getFile()).getParentFile();
				}
			}
		}
		
		if(folder==null)
		{
			URL url = aClass.getResource(".");
			if(url!=null)
			{
				String sPath = url.getFile();
				
				String sPackageName = aClass.getPackage().getName();
				if(sPackageName!=null)
				{
					sPackageName = "/"+sPackageName.replaceAll("\\.", "/")+"/";
				}
				
				if(sPath.endsWith(sPackageName))
				{
					int iLen = sPath.length() - sPackageName.length(); 
					folder = new File(sPath.substring(0, iLen));
				}
				else
				{
					folder = new File(sPath);
				}
			}
		}
		
		return folder;
	}
	
	public static String loadContent(String aResourcePath)
	{
		String sData = null;
		if(aResourcePath!=null)
		{
			File f = new File(aResourcePath);
			if(f.exists())
			{
				try {
					sData = getContent(new FileReader(f));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
			
			if(sData==null)
			{
				//resource bundle
				InputStream in = null;
				try {
					in = FileUtil.class.getResourceAsStream(aResourcePath);
					if(in!=null)
					{
						sData = getContent(new InputStreamReader(in));
					}
				}
				finally
				{
					if(in!=null)
						try {
							in.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				}
			}
		}
		return sData;
	}
	
	private static String getContent(Reader aReader) 
	{
		String sData = null;
		if(aReader!=null)
		{
			StringBuffer sb = new StringBuffer();
			BufferedReader rdr = null;
			try {
				rdr = new BufferedReader(aReader);
				String sLine = null;
				
				while((sLine = rdr.readLine())!=null)
				{
					if(sb.length()>0)
					{
						sb.append("\n");
					}
					sb.append(sLine);
				}
				
				if(sb.length()>0)
				{
					sData = sb.toString();
				}
			}catch(IOException ex)
			{
				ex.printStackTrace();
			}
		}
		return sData;
	}
	
	public static File downloadWebImage(String aImageURL) throws IOException
	{
		File tempFile = File.createTempFile("temp", "");
		tempFile.delete();
		return downloadWebImage(aImageURL, tempFile.getPath());
	}
		
	public static File downloadWebImage(String aImageURL, String aLocalTargetFileName) throws IOException
	{
		URL url = new URL(aImageURL);
		InputStream in = null;
		try{
			
			HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
			httpConn.setRequestMethod("GET");
			
			if(httpConn.getResponseCode()==200)
			{
				inputStreamToFile(httpConn.getInputStream(), aLocalTargetFileName);
			}
			else
			{
				throw new IOException("[download failed] "+aImageURL);
			}
		}
		finally
		{
			if(in!=null)
				in.close();
		}
		File fileImage = new File(aLocalTargetFileName);
		
		if(fileImage.isFile())
			return fileImage;
		else
			return null;
	}
	
    public static void inputStreamToFile(
    		InputStream aInputStream, String aOutputFileName) throws IOException {
    	
        BufferedOutputStream bos = null;
        try{
	        bos = new BufferedOutputStream(new FileOutputStream(aOutputFileName));
	        byte[] bytesIn = new byte[BUFFER_SIZE];
	        int read = 0;
	        while ((read = aInputStream.read(bytesIn)) != -1) {
	            bos.write(bytesIn, 0, read);
	        }
        }finally
        {
	        if(bos!=null)
	        	bos.close();
        }
    }
    
	public static List<String[]> loadCSV(File aCSVFile) throws IOException
	{
		List<String[]> list = new ArrayList<String[]>();
		
		BufferedReader rdr = null;
		try {
			rdr = new BufferedReader(new FileReader(aCSVFile));
			
			String sLine = null;
			
			while((sLine = rdr.readLine())!=null)
			{
				Vector<String> vStr = new Vector<String>();
				
				StringBuffer sb = new StringBuffer();
				boolean isQuoted = false;
				for(int i=0; i<sLine.length(); i++)
				{
					if(i==0)
					{
						isQuoted = sLine.charAt(0)=='\"';
						if(isQuoted)
							continue;
					}
					
					if(isQuoted)
					{
						if(sLine.charAt(i)=='\"')
						{
							if(sLine.length()>i && sLine.charAt(i+1)=='\"')
							{
								//Escape
								i++;
								sb.append(sLine.charAt(i));
								continue;
							}
							
							isQuoted = false;
							continue;
						}
						else
						{
							sb.append(sLine.charAt(i));
						}
					}
					else
					{
						if(sLine.charAt(i)==',')
						{
							vStr.add(sb.toString());
							sb.setLength(0);
							continue;
						}
						else
						{
							sb.append(sLine.charAt(i));
						}
					}
					
				}
				if(sb.length()>0)
				{
					vStr.add(sb.toString());
				}
				
				list.add(vStr.toArray(new String[vStr.size()]));
			}
		}
		finally
		{
			try {
				if(rdr!=null);
				rdr.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return list;
	}
    
    public static void main(String args[]) throws IOException
    {
    }
}
