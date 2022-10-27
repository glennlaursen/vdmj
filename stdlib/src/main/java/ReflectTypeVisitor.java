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
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCInMapType;
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCNaturalOneType;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCOptionalType;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCQuoteType;
import com.fujitsu.vdmj.tc.types.TCRationalType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCSeq1Type;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSet1Type;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCTokenType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;
import com.fujitsu.vdmj.values.BooleanValue;
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
		else if (node instanceof TCNaturalOneType)
		{
			return new QuoteValue("NAT1");
		}
		else if (node instanceof TCIntegerType)
		{
			return new QuoteValue("INT");
		}
		else if (node instanceof TCRationalType)
		{
			return new QuoteValue("RAT");
		}
		else if (node instanceof TCRealType)
		{
			return new QuoteValue("REAL");
		}
		else if (node instanceof TCCharacterType)
		{
			return new QuoteValue("CHAR");
		}
		else if (node instanceof TCBooleanType)
		{
			return new QuoteValue("BOOL");
		}
		else if (node instanceof TCTokenType)
		{
			return new QuoteValue("TOKEN");
		}
		
		throw new RuntimeException("Unknown basic type");
	}
	
	@Override
	public Value caseClassType(TCClassType node, Object arg)
	{
		try
		{
			return ValueFactory.mkRecord("Reflect", "ClassType",
					new SeqValue(node.name.getExplicit(true).toString()));
		}
		catch (ValueException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Value caseQuoteType(TCQuoteType node, Object arg)
	{
		try
		{
			return ValueFactory.mkRecord("Reflect", "QuoteType", new SeqValue(node.value));
		}
		catch (ValueException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Value caseOptionalType(TCOptionalType node, Object arg)
	{
		try
		{
			return ValueFactory.mkRecord("Reflect", "OptionalType", node.type.apply(this, arg));
		}
		catch (ValueException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Value caseSetType(TCSetType node, Object arg)
	{
		try
		{
			String kind = (node instanceof TCSet1Type) ? "SET1" : "SET";
			return ValueFactory.mkRecord("Reflect", "SetSeqType",
					new QuoteValue(kind), node.setof.apply(this, arg));
		}
		catch (ValueException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Value caseSeqType(TCSeqType node, Object arg)
	{
		try
		{
			String kind = (node instanceof TCSeq1Type) ? "SEQ1" : "SEQ";
			return ValueFactory.mkRecord("Reflect", "SetSeqType",
					new QuoteValue(kind), node.seqof.apply(this, arg));
		}
		catch (ValueException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Value caseMapType(TCMapType node, Object arg)
	{
		try
		{
			return ValueFactory.mkRecord("Reflect", "MapType",
					new BooleanValue(node instanceof TCInMapType),
					node.from.apply(this, arg),
					node.to.apply(this, arg));
		}
		catch (ValueException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public Value caseProductType(TCProductType node, Object arg)
	{
		try
		{
			ValueList m = new ValueList();
			
			for (TCType member: node.types)
			{
				m.add(member.apply(this, arg));
			}
			
			return ValueFactory.mkRecord("Reflect", "ProductType", new SeqValue(m));
		}
		catch (ValueException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public Value caseUnionType(TCUnionType node, Object arg)
	{
		try
		{
			ValueList m = new ValueList();
			
			for (TCType member: node.types)
			{
				m.add(member.apply(this, arg));
			}
			
			return ValueFactory.mkRecord("Reflect", "UnionType", new SeqValue(m));
		}
		catch (ValueException e)
		{
			throw new RuntimeException(e);
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
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Value caseFunctionType(TCFunctionType node, Object arg)
	{
		try
		{
			ValueList p = new ValueList();
			
			for (TCType param: node.parameters)
			{
				p.add(param.apply(this, arg));
			}
			
			return ValueFactory.mkRecord("Reflect", "FunctionType",
					new SeqValue(p), node.result.apply(this, arg));
		}
		catch (ValueException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Value caseOperationType(TCOperationType node, Object arg)
	{
		try
		{
			ValueList p = new ValueList();
			
			for (TCType param: node.parameters)
			{
				p.add(param.apply(this, arg));
			}
			
			return ValueFactory.mkRecord("Reflect", "OperationType",
					new SeqValue(p), node.result.apply(this, arg));
		}
		catch (ValueException e)
		{
			throw new RuntimeException(e);
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
			throw new RuntimeException(e);
		}
	}
}
