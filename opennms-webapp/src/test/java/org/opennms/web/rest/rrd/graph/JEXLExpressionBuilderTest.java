package org.opennms.web.rest.rrd.graph;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JEXLExpressionBuilderTest {
    private JEXLExpressionBuilder builder = new JEXLExpressionBuilder();

    @Test
    public void testRPNConversion() {
        String rpn = "maxContext,1,/";
        String expr = "(maxContext / 1)";
        assertEquals(expr, builder.fromRPN(rpn));
    }
}
