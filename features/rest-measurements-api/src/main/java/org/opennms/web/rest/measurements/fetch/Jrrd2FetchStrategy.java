package org.opennms.web.rest.measurements.fetch;

import java.util.List;
import java.util.Map;

import org.jrobin.core.RrdException;
import org.opennms.netmgt.dao.api.ResourceDao;
import org.opennms.netmgt.rrd.jrrd2.JRrd2;
import org.opennms.netmgt.rrd.jrrd2.JniRrdException;
import org.opennms.web.rest.measurements.model.Source;

import com.google.common.collect.Lists;

public class Jrrd2FetchStrategy extends AbstractRrdBasedFetchStrategy {
    private final JRrd2 jrrd2 = new JRrd2();

    public Jrrd2FetchStrategy(ResourceDao resourceDao) {
        super(resourceDao);
    }

    @Override
    protected FetchResults fetchMeasurements(long start, long end, long step,
            int maxrows, Map<Source, String> rrdsBySource,
            Map<String, Object> constants) throws RrdException {

        final long startInSeconds = (long) Math.floor(start / 1000);
        final long endInSeconds = (long) Math.floor(end / 1000);

        long stepInSeconds = (long) Math.floor(step / 1000);
        // The step must be strictly positive
        if (stepInSeconds <= 0) {
            stepInSeconds = 1;
        }

        List<String> argv = Lists.newLinkedList();
        for (final Map.Entry<Source, String> entry : rrdsBySource.entrySet()) {
            final Source source = entry.getKey();
            final String rrdFile = entry.getValue();

            argv.add(String.format("DEF:%s=%s:%s:%s",
                    source.getLabel(), rrdFile, source.getAttribute(),
                    source.getAggregation()));
            argv.add(String.format("XPORT:%s:%s", source.getLabel(),
                    source.getLabel()));
        }

        org.opennms.netmgt.rrd.jrrd2.FetchResults xportResults;
        try {
            xportResults = jrrd2.xport(startInSeconds, endInSeconds, stepInSeconds, maxrows, argv.toArray(new String[argv.size()]));
        } catch (JniRrdException e) {
            throw new RrdException("Xport failed.", e);
        }

        // Convert to ms
        final long[] timestamps = xportResults.getTimestamps();
        for (int i = 0; i < timestamps.length; i++) {
            timestamps[i] *= 1000;
        }

        return new FetchResults(timestamps, xportResults.getColumnsWithValues(), xportResults.getStep() * 1000, constants);
    }
}
