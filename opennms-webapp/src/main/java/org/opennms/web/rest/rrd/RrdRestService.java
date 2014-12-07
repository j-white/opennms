package org.opennms.web.rest.rrd;

import com.sun.jersey.spi.resource.PerRequest;

import org.apache.commons.jexl2.*;
import org.exolab.castor.xml.Unmarshaller;
import org.jrobin.core.RrdException;
import org.jrobin.data.DataProcessor;
import org.opennms.core.utils.StringUtils;
import org.opennms.netmgt.dao.api.ResourceDao;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	
	private static final Logger LOG = LoggerFactory.getLogger(RrdRestService.class);

    @Autowired
    private ResourceDao m_resourceDao;

    @POST
    @Path("/")
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    public Response query(final QueryRequest request) {
        readLock();
        try {
            // Compile the expressions
            final JexlEngine jexl = new JexlEngine();
            final LinkedHashMap<String, Expression> expressions = new LinkedHashMap<String, Expression>();
            for (final QueryRequest.Expression e : request.getExpressions()) {
                expressions.put(e.getLabel(),
                                jexl.createExpression(e.getExpression()));
            }

            // Prepare the response
            final QueryResponse response = new QueryResponse();
            response.setStep(request.getStep());
            response.setStart(request.getStart());
            response.setEnd(request.getEnd());

            // Fetch the data
            SortedMap<Long, Map<String, Double>> data;
			try {
				data = fetchData(request);
			} catch (Exception e) {
				LOG.error("An error occured while retrieve the RRD data for {}",
						request, e);
				throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
			}

            // Do the calculations and build the list of resulting metrics
            final List<QueryResponse.Metric> metrics = new ArrayList<QueryResponse.Metric>(data.size());
            for (final SortedMap.Entry<Long, Map<String, Double>> dataEntry : data.entrySet()) {
            	long timestamp = dataEntry.getKey();
                Map<String, Double> values = dataEntry.getValue();

                for (final Map.Entry<String, Expression> expressionEntry : expressions.entrySet()) {
                    Map<String, Object> jexlValues = new HashMap<String, Object>(values);
                    jexlValues.put("__inf", Double.POSITIVE_INFINITY);
                    jexlValues.put("__neg_inf", Double.NEGATIVE_INFINITY);
                    final JexlContext context = new MapContext(jexlValues);

                    values.put(expressionEntry.getKey(),
                               (Double) expressionEntry.getValue().evaluate(context));
                }

                final QueryResponse.Metric metric = new QueryResponse.Metric();
                metric.setTimestamp(timestamp);
                metric.setValues(values);
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

            RrdStrategy<?, ?> rrdStrategy = findStrategy(RrdUtils.getStrategy());

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

    public SortedMap<Long, Map<String, Double>> fetchData(final QueryRequest request) throws Exception {
    	if (findStrategy() instanceof JniRrdStrategy) {
            return queryRrd(request.getStep(),
                            request.getStart(),
                            request.getEnd(),
                            request.getSources());

        } else if (findStrategy() instanceof JRobinRrdStrategy) {
            return queryJrb(request.getStep(),
                            request.getStart(),
                            request.getEnd(),
                            request.getSources());
            
        } else {
            throw new RuntimeException("No appropriate RRD strategy found");
        }
    }

    private SortedMap<Long, Map<String, Double>> queryRrd(final long step,
                                                          final long start,
                                                          final long end,
                                                          final List<QueryRequest.Source> sources) throws Exception {
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

        for (final QueryRequest.Source source : sources) {
            final OnmsResource resource = m_resourceDao.getResourceById(source.getResource());
            final RrdGraphAttribute rrdGraphAttribute = resource.getRrdGraphAttributes().get(source.getAttribute());

            final String rrdFile = System.getProperty("rrd.base.dir") + File.separator + rrdGraphAttribute.getRrdRelativePath();

            query.append("DEF:")
                    .append(source.getLabel())
                    .append("=")
                    .append(rrdFile)
                    .append(":")
                    .append(source.getAttribute())
                    .append(":")
                    .append(source.getAggregation())
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
                                                          final List<QueryRequest.Source> sources) throws IOException, RrdException {

        final DataProcessor dproc = new DataProcessor(start, end);
        dproc.setStep(step);
        dproc.setFetchRequestResolution(300);

        for (final QueryRequest.Source source : sources) {
            OnmsResource resource = m_resourceDao.getResourceById(source.getResource());
            RrdGraphAttribute rrdGraphAttribute = resource.getRrdGraphAttributes().get(source.getAttribute());

            final String file = System.getProperty("rrd.base.dir") + File.separator + rrdGraphAttribute.getRrdRelativePath();

            dproc.addDatasource(source.getLabel(),
                                file,
                                source.getAttribute(),
                                source.getAggregation());
        }

        SortedMap<Long, Map<String, Double>> results = new TreeMap<Long, Map<String, Double>>();

        dproc.processData();

        long[] timestamps = dproc.getTimestamps();

        for (int i = 0; i < timestamps.length; i++) {
            final long timestamp = timestamps[i] - dproc.getStep();

            Map<String, Double> data = new HashMap<String, Double>();
            for (QueryRequest.Source source : sources) {
                data.put(source.getLabel(), dproc.getValues(source.getLabel())[i]);
            }

            results.put(timestamp, data);
        }

        return results;
    }

    private static RrdStrategy<?, ?> findStrategy() {
        return findStrategy(RrdUtils.getStrategy());
    }

    private static RrdStrategy<?, ?> findStrategy(final RrdStrategy<?, ?> rrdStrategy) {
        if (rrdStrategy instanceof JniRrdStrategy || rrdStrategy instanceof JRobinRrdStrategy) {
            return rrdStrategy;
        }

        if (rrdStrategy instanceof QueuingRrdStrategy) {
            return findStrategy(((QueuingRrdStrategy) rrdStrategy).getDelegate());
        }

        if (rrdStrategy instanceof MultiOutputRrdStrategy) {
            for (final RrdStrategy delegate : ((MultiOutputRrdStrategy) rrdStrategy).getDelegates()) {
                RrdStrategy<?, ?> x = findStrategy(delegate);

                if (x instanceof JniRrdStrategy || x instanceof JRobinRrdStrategy) {
                    return x;
                }
            }
        }

        return rrdStrategy;
    }
}
