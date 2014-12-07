package org.opennms.web.rest.rrd.graph;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.opennms.netmgt.dao.api.ResourceDao;
import org.opennms.web.controller.RrdGraphController;
import org.opennms.web.rest.OnmsRestService;
import org.opennms.web.svclayer.RrdGraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sun.jersey.spi.resource.PerRequest;

@Component
@PerRequest
@Scope("prototype")
@Path("/graphs")
public class RrdGraphConverterRestService extends OnmsRestService {
	private static final Logger LOG = LoggerFactory.getLogger(RrdGraphConverterRestService.class);
	
    @Autowired
    private ResourceDao m_resourceDao;

    @Autowired
    private RrdGraphService m_rrdGraphService;

    @Context
    UriInfo m_uriInfo;

    private NGGraphModelBuilder m_graphBuilder;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{report}/resource/{resource}")
    public NGGraphContainer getGraph(@PathParam("report") final String report,
            @PathParam("resource") final String resource) {
        return getGraph(report, resource, null, null);
    }

    /**
     * TODO: query params are broken, so I use path params instead.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{report}/resource/{resource}/start/{start}/end/{end}")
    public NGGraphContainer getGraph(@PathParam("report") final String report,
            @PathParam("resource") final String resource,
            @PathParam("start") final String start,
            @PathParam("end") final String end) {
        if (m_graphBuilder == null) {
            try {
                m_graphBuilder = new NGGraphModelBuilder();
            } catch (Exception e) {
            	LOG.error("Failed to create the graph builder.", e);
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }
        }

        long times[] = RrdGraphController.parseTimes(start, end);
        long startTime = times[0];
        long endTime = times[1];

        String rrdGraphCommand = m_rrdGraphService.getPrefabGraphCommand(resource,
                                                                         report, startTime, endTime,
                                                                         null, null);

        NGGraphContainer graph = new NGGraphContainer();
        
        graph.setStart(times[0]);
        graph.setEnd(times[1]);

        NGGraphModel ngGraphModel;
        try {
            ngGraphModel = m_graphBuilder.createNGGraph(rrdGraphCommand, m_resourceDao.getRrdDirectory(true));
        } catch (org.jrobin.core.RrdException e) {
        	LOG.error("Failed to create the graph.", e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        graph.setModel(ngGraphModel);

        return graph;
    }
}
