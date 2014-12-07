package org.opennms.web.rest.rrd.graph;

import java.awt.Color;
import java.awt.Paint;
import java.io.File;

import org.jrobin.core.RrdException;
import org.jrobin.graph.RrdGraphDef;
import org.opennms.netmgt.rrd.jrobin.JRobinRrdStrategy;

public class NGGraphModelBuilder extends JRobinRrdStrategy {
    public NGGraphModelBuilder() throws Exception {
        super();
    }

    public RrdGraphDef createGraphDef() {
        return new RrdGraphDefVisitor();
    }

    public NGGraphModel createNGGraph(final String command, final File workDir) throws RrdException, JEXLConversionException {
        RrdGraphDefVisitor visitor = (RrdGraphDefVisitor)createGraphDef(command, workDir);
        return visitor.toModel();
    }

    public static class RrdGraphDefVisitor extends RrdGraphDef  {
        private JEXLExpressionBuilder m_jexlBuilder = new JEXLExpressionBuilder();

        private JEXLConversionException m_jexlConversionException = null;
        
        private NGGraphModel m_model = new NGGraphModel();

        private String m_lastRrdPath = null;

        public NGGraphModel toModel() throws JEXLConversionException {
        	if (m_jexlConversionException != null) {
        		throw m_jexlConversionException;
        	}
            return m_model;
        }

        public void setTitle(String title) {
            m_model.setTitle(title);
        }
        
        public void datasource(String name, String rrdPath, String dsName, String consolFun) {
            datasource(name, rrdPath, dsName, consolFun, null);
        }

        public void datasource(String name, String rrdPath, String dsName, String consolFun, String backend) {
            m_lastRrdPath = rrdPath;
            m_model.addSource(new NGGraphModel.RrdSource(name, rrdPath, dsName, consolFun));
        }
        
        public void datasource(String name, String rpnExpression) {
            try {
				m_model.addSource(new NGGraphModel.ExpressionSource(name, m_lastRrdPath, m_jexlBuilder.fromRPN(rpnExpression)));
			} catch (JEXLConversionException e) {
				m_jexlConversionException = e;
			}
        }

        /*
        public void datasource(String name, String defName, String consolFun) {
            
        }
        
        public void datasource(String name, Plottable plottable) {
            
        }
        
        public void datasource(String name, String sourceName, double percentile) throws RrdException {
            
        }
        
        public void datasource(String name, String sourceName, double percentile, boolean includenan) throws RrdException {
        
        }
        */

        public String toHex(Paint paint) {
            Color color = (Color)paint;
            return "#"+Integer.toHexString(color.getRGB()).substring(2);
        }

        public void line(String srcName, Paint color, String legend, float width) {
            m_model.addSeries(new NGGraphModel.Series(legend, srcName, "line", toHex(color)));
        }

        public void line(String srcName, Paint color, String legend) {
            m_model.addSeries(new NGGraphModel.Series(legend, srcName, "line", toHex(color)));
        }
        
        public void area(String srcName, Paint color, String legend) {
            m_model.addSeries(new NGGraphModel.Series(legend, srcName, "area", toHex(color)));
        }
        
        public void area(String srcName, Paint color) {
            m_model.addSeries(new NGGraphModel.Series("", srcName, "area", toHex(color)));
        }
        
        public void stack(String srcName, Paint color, String legend) throws RrdException {
            m_model.addSeries(new NGGraphModel.Series(legend, srcName, "stack", toHex(color)));
        }
    }
}
