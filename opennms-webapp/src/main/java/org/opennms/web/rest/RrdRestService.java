package org.opennms.web.rest;

import com.sun.jersey.spi.resource.PerRequest;
import org.jrobin.data.DataProcessor;
import org.opennms.netmgt.dao.ResourceDao;
import org.opennms.netmgt.model.OnmsAttribute;
import org.opennms.netmgt.model.OnmsResource;
import org.opennms.netmgt.model.RrdGraphAttribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Map;

@Component
@PerRequest
@Scope("prototype")
@Path("rrd")
public class RrdRestService extends OnmsRestService {

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
            OnmsResource resource = m_resourceDao.getResourceById(resourceId);

            String result = "Not found";

            RrdGraphAttribute rrdGraphAttribute = resource.getRrdGraphAttributes().get(attribute);
            if (rrdGraphAttribute != null) {
                result = rrdGraphAttribute.getName() + "<br>";
                result += rrdGraphAttribute.getRrdRelativePath() + "<br>";
                result += rrdGraphAttribute.getResource().getId() + "<br>";
            }
            try {
                DataProcessor dproc = new DataProcessor(start, end);
                dproc.setStep(300);
                dproc.setFetchRequestResolution(300);
                dproc.addDatasource(attribute, "/opt/opennms/share/rrd/" + rrdGraphAttribute.getRrdRelativePath(), attribute, "AVERAGE");

                dproc.processData();

                long[] timestamps = dproc.getTimestamps();

                for (int i = 0; i < timestamps.length; i++) {
                    timestamps[i] = timestamps[i] - dproc.getStep();
                }

                double[] values = dproc.getValues(attribute);

                for (int i = 0; i < timestamps.length; i++) {
                    result += timestamps[i] + " : " + values[i] + "<br>";
                }
            } catch (org.jrobin.core.RrdException e) {
                result += e.getMessage() + "<br>";
            } catch (IOException e) {
                result += e.getMessage() + "<br>";
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
}
