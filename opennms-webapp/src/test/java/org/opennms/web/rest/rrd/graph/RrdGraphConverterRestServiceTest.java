/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2009-2012 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.web.rest.rrd.graph;

import static org.junit.Assert.assertEquals;

import java.io.File;

import net.sf.json.JSONObject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.test.rest.AbstractSpringJerseyRestTestCase;
import org.opennms.netmgt.dao.DatabasePopulator;
import org.opennms.netmgt.dao.api.ResourceDao;
import org.opennms.netmgt.dao.support.DefaultResourceDao;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(locations={
        "classpath:/org/opennms/web/rest/applicationContext-test.xml",
        "classpath:/META-INF/opennms/applicationContext-commonConfigs.xml",
        "classpath:/META-INF/opennms/applicationContext-soa.xml",
        "classpath:/META-INF/opennms/applicationContext-dao.xml",
        "classpath*:/META-INF/opennms/component-service.xml",
        "classpath*:/META-INF/opennms/component-dao.xml",
        "classpath:/META-INF/opennms/applicationContext-reportingCore.xml",
        "classpath:/META-INF/opennms/applicationContext-databasePopulator.xml",
        "classpath:/org/opennms/web/svclayer/applicationContext-svclayer.xml",
        "classpath:/META-INF/opennms/applicationContext-mockEventProxy.xml",
        "classpath:/applicationContext-jersey-test.xml",
        "classpath:/META-INF/opennms/applicationContext-reporting.xml",
        "classpath:/META-INF/opennms/applicationContext-mock-usergroup.xml",
        "classpath:/META-INF/opennms/applicationContext-minimal-conf.xml",
        "file:src/main/webapp/WEB-INF/applicationContext-spring-security.xml",
        "file:src/main/webapp/WEB-INF/applicationContext-jersey.xml"
})
@JUnitConfigurationEnvironment
public class RrdGraphConverterRestServiceTest extends AbstractSpringJerseyRestTestCase {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Override
    protected void afterServletStart() throws Exception {
        final WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(getServletContext());

        // Populate the DB with some test nodes and interfaces
        DatabasePopulator databasePopulator = context.getBean("databasePopulator", DatabasePopulator.class);
        databasePopulator.populateDatabase();

        // Use the implementation of the resourceDao so that we can explicitly set the
        // RRD root directory to a temporary folder
        DefaultResourceDao resourceDao = (DefaultResourceDao)context.getBean("resourceDao", ResourceDao.class);
        resourceDao.setRrdDirectory(testFolder.getRoot());
    }

    @Test
    public void testConvertRrdGraph() throws Exception {
        // Create the RRD directory structure and blank stores
        // referenced in the requested report
        File rrdSnmpDir = new File(testFolder.getRoot(), "snmp");
        rrdSnmpDir.mkdir();

        File rrdSnmpNodeDir = new File(rrdSnmpDir, "1");
        rrdSnmpNodeDir.mkdir();

        File sysRawContextRrd = new File(rrdSnmpNodeDir, "SysRawContext.rrd");
        sysRawContextRrd.createNewFile();

        File sysRawContextJrb = new File(rrdSnmpNodeDir, "SysRawContext.jrb");
        sysRawContextJrb.createNewFile();

        // Make the request
        String url = "/graphs/netsnmp.rawcontext/resource/node%5b1%5d.nodeSnmp%5b%5d/start/0/end/1400000000";
        String json = sendRequest(GET, url, 200);
 
        // Spot check on the returned JSON
        JSONObject container = JSONObject.fromObject(json);
        assertEquals(0, container.getLong("start"));
        assertEquals(1400000000, container.getLong("end"));

        JSONObject model = container.getJSONObject("model");
        assertEquals("onmsrrd", model.getJSONObject("dataProcessor").getString("type"));
        
        assertEquals(6, model.getJSONArray("sources").size());
        assertEquals(2, model.getJSONArray("series").size());
    }
}
