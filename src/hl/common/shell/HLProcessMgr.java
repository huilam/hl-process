package hl.common.shell;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import hl.common.shell.listeners.HLProcessEvent;
import hl.common.shell.plugins.output.StateOutput;
import hl.common.shell.utils.TimeUtil;
import hl.common.shell.HLProcess.ProcessState;

public class HLProcessMgr
{
	private HLProcessConfig procConfig = null;
	private static Logger logger  	= Logger.getLogger(HLProcessMgr.class.getName());
	
	private long startTimestamp					= 0;
	private HLProcessEvent event 				= null;
	private Map<String, Long> mapInitSuccess 	= null;
	private boolean isShowStateAsciiArt			= true;
	
	private HLProcess terminatingProcess 		= null;
	private boolean is_terminating_all 			= false;
	private boolean waitingAllProcessTerminate 	= false;
		
	public HLProcessMgr(String aPropFileName)
	{
		mapInitSuccess = new LinkedHashMap<String, Long>();
		
		StateOutput.getStateOutput(ProcessState.IDLE);
		try {		
			Runtime.getRuntime().addShutdownHook(new Thread(){
				@Override
				public void run() {
					System.out.println("[ShutdownHook] Executing HLProcessMgr.ShutdownHook ...");
					//random pick an active process as terminating process
					for(HLProcess proc : getAllProcesses())
					{
						if(proc.isProcessAlive())
						{
							terminatingProcess = proc;
							
							if(proc.isShutdownAllOnTermination())
							{
								break;
							}
						}
					}

					terminateAllProcesses();
					waitForAllProcessesToBeTerminated(terminatingProcess);
					
					System.out.println("[ShutdownHook] End of HLProcessMgr.ShutdownHook.");
					System.exit(0);
				}
			});
			
			procConfig = new HLProcessConfig(aPropFileName);
			
			event = new HLProcessEvent()
					{
						public void onProcessStarting(HLProcess p) {
						}
						
						public void onProcessError(HLProcess p, Throwable e) {
						}

						public void onProcessInitSuccess(HLProcess p) 
						{
							if(is_terminating_all)
								return;
							
							if(p.getCurProcessState().isAtLeast(ProcessState.STARTED))
							{
								/**
								if(mapInitSuccess==null)
								{
									mapInitSuccess = new LinkedHashMap<String, Long>();
								}
								**/
								mapInitSuccess.put(p.getProcessCodeName(), new Long(System.currentTimeMillis()));
								
								if(mapInitSuccess.size()==getAllProcesses().length)
								{
									allProcessesStarted();
								}
							}
						}
						
						public void onProcessTerminate(HLProcess p) 
						{
							if(p!=null)
							{
								if(p.isShutdownAllOnTermination() && !is_terminating_all)
								{
									long lterminateStartMs = System.currentTimeMillis();
									terminatingProcess = p;
									terminateAllProcesses();
									waitForAllProcessesToBeTerminated(terminatingProcess);
									consolePrintln("[TERMINATE] Total Terminate Time : "+TimeUtil.milisec2Words(System.currentTimeMillis()-lterminateStartMs));
									consolePrintln();

								}
							}
						}
					};
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void allProcessesStarted()
	{
		if(isShowStateAsciiArt)
		{
			consolePrintln();
			consolePrintln(StateOutput.getStateOutput(ProcessState.STARTED));
		}
		consolePrintln();
		int i = 1;
		SimpleDateFormat df = new SimpleDateFormat("MMM-dd HH:mm:ss.SSS");
		consolePrintln("[STARTED] All processes are ready.");
		StringBuffer sb = new StringBuffer();
		for(String sProcID : mapInitSuccess.keySet())
		{
			Long lInitTimeStamp = mapInitSuccess.get(sProcID);
			
			if(lInitTimeStamp==null)
				lInitTimeStamp = 0L;
			
			sb.setLength(0);
			sb.append(i);
			while(sb.length()<3)
			{
				sb.insert(0, " ");
			}
			
			String sFormattedStartTime = lInitTimeStamp==0?"0":df.format(lInitTimeStamp);
			consolePrintln("[STARTED] "+sb.toString()+"."+sProcID+" started at "+sFormattedStartTime);
			i++;
		}
		consolePrintln("[STARTED] Total Startup Time : "+TimeUtil.milisec2Words(System.currentTimeMillis()-getStartTimestamp()));
		consolePrintln();		
	}
	
	private void consolePrintln()
	{
		consolePrintln(null);
	}
	
	private void consolePrintln(final String aMessage)
	{
		if(aMessage==null)
		{
			System.out.println();
		}
		else
		{
			System.out.println(aMessage);
		}
	}
	
	private synchronized void waitForAllProcessesToBeTerminated(HLProcess aCurrentProcess)
	{
		if(this.waitingAllProcessTerminate)
			return;
		
		this.waitingAllProcessTerminate = true;
		
		long lStart = System.currentTimeMillis();
		long lShutdownElapsed 		= 0;
		long lShutdown_timeout_ms 	= 0;
		long lLast_notification_ms 	= 0;
		Vector<HLProcess> vProcesses = new Vector<HLProcess>();
		
		try {
			if(aCurrentProcess!=null)
			{
				aCurrentProcess.setCurProcessState(ProcessState.STOP_WAIT_OTHERS);
			}
	
			for(HLProcess proc : getAllProcesses())
			{
				if(proc.isProcessAlive())
				{
					if(aCurrentProcess!=null)
					{
						if(proc.getProcessCodeName().equals(aCurrentProcess.getProcessCodeName()))
						{
							continue;
						}
					}
					vProcesses.add(proc);
				}
			}
			
			if(aCurrentProcess!=null)
			{
				lShutdown_timeout_ms 	= aCurrentProcess.getShutdownTimeoutMs();
			}
			else if(getAllProcesses().length==1)
			{
				HLProcess onlyProcess 	= getAllProcesses()[0];
				lShutdown_timeout_ms 	= onlyProcess.getShutdownTimeoutMs();
				if(!onlyProcess.isTerminating())
				{
					onlyProcess.terminateProcess();
				}
			}
			
			int iActiveProcess = 1;
			while(iActiveProcess>0)
			{
				iActiveProcess = 0;
				for(HLProcess proc : vProcesses)
				{
					if(!proc.isTerminated())
					{
						if(terminatingProcess!=null)
						{
							if(terminatingProcess.getProcessCodeName().equalsIgnoreCase(proc.getProcessCodeName()))
								continue;
						}
						
						iActiveProcess++;
					}
					else
					{
						if(proc.isProcessAlive())
						{
							proc.terminate_thread = true;
						}
					}
				}
				
				lShutdownElapsed = System.currentTimeMillis() - lStart;
				
				if(iActiveProcess>0)
				{
					if((System.currentTimeMillis()-lLast_notification_ms>=1000))
					{
						lLast_notification_ms = System.currentTimeMillis();
						consolePrintln("[Termination] Waiting "+iActiveProcess+" processes ... "+TimeUtil.milisec2Words(lShutdownElapsed));
						for(HLProcess proc : vProcesses)
						{
							if(!proc.isTerminated())
							{
								consolePrintln("   - "+proc.getProcessCodeName()+" : "+proc.getCurProcessState());
							}
						}
					}
					
					
					if(lShutdown_timeout_ms>0)
					{	
						if(lShutdownElapsed >= lShutdown_timeout_ms)
						{
							//kill all 
							StringBuffer sb = new StringBuffer();
							
							sb.append("[Termination] Shutdown timeout - ").append(lShutdown_timeout_ms).append("ms, processes pending termination:");
							
							int i = 1;
							for(HLProcess proc : getAllProcesses())
							{
								if(proc.isProcessAlive())
								{
									sb.append("\n ").append(i++).append(". [").append(proc.getProcessCodeName()).append("]:").append(proc.getProcessCommand());
								}
							}
							
							if(aCurrentProcess!=null)
							{
								aCurrentProcess.setCurProcessState(ProcessState.STOP_WAIT_OTHERS_TIMEOUT);
							}
			
							//logger.log(Level.WARNING, sb.toString());
							consolePrintln(sb.toString());
							consolePrintln("[Termination] execute 'System.exit(1)'");
							if(aCurrentProcess!=null && !aCurrentProcess.isTerminated())
							{
								aCurrentProcess.terminate_thread = true;
							}
							printProcessLifeCycle();
							System.exit(1);
						}
					}
				}
				
				///////
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			if(aCurrentProcess!=null && !aCurrentProcess.isTerminated())
			{
				aCurrentProcess.terminate_thread = true;
			}
			
		}finally
		{
			
			//logger.log(Level.INFO, "All processes terminated");
			consolePrintln("[Termination] All processes terminated");
			consolePrintln("[Termination] terminating process : "+terminatingProcess!=null?terminatingProcess.getProcessCodeName():"none");
			printProcessLifeCycle();
		}
	}
	
	private void printProcessLifeCycle()
	{
		for(HLProcess p : getAllProcesses())
		{
			String sRemote = p.isRemoteRef()?" (remote)":"";
				
			consolePrintln("[process.lifecycle] "+p.getProcessCodeName()+sRemote+" : "+p.getProcessStateHist());
		}
	}
	
	public void setLogLevel(Level aLogLevel)
	{
		logger.setLevel(aLogLevel);
		HLProcess.logger.setLevel(aLogLevel);
		HLProcessConfig.logger.setLevel(aLogLevel);
	}
	
	public Level getLogLevel()
	{
		return logger.getLevel();
	}
	
	public long getStartTimestamp()
	{
		return this.startTimestamp;
	}
	
	public HLProcess[] getAllProcesses()
	{
		return procConfig.getProcesses();
	}
	
	public HLProcess[] getDisabledProcesses()
	{
		return procConfig.getDisabledProcesses();
	}
	
	public HLProcess getProcess(String aProcessID)
	{
		return procConfig.getProcess(aProcessID);
	}
	
	public synchronized void terminateAllProcesses()
	{		
		if(this.is_terminating_all)
			return;
			
		this.is_terminating_all = true;
		
		String sTerminateReq = "SYSTEM";
		if(terminatingProcess!=null)
		{
			sTerminateReq = terminatingProcess.getProcessCodeName();
		}
		if(isShowStateAsciiArt)
		{										
			consolePrintln();
			consolePrintln(StateOutput.getStateOutput(ProcessState.TERMINATED));
		}
		consolePrintln();
		consolePrintln("[TERMINATE] "+sTerminateReq+" initial termination ...");		

		if(procConfig!=null)
		{
			for(HLProcess p : procConfig.getProcesses())
			{
				if(!p.isRemoteRef())
				{
					if(terminatingProcess!=null && 
					   terminatingProcess.getProcessCodeName().equals(p.getProcessCodeName()))
					{
						p.setCurProcessState(ProcessState.STOPPING);
					}
					else if(!p.isTerminating())
					{
						p.reqStateChange(terminatingProcess, ProcessState.STOP_REQUEST);
						p.terminateProcess();
					}
				}
			}
		}
	}

	public void startAllProcesses()
	{
		startAllProcesses(200);
	}
	
	private synchronized void startAllProcesses(long lSleepMs)
	{
		this.startTimestamp = System.currentTimeMillis();

		if(this.is_terminating_all)
			return;
		
		this.is_terminating_all = false;
		
		long lStart = System.currentTimeMillis();
		int lPendingStart = procConfig.getProcesses().length;
		
		while(lPendingStart>0)
		{
			for(HLProcess p : procConfig.getProcesses())
			{
				p.setEventListener(event);
				if(p.isNotStarting() && p.isNotStarted())
				{
					long lElapsed = System.currentTimeMillis()-lStart;
					if(lElapsed >= p.getProcessStartDelayMs())
					{
						if(!p.isRemoteRef())
						{
							p.startProcess();
						}
						lPendingStart--;
					}
				}
			}
			
			if(lPendingStart>0)
			{
				try {
					Thread.sleep(lSleepMs);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}