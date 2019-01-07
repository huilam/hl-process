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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HLProcess implements Runnable
{
	private final static String _VERSION = "HLProcess alpha v0.56";
	
	private final static long _SEC_ms 	= 1000;
	private final static long _MIN_ms 	= 60 * _SEC_ms;
	private final static long _HOUR_ms 	= 60 * _MIN_ms;
	private final static long _DAY_ms 	= 24 * _HOUR_ms;
	
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
		public boolean isAfter(ProcessState aProcessState)
		{
			return getCode() > aProcessState.getCode();
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
	
	private String[] commands				= null;
	private String command_end_regex		= null;
	private String terminate_command  		= null;
	private String terminate_end_regex		= null;
	private long terminate_idle_timeout_ms	= 5 * _MIN_ms;
	
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
	
	public static String getVersion()
	{
		return _VERSION;
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

	public void setCommandBlockStart(String aBlockSeparator)
	{
		this.command_block_start = aBlockSeparator;
	}
	
	public String getCommandBlockStart()
	{
		return this.command_block_start;
	}
	
	public void setCommandEndRegex(String aEndRegex)
	{
		this.command_end_regex = aEndRegex;
	}
	
	public String getCommandEndRegex()
	{
		return this.command_end_regex;
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

	public void setTerminateCommand(String aShellCommand)
	{
		this.terminate_command = aShellCommand;
	}
	
	public String getTerminateCommand()
	{
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
	
	public boolean isTerminating()
	{
		return getCurProcessState().isAfter(ProcessState.STARTED);
	}
	
	public boolean isProcessAlive()
	{
		return (proc!=null && proc.isAlive()) || (thread!=null && thread.isAlive());
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
	
	private boolean checkDependenciesB4Start()
	{
		boolean isWaitDepOk = true;
		String sPrefix = (id==null?"":"["+id+"] ");
		
		if(depends!=null && depends.size()>0 && isNotStarted())
		{
			setCurProcessState(ProcessState.START_WAIT_DEP);
			logger.log(Level.INFO, 
					sPrefix + "wait_dependances ... "+depends.size());


			Collection<HLProcess> tmpDepends = new ArrayList<HLProcess>();
			tmpDepends.addAll(depends);
			
			boolean isDependTimeOut = (this.dep_wait_timeout_ms>0);
			StringBuffer sbDepCmd = new StringBuffer();
			
			
			while(tmpDepends.size()>0 && ProcessState.START_WAIT_DEP.is(getCurProcessState()))
			{
				sbDepCmd.setLength(0);
				
				Iterator<HLProcess> iter = tmpDepends.iterator();
				long lElapsed = System.currentTimeMillis() - this.run_start_timestamp;
				while(iter.hasNext())
				{
					HLProcess d = iter.next();
					
					if(d.isStarted())
					{
						iter.remove();
						continue;
					}
					else if(d.isTerminating())
					{
						String sErr = sPrefix+"Dependance process(es) failed to start ! "+sbDepCmd.toString();
						logger.log(Level.SEVERE, sErr);
						isWaitDepOk = false;
						setCurProcessState(ProcessState.START_WAIT_DEP_FAILED);
						onProcessError(this, new Exception(sErr));
						break;
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
						isWaitDepOk = false;
						setCurProcessState(ProcessState.START_WAIT_DEP_TIMEOUT);
						onProcessError(this, new Exception(sErr));
						break;
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
		
		return isWaitDepOk;
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
		String sPrefix = (id==null?"":"["+id+"] ");
		try {
			if(isNotStarting())
			{
				onProcessStarting(this);
				logger.log(Level.INFO, sPrefix+"start - "+getProcessId());
				boolean isDepOk = checkDependenciesB4Start();
				
				if(isNotStarted() && isDepOk)
				{
					ProcessBuilder pb = initProcessBuilder(this.commands, this.is_def_script_dir);
					try {
						proc = pb.start();
					} catch (IOException e1) {
						onProcessError(this, e1);
					}
					setCurProcessState(ProcessState.START_INIT);
					
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
							
							Pattern pattCmdEnd = null;
							
							String sCmdEndRegex = getCommandEndRegex();
							if(sCmdEndRegex!=null && sCmdEndRegex.trim().length()>0)
							{
								pattCmdEnd = Pattern.compile(sCmdEndRegex);
							}
							
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
											if(this.init_timeout_ms>0)
											{
												long lElapsed = System.currentTimeMillis() - lStart;
												if(lElapsed>=this.init_timeout_ms)
												{
													String sErr = sPrefix+"Init timeout ! "+milisec2Words(lElapsed)+" - "+getProcessCommand();
													this.is_init_success = false;
													logger.log(Level.SEVERE, sErr);
													break;
												}
											}
											
											if(this.patt_init_failed!=null)
											{
												Matcher m = this.patt_init_failed.matcher(sLine);
												this.is_init_failed =  m.find();
												if(this.is_init_failed)
												{
													String sErr = sPrefix + "init_error - Elapsed: "+milisec2Words(System.currentTimeMillis()-this.run_start_timestamp);
													logger.log(Level.SEVERE, sErr);
													proc.destroy();
													break;
												}
											}

											if(this.patt_init_success!=null)
											{
												Matcher m = this.patt_init_success.matcher(sLine);
												this.is_init_success = m.find();
												if(this.is_init_success)
												{
													logger.log(Level.INFO, 
															sPrefix + "init_success - Elapsed: "+milisec2Words(System.currentTimeMillis()-this.run_start_timestamp));
												}
											}
											else
											{
												this.is_init_success = true;
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
	
	private boolean executeTerminateCmd()
	{
		boolean isExecuted = false;
		if(!this.is_exec_terminate_cmd && isStarted())
		{
			isExecuted = true;
			this.is_exec_terminate_cmd = true;
			String sPrefix = (id==null?"":id);
			String sEndCmd = getTerminateCommand();
			if(sEndCmd!=null && sEndCmd.trim().length()>0)
			{
				try {
					SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.SSS ");
					sPrefix = (id==null?"":"["+id+"] ");
					
					System.out.println("[Termination] "+sPrefix+" : execute terminated command - "+sEndCmd);
					
					String sSplitEndCmd[] = HLProcessConfig.splitCommands(this, sEndCmd);					
					ProcessBuilder pb = initProcessBuilder(sSplitEndCmd, this.is_def_script_dir);
					
					BufferedReader rdr = null;
					BufferedWriter wrt = null;
					
					setCurProcessState(ProcessState.STOP_EXEC_CMD);
					Process procTeminate = pb.start();
					
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
						
						String sEndRegex 		= getTerminateEndRegex();
						Pattern pattEndRegex 	= null;
						if(sEndRegex!=null && sEndRegex.trim().length()>0)
						{
							pattEndRegex = Pattern.compile(sEndRegex);
						}
						Matcher m = null;
						long lIdleStartTimestamp = System.currentTimeMillis();
						long lIdleTimeout = 5* _MIN_ms;
						
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
								
								if(pattEndRegex!=null)
								{
									m = pattEndRegex.matcher(sLine);
									if(m.find())
									{
										System.out.println("[Termination] Termination end regex matched. - "+sEndRegex);
										break;
									}
								}
							}
							else
							{
								lIdleStartTimestamp = System.currentTimeMillis();
							}
							
							sLine = null;
							if(rdr.ready())
							{
								sLine = rdr.readLine();
							}
							
							long lIdleElapsed = System.currentTimeMillis()-lIdleStartTimestamp;
							if(lIdleElapsed >= lIdleTimeout)
							{
								System.out.println("[Termination] Termination command killed due to idle timeout. - "+lIdleTimeout+"ms");
								break;
							}
							
							try {
								//let the process rest awhile when no output
								Thread.sleep(100);
							} catch (InterruptedException e) {
								e.printStackTrace();
								break;
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
				finally
				{
					System.out.println("[Termination] "+sPrefix+" : complete terminated command.");
				}
			}
		}	
		return isExecuted;
	}
	
	public void setEventListener(HLProcessEvent event)
	{
		this.listener = event;
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
		if(thread==null)
		{
			thread = new Thread(this);
			thread.setDaemon(isRunAsDaemon());
			thread.start();
		}
		return thread;
	}
	
	public void terminateProcess()
	{
		if(getCurProcessState().isBefore(ProcessState.STOPPING))
		{
			setCurProcessState(ProcessState.STOPPING);
			executeTerminateCmd();
		}
		
	}
	
	public static String milisec2Words(long aElapsed)
	{
		StringBuffer sb = new StringBuffer();
		long lTmp = aElapsed;
		
		if(lTmp>=_DAY_ms) //24 hours
		{
			sb.append(lTmp / _DAY_ms).append("d ");
			lTmp = lTmp % _DAY_ms;
		}
		
		if(lTmp>=_HOUR_ms)
		{
			sb.append(lTmp / _HOUR_ms).append("h ");
			lTmp = lTmp % _HOUR_ms;
		}
		
		if(lTmp>=_MIN_ms)
		{
			sb.append(lTmp / _MIN_ms).append("m ");
			lTmp = lTmp % _MIN_ms;
		}
		
		if(lTmp>=_SEC_ms)
		{
			sb.append(lTmp / _SEC_ms).append("s ");
			lTmp = lTmp % _SEC_ms;
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
		sb.append("\n").append(sPrefix).append("process.runas.daemon=").append(isRunAsDaemon());
		sb.append("\n").append(sPrefix).append("process.command.block.start=").append(getCommandBlockStart());
		sb.append("\n").append(sPrefix).append("process.command.block.end=").append(getCommandBlockEnd());
		sb.append("\n").append(sPrefix).append("process.start.delay.ms=").append(getProcessStartDelayMs());
		sb.append("\n").append(sPrefix).append("process.terminate.cmd=").append(getTerminateCommand());
		sb.append("\n").append(sPrefix).append("process.terminate.end.regex=").append(getTerminateCommand());
		
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