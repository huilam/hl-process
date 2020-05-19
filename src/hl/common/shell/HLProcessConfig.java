package hl.common.shell;

import java.io.File;
import java.io.IOException;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import hl.common.shell.HLProcess;
import hl.common.shell.plugins.output.StateOutput;
import hl.common.shell.utils.FileUtil;
import hl.common.shell.utils.PropUtil;

public class HLProcessConfig {
	
	private static String _ENVVAR_JAR_PATH 			= "jar:path";
	//
	public static String _PROP_FILENAME 			= "process.properties";
	//
	public static String _PROP_PREFIX_PROCESS 		= "process.";
	
	public static String _PROP_KEY_DISABLED 		= "disabled";

	//-- SYSTEM
	public static String _PROP_KEY_SYSTEM							= "system.";
	public static String _PROP_KEY_SYSTEM_ASCIIART_FOLDER	 		= _PROP_KEY_SYSTEM+"asciiart.folder";

	//-- SHELL
	public static String _PROP_KEY_SHELL							= "shell.";
	public static String _PROP_KEY_SHELL_CMD_NO_OS	 				= _PROP_KEY_SHELL+"command";
	public static String _PROP_KEY_SHELL_CMD	 					= _PROP_KEY_SHELL+"command.{os.name}";
	public static String _PROP_KEY_SHELL_CMD_END_REGEX 				= _PROP_KEY_SHELL+"command.end.regex";
	public static String _PROP_KEY_SHELL_CMD_IDLE_TIMEOUT_MS 		= _PROP_KEY_SHELL+"command.idle.timeout.ms";
	public static String _PROP_KEY_SHELL_CMD_BLOCK					= _PROP_KEY_SHELL+"command.block";
	public static String _PROP_KEY_SHELL_START_DELAY				= _PROP_KEY_SHELL+"start.delay.ms";
	
	public static String _PROP_KEY_SHELL_OUTPUT_FILENAME_NO_OS		= _PROP_KEY_SHELL+"output.filename";
	public static String _PROP_KEY_SHELL_OUTPUT_FILENAME			= _PROP_KEY_SHELL+"output.filename.{os.name}";
	
	public static String _PROP_KEY_SHELL_OUTPUT_FILE_ROLL_COUNT		= _PROP_KEY_SHELL+"output.file.autoroll.count";
	public static String _PROP_KEY_SHELL_OUTPUT_FILE_ROLL_BYTESIZE	= _PROP_KEY_SHELL+"output.file.autoroll.size.bytes";
	public static String _PROP_KEY_SHELL_OUTPUT_CONSOLE 			= _PROP_KEY_SHELL+"output.console";
	
	public static String _PROP_KEY_SHELL_RUNAS_DAEMON 				= _PROP_KEY_SHELL+"runas.daemon";
	public static String _PROP_KEY_SHELL_DEF2_SCRIPT_DIR 			= _PROP_KEY_SHELL+"default.to.script.dir";

	public static String _PROP_KEY_SHELL_TERMINATE_CMD_NO_OS		= _PROP_KEY_SHELL+"terminate.command";
	public static String _PROP_KEY_SHELL_TERMINATE_CMD				= _PROP_KEY_SHELL+"terminate.command.{os.name}";
	public static String _PROP_KEY_SHELL_TERMINATE_END_REGEX		= _PROP_KEY_SHELL+"terminate.end.regex";
	public static String _PROP_KEY_SHELL_TERMINATE_IDLE_TIMEOUT_MS	= _PROP_KEY_SHELL+"terminate.idle.timeout.ms";
	
	public static String _PROP_KEY_SHELL_SHUTDOWN_ALL_ON_TEMINATE 	= _PROP_KEY_SHELL+"shutdown.all.on.termination";
	public static String _PROP_KEY_SHELL_SHUTDOWN_ALL_TIMEOUT_MS 	= _PROP_KEY_SHELL+"shutdown.all.timeout.ms";

