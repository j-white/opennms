package org.opennms.web.rest.rrd.graph;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.jrobin.graph.RrdGraphConstants;

@XmlRootElement(name="container")
public class NGGraphContainer implements Serializable {
    private static final long serialVersionUID = -1442942041712300929L;

    /** the graph's width */
    private int m_width = RrdGraphConstants.DEFAULT_WIDTH;

    /** the graph's height */
    private int m_height = RrdGraphConstants.DEFAULT_HEIGHT;

    /** start time in seconds */
    private long m_start;

    /** end time in seconds */
    private long m_end;

    /** the model */
    private NGGraphModel m_model;

    @XmlElement(name = "width")
    public int getWidth() {
        return m_width;
    }

    @XmlElement(name = "height")
    public int getHeight() {
        return m_height;
    }

    @XmlElement(name = "start")
    public long getStart() {
        return m_start;
    }

    @XmlElement(name = "end")
    public long getEnd() {
        return m_end;
    }

    @XmlElement(name = "model")
    public NGGraphModel getModel() {
        return m_model;
    }

    public void setWidth(int width) {
        m_width = width;
    }


    public void setHeight(int height) {
        m_height = height;
    }

    public void setStart(long start) {
        m_start = start;
    }


    public void setEnd(long end) {
        m_end = end;
    }

    public void setModel(NGGraphModel model) {
        m_model = model;
    }
}
