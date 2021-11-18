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

import json.JSONObject;
import lsp.LSPMessageUtils;
import rpc.RPCErrors;
import rpc.RPCMessageList;
import rpc.RPCRequest;

abstract public class AnalysisPlugin
{
	protected final LSPMessageUtils messages;
	protected final String CODE_LENS_COMMAND = "vdm-vscode.addRunConfiguration";
	
	public AnalysisPlugin()
	{
		messages = new LSPMessageUtils();
	}
	
	protected RPCMessageList errorResult()
	{
		return new RPCMessageList(null, RPCErrors.InternalError, "?");
	}

	abstract public String getName();
	
	abstract public void init();
	
	/**
	 * External plugins claim to support specific LSP messages. This method
	 * identifies whether the plugin supports the name passed.
	 */
	public boolean supportsMethod(String method)
	{
		return false;
	}

	/**
	 * External plugins override this method to implement their functionality.
	 * @param request
	 * @return responses
	 */
	public RPCMessageList analyse(RPCRequest request)
	{
		return new RPCMessageList(request, RPCErrors.InternalError, "Plugin does not support analysis");
	}

	/**
	 * All plugins can register experimental options that are sent back to the Client
	 * in the experimental section of the initialize response.
	 */
	public JSONObject getExperimentalOptions()
	{
		return new JSONObject();
	}
}
