package hl.common.shell;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import hl.common.shell.utils.TimeUtil;

public class HLProcessCmd 
{
	private String id					= null;
	private String output_filename		= null;
	private boolean is_def_script_dir 	= false;
	private boolean is_output_console	= false;
	
	private Pattern patt_init_failed	= null;
	private Pattern patt_init_success	= null;
	private long init_timeout_ms		= 0;
	
	private long delay_start_ms			= 0;
	
	private long dep_check_interval_ms	= 100;
	private long dep_wait_timeout_ms	= 5 * TimeUtil._MIN_ms; 
	
	private boolean shutdown_all_on_termination = false;	
	private long shutdown_timeout_ms			= 5 * TimeUtil._MIN_ms; 
	
	private Collection<HLProcess> depends 	= new ArrayList<HLProcess>();
	private String command_block_start		= "";
	private String command_block_end		= "";
	
	private String[] commands				= null;
	private String command_end_regex		= "";
	private long command_idle_timeout_ms	= 0;
	
	private boolean runas_daemon		= false;
	private boolean disabled			= false;

	private boolean remote_ref			= false;
	private String remote_hostname		= null;

	protected Thread thread 			= null;
	protected Process proc 				= null; 
	
	public static Logger logger 		= Logger.getLogger(HLProcess.class.getName());

	
	public HLProcessCmd(String aId, String[] aShellCmd)
	{
		this.commands = aShellCmd;
		init(aId);
	}
	
	public HLProcessCmd(String aId)
	{
		init(aId);
	}
	
	private void init(String aId)
	{
		this.id = aId;
	}
	
	public boolean isProcessAlive()
	{
		return (proc!=null && proc.isAlive()) || (thread!=null && thread.isAlive());
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
	
	
	public void setCommandEndRegex(String aEndRegex)
	{
		this.command_end_regex = aEndRegex;
	}
	
	public String getCommandEndRegex()
	{
		return this.command_end_regex;
	}	
	
	public void setCommandIdleTimeoutMs(long aTimeoutMs)
	{
		this.command_idle_timeout_ms = aTimeoutMs;
	}
	
	public long getCommandIdleTimeoutMs()
	{
		return this.command_idle_timeout_ms;
	}	
	
	public void setProcessCommand(String[] aShellCmd)
	{
		this.commands = aShellCmd;
	}
	
	public void setProcessCommand(List<String> aShellCmdList)
	{
		this.commands = aShellCmdList.toArray(new String[aShellCmdList.size()]);
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
	
	public Pattern getInitSuccessRegexPattern()
	{
		return this.patt_init_success;
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
	
	public Pattern getInitFailedRegexPattern()
	{
		return this.patt_init_failed;
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
	
	public String[] getCommands()
	{
		return this.commands;
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
	
	public Collection<HLProcess> getDependProcesses()
	{
		return depends;
	}
	
	public void addDependProcess(HLProcess aDepProcess)
	{
		depends.add(aDepProcess);
	}
	
	public void clearDependProcesses()
	{
		depends.clear();;
	}	
	//////////
	
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
	
	public String toString()
	{
		String sPrefix = "["+getProcessId()+"]";
		StringBuffer sb = new StringBuffer();
		
		sb.append("\n").append(sPrefix).append(HLProcessConfig._PROP_KEY_DISABLED).append("=").append(isDisabled());

		sb.append("\n").append(sPrefix).append("is.remote").append("=").append(isRemoteRef());
		sb.append("\n").append(sPrefix).append("is.process.alive").append("=").append(isProcessAlive());
		
		sb.append("\n").append(sPrefix).append(HLProcessConfig._PROP_KEY_SHELL_RUNAS_DAEMON).append("=").append(isRunAsDaemon());
		sb.append("\n").append(sPrefix).append(HLProcessConfig._PROP_KEY_SHELL_START_DELAY).append("=").append(getProcessStartDelayMs());
		
		sb.append("\n").append(sPrefix).append(HLProcessConfig._PROP_KEY_SHELL_CMD).append("=").append(getProcessCommand());
		sb.append("\n").append(sPrefix).append(HLProcessConfig._PROP_KEY_SHELL_CMD_END_REGEX).append("=").append(getCommandEndRegex());
		sb.append("\n").append(sPrefix).append(HLProcessConfig._PROP_KEY_SHELL_CMD_IDLE_TIMEOUT_MS).append("=").append(getCommandIdleTimeoutMs());
		
		sb.append("\n").append(sPrefix).append(HLProcessConfig._PROP_KEY_INIT_SUCCESS_REGEX).append("=").append(getInitSuccessRegex());
		sb.append("\n").append(sPrefix).append(HLProcessConfig._PROP_KEY_INIT_FAILED_REGEX).append("=").append(getInitFailedsRegex());
		sb.append("\n").append(sPrefix).append(HLProcessConfig._PROP_KEY_INIT_TIMEOUT_MS).append("=").append(getInitTimeoutMs());
			
		sb.append("\n").append(sPrefix).append(HLProcessConfig._PROP_KEY_SHELL_SHUTDOWN_ALL_TIMEOUT_MS).append("=").append(getShutdownTimeoutMs());
		sb.append("\n").append(sPrefix).append(HLProcessConfig._PROP_KEY_SHELL_SHUTDOWN_ALL_ON_TEMINATE).append("=").append(isShutdownAllOnTermination());
		
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
		sb.append("\n").append(sPrefix).append(HLProcessConfig._PROP_KEY_DEP).append("=").append(sbDeps.toString());
		sb.append("\n").append(sPrefix).append(HLProcessConfig._PROP_KEY_DEP_TIMEOUT_MS).append("=").append(getDependTimeoutMs());
		sb.append("\n").append(sPrefix).append(HLProcessConfig._PROP_KEY_DEP_CHK_INTERVAL_MS).append("=").append(getDependCheckIntervalMs());
		return sb.toString();
	}
	
}