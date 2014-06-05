package org.opennms.web.rest.rrd;

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
import org.opennms.web.rest.OnmsRestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.*;

@Component
@PerRequest
@Scope("prototype")
@Path("/rrd")
public class RrdRestService extends OnmsRestService {

    @Autowired
    private ResourceDao m_resourceDao;

    @GET
    @Path("/")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    public Response test() {
        final QueryRequest x = new QueryRequest();
        x.setStep(300);
        x.setStart(1000);
        x.setEnd(2000);
        final Map<String, MetricIdentifier> m = new HashMap<String, MetricIdentifier>();
        m.put("x", new MetricIdentifier("node[1].responseTime[127.0.0.1]", "icmp"));
        x.setSeries(m);

        return Response.ok(x).build();
    }

    @POST
    @Path("/")
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    public Response query(final QueryRequest request) throws Exception {
        readLock();
        try {

            final QueryResponse response = new QueryResponse();
            response.setStep(request.getStep());
            response.setStart(request.getStart());
            response.setEnd(request.getEnd());

            final SortedMap<Long, Map<String, Double>> data;
            if (findStrategy() instanceof JniRrdStrategy) {
                data = queryRrd(request.getStep(),
                        request.getStart(),
                        request.getEnd(),
                        request.getSeries());

            } else if (findStrategy() instanceof JRobinRrdStrategy) {
                data = queryJrb(request.getStep(),
                        request.getStart(),
                        request.getEnd(),
                        request.getSeries());
                
            } else {
                throw new RuntimeException("No appropriate RRD strategy found");
            }

            final List<Metric> metrics = new ArrayList<Metric>(data.size());
            for (final SortedMap.Entry<Long, Map<String, Double>> e : data.entrySet()) {
                final Metric metric = new Metric();
                metric.setTimestamp(e.getKey());
                metric.setValues(e.getValue());
                metrics.add(metric);
            }

            response.setMetrics(metrics);

            return Response
                    .ok(response)
                    .build();

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

    private SortedMap<Long, Map<String, Double>> queryRrd(final long step,
                                                          final long start,
                                                          final long end,
                                                          final Map<String, MetricIdentifier> series) throws Exception {
        String rrdBinary = System.getProperty("rrd.binary");

        if (rrdBinary == null) {
            throw new RrdException("rrd.binary property must be set either in opennms.properties or in iReport");
        }

        //construct the query string out of the requestedMetrics data
        final StringBuilder query = new StringBuilder();
        query.append("--step").append(" ")
                .append(step).append(" ");

        query.append("--start").append(" ")
                .append(start).append(" ");

        query.append("--end").append(" ")
                .append(end).append(" ");

        for (final Map.Entry<String, MetricIdentifier> entry : series.entrySet()) {
            final OnmsResource resource = m_resourceDao.getResourceById(entry.getValue().getResourceId());
            final RrdGraphAttribute rrdGraphAttribute = resource.getRrdGraphAttributes().get(entry.getValue().getAttributeId());

            final String rrdFile = System.getProperty("rrd.base.dir") + File.separator + rrdGraphAttribute.getRrdRelativePath();

            query.append("DEF:")
                    .append(entry.getKey())
                    .append("=")
                    .append(rrdFile)
                    .append(":")
                    .append(entry.getValue().getAttributeId())
                    .append(":")
                    .append("AVERAGE")
                    .append(" ");
        }

        StringBuilder command = new StringBuilder();
        command.append(rrdBinary).append(" ");
        command.append("xport").append(" ");
        command.append(query
                .toString()
                .replaceAll("[\r\n]+", " ")
                .replaceAll("\\s+", " "));

        String[] commandArray = StringUtils.createCommandArray(command.toString(), '@');

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

        SortedMap<Long, Map<String, Double>> results = new TreeMap<Long, Map<String, Double>>();

        /**
         * TODO: constuct the response object out of the unmarshalled xml data
         */
        return results;
    }

    private SortedMap<Long, Map<String, Double>> queryJrb(final long step,
                                                          final long start,
                                                          final long end,
                                                          final Map<String, MetricIdentifier> series) throws IOException, RrdException {

        final DataProcessor dproc = new DataProcessor(start, end);
        dproc.setStep(step);
        dproc.setFetchRequestResolution(300);

        for (final Map.Entry<String, MetricIdentifier> entry : series.entrySet()) {
            String key = entry.getKey();

            OnmsResource resource = m_resourceDao.getResourceById(entry.getValue().getResourceId());
            RrdGraphAttribute rrdGraphAttribute = resource.getRrdGraphAttributes().get(entry.getValue().getAttributeId());

            dproc.addDatasource(key, System.getProperty("rrd.base.dir") + File.separator + rrdGraphAttribute.getRrdRelativePath(), entry.getValue().getAttributeId(), "AVERAGE");
        }

        SortedMap<Long, Map<String, Double>> results = new TreeMap<Long, Map<String, Double>>();

        dproc.processData();

        long[] timestamps = dproc.getTimestamps();

        for (int i = 0; i < timestamps.length; i++) {
            timestamps[i] = timestamps[i] - dproc.getStep();

            Map<String, Double> data = new HashMap<String, Double>();
            for (String key : series.keySet()) {
                data.put(key, dproc.getValues(key)[i]);
            }

            results.put(timestamps[i], data);
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
