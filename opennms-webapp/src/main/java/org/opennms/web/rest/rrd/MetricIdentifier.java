package org.opennms.web.rest.rrd;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

public class MetricIdentifier {
    private String resourceId;
    private String attributeId;

    public MetricIdentifier() {
    }

    public MetricIdentifier(final String resourceId,
                            final String attributeId) {
        this.resourceId = resourceId;
        this.attributeId = attributeId;
    }

    @XmlAttribute(name = "resource")
    @JsonProperty("resource")
    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    @XmlAttribute(name = "attribute")
    @JsonProperty("attribute")
    public String getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(String attributeId) {
        this.attributeId = attributeId;
    }
}
