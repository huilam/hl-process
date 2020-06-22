package hl.common.shell;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.ProtectionDomain;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hl.common.HLFileWriter;
import hl.common.shell.listeners.HLProcessEvent;
import hl.common.shell.utils.TimeUtil;

public class HLProcess extends HLProcessCmd implements Runnable
{
	private final static String _VERSION = "HLProcess beta v0.81";
	
	public static enum ProcessState 
	{ 
		IDLE(0), 
		
		STARTING(10), START_WAIT_DEP(11), START_DEP_READY(12), START_INIT(13), STARTED(14), 
		
		START_WAIT_DEP_FAILED(20), START_WAIT_DEP_TIMEOUT(21), 
		START_FAILED(22),  START_INIT_FAILED(23), START_INIT_TIMEOUT(24), 
		
		STARTED_IDLE_TIMEOUT(30), STOP_REQUEST(31), 
		
		STOPPING(40), STOP_EXEC_CMD_PENDING(41), STOP_EXEC_CMD_PENDING_TIMEOUT(42), STOP_EXEC_CMD(43), 
		STOP_WAIT_OTHERS(44), STOP_WAIT_OTHERS_TIMEOUT(45),
		
		TERMINATED(50);
		
		private final int code;
		 
		private ProcessState(int aCode) 
		{
			this.code = aCode;
		}
		public int getCode() 
		{
			return this.code;
		}
		public boolean is(ProcessState aProcessState)
		{
			return getCode() == aProcessState.getCode();
		}
		public boolean isBefore(ProcessState aProcessState)
		{
			return getCode() < aProcessState.getCode();
		}
		public boolean isAfter(ProcessState aProcessState)
		{
			return getCode() > aProcessState.getCode();
		}
		public boolean isAtLeast(ProcessState aProcessState)
		{
			return getCode() >= aProcessState.getCode();
		}
		public boolean isBetweenIncl(ProcessState aProcessState, ProcessState aProcessState2)
		{
			return (getCode() >= aProcessState.getCode()) && (getCode() <= aProcessState2.getCode());
		}
	};
		
	private int exit_value				= 0;	
	private long run_start_timestamp	= 0;
	private boolean is_init_success		= false;
	private boolean is_init_failed		= false;
	
	private String terminate_command  		= "";
	private String terminate_end_regex		= "";
	private long terminate_idle_timeout_ms	= 5 * TimeUtil._MIN_ms; //enforce 5 min idle timeout
	private boolean is_exec_terminate_cmd  		= false;
	
	private Map<ProcessState, Long> stateMap 	= new LinkedHashMap<ProcessState, Long>();
	private ProcessState stateCurrent	 		= ProcessState.IDLE;
	
	private HLProcessEvent listener				= null;	
	private HLProcess processRequestor			= null;
	
	private long dep_wait_log_interval_ms		= 5000;
	
	public static Logger logger = Logger.getLogger(HLProcess.class.getName());

	
	public HLProcess(String aId, String[] aShellCmd)
	{
		super(aId, aShellCmd);
		init();
	}
	
	public HLProcess(String aId)
	{
		super(aId);
		init();
	}
	
	
	public static String getVersion()
	{
		return _VERSION;
	}
	
	private void init()
	{
		stateMap.clear();
		setCurProcessState(ProcessState.IDLE);
	}

	public void setTerminateCommand(String aShellCommand)
	{
		this.terminate_command = aShellCommand;
	}
	
	public String getTerminateCommand()
	{
		if(terminate_command==null)
			return "";
		return terminate_command;
	}
	
	public void setTerminateEndRegex(String aRegex)
	{
		this.terminate_end_regex = aRegex;
	}
	
	public String getTerminateEndRegex()
	{
		return terminate_end_regex;
	}	
	
	public void setTerminateIdleTimeoutMs(long aTimeoutMs)
	{
		this.terminate_idle_timeout_ms = aTimeoutMs;
	}
	
	public long getTerminateIdleTimeoutMs()
	{
		return terminate_idle_timeout_ms;
	}	
	

	public void setCurProcessState(ProcessState aProcessState)
	{
		this.stateCurrent = aProcessState;
		this.stateMap.put(this.stateCurrent, System.currentTimeMillis());
	}
	
	public ProcessState getCurProcessState()
	{
		return this.stateCurrent;
	}
	
	public Map<ProcessState, Long> getProcessStates()
	{
		return this.stateMap;
	}
	
