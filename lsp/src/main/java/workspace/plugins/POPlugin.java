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

package workspace.plugins;

import java.io.File;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;

import json.JSONArray;
import json.JSONObject;
import lsp.LSPServer;
import lsp.Utils;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.Diag;
import workspace.EventListener;
import workspace.events.CheckFilesEvent;
import workspace.events.Event;

abstract public class POPlugin extends AnalysisPlugin implements EventListener
{
	public static POPlugin factory(Dialect dialect)
	{
		switch (dialect)
		{
			case VDM_SL:
				return new POPluginSL();
				
			case VDM_PP:
			case VDM_RT:
				return new POPluginPR();
				
			default:
				Diag.error("Unknown dialect " + dialect);
				throw new RuntimeException("Unsupported dialect: " + Settings.dialect);
		}
	}

	protected POPlugin()
	{
		super();
	}
	
	@Override
	public String getName()
	{
		return "PO";
	}

	@Override
	public void init()
	{
		eventhub.register(this, "checkFilesEvent/prepare", this);
		eventhub.register(this, "checkFilesEvent/checked", this);
	}
	
	@Override
	public JSONObject getExperimentalOptions(JSONObject standard)
	{
		return new JSONObject("proofObligationProvider", true);
	}

	@Override
	public RPCMessageList handleEvent(Event event) throws Exception
	{
		if (event instanceof CheckFilesEvent)
		{
			CheckFilesEvent ev = (CheckFilesEvent)event;
			
			switch (ev.type)
			{
				case "checkFilesEvent/prepare":
					preCheck(ev);
					return new RPCMessageList();

				case "checkFilesEvent/checked":
					TCPlugin tc = registry.getPlugin("TC");
					checkLoadedFiles(tc.getTC());
					LSPServer.getInstance().writeMessage(RPCRequest.notification("slsp/POG/updated",
							new JSONObject("successful", tc.getErrs().isEmpty())));
					return new RPCMessageList();

				default:
					Diag.error("Unhandled %s CheckFilesEvent %s", getName(), event);
					return new RPCMessageList();
			}
		}
		else
		{
			Diag.error("Unhandled %s event %s", getName(), event);
			return new RPCMessageList();
		}
	}

	protected void preCheck(CheckFilesEvent ev)
	{
		// Nothing
	}

	/**
	 * Event handling above. Supporting methods below. 
	 */
	
	abstract public <T extends Mappable> T getPO();
	
	abstract public <T extends Mappable> boolean checkLoadedFiles(T poList) throws Exception;
	
	abstract protected ProofObligationList getProofObligations();
	
	protected JSONArray splitPO(String value)
	{
		String[] parts = value.trim().split("\\n\\s+");
		JSONArray array = new JSONArray();
		
		for (String part: parts)
		{
			array.add(part);
		}
		
		return array;
	}

	public JSONArray getObligations(File file)
	{
		ProofObligationList poGeneratedList = getProofObligations();
		poGeneratedList.renumber();
		JSONArray results = new JSONArray();
		
		for (ProofObligation po: poGeneratedList)
		{
			if (file != null)
			{
				if (file.isFile())
				{
					if (!po.location.file.equals(file))
					{
						continue;
					}
				}
				else if (file.isDirectory())
				{
					String path = file.getPath();
					
					if (!po.location.file.getPath().startsWith(path))
					{
						continue;
					}
				}
			}
			
			JSONArray name = new JSONArray(po.location.module);
			
			for (String part: po.name.split(";\\s+"))
			{
				name.add(part);
			}

			results.add(
				new JSONObject(
					"id",		Long.valueOf(po.number),
					"kind", 	po.kind.toString(),
					"name",		name,
					"location",	Utils.lexLocationToLocation(po.location),
					"source",	splitPO(po.value),
					"status",	po.status.toString()));
		}
		
		return results;
	}
}
