/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2010-2015 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2015 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.web.rest.measurements.fetch;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.jrobin.core.RrdException;
import org.jrobin.data.DataProcessor;
import org.opennms.netmgt.dao.api.ResourceDao;
import org.opennms.web.rest.measurements.model.Measurement;
import org.opennms.web.rest.measurements.model.Source;

import com.google.common.collect.Maps;

/**
 * Used to fetch measurements from JRB files.
 *
 * @author Jesse White <jesse@opennms.org>
 * @author Dustin Frisch <fooker@lab.sh>
 */
public class JrbFetchStrategy extends AbstractRrdBasedFetchStrategy {

    public JrbFetchStrategy(final ResourceDao resourceDao) {
        super(resourceDao);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected long fetchMeasurements(long start, long end, long step, int maxrows,
            Map<Source, String> rrdsBySource,
            List<Measurement> measurements) throws RrdException {

        final long startInSeconds = (long) Math.floor(start / 1000);
        final long endInSeconds = (long) Math.floor(end / 1000);

        long stepInSeconds = (long) Math.floor(step / 1000);
        // The step must be strictly positive
        if (stepInSeconds <= 0) {
            stepInSeconds = 1;
        }

        final DataProcessor dproc = new DataProcessor(startInSeconds, endInSeconds);
        if (maxrows > 0) {
            dproc.setPixelCount(maxrows);
        }
        dproc.setFetchRequestResolution(stepInSeconds);

        for (final Map.Entry<Source, String> entry : rrdsBySource.entrySet()) {
            final Source source = entry.getKey();
            final String rrdFile = entry.getValue();

            dproc.addDatasource(source.getLabel(), rrdFile, source.getAttribute(),
                    source.getAggregation());
        }

        try {
            dproc.processData();
        } catch (IOException e) {
            throw new RrdException("JRB processing failed.", e);
        }

        final long[] timestamps = dproc.getTimestamps();

        for (int i = 0; i < timestamps.length; i++) {
            final long timestampInSeconds = timestamps[i] - dproc.getStep();

            final Map<String, Double> values = Maps.newHashMap();
            for (Source source : rrdsBySource.keySet()) {
                values.put(source.getLabel(),
                        dproc.getValues(source.getLabel())[i]);
            }

            measurements.add(new Measurement(timestampInSeconds * 1000, values));
        }

        // Actual step size
        return dproc.getStep() * 1000;
    }
}