	public int getExitValue()
	{
		return this.exit_value;
	}
		
	public boolean isTerminated()
	{
		return getCurProcessState().is(ProcessState.TERMINATED);
	}
	
	public boolean isTerminating()
	{
		return getCurProcessState().isAfter(ProcessState.STARTED);
	}
	
	public boolean isProcessAlive()
	{
		return (proc!=null && proc.isAlive()) || (thread!=null && thread.isAlive());
	}
	
	public boolean isInitSuccess()
	{
		return this.is_init_success;
	}
	
	public boolean isStarted()
	{
		return this.run_start_timestamp>0 && getCurProcessState().isAtLeast(ProcessState.STARTED);
	}

	public boolean isNotStarting()
	{
		return getCurProcessState().isBefore(ProcessState.STARTING);
	}

	public boolean isNotStarted()
	{
		return getCurProcessState().isBefore(ProcessState.STARTED);
	}
	
	private static File getCommandScriptDir(String aScript)
	{
		if(aScript==null || aScript.trim().length()==0)
			return null;
		
		String sScript = aScript.split(" ")[0];
		
		sScript = sScript.replaceAll("\\\\", "/");
		
		int iPath = sScript.lastIndexOf("/");
		
		if(iPath==-1)
			return null;
		
		sScript = sScript.substring(0, iPath);
		return new File(sScript);
	}
	
	private boolean checkDependenciesB4Start()
	{
		boolean isWaitDepOk = true;
		String sPrefix = (getProcessCodeName()==null?"":"["+getProcessCodeName()+"] ");
		
		Collection<HLProcess> deps = getDependProcesses();
		if(deps!=null && deps.size()>0 && isNotStarted())
		{
			setCurProcessState(ProcessState.START_WAIT_DEP);

			StringBuffer sbDepList = new StringBuffer();
			Iterator<HLProcess> iterDep = deps.iterator();
			while(iterDep.hasNext())
			{
				HLProcess dep = iterDep.next();
				if(sbDepList.length()>0)
					sbDepList.append(", ");
				sbDepList.append(dep.getProcessCodeName());
			}
			logger.log(Level.INFO, 
					sPrefix + "wait_dependencies ("+deps.size()+") - "+sbDepList.toString());

			
			Collection<HLProcess> tmpDepends = new ArrayList<HLProcess>();
			tmpDepends.addAll(deps);
			
			Map<String, Long> tmpWaitStart = new HashMap<String, Long>();
			Map<String, Long> tmpWaitLastCheck = new HashMap<String, Long>();

			boolean isDependTimeOut = (getDependTimeoutMs()>0);
			StringBuffer sbDepCmd = new StringBuffer();
			
			
			long lWaitDepStartMs = System.currentTimeMillis();
			while(tmpDepends.size()>0 && ProcessState.START_WAIT_DEP.is(getCurProcessState()))
			{
				Iterator<HLProcess> iter = tmpDepends.iterator();
				while(iter.hasNext())
				{
					HLProcess d = iter.next();
					
					if(d.isInitSuccess())
					{
						sbDepCmd.setLength(0);
						sbDepCmd.append("[dep_init_success] ").append(getProcessCodeName()).append(" [ready.dep]=").append(d.getProcessCodeName());
						logger.log(Level.INFO, sbDepCmd.toString());
						deps.remove(d);
						continue;
					}
					else if(d.isTerminating())
					{

						sbDepCmd.setLength(0);
						sbDepCmd.append("[dep_init_failed] ").append(getProcessCodeName()).append(" [failed.dep]=").append(d.getProcessCodeName());
						logger.log(Level.SEVERE, sbDepCmd.toString());
						isWaitDepOk = false;
						setCurProcessState(ProcessState.START_WAIT_DEP_FAILED);
						onProcessError(this, new Exception(sbDepCmd.toString()));
						break;
					}
					else
					{
						Long lWaitStarttime = tmpWaitStart.get(d.getProcessCodeName());
						
						if(lWaitStarttime==null)
						{
							tmpWaitStart.put(d.getProcessCodeName(), System.currentTimeMillis());
							tmpWaitLastCheck.put(d.getProcessCodeName(), System.currentTimeMillis());
							
							sbDepCmd.setLength(0);
							sbDepCmd.append("[wait_dependencies] ").append(getProcessCodeName()).append(" [waiting.for]=").append(d.getProcessCodeName());
							sbDepCmd.append(" - ");
							if(d.isRemoteRef())
								sbDepCmd.append("(remote)").append(d.getRemoteHost()==null?"":d.getRemoteHost());
							else
								sbDepCmd.append(d.getProcessCommand());
							logger.log(Level.FINE, sbDepCmd.toString());
						}
						else
						{
							Long LLastCheckTime = tmpWaitLastCheck.get(d.getProcessCodeName());
							if(LLastCheckTime==null)
								LLastCheckTime = 0L;
							
							long lLastCheck = System.currentTimeMillis() - LLastCheckTime;
							if(lLastCheck >= this.dep_wait_log_interval_ms)
							{
								tmpWaitLastCheck.put(d.getProcessCodeName(), System.currentTimeMillis());
								
								long lElapsed = System.currentTimeMillis() - lWaitStarttime;
								sbDepCmd.setLength(0);
								sbDepCmd.append("    [wait_dep_elapsed] ").append(getProcessCodeName()).append(" [still.waiting]=").append(d.getProcessCodeName()).append(" - ").append(lElapsed).append(" ms");
								logger.log(Level.INFO, sbDepCmd.toString());
							}
						}
					}
					
					if(isDependTimeOut)
					{
						if(TimeUtil.isTimeout(lWaitDepStartMs, getDependTimeoutMs()))
						{
							String sErr = sPrefix+"[dep_init_timeout] "+getProcessCodeName()+" [timeout.dep]="+d.getProcessCodeName()+" - "+getDependTimeoutMs()+"ms ";
							logger.log(Level.SEVERE, sErr);
							isWaitDepOk = false;
							setCurProcessState(ProcessState.START_WAIT_DEP_TIMEOUT);
							onProcessError(this, new Exception(sErr));
							break;
						}
					}
				}
				
				try {
					Thread.sleep(getDependCheckIntervalMs());
				} catch (InterruptedException e) {
					logger.log(Level.WARNING, e.getMessage(), e);
					onProcessError(this, e);
				}
				
				tmpDepends.clear();
				tmpDepends.addAll(deps);
			}
		}
		
		return isWaitDepOk;
	}
	
