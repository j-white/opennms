package org.opennms.tools;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.opennms.netmgt.snmp.SnmpAgentConfig;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpStrategy;
import org.opennms.netmgt.snmp.SnmpWalker;
import org.opennms.netmgt.snmp.TableTracker;
import org.opennms.netmgt.snmp.snmp4j.Snmp4JStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FreebsdJvmCrasher {
    
    private static final Logger LOG = LoggerFactory.getLogger(FreebsdJvmCrasher.class);

    private static final int NUM_THREADS = 1024;

    private static class Task implements Runnable {
        private final int id;
        private final SnmpStrategy snmpStrategy;
        private final List<SnmpAgentConfig> agentConfigs;

        public Task(int id, SnmpStrategy snmpStrategy) throws UnknownHostException {
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
            try {
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
            }
        }
    };

    private void run() throws IOException, InterruptedException {
        final SnmpStrategy snmpStrategy = new Snmp4JStrategy();
        
        final List<Thread> threads = new LinkedList<Thread>();
        LOG.info("Spawning {} threads", NUM_THREADS);
        for (int k = 0; k < NUM_THREADS; k++) {
            final Task task = new Task(k, snmpStrategy);
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

    public static void main(final String[] args) throws Exception {
        new FreebsdJvmCrasher().run();
    }
}
