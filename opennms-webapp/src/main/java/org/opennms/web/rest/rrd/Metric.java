package org.opennms.web.rest.rrd;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.Map;

public class Metric {
    private long timestamp;
    private Map<String, Double> values;

    public Metric() {
    }

    public Metric(final long timestamp,
                  final Map<String, Double> values) {
        this.timestamp = timestamp;
        this.values = values;
    }

    @XmlAttribute(name = "timestamp")
    @JsonProperty("timestamp")
    public long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(final long timestamp) {
        this.timestamp = timestamp;
    }

    @XmlElement(name = "values")
    @JsonProperty("values")
    public Map<String, Double> getValues() {
        return this.values;
    }

    public void setValues(final Map<String, Double> values) {
        this.values = values;
    }
}
