package org.opennms.web.rest.rrd.graph;

import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * References:
 *   http://oss.oetiker.ch/rrdtool/doc/rrdgraph_rpn.en.html
 *   http://commons.apache.org/proper/commons-jexl/reference/syntax.html
 *
 * @author jesse
 */
public class JEXLExpressionBuilder {
	private final List<Operator> ops = Lists.newLinkedList();
	private final Map<String, Operator> opsBySymbol = Maps.newHashMap();
	
	public JEXLExpressionBuilder() {
		ops.add(new SimpleOp("+"));
		ops.add(new SimpleOp("-"));
		ops.add(new SimpleOp("*"));
		ops.add(new SimpleOp("/"));
		ops.add(new SimpleOp("%"));
		ops.add(new IfOp());
		ops.add(new UnOp());
		ops.add(new BooleanOp("LT", "<"));
		ops.add(new BooleanOp("LE", "<="));
		ops.add(new BooleanOp("GT", ">"));
		ops.add(new BooleanOp("GE", ">="));
		ops.add(new BooleanOp("EQ", "=="));
		ops.add(new BooleanOp("NE", "!="));

		for (Operator op : ops) {
			opsBySymbol.put(op.getSymbol(), op);
		}
	}

    public String fromRPN(String rpnExpression) throws JEXLConversionException {
        Stack<String> stack = new Stack<String>();
        
        // Tokenize the string
        for (String token : rpnExpression.split(",")) {
        	Operator op = opsBySymbol.get(token);
        	if (op != null) {
        		stack.push(op.getExpression(stack));
        	} else {
        		stack.push(token);
        	}
        }

        if (stack.size() == 1) {
        	return stack.pop();
        } else {
        	System.out.println(stack);
        	throw new JEXLConversionException("Too many input values.");
        }
    }

	public static class SimpleOp extends Operator {
		public SimpleOp(final String symbol) {
			super(symbol, 2);
		}

		public String getExpression(String... args) {
			return String.format("(%s %s %s)", args[0], getSymbol(), args[1]);
		}
	}

	public static class UnOp extends Operator {
		public UnOp() {
			super("UN", 1);
		}

		public String getExpression(String... args) {
			return String.format("( (%s == __inf) || (%s == __neg_inf) ? 1 : 0)", args[0], args[0]);
		}
	}

	public static class IfOp extends Operator {
		// A,B,C,IF should be read as if (A) then (B) else (C)
		
		public IfOp() {
			super("IF", 3);
		}

		public String getExpression(String... args) {
			return String.format("(%s != 0 ? %s : %s)", args[0], args[1], args[2]);
		}
	}

	public static class BooleanOp extends Operator {
		private final String op;
		
		public BooleanOp(final String symbol, final String op) {
			super(symbol, 2);
			this.op = op;
		}
		
		public String getExpression(String... args) {
			return String.format("(%s %s %s ? 1 : 0)", args[0], op, args[1]);
		}
	}

    public static abstract class Operator {
    	final String symbol;
    	final int numArgs;
    	
    	public Operator(final String symbol, final int numArgs) {
    		this.symbol = symbol;
    		this.numArgs = numArgs;
    	}

    	public String getSymbol() {
    		return symbol;
    	}

    	public abstract String getExpression(String... args);

    	public String getExpression(Stack<String> stack) throws JEXLConversionException {
    		// Verify that we have the required number of arguments on the stack
    		if (stack.size() < numArgs) {
    			throw new JEXLConversionException(
    					String.format("%s requires %d parameters but the stack only has %d elements",
    							symbol, numArgs, stack.size()));
    		}

    		// Retrieve the arguments from the stack, reverse their order
    		String[] args = new String[numArgs];
    		for (int i = numArgs-1; i >= 0; i--) {
    			args[i] = stack.pop();
    		}

    		// Build the JEXL expression - specific to the operator in question
    		return getExpression(args);
    	}

		@Override
		public String toString() {
			return symbol;
		}
    }
}
