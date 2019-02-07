/*
 Copyright (c) 2017 onghuilam@gmail.com
 
 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:
 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.
 The Software shall be used for Good, not Evil.
 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 
 */

package hl.common.shell.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.logging.Logger;

public class FileUtil{
	
	private static Logger logger = Logger.getLogger(FileUtil.class.getName());
	
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
					sb.append(sLine).append("\n");
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
	
	
	public static void main(String args[]) throws Exception
	{
	}
}
