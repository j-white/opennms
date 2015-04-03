package org.opennms.tools;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.opennms.netmgt.config.SnmpPeerFactory;
import org.opennms.netmgt.config.api.SnmpAgentConfigFactory;
import org.opennms.netmgt.model.OnmsIpInterface;
import org.opennms.netmgt.poller.MonitoredService;
import org.opennms.netmgt.poller.NetworkInterface;
import org.opennms.netmgt.poller.PollStatus;
import org.opennms.netmgt.poller.monitors.SnmpMonitor;
import org.opennms.netmgt.provision.AsyncServiceDetector;
import org.opennms.netmgt.provision.DetectFuture;
import org.opennms.netmgt.provision.ServiceDetector;
import org.opennms.netmgt.provision.SyncServiceDetector;
import org.opennms.netmgt.provision.detector.datagram.DnsDetector;
import org.opennms.netmgt.provision.detector.icmp.IcmpDetector;
import org.opennms.netmgt.provision.detector.simple.HttpDetector;
import org.opennms.netmgt.provision.detector.snmp.SnmpDetector;
import org.opennms.netmgt.provision.service.IPAddressTableTracker;
import org.opennms.netmgt.provision.service.IPInterfaceTableTracker;
import org.opennms.netmgt.provision.service.PhysInterfaceTableTracker;
import org.opennms.netmgt.provision.service.Provisioner;
import org.opennms.netmgt.provision.support.ConnectionFactoryNewConnectorImpl;
import org.opennms.netmgt.provision.support.SyncAbstractDetector;
import org.opennms.netmgt.snmp.SnmpAgentConfig;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpStrategy;
import org.opennms.netmgt.snmp.SnmpUtils;
import org.opennms.netmgt.snmp.SnmpWalker;
import org.opennms.netmgt.snmp.TableTracker;
import org.opennms.netmgt.snmp.snmp4j.Snmp4JStrategy;
import org.opennms.netmgt.xml.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.TransportMapping;
import org.snmp4j.log.LogFactory;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class FreebsdJvmCrasher {
    
    private static final Logger LOG = LoggerFactory.getLogger(FreebsdJvmCrasher.class);

    private static final int NUM_THREADS = 32;

    private static class SnmpTask implements Runnable {
        private final int id;
        private final SnmpStrategy snmpStrategy;
        private final List<SnmpAgentConfig> agentConfigs;

        public SnmpTask(int id, SnmpStrategy snmpStrategy) throws UnknownHostException {
            this.id = id;
            this.snmpStrategy = snmpStrategy;
            agentConfigs = buildAgentConfigs();
        }

        private List<SnmpAgentConfig> buildAgentConfigs() throws UnknownHostException {
            final List<SnmpAgentConfig> agentConfigs = new ArrayList<SnmpAgentConfig>();
            for (int offset = 0; offset < 10; offset++) {
                final SnmpAgentConfig agentConfig = new SnmpAgentConfig(InetAddress.getByName("127.0.0.1"));
                agentConfig.setPort(161 + offset);
                agentConfig.setTimeout((id * 10000) % 15000);
                agentConfigs.add(agentConfig);
            }
            return agentConfigs;
        }

        @Override
        public void run() {
            TransportMapping<UdpAddress> transport = null;
            try {
                final UdpAddress udpAddress = new UdpAddress(10000 + id);
                transport = new DefaultUdpTransportMapping(udpAddress);
                transport.listen();

                final SnmpAgentConfig agentConfig = agentConfigs.get(id % agentConfigs.size());
                final TableTracker tracker = new TableTracker(SnmpObjId.get(".1.3.6.1.2.1.1"));
                final SnmpWalker snmpWalker = snmpStrategy.createWalker(agentConfig, "localhost", tracker);
                snmpWalker.start();
                snmpWalker.waitFor();
                snmpWalker.close();
                if (snmpWalker.failed()) {
                    LOG.warn("[{}] Failed:{}", id, snmpWalker.getErrorMessage());
                } else {
                    LOG.info("[{}] Succeeded!", id);
                }
            } catch (Throwable t) {
                LOG.warn("[{}] Walk failed.", id, t);
            } finally {
                if (transport != null) {
                    try {
                        transport.close();
                    } catch (IOException e) {
                        // pass
                    }
                }
            }
        }
    };

    private static class DetectTask implements Runnable {
        private final int id;
        private final List<ServiceDetector> detectors;

        public DetectTask(int id, List<ServiceDetector> detectors) throws UnknownHostException {
            this.id = id;
            this.detectors = detectors;
        }

        @Override
        public void run() {
            for (final ServiceDetector detector : detectors) {
                boolean detected;
                
                if (detector instanceof AsyncServiceDetector) {
                    AsyncServiceDetector asyncDetector = (AsyncServiceDetector)detector;
                    try {
                        DetectFuture detectFuture = asyncDetector.isServiceDetected(InetAddress.getByName(String.format("127.0.0.%d", id % 255 + 1)));
                        detectFuture.awaitFor();
                        detected = detectFuture.isServiceDetected();
                    } catch (UnknownHostException | InterruptedException e) {
                        detected = false;
                    }
                } else {
                    SyncServiceDetector syncDetector = (SyncServiceDetector)detector;
                    try {
                        detected = syncDetector.isServiceDetected(InetAddress.getByName(String.format("192.168.1.%d", id % 255 + 1)));
                    } catch (UnknownHostException e) {
                        detected = false;
                    }
                }

                LOG.info("[{}] - {}: {}", id, detector, detected);
            }
        }
    };

    private static final SnmpAgentConfigFactory snmpAgentConfigFactory = new SnmpAgentConfigFactory() {
        @Override
        public SnmpAgentConfig getAgentConfig(InetAddress address) {
            final SnmpAgentConfig agent = new SnmpAgentConfig(address);
            agent.setTimeout(1800);
            agent.setVersion(2);
            return agent;
        }
    };
    
    private List<ServiceDetector> getDetectors() {
        final List<ServiceDetector> detectors = new LinkedList<ServiceDetector>();
        
        final IcmpDetector icmpDetector = new IcmpDetector();
        detectors.add(icmpDetector);

        final SnmpDetector snmpDetector = new SnmpDetector();
        snmpDetector.setAgentConfigFactory(snmpAgentConfigFactory);
        detectors.add(snmpDetector);

        detectors.add(new DnsDetector());

        //detectors.add(new HttpDetector());

        return detectors;
    }

    private void run() throws IOException, InterruptedException {
        while(true) {
            final SnmpStrategy snmpStrategy = new Snmp4JStrategy();
            final List<ServiceDetector> detectors = getDetectors();

            final List<Thread> threads = new LinkedList<Thread>();
            LOG.info("Spawning {} threads", NUM_THREADS);
            for (int k = 0; k < NUM_THREADS; k++) {
                final DetectTask task = new DetectTask(k, detectors);
                final Thread thread = new Thread(task);
                threads.add(thread);
                thread.start();
            }

            for (int k = 0; k < NUM_THREADS; k++) {
                final SnmpTask task = new SnmpTask(k, snmpStrategy);
                final Thread thread = new Thread(task);
                threads.add(thread);
                thread.start();
            }
            
            LOG.info("Done spawning theads. Waiting for completion...");
            for (final Thread thread : threads) {
                thread.join();
            }
            LOG.info("Done.");
        }
    }
    
    private static class MySnmpPeerFactory extends SnmpPeerFactory {
        public MySnmpPeerFactory() {
            super(new ClassPathResource("/snmp-config.xml"));
        }

        @Override
        public SnmpAgentConfig getAgentConfig(final InetAddress address) {
            return new SnmpAgentConfig(address);
        }
    }

    private void runTargeted() throws IOException {
        SnmpPeerFactory.setInstance(new MySnmpPeerFactory());

        MonitoredService svc = new MonitoredService() {

            @Override
            public String getSvcUrl() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getSvcName() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getIpAddr() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int getNodeId() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public String getNodeLabel() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public NetworkInterface<InetAddress> getNetInterface() {
                return new NetworkInterface<InetAddress>() {

                    @Override
                    public int getType() {
                        // TODO Auto-generated method stub
                        return 0;
                    }

                    @Override
                    public InetAddress getAddress() {
                        return InetAddress.getLoopbackAddress();
                    }

                    @Override
                    public <V> V getAttribute(String property) {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public Object setAttribute(String property, Object value) {
                        // TODO Auto-generated method stub
                        return null;
                    }
                };
            }

            @Override
            public InetAddress getAddress() {
                // TODO Auto-generated method stub
                return null;
            }
            
        };
        Map<String, Object> params = new HashMap<String, Object>();
        SnmpMonitor snmpMonitor = new SnmpMonitor();
        PollStatus pollStatus = snmpMonitor.poll(svc, params);
        LOG.info("Poll status: {}", pollStatus);
    }
    
    private static class WalkTask implements Runnable {
        private final InetAddress ipAddr;
        
        public WalkTask(InetAddress ipAddr) {
            this.ipAddr = ipAddr;
        }

        @Override
        public void run() {
            while(true) {
                LOG.info("Running walk on {}", ipAddr);
                try {
                    doWalk();
                } catch (InterruptedException e) {
                    LOG.warn("Interrupted.");
                    break;
                }
            }
        }

        private void doWalk() throws InterruptedException {
            final SnmpAgentConfig agentConfig = snmpAgentConfigFactory.getAgentConfig(ipAddr);
            final IPInterfaceTableTracker ipIntTracker = new IPInterfaceTableTracker();
            
            SnmpWalker walker = SnmpUtils.createWalker(agentConfig, "IP address tables", ipIntTracker);
            walker.start();
            
            try {
                walker.waitFor();

                if (walker.timedOut()) {
                    LOG.error("Aborting node scan : Agent timed out while scanning the IP address tables");
                }
                else if (walker.failed()) {
                    LOG.error("Aborting node scan : Agent failed while scanning the IP address tables : " + walker.getErrorMessage());
                } else {
                    LOG.info("Success!");

                }
            } catch (final InterruptedException e) {
                LOG.error("Aborting node scan : Scan thread failed while waiting for the IP address tables");
                throw e;
            }

            final PhysInterfaceTableTracker physIntTracker = new PhysInterfaceTableTracker();
            walker = SnmpUtils.createWalker(agentConfig, "ifTable/ifXTable", physIntTracker);
            walker.start();
            
            try {
                walker.waitFor();

                if (walker.timedOut()) {
                    LOG.error("Aborting node scan : Agent timed out while scanning the IP address tables");
                }
                else if (walker.failed()) {
                    LOG.error("Aborting node scan : Agent failed while scanning the IP address tables : " + walker.getErrorMessage());
                } else {
                    LOG.info("Success!");

                }
            } catch (final InterruptedException e) {
                LOG.error("Aborting node scan : Scan thread failed while waiting for the IP address tables");
                throw e;
            }

            final IPAddressTableTracker ipAddressTracker = new IPAddressTableTracker();
            walker = SnmpUtils.createWalker(agentConfig, "ipaddress", ipAddressTracker);
            walker.start();
            
            try {
                walker.waitFor();

                if (walker.timedOut()) {
                    LOG.error("Aborting node scan : Agent timed out while scanning the IP address tables");
                }
                else if (walker.failed()) {
                    LOG.error("Aborting node scan : Agent failed while scanning the IP address tables : " + walker.getErrorMessage());
                } else {
                    LOG.info("Success!");

                }
            } catch (final InterruptedException e) {
                LOG.error("Aborting node scan : Scan thread failed while waiting for the IP address tables");
                throw e;
            }
        }
    }
    
    public void runWalkers() throws UnknownHostException {
        final List<InetAddress> ipAddrs = new ArrayList<InetAddress>();
        ipAddrs.add(InetAddress.getByName("127.0.0.1"));
        ipAddrs.add(InetAddress.getByName("104.236.112.50"));

        final List<Thread> threads = new LinkedList<Thread>();
        LOG.info("Spawning {} threads", NUM_THREADS);
        for (int k = 0; k < NUM_THREADS; k++) {
            final WalkTask task = new WalkTask(ipAddrs.get(k % ipAddrs.size()));
            final Thread thread = new Thread(task);
            thread.setName(String.format("Walker-%d", k));
            threads.add(thread);
            thread.start();
        }
    }

    public void runProvisioner() throws Exception {
        MyProvisioner myProvisioner = new MyProvisioner();
        Provisioner provisioner = myProvisioner.createAndStart();

        while(true) {
            for (int i = 0; i < 10; i++) {
                Event e = new Event();
                e.setNodeid(Long.valueOf(i));
                provisioner.handleForceRescan(e);
            }
            Thread.sleep(15000);
        }
    }

    public static void main(final String[] args) throws Exception {
        System.setProperty(LogFactory.SNMP4J_LOG_FACTORY_SYSTEM_PROPERTY, "org.snmp4j.log.Log4jLogFactory");
        new FreebsdJvmCrasher().runProvisioner();
    }
}