	//-- INIT
	public static String _PROP_KEY_INIT					= "init.";
	public static String _PROP_KEY_INIT_TIMEOUT_MS 		= _PROP_KEY_INIT+"timeout.ms";
	public static String _PROP_KEY_INIT_SUCCESS_REGEX	= _PROP_KEY_INIT+"success.regex";
	public static String _PROP_KEY_INIT_FAILED_REGEX	= _PROP_KEY_INIT+"failed.regex";
	public static String _PROP_KEY_INIT_ERROR_MSG		= _PROP_KEY_INIT+"error.message";
	
	//-- Dependencies
	public static String _PROP_KEY_DEP					= "dependence.";
	public static String _PROP_KEY_DEP_PROCESSES_LOCAL	= _PROP_KEY_DEP+"processes.local";
	public static String _PROP_KEY_DEP_PROCESSES_REMOTE	= _PROP_KEY_DEP+"processes.remote";
	public static String _PROP_KEY_DEP_CHK_INTERVAL_MS 	= _PROP_KEY_DEP+"check.interval.ms";
	public static String _PROP_KEY_DEP_TIMEOUT_MS 		= _PROP_KEY_DEP+"timeout.ms";

	
	public static String osname 	= null;
	
	public static char commandSpace = ' ';
	
	protected static Pattern pattEnvVar 				= Pattern.compile("\\{(.+?)\\}");
	protected static Pattern pattProcessId 				= Pattern.compile(_PROP_PREFIX_PROCESS+"(.+?)\\.");	
	private Map<String, HLProcess> mapProcesses 		= new LinkedHashMap<String, HLProcess>();
	private Map<String, HLProcess> mapDisabledProcesses = new HashMap<String, HLProcess>();
	
	public static Logger logger = Logger.getLogger(HLProcessConfig.class.getName());
	//
	
	static {
		String sOsNameAttrKey = "os.name";
		String sOsName = System.getProperty(sOsNameAttrKey).toLowerCase();
		if(sOsName.indexOf("windows")>-1)
			sOsName = "win";
		else if(sOsName.indexOf("linux")>-1)
			sOsName = "linux";
		else if(sOsName.indexOf("mac")>-1)
			sOsName = "mac";
		osname = sOsName;
		_PROP_KEY_SHELL_CMD = _PROP_KEY_SHELL_CMD.replaceAll("\\{"+sOsNameAttrKey+"\\}", osname);
		_PROP_KEY_SHELL_TERMINATE_CMD = _PROP_KEY_SHELL_TERMINATE_CMD.replaceAll("\\{"+sOsNameAttrKey+"\\}", osname);
		_PROP_KEY_SHELL_OUTPUT_FILENAME = _PROP_KEY_SHELL_OUTPUT_FILENAME.replaceAll("\\{"+sOsNameAttrKey+"\\}", osname);
	}
	
	//
	public HLProcessConfig(String aPropFileName) throws IOException
	{
		init(aPropFileName);
	}
	
	public HLProcessConfig(Properties aProperties) throws IOException
	{
		init(aProperties);
	}
	
	public void init(String aPropFilename) throws IOException
	{		
		Properties props = null;
		if(aPropFilename!=null && aPropFilename.trim().length()>0)
		{
			props = PropUtil.loadProperties(aPropFilename);
		}
		
		/////////
		if(props==null || props.size()==0)
		{
			props = PropUtil.loadProperties(_PROP_FILENAME);
		}
		
		init(props);
	}
	
