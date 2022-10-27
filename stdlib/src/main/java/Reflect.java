/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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

// This must be in the default package to work with VDMJ's native delegation.

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.VDMFunction;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCTypeDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueFactory;

public class Reflect
{
	@VDMFunction
	public static Value getDefinition(Value arg) throws Exception
	{
		if (arg instanceof SeqValue)	// Name of type, function, etc
		{
			SeqValue s = (SeqValue)arg;
			String name = s.stringValue(null);
			return reflectDefinition(lookup(nameOf(name)));
		}
		
		throw new Exception("Expecting definition name");
	}

	private static TCNameToken nameOf(String name) throws Exception
	{
		Interpreter i = Interpreter.getInstance();
		LexTokenReader ltr = new LexTokenReader(name, Dialect.VDM_SL);
		LexToken token = ltr.nextToken();
		ltr.close();
		
		TCNameToken sought = null;

		if (token.is(Token.IDENTIFIER))
		{
			LexIdentifierToken id = (LexIdentifierToken)token;
			sought = new TCNameToken(id.location, i.getDefaultName(), id.name);
		}
		else if (token.is(Token.NAME))
		{
			sought = new TCNameToken((LexNameToken)token);
		}
		else
		{
			throw new Exception("Expecting type name: " + name);
		}
		
		return sought;
	}
	
	private static TCDefinition lookup(TCNameToken sought)
	{
		Interpreter i = Interpreter.getInstance();
		TCDefinition def = null;
		
		if (Settings.dialect == Dialect.VDM_SL)
		{
			TCModuleList tc = i.getTC();
			def = tc.findDefinition(sought);
		}
		else
		{
			TCClassList tc = i.getTC();
			def = tc.findDefinition(sought);
		}
		
		return def;
	}

	private static Value reflectDefinition(TCDefinition def) throws Exception
	{
		if (def instanceof TCTypeDefinition)
		{
			if (def.getType() instanceof TCNamedType)
			{
				TCNamedType nt = (TCNamedType) def.getType();
				
				return ValueFactory.mkRecord("Reflect", "TypeDefinition",
					ValueFactory.mkSeq(def.name.getExplicit(true).toString()),
					reflectType(nt.type));
			}
			else
			{
				return ValueFactory.mkRecord("Reflect", "TypeDefinition",
						ValueFactory.mkSeq(def.name.getExplicit(true).toString()),
						reflectType(def.getType()));				
			}
		}
		else if (def instanceof TCExplicitFunctionDefinition ||
				 def instanceof TCImplicitFunctionDefinition)
		{
			return ValueFactory.mkRecord("Reflect", "FunctionDefinition",
					ValueFactory.mkSeq(def.name.getExplicit(true).toString()),
					reflectType(def.getType()));							
		}
		else if (def instanceof TCExplicitOperationDefinition ||
				 def instanceof TCImplicitOperationDefinition)
		{
			return ValueFactory.mkRecord("Reflect", "OperationDefinition",
					ValueFactory.mkSeq(def.name.getExplicit(true).toString()),
					reflectType(def.getType()));							
		}
		
		throw new Exception("Unsupported definition type");
	}

	private static Value reflectType(TCType type)
	{
		ReflectTypeVisitor visitor = new ReflectTypeVisitor();
		return type.apply(visitor, null);
	}
}
