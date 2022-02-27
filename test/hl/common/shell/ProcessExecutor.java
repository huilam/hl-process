package hl.common.shell;

public class ProcessExecutor
{
	public ProcessExecutor()
	{
	}
	
	public static void main(String args[]) throws Exception
	{
		try {
			String sPropFileName = "process.properties";
			if(args.length>0)
			{
				sPropFileName = args[0];
			}
			System.out.println();
			System.out.println("Process Engine : "+HLProcess.getVersion());
			System.out.println("Process Config : "+sPropFileName);
			
			HLProcessMgr procMgr = new HLProcessMgr(sPropFileName);
			int iLocalCount 	= 0;
			int iRemoteCount 	= 0;
			int iDisabledCount 	= procMgr.getDisabledProcesses().length;
			HLProcess[] processes = procMgr.getAllProcesses();
			for(int i=0; i<processes.length; i++)
			{
				if(processes[i].isRemoteRef())
					iRemoteCount ++;
				else
					iLocalCount ++;
				
				System.out.println(processes[i].toString());
			}
			
			int iTotalProcess = processes.length + iDisabledCount;
			System.out.println("Process Loaded : "+iTotalProcess);
			if(iTotalProcess>0)
			{
				System.out.println("  - Local  : "+iLocalCount);
				System.out.println("  - Remote : "+iRemoteCount);
				if(iDisabledCount>0)
				{
					System.out.println("  - Disabled : "+iDisabledCount);
				}
			}
			System.out.println("Starting processes ...");
			System.out.println();
			
			Thread t = new Thread()
			{
				public void run()
				{
					procMgr.startAllProcesses();
				}
			};
			t.start();
			t.join();
		}
		finally
		{
			System.out.println("End of Process Executor.");			
		}
	}
	
}