	protected static String[] splitCommands(HLProcess aHLProcess, String aCmdString)
	{
		String sGrpStart = aHLProcess.getCommandBlockStart();
		String sGrpEnd = aHLProcess.getCommandBlockEnd();
		String sCmdSpace = String.valueOf(commandSpace);
		
		if(sGrpStart==null)
			sGrpStart = "";
		
		if(sGrpEnd==null)
			sGrpEnd = "";


		if(sGrpStart.trim().length()==0 && sGrpEnd.trim().length()==0)
		{
			return aCmdString.split(sCmdSpace);
		}
		
		boolean isSameStartEndChar = sGrpStart.equals(sGrpEnd);
		
		List<String> list = new ArrayList<String>();
		
		StringBuffer sb = new StringBuffer();
		
		boolean isGrouping = false;
		for(char ch : aCmdString.toCharArray())
		{
			String sCh = String.valueOf(ch);
			if(sGrpStart.equals(sCh) || sGrpEnd.equals(sCh) )
			{
				if(sb.length()>0)
				{
					list.add(sb.toString());
				}
				sb.setLength(0);
				
				if(isSameStartEndChar)
				{
					isGrouping = !isGrouping;
				}
				else
				{
					isGrouping = sGrpStart.equals(sCh);
				}
			}
			
			else if(commandSpace == ch)
			{
				if(isGrouping)
				{
					sb.append(ch);
				}
				else
				{
					if(sb.length()>0)
					{
						list.add(sb.toString());
					}
					sb.setLength(0);
				}
			}
			else
			{
				sb.append(ch);
			}				
		}
		
		if(sb.length()>0)
		{
			list.add(sb.toString());
		}
		
		return list.toArray(new String[list.size()]);
	}
	
