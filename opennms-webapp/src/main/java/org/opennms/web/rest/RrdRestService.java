package org.opennms.web.rest;

import com.sun.jersey.spi.resource.PerRequest;
import org.jrobin.core.RrdException;
import org.jrobin.data.DataProcessor;
import org.opennms.netmgt.dao.ResourceDao;
import org.opennms.netmgt.model.OnmsAttribute;
import org.opennms.netmgt.model.OnmsResource;
import org.opennms.netmgt.model.RrdGraphAttribute;
import org.opennms.netmgt.rrd.MultiOutputRrdStrategy;
import org.opennms.netmgt.rrd.QueuingRrdStrategy;
import org.opennms.netmgt.rrd.RrdStrategy;
import org.opennms.netmgt.rrd.RrdUtils;
import org.opennms.netmgt.rrd.jrobin.JRobinRrdStrategy;
import org.opennms.netmgt.rrd.rrdtool.JniRrdStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

@Component
@PerRequest
@Scope("prototype")
@Path("/rrd")
public class RrdRestService extends OnmsRestService {

    @XmlType
    public static class MetricIdentifier {
        private String m_resourceId;
        private String m_attributeId;

        public MetricIdentifier() {
        }

        public MetricIdentifier(final String resourceId,
                                final String attributeId) {
            this.m_resourceId = resourceId;
            this.m_attributeId = attributeId;
        }

        @XmlAttribute(name = "resource")
        public String getResourceId() {
            return m_resourceId;
        }

        public void setResourceId(String resourceId) {
            m_resourceId = resourceId;
        }

        @XmlAttribute(name = "attribute")
        public String getAttributeId() {
            return m_attributeId;
        }

        public void setAttributeId(String attributeId) {
            m_attributeId = attributeId;
        }
    }

    @XmlRootElement(name = "query")
    public static class QueryRequest {
        private long m_steps;

        private long m_start;
        private long m_end;

        private Map<String, MetricIdentifier> m_series;

        @XmlAttribute(name = "steps")
        public long getSteps() {
            return m_steps;
        }

        public void setSteps(long steps) {
            m_steps = steps;
        }

        @XmlAttribute(name = "start")
        public long getStart() {
            return m_start;
        }

        public void setStart(final long start) {
            m_start = start;
        }

        @XmlAttribute(name = "end")
        public long getEnd() {
            return m_end;
        }

        public void setEnd(final long end) {
            m_end = end;
        }

        @XmlElement(name = "series")
        public Map<String, MetricIdentifier> getSeries() {
            return m_series;
        }

        public void setSeries(final Map<String, MetricIdentifier> series) {
            m_series = series;
        }
    }

    @XmlRootElement(name = "query")
    public static class QueryResponse {
        private long m_steps;

        private long m_start;
        private long m_end;

        private SortedMap<Long, Map<String, Double>> m_series;

        @XmlAttribute(name = "steps")
        public long getSteps() {
            return m_steps;
        }

        public void setSteps(long steps) {
            m_steps = steps;
        }

        @XmlAttribute(name = "start")
        public long getStart() {
            return m_start;
        }

        public void setStart(final long start) {
            m_start = start;
        }

        @XmlAttribute(name = "end")
        public long getEnd() {
            return m_end;
        }

        public void setEnd(final long end) {
            m_end = end;
        }

        @XmlElement(name = "series")
        public SortedMap<Long, Map<String, Double>> getSeries() {
            return m_series;
        }

        public void setSeries(final SortedMap<Long, Map<String, Double>> series) {
            m_series = series;
        }
    }

    @Autowired
    private ResourceDao m_resourceDao;

    @GET
    @Path("/")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    public Response test() {
        final QueryRequest x = new QueryRequest();
        x.setSteps(300);
        x.setStart(1000);
        x.setEnd(2000);
        final Map<String, MetricIdentifier> m = new HashMap<String, MetricIdentifier>();
        m.put("x", new MetricIdentifier("node[1].responseTime[127.0.0.1]", "icmp"));
        x.setSeries(m);

        return Response.ok(x).build();
    }

    @POST
    @Path("/")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    public Response query(final QueryRequest request) {
        readLock();
        try {

            final QueryResponse response = new QueryResponse();
            response.setSteps(request.getSteps());
            response.setStart(request.getStart());
            response.setEnd(request.getEnd());

            try {
                if (findStrategy() instanceof JRobinRrdStrategy) {
                    response.setSeries(queryJrb(request.getSteps(),
                            request.getStart(),
                            request.getEnd(),
                            request.getSeries()));
                } else {
                    return Response
                            .serverError()
                            .entity("No appropriate RRD strategy found")
                            .build();
                }

                return Response
                        .ok(response)
                        .build();

            } catch (Exception e) {
                return Response
                        .serverError()
                        .entity(e)
                        .build();
            }

        } finally {
            readUnlock();
        }
    }

