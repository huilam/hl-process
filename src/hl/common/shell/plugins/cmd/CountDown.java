package hl.common.shell.plugins.cmd;

import java.text.SimpleDateFormat;

public class CountDown
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
					lCountDownTotalms = Long.parseLong(args[0])*1000;					
					long lCountDownStartTime = System.currentTimeMillis();
					long lElapsed = 0;
					while(lCountDownTotalms>lElapsed)
					{
						System.out.println("[CountDown] "+(lCountDownTotalms-lElapsed)/1000);
						Thread.sleep(980);
						lElapsed = System.currentTimeMillis()-lCountDownStartTime;
					}
					isSyntaxErr = false;
					System.out.println("[CountDown-OK] "+df.format(System.currentTimeMillis()));
				}
				catch(NumberFormatException ex)
				{
				}
			}
			
		}
		
		if(isSyntaxErr)
		{
			System.out.println("Syntax  : CountDown <countdown-number> <time-unit>");
			System.out.println("Example : CountDown 20 s");
			System.out.println("        : CountDown 1 m");
		}
		
	}
	
}