	public void init(Properties aProperties) throws IOException
	{		
		if(aProperties==null)
			aProperties = new Properties();
		
		Matcher m = null;
		
		Map<String, Map<String, String>> mapPidConfigs = new HashMap<String, Map<String, String>> ();
		
		Iterator iter = aProperties.keySet().iterator();

		while(iter.hasNext())
		{
			String sKey = (String) iter.next();
			String sVal = aProperties.getProperty(sKey);
			
			if(sKey.contains(".dependance."))
			{
				throw new RuntimeException("Please change 'depend(a)nce' to 'depend(e)nce' - "+sKey);
			}
			
			
			int iPattGrp = 0;
			m = pattEnvVar.matcher(sVal);
			while(m.find())
			{
				iPattGrp++;
				String sEnvName = m.group(iPattGrp);				
				String sEnvVal	= null;
				logger.log(Level.INFO, "[env-val] "+sEnvName+" detected : "+sKey+"="+sVal);
				
				if(sEnvName.equalsIgnoreCase(_ENVVAR_JAR_PATH))
				{
					File f = null;
							
					ProtectionDomain pd = HLProcessConfig.class.getProtectionDomain();
					if(pd!=null)
					{
						CodeSource cs = pd.getCodeSource();
						if(cs!=null)
						{
							f = new File(cs.getLocation().getFile());
							if(f.getName().toLowerCase().endsWith(".jar"))
							{
								f = f.getParentFile();
							}
						}
					}
					
					if(f==null)
					{
						f = FileUtil.getJavaClassPath(HLProcessConfig.class);	
					}
					
					if(f!=null)
					{
						sEnvVal = f.getCanonicalPath();
					}
				}
				//
				if(sEnvVal == null)
				{
					sEnvVal = System.getenv(sEnvName);	
				}
				//
				logger.log(Level.INFO, "[config] sEnvVal="+sEnvVal);
				//
				if(sEnvVal!=null)
				{
					sEnvVal = sEnvVal.replaceAll("\\\\", "/");
					
					String sNewVal = sVal.replaceAll("\\{"+sEnvName+"\\}", sEnvVal);
					aProperties.setProperty(sKey, sNewVal);
					logger.log(Level.INFO, "[config] replaced '"+sEnvName+"' - "+sNewVal);
				}
			}
			
			m = pattProcessId.matcher(sKey);
			if(m.find())
			{
				String sPID = m.group(1);
				String sConfigKey = sKey.substring(m.end(1)+1);
				String sConfigVal = aProperties.getProperty(sKey);
				
				if(sConfigVal==null)
					sConfigVal = "";
				
				sConfigVal = sConfigVal.replaceAll("  {2,8}", " ").trim();
				
				if(sConfigVal.length()==0)
					continue;
				
				Map<String, String> mapProcessConfig = mapPidConfigs.get(sPID);
				if(mapProcessConfig==null)
					mapProcessConfig = new HashMap<String, String>();
				
				mapProcessConfig.put(sConfigKey, sConfigVal);
				mapPidConfigs.put(sPID, mapProcessConfig);
			}
			else
			{
				if(sKey.equalsIgnoreCase(_PROP_KEY_SYSTEM_ASCIIART_FOLDER))
				{
					String sAsciiArtFolder = aProperties.getProperty(sKey);
					if(sAsciiArtFolder!=null && sAsciiArtFolder.trim().length()>0)
					{
						File folder = new File(sAsciiArtFolder.trim());
						StateOutput.setAsciiArtFolder(folder);
					}
				}
			}
		}
		
		if(logger.isLoggable(Level.FINEST)) 
		{
			//Debug
			logger.finest("Loaded configuration :");
			iter = mapPidConfigs.keySet().iterator();
			while(iter.hasNext())
			{
				String sPID = (String) iter.next();
				Map<String, String> mapProcessConfig = mapPidConfigs.get(sPID);
				logger.finest("["+sPID+"]");
				Iterator iter2 = mapProcessConfig.keySet().iterator();
				while(iter2.hasNext())
				{
					String sKey = (String) iter2.next();
					String sVal = mapProcessConfig.get(sKey);
					logger.finest("   - "+sKey+":"+sVal);
				}
			}
			//
		}	
		
		iter = mapPidConfigs.keySet().iterator();
		String sConfigVal = null;
		while(iter.hasNext())
		{
			String sPID = (String) iter.next();
			Map<String, String> mapProcessConfig = mapPidConfigs.get(sPID);
		
			HLProcess p = mapProcesses.get(sPID);
			if(p==null)
			{
				p = new HLProcess(sPID);
			}
				
			// DISABLED
			sConfigVal = mapProcessConfig.get(_PROP_KEY_DISABLED);
			if(sConfigVal!=null)
			{
				p.setDisabled("true".equalsIgnoreCase(sConfigVal));
			}
			
			// LOG
			sConfigVal = mapProcessConfig.get(_PROP_KEY_SHELL_OUTPUT_FILE_ROLL_BYTESIZE);
			if(sConfigVal!=null)
			{
				long lVal = Long.parseLong(sConfigVal);
				p.setProcessLogAutoRollSizeBytes(lVal);
			}
			
			sConfigVal = mapProcessConfig.get(_PROP_KEY_SHELL_OUTPUT_FILE_ROLL_COUNT);
			if(sConfigVal!=null)
			{
				int iVal = Integer.parseInt(sConfigVal);
				p.setProcessLogAutoRollFileCount(iVal);
			}
			
			// SHELL
			sConfigVal = mapProcessConfig.get(_PROP_KEY_SHELL_CMD_BLOCK);
			if(sConfigVal!=null)
			{
				sConfigVal = sConfigVal.trim();
				
				if(sConfigVal.length()==1)
				{
					p.setCommandBlockStart(String.valueOf(sConfigVal.charAt(0)));
					p.setCommandBlockEnd(p.getCommandBlockStart());
				}
				else if(sConfigVal.length()==2)
				{
					p.setCommandBlockStart(String.valueOf(sConfigVal.charAt(0)));
					p.setCommandBlockEnd(String.valueOf(sConfigVal.charAt(1)));
				}
			}
			// 
			sConfigVal = mapProcessConfig.get(_PROP_KEY_SHELL_RUNAS_DAEMON);
			if(sConfigVal!=null)
			{
				p.setRunAsDaemon("true".equalsIgnoreCase(sConfigVal));
			}
			// 
			sConfigVal = mapProcessConfig.get(_PROP_KEY_SHELL_CMD_NO_OS);
			if(sConfigVal!=null)
			{
				p.setProcessCommand(splitCommands(p, sConfigVal));
			}
			else
			{
				sConfigVal = mapProcessConfig.get(_PROP_KEY_SHELL_CMD);
				if(sConfigVal!=null)
				{
					p.setProcessCommand(splitCommands(p, sConfigVal));
				}
			}
			// 
			sConfigVal = mapProcessConfig.get(_PROP_KEY_SHELL_CMD_END_REGEX);
			if(sConfigVal!=null)
			{
				p.setCommandEndRegex(sConfigVal);
			}
			//
			sConfigVal = mapProcessConfig.get(_PROP_KEY_SHELL_OUTPUT_CONSOLE);
			if(sConfigVal!=null)
			{
				p.setOutputConsole("true".equalsIgnoreCase(sConfigVal));
			}
			//
			sConfigVal = mapProcessConfig.get(_PROP_KEY_SHELL_OUTPUT_FILENAME_NO_OS);
			if(sConfigVal!=null)
			{
				p.setProcessOutputFilename(sConfigVal);
			}
			else
			{
				sConfigVal = mapProcessConfig.get(_PROP_KEY_SHELL_OUTPUT_FILENAME);
				if(sConfigVal!=null)
				{
					p.setProcessOutputFilename(sConfigVal);
				}
			}
			//
			sConfigVal = mapProcessConfig.get(_PROP_KEY_SHELL_START_DELAY);
			if(sConfigVal!=null)
			{
				long lVal = Long.parseLong(sConfigVal);
				p.setProcessStartDelayMs(lVal);
			}
			//
			sConfigVal = mapProcessConfig.get(_PROP_KEY_SHELL_TERMINATE_CMD_NO_OS);
			if(sConfigVal!=null)
			{
				p.setTerminateCommand(sConfigVal);
			}
			else
			{
				sConfigVal = mapProcessConfig.get(_PROP_KEY_SHELL_TERMINATE_CMD);
				if(sConfigVal!=null)
				{
					p.setTerminateCommand(sConfigVal);
				}
			}
			//
			sConfigVal = mapProcessConfig.get(_PROP_KEY_SHELL_TERMINATE_END_REGEX);
			if(sConfigVal!=null)
			{
				p.setTerminateEndRegex(sConfigVal);
			}
			sConfigVal = mapProcessConfig.get(_PROP_KEY_SHELL_TERMINATE_IDLE_TIMEOUT_MS);
			if(sConfigVal!=null)
			{
				long lVal = Long.parseLong(sConfigVal);
				p.setTerminateIdleTimeoutMs(lVal);
			}
			//
			sConfigVal = mapProcessConfig.get(_PROP_KEY_SHELL_SHUTDOWN_ALL_ON_TEMINATE);
			if(sConfigVal!=null)
			{
				p.setShutdownAllOnTermination("true".equalsIgnoreCase(sConfigVal));
			}
			//
			sConfigVal = mapProcessConfig.get(_PROP_KEY_SHELL_SHUTDOWN_ALL_TIMEOUT_MS);
			if(sConfigVal!=null)
			{
				long lVal = Long.parseLong(sConfigVal);
				p.setShutdownTimeoutMs(lVal);
			}			
			//
			sConfigVal = mapProcessConfig.get(_PROP_KEY_SHELL_DEF2_SCRIPT_DIR);
			if(sConfigVal!=null)
			{
				p.setDefaultToScriptDir("true".equalsIgnoreCase(sConfigVal));
			}

			//INIT
			sConfigVal = mapProcessConfig.get(_PROP_KEY_INIT_ERROR_MSG);
			if(sConfigVal!=null)
			{
				p.setInitErrorMessage(sConfigVal);
			}
			//
			sConfigVal = mapProcessConfig.get(_PROP_KEY_INIT_SUCCESS_REGEX);
			if(sConfigVal!=null)
			{
				p.setInitSuccessRegex(sConfigVal);
			}
			//
			sConfigVal = mapProcessConfig.get(_PROP_KEY_INIT_FAILED_REGEX);
			if(sConfigVal!=null)
			{
				p.setInitFailedRegex(sConfigVal);
			}
			//
			sConfigVal = mapProcessConfig.get(_PROP_KEY_INIT_TIMEOUT_MS);
			if(sConfigVal!=null)
			{
				long lVal = Long.parseLong(sConfigVal);
				p.setInitTimeoutMs(lVal);
			}
			
			//DEPENDANCIES
			for(String sConfigKey : new String[] {_PROP_KEY_DEP_PROCESSES_LOCAL, _PROP_KEY_DEP_PROCESSES_REMOTE})
			{
				sConfigVal = mapProcessConfig.get(sConfigKey);
				if(sConfigVal!=null)
				{
					String[] sDepIds = sConfigVal.split(",");
					for(String sDepId : sDepIds)
					{
						boolean isRemoteProc = sConfigKey.equals(_PROP_KEY_DEP_PROCESSES_REMOTE);
						sDepId = sDepId.trim();
						if(sDepId.length()>0)
						{
							HLProcess procDep = mapProcesses.get(sDepId);
							if(procDep==null)
							{
								procDep = new HLProcess(sDepId);
								if(isRemoteProc)
								{
									procDep.setRemoteRef(true);
									procDep.setDependCheckIntervalMs(0);
									procDep.setDependTimeoutMs(0);
								}
								mapProcesses.put(sDepId, procDep);
							}
							
							if(procDep.isRemoteRef() != isRemoteProc)
							{
								throw new RuntimeException("["+sPID+"] Mismatch dependancies configuration - "+sDepId);
							}
							
							p.addDependProcess(procDep);
						}
					}
				}
			}
			//
			sConfigVal = mapProcessConfig.get(_PROP_KEY_DEP_TIMEOUT_MS);
			if(sConfigVal!=null)
			{
				long lVal = Long.parseLong(sConfigVal);
				p.setDependTimeoutMs(lVal);
			}
			//
			sConfigVal = mapProcessConfig.get(_PROP_KEY_DEP_CHK_INTERVAL_MS);
			if(sConfigVal!=null)
			{
				long lVal = Long.parseLong(sConfigVal);
				p.setDependCheckIntervalMs(lVal);
			}
			//
			mapProcesses.put(sPID, p);
			
			if(p.isDisabled())
			{
				mapDisabledProcesses.put(sPID, p);
			}
		}
		
		for(HLProcess p : mapDisabledProcesses.values())
		{
			mapProcesses.remove(p.getProcessCodeName());
		}
		
		validateProcessConfigs();
	}
	