    @GET
    @Path("/{resourceId}")
    @Produces({MediaType.TEXT_PLAIN})
    @Consumes(MediaType.TEXT_PLAIN)
    public Response info(@PathParam("resourceId") final String resourceId) {
        readLock();
        try {

            OnmsResource resource = m_resourceDao.getResourceById(resourceId);

            String result = "";

            RrdStrategy rrdStrategy = findStrategy(RrdUtils.getStrategy());

            result += "<h1>" + rrdStrategy.getClass().getSimpleName() + "</h1>";

            result += resource.getId() + "<br>";
            result += resource.getLabel() + "<br>";
            result += resource.getLink() + "<br>";
            result += resource.getName() + "<br>";
            result += resource.getResourceType().getName() + "<br>";
            result += resource.getResourceType().getLabel() + "<br>";

            result += "<h1>Attributes</h1>";

            for (OnmsAttribute onmsAttribute : resource.getAttributes()) {
                result += onmsAttribute.getName() + " " + onmsAttribute.getResource() + "<br>";
            }

            result += "<h1>Child resources</h1>";

            for (OnmsResource onmsResource : resource.getChildResources()) {
                result += onmsResource + "<br>";
            }

            result += "<h1>External Values</h1>";

            for (Map.Entry<String, String> entry : resource.getExternalValueAttributes().entrySet()) {
                result += entry.getKey() + "=" + entry.getValue() + "<br>";
            }

            result += "<h1>Rrd Graph Attributes</h1>";

            for (Map.Entry<String, RrdGraphAttribute> entry : resource.getRrdGraphAttributes().entrySet()) {
                result += entry.getKey() + "=" + entry.getValue().getName() + "/" + entry.getValue().getRrdRelativePath() + "/" + entry.getValue().getResource().getName() + "<br>";
            }

            result += "<h1>String Property Attributes</h1>";

            for (Map.Entry<String, String> entry : resource.getStringPropertyAttributes().entrySet()) {
                result += entry.getKey() + "=" + entry.getValue() + "<br>";
            }

            return Response.ok(result, "text/html").build();
        } finally {
            readUnlock();
        }
    }

    private SortedMap<Long, Map<String, Double>> queryJrb(long step, long start, long end, Map<String, MetricIdentifier> requestedMetrics) throws IOException, RrdException {

        DataProcessor dproc = new DataProcessor(start, end);
        dproc.setStep(step);
        dproc.setFetchRequestResolution(300);

        for (Map.Entry<String, MetricIdentifier> entry : requestedMetrics.entrySet()) {

            String key = entry.getKey();

            OnmsResource resource = m_resourceDao.getResourceById(entry.getValue().getResourceId());
            RrdGraphAttribute rrdGraphAttribute = resource.getRrdGraphAttributes().get(entry.getValue().getAttributeId());

            dproc.addDatasource(key, System.getProperty("rrd.base.dir") + "/" + rrdGraphAttribute.getRrdRelativePath(), entry.getValue().getAttributeId(), "AVERAGE");
        }

        SortedMap<Long, Map<String, Double>> results = new TreeMap<Long, Map<String, Double>>();

        dproc.processData();

        long[] timestamps = dproc.getTimestamps();

        for (int i = 0; i < timestamps.length; i++) {
            timestamps[i] = timestamps[i] - dproc.getStep();

            for (String key : requestedMetrics.keySet()) {
                if (!results.containsKey(timestamps[i])) {
                    results.put(timestamps[i], new TreeMap<String, Double>());
                }

                results.get(timestamps[i]).put(key, dproc.getValues(key)[i]);
            }
        }

        return results;
    }

    private static RrdStrategy findStrategy() {
        return findStrategy(RrdUtils.getStrategy());
    }

    private static RrdStrategy findStrategy(final RrdStrategy rrdStrategy) {
        if (rrdStrategy instanceof JniRrdStrategy || rrdStrategy instanceof JRobinRrdStrategy) {
            return rrdStrategy;
        }

        if (rrdStrategy instanceof QueuingRrdStrategy) {
            return findStrategy(((QueuingRrdStrategy) rrdStrategy).getDelegate());
        }

        if (rrdStrategy instanceof MultiOutputRrdStrategy) {
            for (final RrdStrategy delegate : ((MultiOutputRrdStrategy) rrdStrategy).getDelegates()) {
                RrdStrategy x = findStrategy(delegate);

                if (x instanceof JniRrdStrategy || x instanceof JRobinRrdStrategy) {
                    return x;
                }
            }
        }

        return rrdStrategy;
    }
}
