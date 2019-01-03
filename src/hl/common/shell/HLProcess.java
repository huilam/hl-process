package hl.common.shell;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HLProcess implements Runnable
{
	public static enum ProcessState 
	{ 
		IDLE(0), 
		
		STARTING(10), START_WAIT_DEP(11), START_INIT(12), STARTED(13), 
		
		START_WAIT_DEP_FAILED(20), START_WAIT_DEP_TIMEOUT(21), 
		START_FAILED(22),  START_INIT_FAILED(23), START_INIT_TIMEOUT(24), 

		STOPPING(30), STOP_EXEC_CMD(31), STOP_WAIT_OTHERS(32), STOP_WAIT_OTHERS_TIMEOUT(33),
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
		public boolean isAtLeast(ProcessState aProcessState)
		{
			return getCode() >= aProcessState.getCode();
		}
	};
	
	private String id					= null;
	private String output_filename		= null;
	private boolean is_def_script_dir 	= false;
	private boolean is_output_console	= false;
	
	private Pattern patt_init_failed	= null;
	private Pattern patt_init_success	= null;
	private boolean is_init_success		= false;
	private boolean is_init_failed		= false;
	private long init_timeout_ms		= 0;
	
	private int exit_value				= 0;
	
	private long run_start_timestamp	= 0;
	private long delay_start_ms			= 0;
	
	private long dep_check_interval_ms	= 100;
	private long dep_wait_timeout_ms	= 30000;
	private long shutdown_timeout_ms	= 30000;
	
	private Collection<HLProcess> depends = new ArrayList<HLProcess>();
	private String command_block_start	= "";
	private String command_block_end	= "";
	
	private String[] commands			= null;
	private String terminated_command  	= null;
	private boolean runas_daemon		= false;
	private boolean disabled			= false;

	private Map<ProcessState, Long> stateMap 	= new LinkedHashMap<ProcessState, Long>();
	private ProcessState stateCurrent	 		= ProcessState.IDLE;
	
	private boolean remote_ref			= false;
	private String remote_hostname		= null;

	private boolean is_exec_terminate_cmd  		 = false;
	private boolean shutdown_all_on_termination  = false;
	private Thread thread 				= null;
	private Process proc 				= null;
	private HLProcessEvent listener		= null; 
	
	public static Logger logger 		= Logger.getLogger(HLProcess.class.getName());
	
	public HLProcess(String aId, String[] aShellCmd)
	{
		this.commands = aShellCmd;
		init(aId);
	}

	public HLProcess(String aId)
	{
		init(aId);
	}
	
	private void init(String aId)
	{
		this.id = aId;
		stateMap.clear();
		setCurProcessState(ProcessState.IDLE);
	}
	
	public static String getVersion()
	{
		return "HLProcess alpha v0.54";
	}

	public void setCommandBlockStart(String aBlockSeparator)
	{
		this.command_block_start = aBlockSeparator;
	}
	
	public String getCommandBlockStart()
	{
		return this.command_block_start;
	}
	
	public void setCommandBlockEnd(String aBlockSeparator)
	{
		this.command_block_end = aBlockSeparator;
	}
	
	public String getCommandBlockEnd()
	{
		return this.command_block_end;
	}
	

	public void setProcessCommand(String[] aShellCmd)
	{
		this.commands = aShellCmd;
	}
	
	public void setProcessCommand(List<String> aShellCmdList)
	{
		this.commands = aShellCmdList.toArray(new String[aShellCmdList.size()]);
	}

	public void setTerminatedCommand(String aShellCommand)
	{
		this.terminated_command = aShellCommand;
	}
	
	public String getTerminatedCommand()
	{
		return terminated_command;
	}
	
	public void setProcessId(String aId)
	{
		this.id = aId;
	}
	
	public String getProcessId()
	{
		return this.id;
	}	
	
	public void setShutdownAllOnTermination(boolean aShutdownAll)
	{
		this.shutdown_all_on_termination = aShutdownAll;
	}
	
	public boolean isShutdownAllOnTermination()
	{
		return this.shutdown_all_on_termination;
	}	
	
	public void setRemoteRef(boolean aRemoteRef)
	{
		this.remote_ref = aRemoteRef;
	}
	
	public boolean isRemoteRef()
	{
		return this.remote_ref;
	}	
	
	public void setDisabled(boolean aDisabled)
	{
		this.disabled = aDisabled;
	}
	
	public boolean isDisabled()
	{
		return this.disabled;
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
	
	public void setRemoteHost(String aRemoteHost)
	{
		this.remote_hostname = aRemoteHost;
	}
	
	public String getRemoteHost()
	{
		return this.remote_hostname;
	}	
	
	public void setOutputConsole(boolean isOutputConsole)
	{
		this.is_output_console = isOutputConsole;
	}
	
	public boolean isOutputConsole()
	{
		return this.is_output_console;
	}
	
	public void setRunAsDaemon(boolean isRunAsDaemon)
	{
		this.runas_daemon = isRunAsDaemon;
	}
	
	public boolean isRunAsDaemon()
	{
		return this.runas_daemon;
	}
	
	public void setProcessOutputFilename(String aProcessOutputFilename)
	{
		this.output_filename = aProcessOutputFilename;
	}
	
	public String getProcessOutputFilename()
	{
		return this.output_filename;
	}
	
	public void setDefaultToScriptDir(boolean isDefScriptDir)
	{
		this.is_def_script_dir = isDefScriptDir;
	}
	public boolean isDefaultToScriptDir()
	{
		return this.is_def_script_dir;
	}
	
	public void setProcessStartDelayMs(long aDelayMs)
	{
		this.delay_start_ms = aDelayMs;
	}
	
	public long getProcessStartDelayMs()
	{
		return this.delay_start_ms;
	}
	
	public int getExitValue()
	{
		return this.exit_value;
	}
	
	public void setShutdownTimeoutMs(long aTimeoutMs)
	{
		this.shutdown_timeout_ms = aTimeoutMs;
	}
	
	public long getShutdownTimeoutMs()
	{
		return this.shutdown_timeout_ms;
	}
	
	public void setInitTimeoutMs(long aTimeoutMs)
	{
		this.init_timeout_ms = aTimeoutMs;
	}
	
	public long getInitTimeoutMs()
	{
		return this.init_timeout_ms;
	}
	
	public void setInitSuccessRegex(String aRegex)
	{
		if(aRegex==null || aRegex.trim().length()==0)
			this.patt_init_success = null;
		else
			this.patt_init_success = Pattern.compile(aRegex);
	}
	
	public String getInitSuccessRegex()
	{
		if(this.patt_init_success==null)
			return null;
		else
			return this.patt_init_success.pattern();
	}
	
	public void setInitFailedRegex(String aRegex)
	{
		if(aRegex==null || aRegex.trim().length()==0)
			this.patt_init_failed = null;
		else
			this.patt_init_failed = Pattern.compile(aRegex);
	}
	
	public String getInitFailedsRegex()
	{
		if(this.patt_init_failed==null)
			return null;
		else
			return this.patt_init_failed.pattern();
	}	
	//

	public String getProcessCommand()
	{
		if(this.commands==null)
			return "";
		return String.join(" ",this.commands);
	}

	public void setDependTimeoutMs(long aTimeoutMs)
	{
		this.dep_wait_timeout_ms = aTimeoutMs;
	}
	
	public long getDependTimeoutMs()
	{
		return this.dep_wait_timeout_ms;
	}
	
	public void setDependCheckIntervalMs(long aCheckIntervalMs)
	{
		this.dep_check_interval_ms = aCheckIntervalMs;
	}
	
	public long getDependCheckIntervalMs()
	{
		return this.dep_check_interval_ms;
	}
	
	//////////
	
	public void addDependProcess(HLProcess aDepProcess)
	{
		depends.add(aDepProcess);
	}
	
	public void clearDependProcesses()
	{
		depends.clear();;
	}	
	//////////
	
	public boolean isTerminated()
	{
		return getCurProcessState().is(ProcessState.TERMINATED);
	}
	
	public boolean isProcessAlive()
	{
		return (proc!=null && proc.isAlive()) || (thread!=null && thread.isAlive());
	}
	
	public boolean isStarted()
	{
		return this.run_start_timestamp>0;
	}
	
	public boolean isInitSuccess()
	{
		return this.is_init_success;
	}
	
	private void logDebug(String aMsg)
	{
		logger.log(Level.FINEST, aMsg);
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
	
	private void checkDependenciesB4Start()
	{
		String sPrefix = (id==null?"":"["+id+"] ");
		
		if(depends!=null && depends.size()>0)
		{
			logger.log(Level.INFO, 
					sPrefix + "wait_dependances ...");

			Collection<HLProcess> tmpDepends = new ArrayList<HLProcess>();
			tmpDepends.addAll(depends);
			
			boolean isDependTimeOut = (this.dep_wait_timeout_ms>0);
			StringBuffer sbDepCmd = new StringBuffer();
			
			setCurProcessState(ProcessState.START_WAIT_DEP);
			while(tmpDepends.size()>0 && ProcessState.START_WAIT_DEP.is(getCurProcessState()))
			{
				sbDepCmd.setLength(0);
				
				Iterator<HLProcess> iter = tmpDepends.iterator();
				long lElapsed = System.currentTimeMillis() - this.run_start_timestamp;
				while(iter.hasNext())
				{
					HLProcess d = iter.next();
					
					if(d.isInitSuccess())
					{
						iter.remove();
						continue;
					}
					else if(d.getCurProcessState().isAtLeast(ProcessState.STOPPING))
					{
						String sErr = sPrefix+"Dependance process(es) failed to start ! "+sbDepCmd.toString();
						logger.log(Level.SEVERE, sErr);
						setCurProcessState(ProcessState.START_WAIT_DEP_FAILED);
						onProcessError(this, new Exception(sErr));
					}
					else
					{
						sbDepCmd.append("\n - ");
						sbDepCmd.append(d.id).append(" : ");
						if(d.isRemoteRef())
							sbDepCmd.append("(remote)").append(d.getRemoteHost()==null?"":d.getRemoteHost());
						else
							sbDepCmd.append(d.getProcessCommand());
					}
					
					if(isDependTimeOut && lElapsed >= this.dep_wait_timeout_ms)
					{
						String sErr = sPrefix+"Dependance process(es) init timeout ! "+this.dep_wait_timeout_ms+"ms : "+sbDepCmd.toString();
						logger.log(Level.SEVERE, sErr);
						setCurProcessState(ProcessState.START_INIT_FAILED);
						onProcessError(this, new Exception(sErr));
					}
				}
				
				try {
					Thread.sleep(this.dep_check_interval_ms);
				} catch (InterruptedException e) {
					logger.log(Level.WARNING, e.getMessage(), e);
					onProcessError(this, e);
				}
			}
		}
				
	}
	
	protected static ProcessBuilder initProcessBuilder(final String aCommands[], boolean isDefToScriptDir)
	{
		ProcessBuilder pb = null;
		if(aCommands!=null)
		{
			String sCommandLine = String.join(" ",aCommands);
			if(sCommandLine.trim().length()>0)
			{
				pb = new ProcessBuilder(aCommands);
				if(isDefToScriptDir)
				{
					File fileDir = getCommandScriptDir(sCommandLine);
					if(fileDir!=null)
					{
						pb.directory(fileDir);
					}
				}
				pb.redirectErrorStream(true);
			}
		}
		return pb;
	}
	
	public void run() {
		
		this.run_start_timestamp = System.currentTimeMillis();
		setCurProcessState(ProcessState.STARTING);
		
		String sPrefix = (id==null?"":"["+id+"] ");
		
		logger.log(Level.INFO, sPrefix+"start - "+getProcessId());
		try {
			checkDependenciesB4Start();
			
			if(getCurProcessState().isBefore(ProcessState.STARTED))
			{
				ProcessBuilder pb = initProcessBuilder(this.commands, this.is_def_script_dir);
				try {
					proc = pb.start();
				} catch (IOException e1) {
					onProcessError(this, e1);
				}
				
				onProcessStart(this);
				long lStart = System.currentTimeMillis();
				BufferedReader rdr = null;
				BufferedWriter wrt = null;
				
				SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.SSS ");
				
				if(proc!=null)
				{
					try {
						rdr = new BufferedReader(new InputStreamReader(proc.getInputStream()));
						
						if(this.output_filename!=null)
						{
							if(this.output_filename.trim().length()>0)
							{
								File fileOutput = new File(this.output_filename);
								if(!fileOutput.exists())
								{
									if(fileOutput.getParentFile()!=null)
									{
										fileOutput.getParentFile().mkdirs();
									}
								}
								
								wrt = new BufferedWriter(new FileWriter(fileOutput, true));
							}
						}
						
						String sLine = null;
						
						if(rdr.ready())
							sLine = rdr.readLine();
						
						String sDebugLine = null;
						
						while(proc.isAlive() || sLine!=null)
						{
		
							if(sLine!=null)
							{
								sDebugLine = sPrefix + df.format(System.currentTimeMillis()) + sLine;
		
								if(wrt!=null)
								{
									wrt.write(sDebugLine);
									wrt.newLine();
									wrt.flush();
								}
								
								if(this.is_output_console)
								{
									System.out.println(sLine);
								}
								
								logDebug(sDebugLine);
								
								if(sLine.length()>0)
								{
									if(!this.is_init_failed && this.patt_init_failed!=null)
									{
										Matcher m = this.patt_init_failed.matcher(sLine);
										this.is_init_failed =  m.find();
										if(this.is_init_failed)
										{
											String sErr = sPrefix + "init_error - Elapsed: "+milisec2Words(System.currentTimeMillis()-this.run_start_timestamp);
											logger.log(Level.SEVERE, sErr);
											setCurProcessState(ProcessState.START_INIT_FAILED);
											onProcessError(this, new Exception(sErr));
											break;
										}
									}
								
									if(!this.is_init_success)
									{
										if(this.patt_init_success!=null)
										{
											Matcher m = this.patt_init_success.matcher(sLine);
											this.is_init_success = m.find();
											if(this.is_init_success)
											{
												setCurProcessState(ProcessState.STARTED);
												logger.log(Level.INFO, 
														sPrefix + "init_success - Elapsed: "+milisec2Words(System.currentTimeMillis()-this.run_start_timestamp));
											}
										}
									}
								}
							}
							else
							{
								
								try {
									//let the process rest awhile when no output
									Thread.sleep(100);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
									onProcessError(this,e);
									break;
								}
							}
							
							if(!this.is_init_success)
							{
								if(this.init_timeout_ms>0)
								{
									long lElapsed = System.currentTimeMillis() - lStart;
									if(lElapsed>=this.init_timeout_ms)
									{
										String sErr = sPrefix+"Init timeout ! "+milisec2Words(lElapsed)+" - "+getProcessCommand();
										this.is_init_success = false;
										logger.log(Level.SEVERE, sErr);
										setCurProcessState(ProcessState.START_INIT_TIMEOUT);
										onProcessError(this, new Exception(sErr));
										break;
									}
								}
								
								if(this.patt_init_success==null)
								{
									this.is_init_success = true;
									setCurProcessState(ProcessState.STARTED);
								}
							}
							
							sLine = null;
							if(rdr.ready())
							{
								sLine = rdr.readLine();
							}
						}
					} catch (Throwable e) {
						setCurProcessState(ProcessState.STOPPING);
						this.exit_value = -1;
						logger.log(Level.SEVERE, e.getMessage(), e);
						onProcessError(this, e);
					}
					finally
					{
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
		finally
		{
			long lElapsed = (System.currentTimeMillis()-this.run_start_timestamp);
			logger.log(Level.INFO, sPrefix+"end - "+getProcessCommand()+" (elapsed: "+milisec2Words(lElapsed)+")");

			executeTerminateCmd();
			
			if(this.is_init_failed || !this.is_init_success)
			{
				this.exit_value = -1;
			}
			onProcessTerminate(this);
			setCurProcessState(ProcessState.TERMINATED);
		}
	}
	
	private void executeTerminateCmd()
	{
		if(!this.is_exec_terminate_cmd && isStarted())
		{
			this.is_exec_terminate_cmd = true;
			String sPrefix = (id==null?"":id);
			String sEndCmd = getTerminatedCommand();
			if(sEndCmd!=null && sEndCmd.trim().length()>0)
			{
				try {
					System.out.println("[Termination] "+sPrefix+" : execute terminated command - "+sEndCmd);
					
					String sSplitEndCmd[] = HLProcessConfig.splitCommands(this, sEndCmd);					
					setCurProcessState(ProcessState.STOP_EXEC_CMD);
					ProcessBuilder pb = initProcessBuilder(sSplitEndCmd, this.is_def_script_dir);
					Process procTeminate = pb.start();
					
					BufferedReader rdr = null;
					BufferedWriter wrt = null;
					
					sPrefix = (id==null?"":"["+id+"] ");
					SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.SSS ");
					
					try {
						rdr = new BufferedReader(new InputStreamReader(procTeminate.getInputStream()));
						
						if(this.output_filename!=null)
						{
							if(this.output_filename.trim().length()>0)
							{
								File fileOutput = new File(this.output_filename);
								if(!fileOutput.exists())
								{
									if(fileOutput.getParentFile()!=null)
									{
										fileOutput.getParentFile().mkdirs();
									}
								}
								
								wrt = new BufferedWriter(new FileWriter(fileOutput, true));
							}
						}
						
						String sLine = null;
						
						if(rdr.ready())
							sLine = rdr.readLine();
						
						String sDebugLine = null;
						
						while(procTeminate.isAlive() || sLine!=null)
						{
							if(sLine!=null)
							{
								sDebugLine = sPrefix + df.format(System.currentTimeMillis()) + sLine;
		
								if(wrt!=null)
								{
									wrt.write(sDebugLine);
									wrt.newLine();
									wrt.flush();
								}
								
								if(this.is_output_console)
								{
									System.out.println(sLine);
								}					
							}
							
							sLine = null;
							if(rdr.ready())
							{
								sLine = rdr.readLine();
							}
						}
					}
					finally
					{
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
				} catch (IOException e) {
					
					if(e.getMessage()!=null)
					{
						if(e.getMessage().indexOf("Cannot run program ")>-1)
						{
							if(sEndCmd.startsWith("."))
							{
								System.out.println("[Termination] Current working directory : "+new File(".").getAbsolutePath());
							}
						}
					}
					
					e.printStackTrace();
				}
			}
		}		
	}
	
	public void setEventListener(HLProcessEvent event)
	{
		this.listener = event;
	}

	private void onProcessStart(HLProcess aHLProcess)
	{
		if(this.listener!=null)
			listener.onProcessStart(aHLProcess);
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
		setCurProcessState(ProcessState.STARTING);
		thread = new Thread(this);
		thread.setDaemon(isRunAsDaemon());
		thread.start();
		return thread;
	}
	
	public void terminateProcess()
	{
		setCurProcessState(ProcessState.STOPPING);
		executeTerminateCmd();
	}
	
	public static String milisec2Words(long aElapsed)
	{
		StringBuffer sb = new StringBuffer();
		long lTmp = aElapsed;
		
		long _sec 	= 1000;
		long _min 	= _sec*60;
		long _hour 	= _min*60;
		long _day 	= _hour*24;
		
		if(lTmp>=_day) //24 hours
		{
			sb.append(lTmp / _day).append("d ");
			lTmp = lTmp % _day;
		}
		
		if(lTmp>=_hour)
		{
			sb.append(lTmp / _hour).append("h ");
			lTmp = lTmp % _hour;
		}
		
		if(lTmp>=_min)
		{
			sb.append(lTmp / _min).append("m ");
			lTmp = lTmp % _min;
		}
		
		if(lTmp>=_sec)
		{
			sb.append(lTmp / _sec).append("s ");
			lTmp = lTmp % _sec;
		}
		
		if(lTmp>0)
		{
			sb.append(lTmp).append("ms ");
		}
		
		return sb.toString().trim();
	}
	
	public String getProcessStateHist()
	{
		StringBuffer sbState = new StringBuffer();
		Map<ProcessState, Long> mapStates = getProcessStates();
		if(mapStates.size()>0)
		{
			Long[] arrStartTimes = mapStates.values().toArray(new Long[mapStates.size()]);
			ProcessState[] arrStates = mapStates.keySet().toArray(new ProcessState[mapStates.size()]);
			SimpleDateFormat df = new SimpleDateFormat("dd-MMM HH:mm:ss");
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
				if(i==0)
				{
					sbState.append(" (").append(df.format(arrStartTimes[i])).append(")");
				}
				else if(i>=arrStates.length-1)
				{
					lElapseMs = System.currentTimeMillis()-arrStartTimes[i];
				}
				else
				{
					lElapseMs = arrStartTimes[i]-arrStartTimes[i-1];
				}
				
				if(lElapseMs!=null && lElapseMs>0)
				{
					sbState.append(" (").append(milisec2Words(lElapseMs)).append(")");
				}
			}
		}
		return sbState.toString();
	}
	
	public String toString()
	{
		String sPrefix = "["+getProcessId()+"]";
		StringBuffer sb = new StringBuffer();
		sb.append("\n").append(sPrefix).append("is.disabled=").append(isDisabled());
		sb.append("\n").append(sPrefix).append("is.process.alive=").append(isProcessAlive());
		sb.append("\n").append(sPrefix).append("is.remote=").append(isRemoteRef());
		
		
		sb.append("\n").append(sPrefix).append("runtime.state.lifecycle=").append(getProcessStateHist());

		sb.append("\n").append(sPrefix).append("process.command.").append(HLProcessConfig.osname).append("=").append(getProcessCommand());
		sb.append("\n").append(sPrefix).append("process.runas.darmon=").append(isRunAsDaemon());
		sb.append("\n").append(sPrefix).append("process.command.block.start=").append(getCommandBlockStart());
		sb.append("\n").append(sPrefix).append("process.command.block.end=").append(getCommandBlockEnd());
		sb.append("\n").append(sPrefix).append("process.start.delay.ms=").append(getProcessStartDelayMs());
		sb.append("\n").append(sPrefix).append("process.terminate.cmd=").append(getTerminatedCommand());
		sb.append("\n").append(sPrefix).append("process.shutdown.all.on.termination=").append(isShutdownAllOnTermination());
		sb.append("\n").append(sPrefix).append("process.shutdown.all.timeout.ms=").append(isShutdownAllOnTermination());
		
		sb.append("\n").append(sPrefix).append("init.timeout.ms=").append(this.init_timeout_ms);
		sb.append("\n").append(sPrefix).append("init.success.regex=").append(this.patt_init_success==null?"":this.patt_init_success.pattern());
		
		StringBuffer sbDeps = new StringBuffer();
		if(this.depends!=null)
		{
			for(HLProcess d : this.depends)
			{
				if(sbDeps.length()>0)
				{
					sbDeps.append(",");
				}
				sbDeps.append(d.getProcessId());
				
				if(d.isRemoteRef())
					sbDeps.append("(remote)");
			}
		}
		sb.append("\n").append(sPrefix).append("dep.processes=").append(sbDeps.toString());
		sb.append("\n").append(sPrefix).append("dep.timeout.ms=").append(this.dep_wait_timeout_ms);
		sb.append("\n").append(sPrefix).append("dep.check.interval.ms=").append(this.dep_check_interval_ms);
		return sb.toString();
	}
	
}