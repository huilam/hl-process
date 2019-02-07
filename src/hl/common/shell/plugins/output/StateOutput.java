package hl.common.shell.plugins.output;

import hl.common.shell.HLProcess.ProcessState;
import hl.common.shell.utils.FileUtil;

public class StateOutput 
{
	private static String _SMILEY 	= null;
	private static String _SKULL 	= null;
	
	static
	{
		_SMILEY = FileUtil.loadContent("/STARTED.ascii");
		_SKULL = FileUtil.loadContent("/TERIMATED.ascii");
	}
	

	public static String getStateOutput(ProcessState aProcessState) {
		if(aProcessState!=null)
		{
			if(aProcessState.getCode() == ProcessState.STARTED.getCode())
			{
				return _SMILEY;
			}
			else if(aProcessState.getCode() == ProcessState.TERMINATED.getCode())
			{
				return _SKULL;
			}
		}
		return null;
	}
}