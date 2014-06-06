package org.opennms.web.rest.rrd.graph;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class JEXLExpressionBuilder {
    public String fromRPN(String rpnExpression) {
        Stack<String> stack = new Stack<String>();
        
        // Split the string by token and push them to the stack
        for (String token : rpnExpression.split(",")) {
            stack.push(token);
        }

        // Unwind
        StringBuilder sb = new StringBuilder();
        evalrpn(stack, sb);
        return sb.toString();
    }

    private String evalrpn(Stack<String> tks, StringBuilder sb) {
        String tk = tks.pop();
        if (isOperator(tk)) {
            String y = evalrpn(tks, sb);
            String x = evalrpn(tks, sb);
            sb.append(String.format("(%s %s %s)", x, tk, y));
        }
        return tk;
    }

    public boolean isOperator(String token) {
        Set<String> ops = new HashSet<String>();
        ops.add("/");
        ops.add("-");
        ops.add("+");
        ops.add("*");
        return ops.contains(token);
    }
}
