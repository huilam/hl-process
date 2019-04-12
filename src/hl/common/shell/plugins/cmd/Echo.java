package hl.common.shell.plugins.cmd;

import java.text.SimpleDateFormat;

public class Echo
{
	
	public static void main(String args[]) throws Exception
	{
		boolean isSyntaxErr = true;
		
		if(args.length==2)
		{
			isSyntaxErr = false;
			
			String sEchoMsg = args[0];
			int iRepeat 	= 0;
			
			try 
			{
				iRepeat = Integer.parseInt(args[1]);
				for(int i=0; i<iRepeat; i++)
				{
					System.out.println(sEchoMsg);
				}
			}
			catch(NumberFormatException ex)
			{
				isSyntaxErr = true;
				ex.printStackTrace();
			}
			
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss.SSS");
			System.out.println("[Echo-OK] "+df.format(System.currentTimeMillis()));
		}
		
		if(isSyntaxErr)
		{
			System.out.println("Syntax  : Echo \"<message>\" <repeat-count>");
			System.out.println("Example : Echo  \"hello hlprocess !\" 50");
		}		
	}
}