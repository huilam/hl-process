package hl.common.shell.plugins.cmd;

public class Sleep {
    
	public static void main(String args[])
	{
		boolean isSyntaxErr = true;
		
		if(args.length==1)
		{
			int iSleepSecs 	= Integer.parseInt(args[0]);
			
			try {
				isSyntaxErr = false;
				System.out.println("[SLEEP] Going to sleep for "+iSleepSecs+" secs");
				int iSleepCount = -1;
				while(iSleepCount<iSleepSecs)
				{				
					iSleepCount++;
					Thread.sleep(1000);
					System.out.println("[SLEEPING] "+iSleepCount+"/"+iSleepSecs+" secs ...");
				}
				System.out.println("[WAKEUP] Wake up after slept "+iSleepSecs+" secs.");
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
				isSyntaxErr = true;
			}			
		}
		
		if(isSyntaxErr)
		{
			System.out.println("Syntax  : Sleep <secs>");
			System.out.println("Example : Sleep 30");
		}
	}
	
}