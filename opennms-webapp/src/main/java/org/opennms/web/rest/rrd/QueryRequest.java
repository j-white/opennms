package org.opennms.web.rest.rrd;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import com.google.common.base.Objects;

import java.util.LinkedList;
import java.util.List;

@XmlRootElement(name = "query-request")
public class QueryRequest {
    public static class Source {
        private String label;
        private String resource;
        private String attribute;
        private String aggregation = "AVERAGE";

        public Source() {
        }

        public Source(final String label,
                      final String resource,
                      final String attribute) {
            this.label = label;
            this.resource = resource;
            this.attribute = attribute;
        }

        @XmlAttribute(name = "label")
        public String getLabel() {
            return this.label;
        }

        public void setLabel(final String label) {
            this.label = label;
        }

        @XmlAttribute(name = "resource")
        public String getResource() {
            return this.resource;
        }

        public void setResource(final String resource) {
            this.resource = resource;
        }

        @XmlAttribute(name = "attribute")
        public String getAttribute() {
            return this.attribute;
        }

        public void setAttribute(final String attribute) {
            this.attribute = attribute;
        }

        @XmlAttribute(name = "aggregation")
        public String getAggregation() {
            return this.aggregation;
        }

        public void setAggregation(final String aggregation) {
            this.aggregation = aggregation;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this.getClass())
            		.add("label", label)
                    .add("resource", resource)
                    .add("attribute", attribute)
                    .add("aggregation", aggregation)
                    .toString();
        }
    }

    public static class Expression {
        private String label;
        private String expression;

        public Expression() {
        }

        public Expression(final String label,
                          final String expression) {
            this.label = label;
            this.expression = expression;
        }

        @XmlAttribute(name = "label")
        public String getLabel() {
            return this.label;
        }

        public void setLabel(final String label) {
            this.label = label;
        }

        @XmlValue
        public String getExpression() {
            return this.expression;
        }

        public void setExpression(final String expression) {
            this.expression = expression;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this.getClass())
            		.add("label", label)
                    .add("expression", expression)
                    .toString();
        }
    }

    private long step;

    private long start;
    private long end;

    private List<Source> sources = new LinkedList<Source>();
    private List<Expression> expressions = new LinkedList<Expression>();

    @XmlAttribute(name = "step")
    public long getStep() {
        return step;
    }

    public void setStep(long step) {
        this.step = step;
    }

    @XmlAttribute(name = "start")
    public long getStart() {
        return start;
    }

    public void setStart(final long start) {
        this.start = start;
    }

    @XmlAttribute(name = "end")
    public long getEnd() {
        return end;
    }

    public void setEnd(final long end) {
        this.end = end;
    }

    @XmlElement(name = "source")
    public List<Source> getSources() {
        return sources;
    }

    public void setSources(final List<Source> sources) {
        this.sources = sources;
    }

    @XmlElement(name = "expression")
    public List<Expression> getExpressions() {
        return expressions;
    }

    public void setExpressions(final List<Expression> expressions) {
        this.expressions = expressions;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this.getClass())
        		.add("start", start)
                .add("end", end)
                .add("step", step)
                .add("sources", getSources())
                .add("expressions", getExpressions())
                .toString();
    }
}
