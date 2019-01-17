package hl.common.shell.plugins.cmd;

import java.io.File;
import java.text.SimpleDateFormat;

public class IsFileExist
{
	
	public static void main(String args[]) throws Exception
	{
		boolean isSyntaxErr = true;
		
		if(args.length==2)
		{
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss.SSS");
			long lCountDownTotalms	= 0;
			String lCountDownUnit 	= args[1];
			
			if(lCountDownUnit.equalsIgnoreCase("s") || lCountDownUnit.equalsIgnoreCase("m"))
			{
				try {
					lCountDownTotalms = Long.parseLong(args[1])*1000;					
					long lCountDownStartTime = System.currentTimeMillis();
					long lElapsed = 0;
					File f = new File(args[0]);
					boolean isFileExist = false;
					while(lCountDownTotalms>lElapsed || !isFileExist)
					{
						isFileExist = f.exists();
						System.out.println("[IsFileExist] "+f.getAbsolutePath()+" : "+isFileExist+" - "+(lCountDownTotalms-lElapsed)/1000);
						
						if(!isFileExist)
						{
							Thread.sleep(980);
							lElapsed = System.currentTimeMillis()-lCountDownStartTime;
						}
					}
					isSyntaxErr = false;
					
					if(isFileExist)
					{
						System.out.println("[IsFileExist-OK] "+df.format(System.currentTimeMillis()));
					}
				}
				catch(NumberFormatException ex)
				{
				}
			}
			
		}
		
		if(isSyntaxErr)
		{
			System.out.println("Syntax  : IsFileExist <file-or-folder-path> <timeout-secs>");
			System.out.println("Example : IsFileExist c://a.txt 60");
		}
		
	}
	
}