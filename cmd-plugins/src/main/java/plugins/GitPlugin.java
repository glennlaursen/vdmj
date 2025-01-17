/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
 *
 *	Author: Nick Battle
 *
 *	This file is part of VDMJ.
 *
 *	VDMJ is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	VDMJ is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package plugins;

import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import com.fujitsu.vdmj.plugins.AnalysisCommand;

public class GitPlugin extends AnalysisCommand
{
	private final static String USAGE = "Usage: git <command> [args]";
	
	public GitPlugin(String line)
	{
		super(line);
		
		if (!argv[0].equals("git"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public String run(String line)
	{
		if (argv.length == 1)
		{
			return USAGE;
		}
		
		try
		{
			ProcessBuilder pb = new ProcessBuilder(argv);
			pb.inheritIO();
			Process p = pb.start();
			p.waitFor();
			
			if (p.exitValue() != 0)
			{
				println("Process exit code = " + p.exitValue());
			}
		}
		catch (Exception e)
		{
			println(e);
		}
		
		return null;
	}

	/**
	 * This would be used if the GitCommand was also part of an AnalysisPlugin
	 */
	public static void help()
	{
		println("git <command> [<args>] - run a git command");
	}
}
