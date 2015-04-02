package org.opennms.tools;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.opennms.netmgt.config.api.SnmpAgentConfigFactory;
import org.opennms.netmgt.provision.ServiceDetector;
import org.opennms.netmgt.provision.detector.icmp.IcmpDetector;
import org.opennms.netmgt.provision.detector.snmp.SnmpDetector;
import org.opennms.netmgt.provision.support.SyncAbstractDetector;
import org.opennms.netmgt.snmp.SnmpAgentConfig;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpStrategy;
import org.opennms.netmgt.snmp.SnmpWalker;
import org.opennms.netmgt.snmp.TableTracker;
import org.opennms.netmgt.snmp.snmp4j.Snmp4JStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.TransportMapping;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class FreebsdJvmCrasher {
    
    private static final Logger LOG = LoggerFactory.getLogger(FreebsdJvmCrasher.class);

    private static final int NUM_THREADS = 512;

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
        private final List<SyncAbstractDetector> detectors;

        public DetectTask(int id, List<SyncAbstractDetector> detectors) throws UnknownHostException {
            this.id = id;
            this.detectors = detectors;
        }

        @Override
        public void run() {
            for (final SyncAbstractDetector detector : detectors) {
                boolean detected;
                try {
                    detected = detector.isServiceDetected(InetAddress.getByName(String.format("127.0.0.%d", id % 255 + 1)));
                } catch (UnknownHostException e) {
                    detected = false;
                }
                LOG.info("[{}] - {}: {}", id, detector, detected);
            }
        }
    };

    private List<SyncAbstractDetector> getDetectors() {
        final List<SyncAbstractDetector> detectors = new LinkedList<SyncAbstractDetector>();
        
        final IcmpDetector icmpDetector = new IcmpDetector();
        detectors.add(icmpDetector);

        final SnmpAgentConfigFactory snmpAgentConfigFactory = new SnmpAgentConfigFactory() {
            @Override
            public SnmpAgentConfig getAgentConfig(InetAddress address) {
                return new SnmpAgentConfig(address);
            }
        };
        final SnmpDetector snmpDetector = new SnmpDetector();
        snmpDetector.setAgentConfigFactory(snmpAgentConfigFactory);
        detectors.add(snmpDetector);
        
        return detectors;
    }

    private void run() throws IOException, InterruptedException {
        while(true) {
            final SnmpStrategy snmpStrategy = new Snmp4JStrategy();
            final List<SyncAbstractDetector> detectors = getDetectors(); 

            final List<Thread> threads = new LinkedList<Thread>();
            LOG.info("Spawning {} threads", NUM_THREADS);
            for (int k = 0; k < NUM_THREADS; k++) {
                final DetectTask task = new DetectTask(k, detectors);
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

    public static void main(final String[] args) throws Exception {
        new FreebsdJvmCrasher().run();
    }
}
