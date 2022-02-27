package hl.common.shell;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
	
	private static HLProcess TERMINATING_PROCESS 		= null;
	private static boolean IS_TERMINATING_ALL			= false;
	private static boolean WAIT_ALL_PROCS_TO_TERMINAT 	= false;
		
	public HLProcessMgr(String aPropFileName)
	{
		try {
			procConfig = new HLProcessConfig(aPropFileName);
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}
		
		mapInitSuccess = new LinkedHashMap<String, Long>();
		
		StateOutput.getStateOutput(ProcessState.IDLE);
			
		event = new HLProcessEvent()
				{
					public void onProcessStarting(HLProcess p) {
					}
					
					public void onProcessError(HLProcess p, Throwable e) {
					}

					public void onProcessInitSuccess(HLProcess p) 
					{
						if(IS_TERMINATING_ALL)
							return;
						
						if(p.getCurProcessState().isAtLeast(ProcessState.STARTED))
						{
							mapInitSuccess.put(p.getProcessCodeName(), new Long(System.currentTimeMillis()));
							
							if(mapInitSuccess.size()==getAllProcesses().length)
							{
								allProcessesStarted();
							}
						}
					}
					
					public void onProcessTerminated(HLProcess p) 
					{
						System.out.println("onProcessTerminated()"+p);
						
						if(p!=null && !IS_TERMINATING_ALL)
						{
							if(p.isShutdownAllOnTermination() && TERMINATING_PROCESS==null)
							{
								long lterminateStartMs = System.currentTimeMillis();
								TERMINATING_PROCESS = p;
								
								if(!IS_TERMINATING_ALL)
								{
									terminateAllProcesses();
								}
								waitForAllProcessesToBeTerminated(TERMINATING_PROCESS);
								consolePrintln("[TERMINATE] "+p.getProcessCodeName()+" - Total Terminate Time : "+TimeUtil.milisec2Words(System.currentTimeMillis()-lterminateStartMs));
								consolePrintln();

							}
						}
					}
				};

		
		regShutdownHook();
	}
	
	private synchronized void regShutdownHook()
	{
		System.out.println("regShutdownHook()");
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run() {
				System.out.println("[ShutdownHook] Executing HLProcessMgr.ShutdownHook ...");
				
				if(TERMINATING_PROCESS==null)
				{
					//random pick an active process as terminating process
					for(HLProcess proc : getAllProcesses())
					{
						if(proc.isProcessAlive())
						{
							TERMINATING_PROCESS = proc;
							
							if(proc.isShutdownAllOnTermination())
							{
								break;
							}
						}
					}
					terminateAllProcesses();
					waitForAllProcessesToBeTerminated(TERMINATING_PROCESS);
				}
				
				System.out.println("[ShutdownHook] End of HLProcessMgr.ShutdownHook.");
			}
		});		
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
		if(WAIT_ALL_PROCS_TO_TERMINAT)
			return;
		
		WAIT_ALL_PROCS_TO_TERMINAT = true;
		
		long lStart = System.currentTimeMillis();
		long lShutdownElapsed 		= 0;
		long lShutdown_timeout_ms 	= 0;
		long lLast_notification_ms 	= 0;
		Vector<HLProcess> vProcesses = new Vector<HLProcess>();
		
		try {
			if(aCurrentProcess!=null)
			{
				aCurrentProcess.setCurProcessState(ProcessState.STOP_WAIT_OTHERS_TERMINATE);
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
			int lastActiveProcess = 0;
			while(iActiveProcess>0)
			{
				iActiveProcess = 0;
				for(HLProcess proc : vProcesses)
				{
					if(!proc.isTerminated())
					{
						if(aCurrentProcess!=null)
						{
							if(aCurrentProcess.getProcessCodeName().equalsIgnoreCase(proc.getProcessCodeName()))
								continue;
						}
						
						iActiveProcess++;
					}
					else
					{
						if(!proc.isTerminated())
						{
							proc.terminateProcess();
						}
					}
				}
				
				lShutdownElapsed = System.currentTimeMillis() - lStart;
				
				if(iActiveProcess>0)
				{
					if((System.currentTimeMillis()-lLast_notification_ms>=1000) || iActiveProcess!=lastActiveProcess)
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
						
						lastActiveProcess = iActiveProcess;
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
								aCurrentProcess.setCurProcessState(ProcessState.STOP_WAIT_OTHERS_TERMINATE_TIMEOUT);
							}
			
							//logger.log(Level.WARNING, sb.toString());
							consolePrintln(sb.toString());
							consolePrintln("[Termination] execute 'System.exit(1)'");
							if(aCurrentProcess!=null && !aCurrentProcess.isTerminated())
							{
								aCurrentProcess.terminateProcess();
							}
							printProcessLifeCycle();
							System.exit(1);
						}
					}
					///////
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
			}
			
		}finally
		{	
			if(aCurrentProcess!=null)
			{
				if(aCurrentProcess.isProcessAlive())
				{
					aCurrentProcess.executeTerminateCmd();
					aCurrentProcess.setCurProcessState(ProcessState.TERMINATED);
					aCurrentProcess.terminate_thread = true;
				}
				
				if(aCurrentProcess.proc!=null)
				{
					int iWaitMaxLoop = 100;
					while(aCurrentProcess.proc.isAlive() && iWaitMaxLoop>0)
					{
						iWaitMaxLoop--;
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
			
			//logger.log(Level.INFO, "All processes terminated");
			consolePrintln("[Termination] All processes terminated");
			
			String sTerminateRequestor = TERMINATING_PROCESS!=null?TERMINATING_PROCESS.getProcessCodeName():"none";
			consolePrintln("[Termination] Terminating process : "+sTerminateRequestor);
			printProcessLifeCycle();
			
			/**
			StringBuffer sb = new StringBuffer();
			for(HLProcess hlproc : getAllProcesses())
			{
				String sPrefix = "[terminate."+hlproc.getProcessCodeName()+"]";
				sb.setLength(0);
				sb.append("\n").append(sPrefix).append(" curProcessState:").append(hlproc.getCurProcessState());
				sb.append("\n").append(sPrefix).append(" proc.isAlive:").append(hlproc.proc!=null?hlproc.proc.isAlive():null);
				sb.append("\n").append(sPrefix).append(" thread.isAlive()").append(hlproc.thread!=null?hlproc.thread.isAlive():null);

				consolePrintln(sb.toString());
			}
			**/
			
		}
	}
	
	public HLProcess[] getActiveProcesses()
	{
		List<HLProcess> listProcess = new ArrayList<HLProcess>();
		for(HLProcess hlproc : getAllProcesses())
		{
			if(hlproc.isProcessAlive())
			{
				listProcess.add(hlproc);
			}
		}
		return listProcess.toArray(new HLProcess[listProcess.size()]);
	}
	
	private void printProcessLifeCycle()
	{
		List<String> listInitSuccess 	= new ArrayList<String>();
		List<String> listInitFailed 	= new ArrayList<String>();
		List<String> listIniting		= new ArrayList<String>();
		
		for(HLProcess p : getAllProcesses())
		{
			String sRemote 				= p.isRemoteRef()?" (remote)":"";
			String sProcessName 		= p.getProcessCodeName()+sRemote;
			String sProcessLifecycle 	= "  [process.lifecycle] "+sProcessName+" : "+p.getProcessStateHist();
			
			if(p.isInitSuccess())
			{
				listInitSuccess.add(sProcessLifecycle);
			}
			else
			{
				if(sProcessLifecycle.indexOf(ProcessState.START_INIT_FAILED.toString())>-1)
				{
					listInitFailed.add(sProcessLifecycle);
				}
				else
				{
					listIniting.add(sProcessLifecycle);
				}
			}
		}

		if(listInitSuccess.size()>0)
		{
			consolePrintln();
			consolePrintln("=[Init Success Process(es)]=");
			for(String sProcessLifecycle: listInitSuccess)
			{
				consolePrintln(sProcessLifecycle);
			}
		}
		
		if(listIniting.size()>0)
		{
			consolePrintln();
			consolePrintln("=[initializing Process(es)]=");
			for(String sProcessLifecycle: listIniting)
			{
				consolePrintln(sProcessLifecycle);
			}
		}
		
		if(listInitFailed.size()>0)
		{
			consolePrintln();
			consolePrintln("=[Init FAILED Process(es)]=");
			for(String sProcessLifecycle: listInitFailed)
			{
				consolePrintln(sProcessLifecycle);
			}
		}
		consolePrintln();
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
		if(IS_TERMINATING_ALL)
			return;
			
		IS_TERMINATING_ALL = true;
		
		String sTerminateReq = "SYSTEM";
		if(TERMINATING_PROCESS!=null)
		{
			sTerminateReq = TERMINATING_PROCESS.getProcessCodeName();
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
			StringBuffer sb = new StringBuffer();
			for(HLProcess p : procConfig.getProcessesByDepCntSeq())
			{
				if(p!=TERMINATING_PROCESS)
				{
					if(sb.length()>0)
						sb.append(" > ");
					sb.append(p.getProcessCodeName());
				}
				
				if(!p.isRemoteRef())
				{
					if(TERMINATING_PROCESS!=null && 
							TERMINATING_PROCESS.getProcessCodeName().equals(p.getProcessCodeName()))
					{
						p.setCurProcessState(ProcessState.STOPPING);
					}
					else if(!p.isTerminating())
					{
						p.reqStateChange(TERMINATING_PROCESS, ProcessState.STOP_REQUEST);
						p.terminateProcess();
					}
				}
			}
			
			if(TERMINATING_PROCESS!=null)
			{
				if(sb.length()>0)
					sb.append(" > ");
				sb.append(TERMINATING_PROCESS.getProcessCodeName());
			}
			consolePrintln("[TERMINATE] Termination Sequence : "+sb.toString());
		}
		
	}

	public void startAllProcesses()
	{
		startAllProcesses(200);
	}
	
	private synchronized void startAllProcesses(long lSleepMs)
	{
		this.startTimestamp = System.currentTimeMillis();

		if(IS_TERMINATING_ALL)
			return;
		
		IS_TERMINATING_ALL = false;
		
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