	protected static ProcessBuilder initProcessBuilder(final HLProcess aHLProcess)
	{
		ProcessBuilder pb = null;
		
		String[] sCommands = aHLProcess.getCommands();
		String sPrefix = "["+aHLProcess.getProcessCodeName()+"]";
				
		if(sCommands!=null)
		{
			String sCommandLine = String.join(" ",sCommands);
			if(sCommandLine.trim().length()>0)
			{
				pb = new ProcessBuilder(sCommands);
				if(aHLProcess.isDefaultToScriptDir())
				{
					File fileDir = getCommandScriptDir(sCommandLine);
					if(fileDir!=null)
					{
						pb.directory(fileDir);
						logger.log(Level.INFO, sPrefix+" Auto default to script directory : "+pb.directory());
					}
				}
				else
				{
					ProtectionDomain d = HLProcess.class.getProtectionDomain();
					if(d!=null)
					{
						String sPath = d.getCodeSource().getLocation().getFile();
						File folder = new File(sPath);
						
						if(folder.isDirectory())
						{
							pb.directory(folder);
							logger.log(Level.INFO, sPrefix+" Changing working directory to "+pb.directory());
						}
						else if(folder.isFile())
						{
							if(folder.getParentFile().isDirectory())
							{
								pb.directory(folder.getParentFile());
								logger.log(Level.INFO, sPrefix+" Changing working directory to "+pb.directory());
							}
						}
					}
				}
				pb.redirectErrorStream(true);
			}
		}
		return pb;
	}
	
