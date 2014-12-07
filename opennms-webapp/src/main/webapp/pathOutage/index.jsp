<%--
/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2006-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
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

--%>

<%@page language="java" contentType="text/html" session="true"
	import="
		java.util.List,
		org.opennms.netmgt.config.OpennmsServerConfigFactory,
		org.opennms.netmgt.poller.PathOutageManagerDaoImpl
	"
%>

<jsp:include page="/includes/header.jsp" flush="false">
  <jsp:param name="title" value="Path Outages" />
  <jsp:param name="headTitle" value="Path Outages" />
  <jsp:param name="location" value="pathOutage" />
  <jsp:param name="breadcrumb" value="Path Outages" />
</jsp:include>

<% OpennmsServerConfigFactory.init(); %>

<%
        List<String[]> testPaths = PathOutageManagerDaoImpl.getInstance().getAllCriticalPaths();
        String dcpip = OpennmsServerConfigFactory.getInstance().getDefaultCriticalPathIp();
        String[] pthData = PathOutageManagerDaoImpl.getInstance().getCriticalPathData(dcpip, "ICMP");
%>
<% if (dcpip != null && !"".equals(dcpip)) { %>
	<p>The default critical path is service ICMP on interface <%= dcpip %>.</p>
<% } %>
	<h3>All Path Outages</h3>
	<table>
		<tr>
			<th>Critical Path Node</th>
			<th>Critical Path IP</th>
			<th>Critical Path Service</th>
			<th>Number of Nodes</th>
		</tr>
		<% for (String[] pth : testPaths) {
			pthData = PathOutageManagerDaoImpl.getInstance().getCriticalPathData(pth[1], pth[2]); %>
			<tr class="CellStatus">
				<% if((pthData[0] == null) || (pthData[0].equals(""))) { %>
					<td>(Interface not in database)</td>
				<% } else if (pthData[0].indexOf("nodes have this IP") > -1) { %>
					<td><a href="element/nodeList.htm?iplike=<%= pth[1] %>"><%= pthData[0] %></a></td>
				<% } else { %>
					<td><a href="element/node.jsp?node=<%= pthData[1] %>"><%= pthData[0] %></a></td>
				<% } %>
				<td><%= pth[1] %></td>
				<td class="<%= pthData[3] %>"><%= pth[2] %></td>
				<td><a href="pathOutage/showNodes.jsp?critIp=<%= pth[1] %>&critSvc=<%= pth[2] %>"><%= pthData[2] %></a></td>
			</tr>
		<% } %>
</table>

<jsp:include page="/includes/footer.jsp" flush="false" />
