//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2002-2003 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified 
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Modifications:
//
// 2008 Jan 26: Dependency inject the last of the objects.  Move local
//              host address code into DaemonUtils.  Reorganize so that
//              Ron* methods match the lifecycle ordering. - dj@opennms.org
// 2008 Jan 26: Move m_serviceTableMap into JdbcEventdServiceManager. - dj@opennms.org
// 2008 Jan 26: Get rid of the Eventd singleton and getInstance. - dj@opennms.org
// 2008 Jan 17: Use JdbcTemplate for getting service name -> ID mapping. - dj@opennms.org
// 2008 Jan 06: Dependency injection of EventConfDao, delay creation of
//              BroadcastEventProcessor until onInit instead of in
//              setEventIpcManager, and pass in EventConfDao. - dj@opennms.org
// 2008 Jan 06: Indent, format code, Java 5 generics, eliminate warnings,
//              use log() from superclass. - dj@opennms.org
// 2003 Jan 31: Cleaned up some unused imports.
// 2002 Nov 14: Used non-blocking socket class to speed up capsd and pollerd.
//
// Original code base Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.                                                            
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//       
// For more information contact: 
//      OpenNMS Licensing       <license@opennms.org>
//      http://www.opennms.org/
//      http://www.opennms.com/
//
package org.opennms.netmgt.eventd;


import org.opennms.netmgt.daemon.AbstractServiceDaemon;
import org.opennms.netmgt.eventd.adaptors.EventReceiver;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.xml.event.EventReceipt;
import org.springframework.util.Assert;

/**
 * <p>
 * Eventd listens for events from the discovery, capsd, trapd processes and
 * sends events to the Master Station when queried for.
 * </p>
 * 
 * <p>
 * Eventd receives events sent in as XML, looks up the event.conf and adds
 * information to these events and stores them to the db. It also reconverts
 * them back to XML to be sent to other processes like 'actiond'
 * </p>
 * 
 * <p>
 * Process like trapd, capsd etc. that are local to the distributed poller send
 * events to the eventd. Events can also be sent via TCP or UDP to eventd.
 * </p>
 * 
 * <p>
 * Eventd listens for incoming events, loads info from the 'event.conf', adds
 * events to the database and sends the events added to the database to
 * subscribed listeners. It also maintains a servicename to serviceid mapping
 * from the services table so as to prevent a database lookup for each incoming
 * event
 * </P>
 * 
 * <P>
 * The number of threads that processes events is configurable via the eventd
 * configuration xml
 * </P>
 * 
 * @author <A HREF="mailto:sowmya@opennms.org">Sowmya Nataraj </A>
 * @author <A HREF="http://www.opennms.org">OpenNMS.org </A>
 */
public final class Eventd extends AbstractServiceDaemon implements org.opennms.netmgt.eventd.adaptors.EventHandler {
    /**
     * Our fearless IPC manager.
     */
    private EventIpcManager m_eventIpcManager;
    
    /**
     * The log4j category used to log debug messsages and statements.
     */
    public static final String LOG4J_CATEGORY = "OpenNMS.Eventd";

    /**
     * The handler for events coming in through TCP
     */
    private EventReceiver m_tcpReceiver;

    /**
     * The handler for events coming in through UDP
     */
    private EventReceiver m_udpReceiver;

    /**
     * Reference to the event processor
     */
    private BroadcastEventProcessor m_receiver;

    /**
     * Contains dotted-decimal representation of the IP address where Eventd is
     * running. Used when eventd broadcasts events.
     */
    private String m_localHostAddress;

    /**
     * Class that handles mapping of service names to service IDs.
     */
    private EventdServiceManager m_eventdServiceManager;
    
    /**
     * Constuctor creates the localhost address(to be used eventually when
     * eventd originates events during correlation) and the broadcast queue
     */
    public Eventd() {
        super("OpenNMS.Eventd");
    }

    protected void onInit() {
        Assert.state(m_eventdServiceManager != null, "property eventdServiceManager must be set");
        Assert.state(m_tcpReceiver != null, "property tcpReceiver must be set");
        Assert.state(m_udpReceiver != null, "property udpReceiver must be set");
        Assert.state(m_receiver != null, "property receiver must be set");
        Assert.state(m_localHostAddress != null, "property localHostAddress must be set");
        
        m_eventdServiceManager.dataSourceSync();

        m_tcpReceiver.addEventHandler(this);
        m_udpReceiver.addEventHandler(this);
    }

    protected void onStart() {
        m_tcpReceiver.start();
        m_udpReceiver.start();

        if (log().isDebugEnabled()) {
            log().debug("Listener threads started");
        }

        if (log().isDebugEnabled()) {
            log().debug("Eventd running");
        }
    }

    protected void onStop() {
        // Stop listener threads
        if (log().isDebugEnabled()) {
            log().debug("calling shutdown on tcp/udp listener threads");
        }

        if (m_tcpReceiver != null) {
            m_tcpReceiver.stop();
        }

        if (m_udpReceiver != null) {
            m_udpReceiver.stop();
        }

        if (m_receiver != null) {
            m_receiver.close();
        }

        if (log().isDebugEnabled()) {
            log().debug("shutdown on tcp/udp listener threads returned");
        }
    }

    public boolean processEvent(Event event) {
        m_eventIpcManager.sendNow(event);
        return true;
    }

    public void receiptSent(EventReceipt event) {
        // do nothing
    }

    public EventIpcManager getEventIpcManager() {
        return m_eventIpcManager;
    }

    public void setEventIpcManager(EventIpcManager manager) {
        m_eventIpcManager = manager;
    }

    public EventdServiceManager getEventdServiceManager() {
        return m_eventdServiceManager;
    }

    public void setEventdServiceManager(EventdServiceManager eventdServiceManager) {
        m_eventdServiceManager = eventdServiceManager;
    }

    public EventReceiver getTcpReceiver() {
        return m_tcpReceiver;
    }

    public void setTcpReceiver(EventReceiver tcpReceiver) {
        m_tcpReceiver = tcpReceiver;
    }

    public EventReceiver getUdpReceiver() {
        return m_udpReceiver;
    }

    public void setUdpReceiver(EventReceiver udpReceiver) {
        m_udpReceiver = udpReceiver;
    }

    public BroadcastEventProcessor getReceiver() {
        return m_receiver;
    }

    public void setReceiver(BroadcastEventProcessor receiver) {
        m_receiver = receiver;
    }

    /**
     * Used to retrieve the local host address. The address of the machine on
     * which Eventd is running.
     * 
     * @return The local machines hostname.
     */
    public String getLocalHostAddress() {
        return m_localHostAddress;
    }

    public void setLocalHostAddress(String localHostAddress) {
        m_localHostAddress = localHostAddress;
    }
}
