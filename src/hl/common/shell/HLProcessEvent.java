package hl.common.shell;

public interface HLProcessEvent 
{
	void onProcessStarting(HLProcess p);
	void onProcessInitSuccess(HLProcess p);
	void onProcessError(HLProcess p, Throwable e);
	void onProcessTerminate(HLProcess p);
}