/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package com.fujitsu.vdmj.plugins;

import static com.fujitsu.vdmj.plugins.PluginConsole.verbose;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fujitsu.vdmj.plugins.commands.ErrorCommand;

public class PluginRegistry
{
	private static PluginRegistry INSTANCE = null;
	private final Map<String, AnalysisPlugin> plugins;

	private PluginRegistry()
	{
		plugins = new LinkedHashMap<String, AnalysisPlugin>();
	}

	public static synchronized PluginRegistry getInstance()
	{
		if (INSTANCE == null)
		{
			INSTANCE = new PluginRegistry();
		}
		
		return INSTANCE;
	}
	
	public static void reset()
	{
		if (INSTANCE != null)
		{
			INSTANCE.plugins.clear();
			INSTANCE = null;
		}
	}
	
	public void registerPlugin(AnalysisPlugin plugin)
	{
		plugins.put(plugin.getName(), plugin);
		plugin.init();
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getPlugin(String name)
	{
		return (T)plugins.get(name);
	}
	
	public Map<String, AnalysisPlugin> getPlugins()
	{
		return plugins;
	}
	
	public AnalysisCommand getCommand(String line)
	{
		String[] argv = line.split("\\s+");
		AnalysisCommand result = null;
		
		for (AnalysisPlugin plugin: plugins.values())
		{
			try
			{
				AnalysisCommand c = plugin.getCommand(line);
				
				if (c != null)
				{
					if (result != null)
					{
						verbose("Multiple plugins support " + argv[0]);
					}
					
					result = c;		// Note, override earlier results
				}
			}
			catch (IllegalArgumentException e)
			{
				return new ErrorCommand(e.getMessage());
			}
			catch (Throwable e)
			{
				// Ignore misbehaving plugins
			}
		}
		
		return result;
	}

	public void getHelp()
	{
		for (AnalysisPlugin plugin: plugins.values())
		{
			try
			{
				plugin.help();
			}
			catch (Throwable e)
			{
				// Ignore misbehaving plugins
			}
		}
	}
}
