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

import java.util.logging.Logger;

public class TimeUtil{
	
	public final static long _SEC_ms 	= 1000;
	public final static long _MIN_ms 	= 60 * _SEC_ms;
	public final static long _HOUR_ms 	= 60 * _MIN_ms;
	public final static long _DAY_ms 	= 24 * _HOUR_ms;
	
	private static Logger logger = Logger.getLogger(TimeUtil.class.getName());

	public static String milisec2Words(long aElapsed)
	{
		StringBuffer sb = new StringBuffer();
		long lTmp = aElapsed;
		
		if(lTmp>=_DAY_ms) //24 hours
		{
			sb.append(lTmp / _DAY_ms).append("d ");
			lTmp = lTmp % _DAY_ms;
		}
		
		if(lTmp>=_HOUR_ms)
		{
			sb.append(lTmp / _HOUR_ms).append("h ");
			lTmp = lTmp % _HOUR_ms;
		}
		
		if(lTmp>=_MIN_ms)
		{
			sb.append(lTmp / _MIN_ms).append("m ");
			lTmp = lTmp % _MIN_ms;
		}
		
		if(lTmp>=_SEC_ms)
		{
			sb.append(lTmp / _SEC_ms).append("s ");
			lTmp = lTmp % _SEC_ms;
		}
		
		if(lTmp>0)
		{
			sb.append(lTmp).append("ms ");
		}
		
		return sb.toString().trim();
	}

	
	public static void main(String args[]) throws Exception
	{
	}
}
