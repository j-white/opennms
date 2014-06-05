package org.opennms.web.rest.rrd;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "query-response")
public class QueryResponse {

    private long step;

    private long start;
    private long end;

    private List<Metric> metrics;

    @XmlAttribute(name = "step")
    @JsonProperty("step")
    public long getStep() {
        return step;
    }

    public void setStep(long step) {
        this.step = step;
    }

    @XmlAttribute(name = "start")
    @JsonProperty("start")
    public long getStart() {
        return start;
    }

    public void setStart(final long start) {
        this.start = start;
    }

    @XmlAttribute(name = "end")
    @JsonProperty("end")
    public long getEnd() {
        return end;
    }

    public void setEnd(final long end) {
        this.end = end;
    }

    @XmlElement(name = "metrics")
    @JsonProperty("metrics")
    public List<Metric> getMetrics() {
        return metrics;
    }

    public void setMetrics(final List<Metric> metrics) {
        this.metrics = metrics;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueryResponse that = (QueryResponse) o;

        if (end != that.end) return false;
        if (start != that.start) return false;
        if (step != that.step) return false;
        if (metrics != null ? !metrics.equals(that.metrics) : that.metrics != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (step ^ (step >>> 32));
        result = 31 * result + (int) (start ^ (start >>> 32));
        result = 31 * result + (int) (end ^ (end >>> 32));
        result = 31 * result + (metrics != null ? metrics.hashCode() : 0);
        return result;
    }
}
