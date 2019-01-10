package hl.common.shell.cmd;


public class CountDown
{
	
	public static void main(String args[]) throws Exception
	{
		boolean isSyntaxErr = true;
		
		if(args.length==2)
		{
			
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
						System.out.println((lCountDownTotalms-lElapsed)/1000);
						
						
						Thread.sleep(980);
						
						lElapsed = System.currentTimeMillis()-lCountDownStartTime;
					}
					isSyntaxErr = false;
					System.out.println("done.");
				}
				catch(NumberFormatException ex)
				{
				}
			}
			
		}
		
		if(isSyntaxErr)
		{
			System.out.println("Syntax  : CountDownClock <countdown-number> <time-unit>");
			System.out.println("Example : CountDownClock 20 s");
			System.out.println("        : CountDownClock 1 m");
		}
		
	}
	
}