	private void validateProcessConfigs()
	{
		for(HLProcess p : getProcesses())
		{	
			if(!p.isDisabled())
			{	
				//Remote process
				if(p.isRemoteRef())
				{
					if(p.getProcessCommand().trim().length()>0
						 
						 || p.getDependCheckIntervalMs()>0
						 || p.getDependTimeoutMs()>0
						 
						 || p.getProcessStartDelayMs()>0
						)
					{
						throw new RuntimeException("["+p.getProcessCodeName()+"] Remote Process should not have local configuration !");
					}
				}
				//Dependencies 
				else if(p.getDependProcesses().size()>0)
				{
					Iterator<HLProcess> iter = p.getDependProcesses().iterator();
					while(iter.hasNext())
					{
						HLProcess dep = iter.next();
						if(dep.isDisabled())
						{
							throw new RuntimeException("["+p.getProcessCodeName()+"] Dependence Process is disabled !" + dep.getProcessCodeName());
						}
					}
				}
				//Process command
				else if(p.getProcessCommand().trim().length()==0)
				{
					throw new RuntimeException("["+p.getProcessCodeName()+"] Process command cannot be empty ! - "+_PROP_KEY_SHELL_CMD);
				}
				else if(p.getProcessCommand().trim().length()>0)
				{

					File fileScript = new File(p.getCommands()[0]);
					
					if(fileScript.exists())
					{
						if(!fileScript.canExecute())
						{
							//file permission problem
							throw new RuntimeException("["+p.getProcessCodeName()+"] Insufficient execution permission - "+_PROP_KEY_SHELL_CMD+":"+p.getProcessCommand());
						}
					}
				}
				//Process Terminate comman
				else if(p.getTerminateCommand().trim().length()>0)
				{
					File fileScript = new File(p.getTerminateCommand());
					
					if(fileScript.exists())
					{
						if(!fileScript.canExecute())
						{
							//file permission problem
							throw new RuntimeException("["+p.getProcessCodeName()+"] Insufficient execution permission - "+_PROP_KEY_SHELL_TERMINATE_CMD+":"+p.getTerminateCommand());
						}
					}
				}
				//Process output/log file
				else if(p.getProcessOutputFilename().trim().length()>0)
				{
					File fileOutput = new File(p.getProcessOutputFilename());
					
					if(fileOutput.exists())
					{
						if(!fileOutput.canWrite())
						{
							//file permission problem
							throw new RuntimeException("["+p.getProcessCodeName()+"] Insufficient write permission - "+_PROP_KEY_SHELL_TERMINATE_CMD+":"+p.getTerminateCommand());
						}
					}
					else 
					{
						//File not exist
						boolean isCreated = false;
						try {
							isCreated = fileOutput.mkdirs();
						}
						catch(Exception ex)
						{
							isCreated = false;
						}
						
						if(!isCreated)
						{
							//failed to create logfile
							throw new RuntimeException("["+p.getProcessCodeName()+"] Invalid process output filename - "+_PROP_KEY_SHELL_OUTPUT_FILENAME+":"+p.getProcessOutputFilename());
						}
					}
				}
			}
			
			//
			
		}
	}
	
