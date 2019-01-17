package hl.common.shell.plugins.output;

import hl.common.shell.HLProcess.ProcessState;

public interface IProcessStateOutput
{
	public String getStateOutput(ProcessState aProcessState);
}