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

package hl.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HLFileWriter{
	
	private String filename	 	= null;
	private boolean autoroll 	= true;
	private BufferedWriter wrt 	= null;
	private File file			= null;
	private long repeat_count 	= 0;
	private String last_line	= null;
	
	private long roll_threshold_bytes = 1000000;
	
	public HLFileWriter(String aFileName)
	{
		this.filename = aFileName;
	}
	
	private BufferedWriter initWritter() throws IOException
	{
		file = new File(this.filename);
		if(!file.exists())
		{
			if(file.getParentFile()!=null)
			{
				file.getParentFile().mkdirs();
			}
		}
		return new BufferedWriter(new FileWriter(file, true));
	}
	
	public void write(String aLine) throws IOException
	{
		if(file!=null && this.autoroll)
		{
			System.out.println("size="+file.length());;
			if(file.length()>this.roll_threshold_bytes)
			{
				SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd.HHmmss.SSS");
				closeWriter();
				File newFile = new File(this.filename+"_"+df.format(new Date()));
				file.renameTo(newFile);
			}
		}

		if(wrt==null)
		{
			wrt = initWritter();
		}
		
		if(aLine.equals(this.last_line))
		{
			this.repeat_count++;
			if(this.repeat_count%10==0)
			{
				return; //silent
			}
		}
		else
		{
			this.last_line = aLine;
			this.repeat_count = 0;
		}
		
		wrt.write(aLine);
		if(this.repeat_count>0)
		{
			wrt.write(" [repeated x"+this.repeat_count+"]");
		}
	}
	
	public void flush() throws IOException
	{
		if(wrt!=null)
		{
			wrt.flush();
		}
	}
	
	public void newLine() throws IOException
	{
		if(wrt==null)
		{
			wrt = initWritter();
		}
		
		wrt.newLine();
	}
	
	public void close()
	{
		closeWriter();
		file = null;
		repeat_count = 0;
		last_line = null;
	}
	
	public void closeWriter()
	{
		if(wrt!=null)
		{
			try {
				wrt.flush();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				wrt.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			wrt	 = null;
		}
	}
}
