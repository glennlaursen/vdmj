/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.in.definitions;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INPattern;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCInvariantType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;

/**
 * A class to hold a type definition.
 */
public class INTypeDefinition extends INDefinition
{
	private static final long serialVersionUID = 1L;
	public final TCInvariantType type;
	public final INPattern invPattern;
	public final INExpression invExpression;
	public final INExplicitFunctionDefinition invdef;
	public final INExplicitFunctionDefinition eqdef;
	public final INExplicitFunctionDefinition orddef;

	public INTypeDefinition(INAccessSpecifier accessSpecifier, TCNameToken name,
			TCInvariantType type, INPattern invPattern, INExpression invExpression,
			INExplicitFunctionDefinition invdef, INExplicitFunctionDefinition eqdef,
			INExplicitFunctionDefinition orddef)
	{
		super(name.getLocation(), accessSpecifier, name);

		this.type = type;
		this.invPattern = invPattern;
		this.invExpression = invExpression;
		this.invdef = invdef;
		this.eqdef = eqdef;
		this.orddef = orddef;
	}

	@Override
	public String toString()
	{
		return name.getName() + " = " + type.toDetailedString() +
				(invPattern == null ? "" : "\n\tinv " + invPattern + " == " + invExpression);
	}

	@Override
	public TCType getType()
	{
		return type;
	}

	@Override
	public INDefinition findName(TCNameToken sought)
	{
		if (invdef != null && invdef.findName(sought) != null)
		{
			return invdef;
		}

		if (eqdef != null && eqdef.findName(sought) != null)
		{
			return eqdef;
		}

		if (orddef != null && orddef.findName(sought) != null)
		{
			return orddef;
		}

		return null;
	}

	@Override
	public INExpression findExpression(int lineno)
	{
		if (invdef != null)
		{
			INExpression found = invdef.findExpression(lineno);
			if (found != null) return found;
		}

		if (eqdef != null)
		{
			INExpression found = eqdef.findExpression(lineno);
			if (found != null) return found;
		}

		if (orddef != null)
		{
			INExpression found = orddef.findExpression(lineno);
			if (found != null) return found;
		}

		return null;
	}

	@Override
	public NameValuePairList getNamedValues(Context ctxt)
	{
		NameValuePairList nvl = new NameValuePairList();

		if (invdef != null)
		{
			FunctionValue invfunc =	new FunctionValue(invdef, null, null, ctxt);
			nvl.add(new NameValuePair(invdef.name, invfunc));
		}

		if (eqdef != null)
		{
			FunctionValue invfunc =	new FunctionValue(eqdef, null, null, ctxt);
			nvl.add(new NameValuePair(eqdef.name, invfunc));
		}

		if (orddef != null)
		{
			FunctionValue invfunc =	new FunctionValue(orddef, null, null, ctxt);
			nvl.add(new NameValuePair(orddef.name, invfunc));
		}

		return nvl;
	}

	@Override
	public boolean isRuntime()
	{
		return false;	// Though the inv definition is, of course
	}

	@Override
	public boolean isTypeDefinition()
	{
		return true;
	}
}
