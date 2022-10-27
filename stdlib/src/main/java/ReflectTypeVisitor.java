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

import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCCharacterType;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;
import com.fujitsu.vdmj.values.QuoteValue;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueFactory;
import com.fujitsu.vdmj.values.ValueList;

public class ReflectTypeVisitor extends TCTypeVisitor<Value, Object>
{
	@Override
	public Value caseType(TCType node, Object arg)
	{
		if (node instanceof TCNaturalType)
		{
			return new QuoteValue("NAT");
		}
		else if (node instanceof TCCharacterType)
		{
			return new QuoteValue("CHAR");
		}
		else if (node instanceof TCBooleanType)
		{
			return new QuoteValue("BOOL");
		}
		
		return null;
	}
	
	@Override
	public Value caseProductType(TCProductType node, Object arg)
	{
		try
		{
			TCProductType p =(TCProductType)node;
			ValueList m = new ValueList();
			
			for (TCType member: p.types)
			{
				m.add(member.apply(this, arg));
			}
			
			return ValueFactory.mkRecord("Reflect", "AggregateType",
					new QuoteValue("PRODUCT"),
					new SeqValue(m));
		}
		catch (ValueException e)
		{
			return null;
		}
	}
	
	@Override
	public Value caseSetType(TCSetType node, Object arg)
	{
		try
		{
			return ValueFactory.mkRecord("Reflect", "Type", new QuoteValue("SET"), new SeqValue());
		}
		catch (ValueException e)
		{
			return null;
		}
	}
	
	@Override
	public Value caseSeqType(TCSeqType node, Object arg)
	{
		try
		{
			return ValueFactory.mkRecord("Reflect", "Type", new QuoteValue("SEQ"), new SeqValue());
		}
		catch (ValueException e)
		{
			return null;
		}
	}
	
	@Override
	public Value caseNamedType(TCNamedType node, Object arg)
	{
		try
		{
			return ValueFactory.mkRecord("Reflect", "NamedType",
					ValueFactory.mkSeq(node.typename.getExplicit(true).toString()));
		}
		catch (ValueException e)
		{
			return null;
		}
	}
	
	@Override
	public Value caseRecordType(TCRecordType node, Object arg)
	{
		try
		{
			ValueList fields = new ValueList();
			
			for (TCField f: node.fields)
			{
				fields.add(ValueFactory.mkRecord("Reflect", "Field",
						ValueFactory.mkSeq(f.tag),
						f.type.apply(this, arg)));
			}
			
			return ValueFactory.mkRecord("Reflect", "RecordType",
					ValueFactory.mkSeq(node.name.getExplicit(true).toString()),
					new SeqValue(fields));
		}
		catch (ValueException e)
		{
			return null;
		}
	}
	
	@Override
	public Value caseUnionType(TCUnionType node, Object arg)
	{
		try
		{
			TCUnionType u =(TCUnionType)node;
			ValueList m = new ValueList();
			
			for (TCType member: u.types)
			{
				m.add(member.apply(this, arg));
			}
			
			return ValueFactory.mkRecord("Reflect", "AggregateType",
					new QuoteValue("UNION"),
					new SeqValue(m));
		}
		catch (ValueException e)
		{
			return null;
		}
	}
}