	protected HLProcess getProcess(String aProcessID)
	{
		return mapProcesses.get(aProcessID);
	}
	
	protected HLProcess[] getProcesses()
	{
		if(mapProcesses==null)
			return new HLProcess[]{};
		
		Collection<HLProcess> c = mapProcesses.values();
		return c.toArray(new HLProcess[c.size()]);
	}
	
	protected HLProcess[] getDisabledProcesses()
	{
		if(mapDisabledProcesses==null)
			return new HLProcess[]{};
		
		Collection<HLProcess> c = mapDisabledProcesses.values();
		return c.toArray(new HLProcess[c.size()]);
	}
	
	public static String generateCfgTemp()
	{
		StringBuffer sb = new StringBuffer();
		String sNewLine = "\n";
		sb.append("//--").append(sNewLine);
		sb.append("/** sample 'process.properties'").append(sNewLine);
		sb.append("//--").append(sNewLine);
		sb.append("#").append(_PROP_KEY_DISABLED).append("=false").append(sNewLine);
		sb.append("#").append(sNewLine);
		sb.append("#").append(_PROP_KEY_SHELL_START_DELAY).append("=").append(sNewLine);
		sb.append("#").append(_PROP_KEY_SHELL_RUNAS_DAEMON).append("=false").append(sNewLine);
		sb.append("#").append(_PROP_KEY_SHELL_DEF2_SCRIPT_DIR).append("=false").append(sNewLine);
		sb.append("#").append(sNewLine);
		sb.append("#").append(_PROP_KEY_SHELL_CMD_BLOCK).append("=").append(sNewLine);
		sb.append("#").append(_PROP_KEY_SHELL_CMD_END_REGEX).append("=").append(sNewLine);
		sb.append("#").append(_PROP_KEY_SHELL_CMD_NO_OS).append("=").append(sNewLine);
		sb.append("#").append(_PROP_KEY_SHELL_CMD_NO_OS).append(".win=").append(sNewLine);
		sb.append("#").append(_PROP_KEY_SHELL_CMD_NO_OS).append(".linux=").append(sNewLine);
		sb.append("#").append(_PROP_KEY_SHELL_CMD_NO_OS).append(".mac=").append(sNewLine);
		sb.append("#").append(sNewLine);
		sb.append("#").append(_PROP_KEY_SHELL_OUTPUT_FILENAME_NO_OS).append("=").append(sNewLine);
		sb.append("#").append(_PROP_KEY_SHELL_OUTPUT_FILENAME_NO_OS).append(".win=").append(sNewLine);
		sb.append("#").append(_PROP_KEY_SHELL_OUTPUT_FILENAME_NO_OS).append(".linux=").append(sNewLine);
		sb.append("#").append(_PROP_KEY_SHELL_OUTPUT_FILENAME_NO_OS).append(".mac=").append(sNewLine);
		sb.append("#").append(_PROP_KEY_SHELL_OUTPUT_CONSOLE).append("=false").append(sNewLine);
		sb.append("#").append(sNewLine);
		sb.append("#").append(_PROP_KEY_SHELL_TERMINATE_CMD_NO_OS).append("=").append(sNewLine);
		sb.append("#").append(_PROP_KEY_SHELL_TERMINATE_CMD_NO_OS).append(".win=").append(sNewLine);
		sb.append("#").append(_PROP_KEY_SHELL_TERMINATE_CMD_NO_OS).append(".linux=").append(sNewLine);
		sb.append("#").append(_PROP_KEY_SHELL_TERMINATE_CMD_NO_OS).append(".mac=").append(sNewLine);
		sb.append("#").append(_PROP_KEY_SHELL_TERMINATE_END_REGEX).append("=").append(sNewLine);
		sb.append("#").append(_PROP_KEY_SHELL_TERMINATE_IDLE_TIMEOUT_MS).append("=18000000 (5 min)").append(sNewLine);
	    sb.append("#").append(sNewLine);
		sb.append("#").append(_PROP_KEY_SHELL_SHUTDOWN_ALL_ON_TEMINATE).append("=").append(sNewLine);
		sb.append("#").append(_PROP_KEY_SHELL_SHUTDOWN_ALL_TIMEOUT_MS).append("=").append(sNewLine);
		sb.append("#").append(sNewLine);
		sb.append("#").append(_PROP_KEY_INIT_TIMEOUT_MS).append("=18000000 (5 min)").append(sNewLine);
		sb.append("#").append(_PROP_KEY_INIT_SUCCESS_REGEX).append("=").append(sNewLine);
		sb.append("#").append(_PROP_KEY_INIT_FAILED_REGEX).append("=").append(sNewLine);
		sb.append("#").append(_PROP_KEY_INIT_ERROR_MSG).append("=").append(sNewLine);
		sb.append("#").append(sNewLine);
		sb.append("#").append(_PROP_KEY_DEP_PROCESSES_LOCAL).append("=").append(sNewLine);
		sb.append("#").append(_PROP_KEY_DEP_PROCESSES_REMOTE).append("=").append(sNewLine);
		sb.append("#").append(_PROP_KEY_DEP_CHK_INTERVAL_MS).append("=").append(sNewLine);
		sb.append("#").append(_PROP_KEY_DEP_TIMEOUT_MS).append("=18000000 (5 min)").append(sNewLine);
		sb.append("//--").append(sNewLine);
		sb.append("**/").append(sNewLine);
		
		return sb.toString();
	}

	public static void main(String args[])
	{
	}
}