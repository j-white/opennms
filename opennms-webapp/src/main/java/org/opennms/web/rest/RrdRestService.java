package org.opennms.web.rest;

import com.sun.jersey.spi.resource.PerRequest;
import org.exolab.castor.xml.Unmarshaller;
import org.jrobin.core.RrdException;
import org.jrobin.data.DataProcessor;
import org.opennms.core.utils.StringUtils;
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
import org.springframework.util.FileCopyUtils;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Component
@PerRequest
@Scope("prototype")
@Path("rrd")
public class RrdRestService extends OnmsRestService {

    public static class MetricIdentifier {
        private String m_resourceId;
        private String m_attributeId;

        public MetricIdentifier(String resourceId, String attributeId) {
            this.m_resourceId = resourceId;
            this.m_attributeId = attributeId;
        }

        public String getResourceId() {
            return m_resourceId;
        }

        public void setResourceId(String m_resourceId) {
            this.m_resourceId = m_resourceId;
        }

        public String getAttributeId() {
            return m_attributeId;
        }

        public void setAttributeId(String m_attributeId) {
            this.m_attributeId = m_attributeId;
        }
    }

    @Autowired
    private ResourceDao m_resourceDao;

    /*
    [
      [
        {
            "name": "high",
            "timestamp": 1401824160000,
            "value": 680.99
        }, {
            "name": "ask",
            "timestamp": 1401824160000,
            "value": 675.8095833333334
        }, {
            "name": "low",
            "timestamp": 1401824160000,
            "value": 638.5215833333333
        }, {
            "name": "spread",
            "timestamp": 1401824160000,
            "value": 2.3978333333334376
        }, {
            "name": "bid",
            "timestamp": 1401824160000,
            "value": 673.41175
        }
      ],
      [
        {
        "name": "high",
        "timestamp": 1401824400000,
        "value": 680.99
    }, {
     */
    @GET
    @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    @Path("export/{resourceId}/{attribute}/{start}/{end}")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response export(@PathParam("resourceId") final String resourceId, @PathParam("attribute") final String attribute, @PathParam("start") final long start, @PathParam("end") final long end) {
        readLock();
        try {

            String result = "";

            if (getStrategy(RrdUtils.getStrategy()) instanceof JRobinRrdStrategy) {
                Map<String, MetricIdentifier> requestedMetrics = new HashMap<String, MetricIdentifier>();
                requestedMetrics.put("key", new MetricIdentifier(resourceId, attribute));
                try {
                    Map<Long, Map<String, Double>> results = exportJrb(300, start, end, requestedMetrics);

                    for (Map.Entry<Long, Map<String, Double>> entry : results.entrySet()) {
                        for (Map.Entry<String, Double> metricValue : entry.getValue().entrySet()) {
                            result += entry.getKey() + " " + metricValue.getKey() + " " + metricValue.getValue();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (RrdException e) {
                    e.printStackTrace();
                }
            }

            return Response.ok(result, "text/html").build();
        } finally {
            readUnlock();
        }
    }

    @GET
    @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    @Path("query/{resourceId}")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response query(@PathParam("resourceId") final String resourceId) {
        readLock();
        try {

            OnmsResource resource = m_resourceDao.getResourceById(resourceId);

            String result = "";

            RrdStrategy rrdStrategy = getStrategy(RrdUtils.getStrategy());

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

    private Map<Long, Map<String, Double>> exportRrd(int step, long start, long end, Map<String, MetricIdentifier> requestedMetrics) throws IOException, RrdException {
        String rrdBinary = System.getProperty("rrd.binary");

        if (rrdBinary == null) {
            throw new RrdException("rrd.binary property must be set either in opennms.properties or in iReport");
        }

        /**
         * TODO: construct the query string out of the requestedMetrics data
         */
        String queryString = "";

        String command = rrdBinary + " xport " + queryString.replaceAll("[\r\n]+", " ").replaceAll("\\s+", " ");

        String[] commandArray = StringUtils.createCommandArray(command, '@');

        /**
         * TODO: create class for the Xml unmarshalling...
         */
        Object data = null;

        try {
            Process process = Runtime.getRuntime().exec(commandArray);
            byte[] byteArray = FileCopyUtils.copyToByteArray(process.getInputStream());
            String errors = FileCopyUtils.copyToString(new InputStreamReader(process.getErrorStream()));

            if (errors.length() > 0) {
                return null;
            }

            BufferedReader reader = null;

            try {
                InputStream is = new ByteArrayInputStream(byteArray);
                reader = new BufferedReader(new InputStreamReader(is));
                /**
                 * TODO: ...and use the class here
                 */
                data = (Object) Unmarshaller.unmarshal(Object.class, reader);
            } finally {
                reader.close();
            }
        } catch (Exception e) {
            throw new RrdException("exportRrd: can't execute command '" + command + ": ", e);
        }

        Map<Long, Map<String, Double>> results = new TreeMap<Long, Map<String, Double>>();

        /**
         * TODO: constuct the response object out of the unmarshalled xml data
         */
        return results;
    }

    private Map<Long, Map<String, Double>> exportJrb(int step, long start, long end, Map<String, MetricIdentifier> requestedMetrics) throws IOException, RrdException {

        DataProcessor dproc = new DataProcessor(start, end);
        dproc.setStep(300);
        dproc.setFetchRequestResolution(300);

        for (Map.Entry<String, MetricIdentifier> entry : requestedMetrics.entrySet()) {

            String key = entry.getKey();

            OnmsResource resource = m_resourceDao.getResourceById(entry.getValue().getResourceId());
            RrdGraphAttribute rrdGraphAttribute = resource.getRrdGraphAttributes().get(entry.getValue().getAttributeId());

            dproc.addDatasource(key, System.getProperty("rrd.base.dir") + "/" + rrdGraphAttribute.getRrdRelativePath(), entry.getValue().getAttributeId(), "AVERAGE");
        }

        Map<Long, Map<String, Double>> results = new TreeMap<Long, Map<String, Double>>();

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

    private static RrdStrategy getStrategy(RrdStrategy rrdStrategy) {
        if (rrdStrategy instanceof JniRrdStrategy || rrdStrategy instanceof JRobinRrdStrategy) {
            return rrdStrategy;
        }

        if (rrdStrategy instanceof QueuingRrdStrategy) {
            return getStrategy(((QueuingRrdStrategy) rrdStrategy).getDelegate());
        }

        if (rrdStrategy instanceof MultiOutputRrdStrategy) {
            for (RrdStrategy delegate : ((MultiOutputRrdStrategy) rrdStrategy).getDelegates()) {
                RrdStrategy x = getStrategy(delegate);

                if (x instanceof JniRrdStrategy || x instanceof JRobinRrdStrategy) {
                    return x;
                }
            }
        }

        return rrdStrategy;
    }
}
