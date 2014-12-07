package org.opennms.web.rest.rrd;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.opennms.web.rest.rrd.QueryResponse.Metric;

import com.google.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

public class RrdRestServiceTest {
	@Test
	public void requestWithExpressions() throws Exception {
		RrdRestService restService = new RrdRestService() {
			public SortedMap<Long, Map<String, Double>> fetchData(final QueryRequest request) throws Exception {
				SortedMap<Long, Map<String, Double>> data =  Maps.newTreeMap();
				for (long i = 1; i <= 10; i++) {
					Map<String, Double> values = Maps.newHashMap();
					values.put("x", i * 1d);
					data.put(i, values);
				}
				return data;
			}
		};

		QueryRequest queryRequest = new QueryRequest();
		queryRequest.setStart(1);
		queryRequest.setEnd(10);
		queryRequest.setStep(1);
		
		QueryRequest.Expression y = new QueryRequest.Expression();
		y.setLabel("y");
		y.setExpression("(x * 1024)");
		queryRequest.setExpressions(Lists.newArrayList(y));

		Response response = restService.query(queryRequest);
		QueryResponse queryResponse = (QueryResponse)response.getEntity();
		List<Metric> metrics = queryResponse.getMetrics();
		
		assertEquals(10, metrics.size());
		for (int i = 0; i < 10; i++) {
			Map<String, Double> values = metrics.get(i).getValues();
			assertEquals((i+1) * 1d, values.get("x"), 0.0001d);
			assertEquals(values.get("x") * 1024, values.get("y"), 0.0001d);
		}
	}
}
