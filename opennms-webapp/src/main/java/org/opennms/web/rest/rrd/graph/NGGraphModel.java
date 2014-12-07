package org.opennms.web.rest.rrd.graph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.google.common.collect.Lists;

@XmlRootElement(name="model")
public class NGGraphModel implements Serializable {
    private static final long serialVersionUID = -6184505720990699926L;

    /** the class used to retrieve the data */
    private DataProcessor m_dataProcessor;

    private String m_title;

    private List<RrdSource> m_rrdSources = new ArrayList<RrdSource>();
    
    private List<ExpressionSource> m_expressionSources = new ArrayList<ExpressionSource>();

    private List<Series> m_series = new ArrayList<Series>();

    private List<String> m_legend = new ArrayList<String>();

    public NGGraphModel() {
        m_dataProcessor = new DataProcessor("onmsrrd");
    }

    @XmlElement(name = "dataProcessor")
    public DataProcessor getDataProcessor() {
        return m_dataProcessor;
    }

    @XmlElement(name = "title")
    public String getTitle() {
        return m_title;
    }

    @XmlElement(name = "sources")
    public List<Source> getSources() {
    	List<Source> sources = Lists.newLinkedList();
    	sources.addAll(getRrdSources());
    	sources.addAll(getExpressionSources());
    	return sources;
    }

    @XmlTransient
    @JsonIgnore
    public List<RrdSource> getRrdSources() {
        return m_rrdSources;
    }

    @XmlTransient
    @JsonIgnore
    public List<ExpressionSource> getExpressionSources() {
        return m_expressionSources;
    }

    @XmlElement(name = "series")
    public List<Series> getSeries() {
        return m_series;
    }

    @XmlElement(name = "legend")
    public List<String> getLegend() {
        return m_legend;
    }

    public void setTitle(String title) {
        m_title = title;
    }

    public void addSource(RrdSource source) {
        m_rrdSources.add(source);
    }

    public void addSource(ExpressionSource source) {
        m_expressionSources.add(source);
    }
    
    public void addSeries(Series series) {
        m_series.add(series);
    }

    public void setDataProcessor(DataProcessor dataProcessor) {
        m_dataProcessor = dataProcessor;
    }

    public static class Source {
        private String m_name;

        private String m_resource;

        @XmlElement(name = "name")
        public String getName() {
            return m_name;
        }

        @XmlElement(name = "resource")
        public String getResource() {
            return m_resource;
        }
        
        public Source() { }
        
        public Source(String name, String resource) { 
            m_name = name;
            m_resource = resource;
        }
    }

    @XmlRootElement(name="rrdSource")
    public static class RrdSource extends Source {
        private String m_dsName;

        private String m_csFunc;

        public RrdSource() { }

        public RrdSource(String name, String resource, String dsName, String csFunc) {
            super(name, resource);
            m_dsName = dsName;
            m_csFunc = csFunc;
        }

        @XmlElement(name = "dsName")
        public String getDsName() {
            return m_dsName;
        }

        @XmlElement(name = "csFunc")
        public String getCsFunc() {
            return m_csFunc;
        }
    }

    @XmlRootElement(name="expressionSource")
    public static class ExpressionSource extends Source {
        private String m_expression;

        public ExpressionSource() { }

        public ExpressionSource(String name, String resource, String expression) {
            super(name, resource);
            m_expression = expression;
        }

        @XmlElement(name = "expression")
        public String getExpression() {
            return m_expression;
        }
    }

    @XmlRootElement(name="series")
    public static class Series {
        private String m_name;

        private String m_source;

        private String m_type;

        private String m_color;

        public Series() { }

        public Series(String name, String source, String type, String color) {
            m_name = name;
            m_source = source;
            m_type = type;
            m_color = color;
        }

        @XmlElement(name = "name")
        public String getName() {
            return m_name;
        }

        @XmlElement(name = "source")
        public String getSource() {
            return m_source;
        }

        @XmlElement(name = "type")
        public String getType() {
            return m_type;
        }

        @XmlElement(name = "color")
        public String getColor() {
            return m_color;
        }
    }

    @XmlRootElement(name="dataProcessor")
    public static class DataProcessor {
        /** the type of data processor - newts or onmsrrd */
        private String m_type;
        
        /** url to the rest endpoint */
        private String m_url;

        public DataProcessor() {}

        public DataProcessor(String type) {
            m_type = type;
        }

        @XmlElement(name = "type")
        public String getType() {
            return m_type;
        }

        public void setType(String type) {
            m_type = type;
        }

        @XmlElement(name = "url")
        public String getUrl() {
            return m_url;
        }

        public void setUrl(String url) {
            m_url = url;
        }
    }
}
