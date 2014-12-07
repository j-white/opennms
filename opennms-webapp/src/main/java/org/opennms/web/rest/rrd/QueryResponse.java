package org.opennms.web.rest.rrd;

import javax.xml.bind.annotation.*;
import java.util.List;
import java.util.Map;

@XmlRootElement(name = "query-response")
public class QueryResponse {
    public static class Metric {
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
        public long getTimestamp() {
            return this.timestamp;
        }

        public void setTimestamp(final long timestamp) {
            this.timestamp = timestamp;
        }

        @XmlElement(name = "values")
        public Map<String, Double> getValues() {
            return this.values;
        }

        public void setValues(final Map<String, Double> values) {
            this.values = values;
        }
    }

    private long step;

    private long start;
    private long end;

    private List<Metric> metrics;

    @XmlAttribute(name = "step")
    public long getStep() {
        return this.step;
    }

    public void setStep(long step) {
        this.step = step;
    }

    @XmlAttribute(name = "start")
    public long getStart() {
        return this.start;
    }

    public void setStart(final long start) {
        this.start = start;
    }

    @XmlAttribute(name = "end")
    public long getEnd() {
        return this.end;
    }

    public void setEnd(final long end) {
        this.end = end;
    }

    @XmlElement(name = "metrics")
    public List<Metric> getMetrics() {
        return this.metrics;
    }

    public void setMetrics(final List<Metric> metrics) {
        this.metrics = metrics;
    }
}