	public void run() {		
		String sPrefix = (getProcessCodeName()==null?"":"["+getProcessCodeName()+"] ");
		try {
			if(isNotStarting())
			{
				onProcessStarting(this);
				logger.log(Level.INFO, sPrefix+"start - "+getProcessCodeName());
				boolean isDepOk = checkDependenciesB4Start();
				
				if(isDepOk)
				{
					setCurProcessState(ProcessState.START_DEP_READY);
				}
				
				if(getDependProcesses().size()>0)
				{
					logger.log(Level.INFO, sPrefix +" dependencies ready - "+isDepOk);
				}
				
				if(isNotStarted() && isDepOk)
				{
					HLFileWriter wrt = null;
					String sLine = null;
					
					if(getProcessOutputFilename()!=null)
					{
						if(getProcessOutputFilename().trim().length()>0)
						{
							wrt = new HLFileWriter(
									getProcessOutputFilename(),
									getProcessLogAutoRollSizeBytes(),
									getProcessLogAutoRollFileCount()
									);
						}
					}
					
					ProcessBuilder pb = initProcessBuilder(this);
					try {
						proc = pb.start();
					} catch (IOException e1) {
						
						if(proc!=null)
						{
							proc.destroy();
						}
						proc = null;
						
						sLine = e1.getMessage();
						if(wrt!=null)
						{
							try {
								wrt.writeln(sLine);
								wrt.flush();
							} catch (IOException e) {
								//do nothing
							} finally {
								wrt.close();
							}
						}
						if(isOutputConsole())
						{
							System.out.println(sLine);
						}
						logger.log(Level.SEVERE, sLine);
						onProcessError(this, e1);
					}
					setCurProcessState(ProcessState.START_INIT);
					
					long lProcStartMs = System.currentTimeMillis();
					BufferedReader rdr = null;

					
					if(proc!=null)
					{
						try {
							rdr = new BufferedReader(new InputStreamReader(proc.getInputStream()));			
							
							Pattern pattCmdEnd = null;
							String sCmdEndRegex = getCommandEndRegex();
							if(sCmdEndRegex!=null && sCmdEndRegex.trim().length()>0)
							{
								pattCmdEnd = Pattern.compile(sCmdEndRegex);
							}
							
							if(rdr.ready())
								sLine = rdr.readLine();
							
							long lLastNonIdleTimestamp 	= System.currentTimeMillis();
							long lIdleTimeoutMs 		= getCommandIdleTimeoutMs();
							
							while(proc.isAlive() || sLine!=null)
							{
			
								if(sLine!=null)
								{
									lLastNonIdleTimestamp = System.currentTimeMillis();
			
									if(wrt!=null)
									{
										wrt.writeln(sLine);
									}
									
									if(isOutputConsole())
									{
										System.out.println(sLine);
									}
									
									if(sLine.length()>0)
									{
										if(this.is_init_success)
										{
											//if end
											if(pattCmdEnd!=null)
											{
												Matcher m = pattCmdEnd.matcher(sLine);
												if(m.find())
												{
													proc.destroy();
													break;
												}
											}
										}
										else
										{
											if(getInitTimeoutMs()>0)
											{
												if(TimeUtil.isTimeout(lProcStartMs, getInitTimeoutMs()))
												{
													onProcessInitTimeout(this);
													String sErr = sPrefix+"Init timeout ! "+TimeUtil.milisec2Words(System.currentTimeMillis()-lProcStartMs)+" - "+getProcessCommand();
													this.is_init_success = false;
													logger.log(Level.SEVERE, sErr);
													proc.destroy();
													break;
												}
											}
											
											if(getInitFailedRegexPattern()!=null)
											{
												Matcher m = getInitFailedRegexPattern().matcher(sLine);
												this.is_init_failed =  m.find();
												if(this.is_init_failed)
												{
													onProcessInitFailed(this);
													String sErr = sPrefix + "init_error - Elapsed: "+TimeUtil.milisec2Words(System.currentTimeMillis()-lProcStartMs);
													logger.log(Level.SEVERE, sErr);
													proc.destroy();
													break;
												}
											}

											if(getInitSuccessRegexPattern()!=null)
											{
												Matcher m = getInitSuccessRegexPattern().matcher(sLine);
												this.is_init_success = m.find();
												if(this.is_init_success)
												{
													onProcessInitSuccess(this);
													logger.log(Level.INFO, 
															sPrefix + "init_success - Elapsed: "+TimeUtil.milisec2Words(System.currentTimeMillis()-lProcStartMs));
												}
											}
											else
											{
												if(System.currentTimeMillis()-lProcStartMs<100)
												{
													Thread.sleep(100);
												}
												this.is_init_success = true;
												onProcessInitSuccess(this);
											}
											
										}
									}
								}
								else
								{
									if(lIdleTimeoutMs>0)
									{
										if(TimeUtil.isTimeout(lLastNonIdleTimestamp, lIdleTimeoutMs))
										{
											setCurProcessState(ProcessState.STARTED_IDLE_TIMEOUT);
											logger.log(Level.INFO, 
													sPrefix + "idle_timeout - Elapsed: "+TimeUtil.milisec2Words(System.currentTimeMillis()-lLastNonIdleTimestamp));
											break;
										}
									}
									
									try {
										//let the process rest awhile when no output
										Thread.sleep(100);
									} catch (InterruptedException e) {
										e.printStackTrace();
										break;
									}
								}
								
								sLine = null;
								if(rdr.ready())
								{
									sLine = rdr.readLine();
								}
							}
							
						} catch (Throwable e) {
							if(getCurProcessState().isBefore(ProcessState.STOPPING))
							{
								setCurProcessState(ProcessState.STOPPING);
							}
							this.exit_value = -1;
							logger.log(Level.SEVERE, e.getMessage(), e);
							onProcessError(this, e);
						}
						finally
						{
							if(!this.is_init_success)
							{
								onProcessInitFailed(this);
							}
							
							if(getCurProcessState().isBefore(ProcessState.STOPPING))
							{
								setCurProcessState(ProcessState.STOPPING);
							}
							
							if(wrt!=null)
							{
								try {
									wrt.flush();
									wrt.close();
								} catch (IOException e) {
								}
							}
							//
							if(rdr!=null)
							{
								try {
									rdr.close();
								} catch (IOException e) {
								}
							}
						}
					}			
				}
			}
		}
		finally
		{
			logger.log(Level.INFO, sPrefix+"end - "+getProcessCommand()+" (elapsed: "+TimeUtil.milisec2Words(System.currentTimeMillis()-this.run_start_timestamp)+")");

			executeTerminateCmd();
			
			if(this.is_init_failed || !this.is_init_success)
			{
				this.exit_value = -1;
			}
			onProcessTerminate(this);
			setCurProcessState(ProcessState.TERMINATED);
		}
	}
	
