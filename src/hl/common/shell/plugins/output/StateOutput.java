package hl.common.shell.plugins.output;

import java.util.HashMap;
import java.util.Map;

import hl.common.shell.HLProcess.ProcessState;
import hl.common.shell.utils.FileUtil;

public class StateOutput 
{
	private static Map<String, String> mapStateOutput = new HashMap<String, String>();
	
	private static String asciiPath = "/hl/common/shell/plugins/output/";

	public static String getStateOutput(ProcessState aProcessState) {
		
		String sOutput = null;
		if(aProcessState!=null)
		{
			
			String sState = aProcessState.toString().toUpperCase();
			
			sOutput = mapStateOutput.get(sState);
			
			if(sOutput==null)
			{
				sOutput = FileUtil.loadContent(asciiPath+sState+".ascii");
				if(sOutput!=null)
				{
					mapStateOutput.put(sState, sOutput);
				}
			}
			
			if(sOutput==null || sOutput.trim().length()==0)
			{
				sOutput = null;
			}
			
		}
		return sOutput;
	}
}