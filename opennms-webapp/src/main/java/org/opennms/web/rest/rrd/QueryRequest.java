package org.opennms.web.rest.rrd;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.xml.bind.annotation.*;
import java.util.Map;

@XmlRootElement(name = "query-request")
public class QueryRequest {
    private long step;

    private long start;
    private long end;

    private Map<String, MetricIdentifier> series;

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

    @XmlElement(name = "series")
    @JsonProperty("series")
    public Map<String, MetricIdentifier> getSeries() {
        return series;
    }

    public void setSeries(final Map<String, MetricIdentifier> series) {
        this.series = series;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueryRequest that = (QueryRequest) o;

        if (end != that.end) return false;
        if (start != that.start) return false;
        if (step != that.step) return false;
        if (series != null ? !series.equals(that.series) : that.series != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (step ^ (step >>> 32));
        result = 31 * result + (int) (start ^ (start >>> 32));
        result = 31 * result + (int) (end ^ (end >>> 32));
        result = 31 * result + (series != null ? series.hashCode() : 0);
        return result;
    }
}
