package org.opennms.web.rest.rrd.graph;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JEXLExpressionBuilderTest {
    private JEXLExpressionBuilder builder = new JEXLExpressionBuilder();

    @Test
    public void testArithmeticRPNConversion() throws JEXLConversionException {
        String rpn = "5,1,2,+,4,*,+,3,-";
        String expr = "((5 + ((1 + 2) * 4)) - 3)";
        assertEquals(expr, builder.fromRPN(rpn));
    }

    @Test
    public void testRPNFromGraphsConversion() throws JEXLConversionException {
    	// Pulled from report.netsnmp.memStats
    	String rpn = "memtotalrealBytes,membufferBytes,-,memcachedBytes,-,memsharedBytes,-,memavailrealBytes,-";
    	String expr = "((((memtotalrealBytes - membufferBytes) - memcachedBytes) - memsharedBytes) - memavailrealBytes)";
    	assertEquals(expr, builder.fromRPN(rpn));

    	// Pulled from report.netsnmp.memStats
    	rpn = "maxMemshared,UN,0,maxMemshared,IF,1024,*";
    	expr = "((( (maxMemshared == __inf) || (maxMemshared == __neg_inf) ? 1 : 0) != 0 ? 0 : maxMemshared) * 1024)";
    	assertEquals(expr, builder.fromRPN(rpn));
    	
    	// Pulled from netsnmp.cpuStatsFull
    	rpn = "0,cpuUse,GE,0,float15,IF";
    	expr = "((0 >= cpuUse ? 1 : 0) != 0 ? 0 : float15)";
    	assertEquals(expr, builder.fromRPN(rpn));
    }
}
