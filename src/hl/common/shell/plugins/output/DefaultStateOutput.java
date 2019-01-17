package hl.common.shell.plugins.output;

import hl.common.shell.HLProcess.ProcessState;

public class DefaultStateOutput implements IProcessStateOutput
{
	private static String _SMILEY 	= null;
	private static String _SKULL 	= null;
	
	static
	{
		StringBuffer sb = new StringBuffer();
		sb.append("\n").append("                            OOOOOOOOOO");
		sb.append("\n").append("                        OOOOOOOOOOOOOOOOOO");
		sb.append("\n").append("                     OOOOOO  OOOOOOOO  OOOOOO");
		sb.append("\n").append("                   OOOOOO      OOOO      OOOOOO");
		sb.append("\n").append("                 OOOOOOOO      OOOO      OOOOOOOO");
		sb.append("\n").append("                OOOOOOOOOO    OOOOOO    OOOOOOOOOO");
		sb.append("\n").append("               OOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
		sb.append("\n").append("               OOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
		sb.append("\n").append("               OOOO  OOOOOOOOOOOOOOOOOOOOOOOO  OOOO");
		sb.append("\n").append("                OOOO  OOOOOOOOOOOOOOOOOOOOOO  OOOO");
		sb.append("\n").append("                 OOOO   OOOOOOOOOOOOOOOOOOO  OOOO");
		sb.append("\n").append("                   OOOOO   OOOOOOOOOOOOOO   OOOO");
		sb.append("\n").append("                     OOOOOO   OOOOOOOO   OOOOOO");
		sb.append("\n").append("                        OOOOOO        OOOOOO");
		sb.append("\n").append("                            OOOOOOOOOOO");
		sb.append("\n");
		sb.append("\n").append("   ######  ########  ######   #######  ######## ######## #######");
		sb.append("\n").append("  ##    ##    ##    ##    ##  ##    ##    ##    ##       ##     #");
		sb.append("\n").append("  ##          ##    ##    ##  ##    ##    ##    ##       ##      #");
		sb.append("\n").append("   ######     ##    ########  #######     ##    ######   ##      #");
		sb.append("\n").append("        ##    ##    ##    ##  ##   ##     ##    ##       ##      #");
		sb.append("\n").append("  ##    ##    ##    ##    ##  ##    ##    ##    ##       ##     # ");
		sb.append("\n").append("   ######     ##    ##    ##  ##    ##    ##    ######## #######");
		_SMILEY = sb.toString();
		
		sb.setLength(0);
		sb.append("\n").append("                   #######");
		sb.append("\n").append("               ###############");
		sb.append("\n").append("             #####################");
		sb.append("\n").append("            #######################");
		sb.append("\n").append("           #########################");
		sb.append("\n").append("          ###########################");
		sb.append("\n").append("          ###########################");
		sb.append("\n").append("          ########   #####   ########");
		sb.append("\n").append("          ######      ###       #####");
		sb.append("\n").append("           ####       ###       ####");
		sb.append("\n").append("           ####      #####      ####");
		sb.append("\n").append("            ##########   ##########");
		sb.append("\n").append("             #########   #########");
		sb.append("\n").append("               #################");
		sb.append("\n").append("                ###############");
		sb.append("\n").append("     ###        #### # # # ####       ###");
		sb.append("\n").append("    #####        #############       #####");
		sb.append("\n").append("     #######      ###########     ########");
		sb.append("\n").append("   ##############    #####    ##############");
		sb.append("\n").append("  ####################   ##################");
		sb.append("\n").append("  ###        ############### ######");
		sb.append("\n").append("      ####     ###############");
		sb.append("\n").append("    ################## ###################");
		sb.append("\n").append("   ##############           ##############");
		sb.append("\n").append("    #######                      ########");
		sb.append("\n").append("    ####                         #####");
		_SKULL = sb.toString();
	}
	
	@Override
	public String getStateOutput(ProcessState aProcessState) {
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