	private boolean executeTerminateCmd()
	{
		boolean isExecuted = false;
		if(!this.is_exec_terminate_cmd)
		{
			//Waiting for started
			if(isStarted())
			{
				this.is_exec_terminate_cmd = true;
				isExecuted = true;
				String sPrefix = (getProcessCodeName()==null?"":getProcessCodeName());
				String sEndCmd = getTerminateCommand();
				if(sEndCmd!=null && sEndCmd.trim().length()>0)
				{
					sPrefix = "["+sPrefix+"]";
					
					String sLine = "[Termination] "+sPrefix+" : execute terminated command - "+sEndCmd;
					if(isOutputConsole())
					{
						System.out.println(sLine);
					}
					logger.log(Level.INFO, sLine);
					
					String sSplitEndCmd[] = HLProcessConfig.splitCommands(this, sEndCmd);
					
					setCurProcessState(ProcessState.STOP_EXEC_CMD);
					HLProcess procTerminate = new HLProcess(getProcessCodeName()+".terminate", sSplitEndCmd);
					procTerminate.setCommandEndRegex(getTerminateEndRegex());
					procTerminate.setCommandIdleTimeoutMs(getTerminateIdleTimeoutMs());
					procTerminate.startProcess();
					
					long lTerminateStart 	= System.currentTimeMillis();
					long lTerminateElapse 	= 0;
					long lTerminateTimeoutMs = getTerminateIdleTimeoutMs();
					BufferedReader rdr = new BufferedReader(new InputStreamReader(procTerminate.proc.getInputStream()));	
					
					try {
						
						if(rdr.ready())
						{
							sLine = "";
							while(procTerminate.isProcessAlive() && sLine!=null)
							{
								sLine = rdr.readLine();
								
								if(sLine!=null)
								{
									if(isOutputConsole())
									{
										System.out.println(sLine);
									}
									logger.log(Level.INFO, sLine);
								}
								
								if(lTerminateTimeoutMs>0)
								{
									lTerminateElapse = System.currentTimeMillis() - lTerminateStart;
									
									if(lTerminateElapse > lTerminateTimeoutMs)
									{
										procTerminate.proc.destroy();
										break;
									}
								}
							}
							
						}
						
					} catch (IOException e) 
					{
						e.printStackTrace();
					}
					finally
					{
						if(rdr!=null)
						{
							try {
								rdr.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}	
		return isExecuted;
	}
	
	public void setEventListener(HLProcessEvent event)
	{
		this.listener = event;
	}
	
	private void onProcessInitTimeout(HLProcess aHLProcess)
	{
		setCurProcessState(ProcessState.START_INIT_TIMEOUT);
		printInitErrMsg(aHLProcess);		
	}
	
	private void onProcessInitFailed(HLProcess aHLProcess)
	{
		if(aHLProcess.getCurProcessState().isBefore(ProcessState.START_INIT_FAILED))
		{
			setCurProcessState(ProcessState.START_INIT_FAILED);
			printInitErrMsg(aHLProcess);
		}
	}
	
	private void printInitErrMsg(HLProcess aHLProcess)
	{
		String sInitErrMsg = aHLProcess.getInitErrorMessage();
		if(sInitErrMsg!=null && sInitErrMsg.trim().length()>0)
		{
			StringBuffer sbLine = new StringBuffer();
			int iLen = aHLProcess.getProcessCodeName().length()+sInitErrMsg.length()+14;
			for(int i=0; i<iLen; i++)
			{
				sbLine.append("#");
			}

			StringBuffer sbMsg = new StringBuffer();
			sbMsg.append(sbLine.toString()).append("\n");
			sbMsg.append("# [").append(aHLProcess.getProcessCodeName()).append("] error: ").append(sInitErrMsg).append("\n");
			sbMsg.append(sbLine.toString()).append("\n");
			
			logger.log(Level.SEVERE, "\n"+sbMsg.toString());
			if(isOutputConsole())
			{
				System.out.println(sbMsg.toString());
			}
		}
	}
	
	
	
	private void onProcessInitSuccess(HLProcess aHLProcess)
	{
		setCurProcessState(ProcessState.STARTED);
		if(this.listener!=null)
			listener.onProcessInitSuccess(aHLProcess);
	}
	private void onProcessStarting(HLProcess aHLProcess)
	{
		this.run_start_timestamp = System.currentTimeMillis();
		setCurProcessState(ProcessState.STARTING);

		if(this.listener!=null)
			listener.onProcessStarting(aHLProcess);
	}
	private void onProcessError(HLProcess aHLProcess, Throwable e)
	{
		if(this.listener!=null)
			listener.onProcessError(aHLProcess, e);
	}
	private void onProcessTerminate(HLProcess aHLProcess)
	{
		if(this.listener!=null)
			listener.onProcessTerminate(aHLProcess);
	}
	
	public Thread startProcess()
	{
		logger.info("Starting "+this.getProcessCodeName()+" ...");
		logger.info(this.toString());
		
		thread = new Thread(this);
		thread.setDaemon(isRunAsDaemon());
		thread.start();
		return thread;
	}
	
	public void terminateProcess()
	{
		if(getCurProcessState().isBefore(ProcessState.STOPPING))
		{
			setCurProcessState(ProcessState.STOPPING);
			executeTerminateCmd();
			if(proc!=null)
			{
				proc.destroy();
			}
		}
		
	}
	
	public void reqStateChange(HLProcess aRequestor, ProcessState aProcessState)
	{
		if(getCurProcessState().isBefore(aProcessState))
		{
			processRequestor = aRequestor;
			setCurProcessState(aProcessState);
		}
	}
	
	public String getProcessStateHist()
	{
		StringBuffer sbState = new StringBuffer();
		Map<ProcessState, Long> mapStates = getProcessStates();
		if(mapStates.size()>0)
		{
			Long[] arrStartTimes = mapStates.values().toArray(new Long[mapStates.size()]);
			ProcessState[] arrStates = mapStates.keySet().toArray(new ProcessState[mapStates.size()]);
			SimpleDateFormat df = new SimpleDateFormat("dd-MMM HH:mm:ss.SSS");
			for(int i=0; i<arrStates.length; i++)
			{
				ProcessState s = arrStates[i];
				
				if(sbState.length()>0)
				{
					sbState.append(" -> ");
				}
				sbState.append(s.toString());
				
				///
				Long lElapseMs = 0L;
				if(s.is(ProcessState.IDLE))
				{
					sbState.append(" (").append(df.format(arrStartTimes[i])).append(")");
				}
				else if(i<arrStates.length-1)
				{
					lElapseMs = arrStartTimes[i+1]-arrStartTimes[i];
				}
				else
				{
					lElapseMs = System.currentTimeMillis()-arrStartTimes[i];
				}
				
				if(s.is(ProcessState.STOP_REQUEST) && processRequestor!=null)
				{
					sbState.append("[req:").append(processRequestor.getProcessCodeName()).append("]");
				}
				
				if(lElapseMs!=null && lElapseMs>0)
				{
					sbState.append(" (").append(TimeUtil.milisec2Words(lElapseMs)).append(")");
				}
			}
		}
		return sbState.toString();
	}
	
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(super.toString());
		return sb.toString();
	}
	
}