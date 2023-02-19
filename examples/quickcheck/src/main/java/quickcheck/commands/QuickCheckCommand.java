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

package quickcheck.commands;

import static com.fujitsu.vdmj.plugins.PluginConsole.printf;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import com.fujitsu.vdmj.ast.expressions.ASTExpressionList;
import com.fujitsu.vdmj.ast.lex.LexBooleanToken;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleBindList;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.expressions.INBooleanLiteralExpression;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.expressions.INForAllExpression;
import com.fujitsu.vdmj.in.patterns.INBindingSetter;
import com.fujitsu.vdmj.in.patterns.INMultipleBindList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexException;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.analyses.POPlugin;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.RootContext;
import com.fujitsu.vdmj.syntax.BindReader;
import com.fujitsu.vdmj.syntax.ExpressionReader;
import com.fujitsu.vdmj.syntax.ParserException;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBindList;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeCheckException;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.SetValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

import quickcheck.visitors.DefaultRangeCreator;
import quickcheck.visitors.TypeBindFinder;

public class QuickCheckCommand extends AnalysisCommand
{
	private final static String USAGE = "Usage: quickcheck [-c <file>]|[-f <file>] [<PO numbers>]]";
	
	public QuickCheckCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("quickcheck") && !argv[0].equals("qc"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public void run()
	{
		String rangesFile = "ranges.qc";
		boolean createFile = false;
		List<Integer> poList = new Vector<Integer>();

		for (int i=1; i < argv.length; i++)
		{
			try
			{
				switch (argv[i])
				{
					case "-?":
					case "-help":
						println(USAGE);
						return;
						
					case "-f":
						rangesFile = argv[++i];
						createFile = false;
						break;
						
					case "-c":
						if (++i < argv.length) rangesFile = argv[i];
						createFile = true;
						break;
						
					default:
						poList.add(Integer.parseInt(argv[i]));
						break;
				}
			}
			catch (NumberFormatException e)
			{
				println("Malformed PO#: " + e.getMessage());
				println(USAGE);
				return;
			}
			catch (ArrayIndexOutOfBoundsException e)
			{
				println("Missing argument");
				println(USAGE);
				return;
			}
		}
		
		ProofObligationList chosen = getPOs(poList);

		if (chosen != null)
		{
			if (createFile)
			{
				createRanges(rangesFile, chosen);
			}
			else
			{
				Map<String, ValueList> ranges = readRanges(rangesFile);
				
				if (ranges != null)
				{
					checkObligations(chosen, ranges);
				}
			}
		}
	}
	
	private ProofObligationList getPOs(List<Integer> poList)
	{
		POPlugin plugin = registry.getPlugin("PO");
		ProofObligationList all = plugin.getProofObligations();
		all.renumber();

		if (poList.isEmpty())
		{
			return all;		// No PO#s specified
		}
		else
		{
			ProofObligationList list = new ProofObligationList();
			
			for (Integer n: poList)
			{
				if (n > 0 && n <= all.size())
				{
					list.add(all.get(n-1));
				}
				else
				{
					println("PO# " + n + " unknown. Must be between 1 and " + all.size());
				}
			}
			
			return list;
		}
	}
	
	private void checkFor(LexTokenReader reader, Token expected, String message) throws LexException, ParserException
	{
		LexToken last = reader.getLast();
		
		if (last.isNot(expected))
		{
			throw new ParserException(9000, message, last.location, 0);
		}
		
		reader.nextToken();
	}
	
	private Map<String, ValueList> readRanges(String filename)
	{
		try
		{
			File file = new File(filename);
			LexTokenReader ltr = new LexTokenReader(file, Dialect.VDM_SL);
			Interpreter interpreter = Interpreter.getInstance();
			String module = interpreter.getDefaultName();
			
			ASTMultipleBindList astbinds = new ASTMultipleBindList();
			ASTExpressionList astexps = new ASTExpressionList();
			
			while (ltr.getLast().isNot(Token.EOF))
			{
				BindReader br = new BindReader(ltr);
				br.setCurrentModule(module);
				astbinds.add(br.readMultipleBind());
				checkFor(ltr, Token.EQUALS, "Expecting <multiple bind> '=' <set expression>;");

				ExpressionReader er = new ExpressionReader(ltr);
				er.setCurrentModule(module);
				astexps.add(er.readExpression());
				checkFor(ltr, Token.SEMICOLON, "Expecting semi-colon after previous <set expression>");
			}
			
			TCMultipleBindList tcbinds = ClassMapper.getInstance(TCNode.MAPPINGS).convert(astbinds);
			TCExpressionList tcexps = ClassMapper.getInstance(TCNode.MAPPINGS).convert(astexps);
			Environment env = interpreter.getGlobalEnvironment();
			int errors = 0;
			
			for (int i=0; i<tcbinds.size(); i++)
			{
				TCMultipleBind mb = tcbinds.get(i);
				TCType mbtype = mb.typeCheck(env, NameScope.NAMESANDSTATE);
				TCSetType mbset = new TCSetType(mb.location, mbtype);
				
				TCExpression exp = tcexps.get(i);
				TCType exptype = exp.typeCheck(env, null, NameScope.NAMESANDSTATE, null);
				
				if (!TypeComparator.compatible(mbset, exptype))
				{
					println("Range bind and expression do not match at " + exp.location);
					println("Bind type: " + mbtype);
					println("Expression type: " + exptype + ", expecting " + mbset);
					errors++;
				}
			}
			
			if (errors > 0)
			{
				return null;
			}
			
			INMultipleBindList inbinds = ClassMapper.getInstance(INNode.MAPPINGS).convert(tcbinds);
			INExpressionList inexps = ClassMapper.getInstance(INNode.MAPPINGS).convert(tcexps);
			RootContext ctxt = interpreter.getInitialContext();
			Map<String, ValueList> ranges = new HashMap<String, ValueList>();
			long before = System.currentTimeMillis();
			
			for (int i=0; i<inbinds.size(); i++)
			{
				ctxt.threadState.init();
				String key = inbinds.get(i).toString();
				INExpression exp = inexps.get(i);
				Value value = exp.eval(ctxt);
				
				if (value instanceof SetValue)
				{
					SetValue svalue = (SetValue)value;
					ValueList list = new ValueList();
					list.addAll(svalue.values);
					ranges.put(key, list);
				}
				else
				{
					println("Range did not evaluate to a set " + exp.location);
					errors++;
				}
			}
			
			if (errors > 0)
			{
				return null;
			}
			
			long after = System.currentTimeMillis();
			println("Ranges expanded " + duration(before, after));

			return ranges;
		}
		catch (LexException e)
		{
			println(e.toString());
		}
		catch (ParserException e)
		{
			println(e.toString());
		}
		catch (TypeCheckException e)
		{
			println("Error: " + e.getMessage() + " " + e.location);
		}
		catch (InternalException e)
		{
			println(e.getMessage());
		}
		catch (ContextException e)
		{
			println(e.getMessage());
		}
		catch (Exception e)
		{
			println(e);
		}
		
		return null;
	}
	
	private INExpression getPOExpression(ProofObligation po) throws Exception
	{
		if (po.isCheckable)
		{
			TCExpression tcexp = po.getCheckedExpression();
			return ClassMapper.getInstance(INNode.MAPPINGS).convert(tcexp);
		}
		else
		{
			// Not checkable, so just use "true"
			return new INBooleanLiteralExpression(new LexBooleanToken(true, po.location));
		}
	}
	
	private List<INBindingSetter> getBindList(INExpression inexp, boolean foralls) throws Exception
	{
		return inexp.apply(new TypeBindFinder(), null);
	}
	
	private void createRanges(String filename, ProofObligationList chosen)
	{
		try
		{
			File file = new File(filename);
			PrintWriter writer = new PrintWriter(new FileWriter(file));
			Set<String> done = new HashSet<String>();
			DefaultRangeCreator rangeCreator = new DefaultRangeCreator();

			for (ProofObligation po: chosen)
			{
				for (INBindingSetter mbind: getBindList(getPOExpression(po), false))
				{
					if (!done.contains(mbind.toString()))
					{
						String range = mbind.getType().apply(rangeCreator, null);
						writer.println(mbind + " = " + range + ";");
						done.add(mbind.toString());
					}
				}
			}

			writer.close();
			println("Created " + done.size() + " default ranges in " + filename + ". Check them! Then run 'qc'");
		}
		catch (Exception e)
		{
			println("Can't create range file: " + e.getMessage());
		}
	}
	
	private void checkObligations(ProofObligationList chosen, Map<String, ValueList> ranges)
	{
		try
		{
			RootContext ctxt = Interpreter.getInstance().getInitialContext();
			List<INBindingSetter> bindings = null;

			for (ProofObligation po: chosen)
			{
				if (!po.isCheckable)
				{
					printf("PO# %d, UNCHECKED\n", po.number);
					continue;
				}

				Stack<Context> failPath = new Stack<Context>();
				INForAllExpression.setFailPath(failPath);
				INExpression poexp = getPOExpression(po);
				bindings = getBindList(poexp, false);
				
				for (INBindingSetter mbind: bindings)
				{
					ValueList values = ranges.get(mbind.toString());
					
					if (values != null)
					{
						mbind.setBindValues(values);
					}
					else
					{
						println("PO# " + po.number + ": No range defined for " + mbind);
					}
				}
				
				try
				{
					long before = System.currentTimeMillis();
					Value result = poexp.eval(ctxt);
					long after = System.currentTimeMillis();
					
					if (result instanceof BooleanValue)
					{
						if (result.boolValue(ctxt))
						{
							printf("PO# %d, PASSED %s\n", po.number, duration(before, after));
						}
						else
						{
							printf("PO# %d, FAILED %s: ", po.number, duration(before, after));
							printFailPath(failPath);
							println("\n" + po);
						}
					}
					else
					{
						printf("PO# %d, Error: PO evaluation returns %s?\n\n", po.number, result.kind());
						println(po);
					}
				}
				catch (Exception e)
				{
					printf("PO# %d, %s\n\n", po.number, e.getMessage());
					println(po);
				}
				finally
				{
					INForAllExpression.setFailPath(null);

					for (INBindingSetter mbind: bindings)
					{
						mbind.setBindValues(null);
					}
				}
			}
		}
		catch (Exception e)
		{
			println(e);
			return;
		}
	}
	
	private void printFailPath(Stack<Context> failPath)
	{
		printf("Counterexample: ");
		String sep = "";
		
		for (Context path: failPath)
		{
			for (TCNameToken name: path.keySet())
			{
				printf("%s%s = %s", sep, name, path.get(name));
				sep = ", ";
			}
		}
	}

	private Object duration(long before, long after)
	{
		double duration = (double)(after - before)/1000;
		return "in " + duration + "s";
	}
	
	public static void help()
	{
		println("Usage: quickcheck [-c <file>]|[-f <file>] [<PO numbers>]] - lightweight PO verification");
	}
}
