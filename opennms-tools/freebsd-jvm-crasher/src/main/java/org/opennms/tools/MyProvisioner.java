package org.opennms.tools;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.opennms.netmgt.dao.api.NodeDao;
import org.opennms.netmgt.events.api.EventForwarder;
import org.opennms.core.concurrent.PausibleScheduledThreadPoolExecutor;
import org.opennms.core.tasks.DefaultTaskCoordinator;
import org.opennms.netmgt.config.api.SnmpAgentConfigFactory;
import org.opennms.netmgt.model.OnmsCategory;
import org.opennms.netmgt.model.OnmsDistPoller;
import org.opennms.netmgt.model.OnmsIpInterface;
import org.opennms.netmgt.model.OnmsMonitoredService;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsServiceType;
import org.opennms.netmgt.model.OnmsSnmpInterface;
import org.opennms.netmgt.provision.IpInterfacePolicy;
import org.opennms.netmgt.provision.NodePolicy;
import org.opennms.netmgt.provision.ProvisioningAdapter;
import org.opennms.netmgt.provision.ServiceDetector;
import org.opennms.netmgt.provision.SnmpInterfacePolicy;
import org.opennms.netmgt.provision.detector.datagram.DnsDetector;
import org.opennms.netmgt.provision.detector.icmp.IcmpDetector;
import org.opennms.netmgt.provision.detector.simple.HttpDetector;
import org.opennms.netmgt.provision.detector.snmp.SnmpDetector;
import org.opennms.netmgt.provision.persist.ForeignSourceRepository;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.netmgt.provision.service.HostnameResolver;
import org.opennms.netmgt.provision.service.ImportScheduler;
import org.opennms.netmgt.provision.service.NodeScanSchedule;
import org.opennms.netmgt.provision.service.ProvisionService;
import org.opennms.netmgt.provision.service.Provisioner;
import org.opennms.netmgt.provision.service.ProvisioningAdapterManager;
import org.opennms.netmgt.snmp.SnmpAgentConfig;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.xml.event.Log;
import org.quartz.Calendar;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.SchedulerMetaData;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.quartz.UnableToInterruptJobException;
import org.quartz.impl.StdScheduler;
import org.quartz.spi.JobFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ScheduledExecutorFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class MyProvisioner {
    
    private static final Logger LOG = LoggerFactory.getLogger(MyProvisioner.class);
    
    private final List<InetAddress> ipAddrs;
    
    public MyProvisioner(List<InetAddress> ipAddrs) {
        this.ipAddrs = ipAddrs;
    }

    public Provisioner createAndStart() throws Exception {
        ProvisioningAdapterManager proAdaMan = new ProvisioningAdapterManager();
        proAdaMan.setAdapters(new HashSet<ProvisioningAdapter>());
        
        SchedulerFactoryBean schedulerFactory = new SchedulerFactoryBean();
        schedulerFactory.setSchedulerName("provisiond");
        schedulerFactory.afterPropertiesSet();

        ImportScheduler importScheduler = new ImportScheduler(schedulerFactory.getObject());

        ProvisionService provisionService = new MyProvisionService(new MyNodeDao(ipAddrs));

        CustomizableThreadFactory nodeScanExecutor = new CustomizableThreadFactory();
        nodeScanExecutor.setThreadNamePrefix("nodeScanExecutor-");
        
        ScheduledExecutorService scheduledExector = new PausibleScheduledThreadPoolExecutor(10, nodeScanExecutor);
        
        ScheduledExecutorFactoryBean importExecutor = new ScheduledExecutorFactoryBean();
        importExecutor.setBeanName("importExecutor");
        importExecutor.setPoolSize(8);
        importExecutor.initialize();
        
        ScheduledExecutorFactoryBean scanExecutor = new ScheduledExecutorFactoryBean();
        scanExecutor.setBeanName("scanExecutor");
        scanExecutor.setPoolSize(10);
        scanExecutor.initialize();
        
        ScheduledExecutorFactoryBean writeExecutor = new ScheduledExecutorFactoryBean();
        writeExecutor.setBeanName("writeExecutor");
        writeExecutor.setPoolSize(10);
        writeExecutor.initialize();

        Map<String, Executor> executors = Maps.newHashMap();
        executors.put("import", importExecutor.getObject());
        executors.put("scan", scanExecutor.getObject());
        executors.put("default", importExecutor.getObject());
        executors.put("write", writeExecutor.getObject());

        DefaultTaskCoordinator taskCoordinator = new DefaultTaskCoordinator("Provisiond");
        taskCoordinator.setDefaultExecutor("scan");
        taskCoordinator.setExecutors(executors);

        EventForwarder eventForwarder = new MyEventForwarder();
        
        Provisioner provisioner = new Provisioner();
        provisioner.setProvisioningAdapterManager(proAdaMan);
        provisioner.setProvisionService(provisionService);
        provisioner.setImportSchedule(importScheduler);
        provisioner.setScheduledExecutor(scheduledExector);
        provisioner.setTaskCoordinator(taskCoordinator);
        provisioner.setAgentConfigFactory(snmpAgentConfigFactory);
        provisioner.setEventForwarder(eventForwarder);
        provisioner.start();
        
        return provisioner;
    }

    private static List<ServiceDetector> getDetectors() {
        final List<ServiceDetector> detectors = new LinkedList<ServiceDetector>();
        
        final IcmpDetector icmpDetector = new IcmpDetector();
        detectors.add(icmpDetector);

        final SnmpDetector snmpDetector = new SnmpDetector();
        snmpDetector.setAgentConfigFactory(snmpAgentConfigFactory);
        detectors.add(snmpDetector);

        detectors.add(new DnsDetector());

        detectors.add(new HttpDetector());

        return detectors;
    }

    private static final SnmpAgentConfigFactory snmpAgentConfigFactory = new SnmpAgentConfigFactory() {
        @Override
        public SnmpAgentConfig getAgentConfig(InetAddress address) {
            final SnmpAgentConfig agent = new SnmpAgentConfig(address);
            agent.setTimeout(1800);
            agent.setVersion(2);
            agent.setRetries(1);
            return agent;
        }
    };

    private static class MyEventForwarder implements EventForwarder {
        @Override
        public void sendNow(Event event) {
            LOG.info("Sending event: {}", event);
        }

        @Override
        public void sendNow(Log eventLog) {
            LOG.info("Sending event log: {}", eventLog);
        }
    }

    private static class MyProvisionService implements ProvisionService {

        private final NodeDao nodeDao;
        
        public MyProvisionService(NodeDao nodeDao) {
            this.nodeDao = nodeDao;
        }

        @Override
        public boolean isRequisitionedEntityDeletionEnabled() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isDiscoveryEnabled() {
            return true;
        }

        @Override
        public void clearCache() {
            // pass
        }

        @Override
        public OnmsDistPoller createDistPollerIfNecessary(String dpName, String dpAddr) {
            // pass
            return null;
        }

        @Override
        public void updateNode(OnmsNode node, String rescanExisting) {
            // pass
        }

        @Override
        public OnmsNode updateNodeAttributes(OnmsNode node) {
            return node;
        }

        @Override
        public OnmsNode getDbNodeInitCat(Integer nodeId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public OnmsIpInterface updateIpInterfaceAttributes(Integer nodeId,
                OnmsIpInterface ipInterface) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public OnmsSnmpInterface updateSnmpInterfaceAttributes(Integer nodeId,
                OnmsSnmpInterface snmpInterface) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public OnmsMonitoredService addMonitoredService(Integer ipInterfaceId,
                String svcName) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public OnmsMonitoredService addMonitoredService(Integer nodeId,
                String ipAddress, String serviceName) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public OnmsMonitoredService updateMonitoredServiceState(Integer nodeId,
                String ipAddress, String serviceName) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public OnmsNode getRequisitionedNode(String foreignSource,
                String foreignId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void deleteNode(Integer nodeId) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void deleteInterface(Integer nodeId, String ipAddr) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void deleteService(Integer nodeId, InetAddress addr,
                String service) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void insertNode(OnmsNode node) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public OnmsServiceType createServiceTypeIfNecessary(String serviceName) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public OnmsCategory createCategoryIfNecessary(String name) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map<String, Integer> getForeignIdToNodeIdMap(String foreignSource) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setNodeParentAndDependencies(String foreignSource,
                String foreignId, String parentForeignSource,
                String parentForeignId, String parentNodeLabel) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public List<NodeScanSchedule> getScheduleForNodes() {
            return Lists.newArrayList();
        }

        @Override
        public NodeScanSchedule getScheduleForNode(int nodeId, boolean force) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setForeignSourceRepository(
                ForeignSourceRepository foriengSourceRepository) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Requisition loadRequisition(Resource resource) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<ServiceDetector> getDetectorsForForeignSource(
                String foreignSource) {
            return getDetectors();
        }

        @Override
        public List<NodePolicy> getNodePoliciesForForeignSource(
                String foreignSourceName) {
            return Lists.newArrayList();
        }

        @Override
        public List<IpInterfacePolicy> getIpInterfacePoliciesForForeignSource(
                String foreignSourceName) {
            return Lists.newArrayList();
        }

        @Override
        public List<SnmpInterfacePolicy> getSnmpInterfacePoliciesForForeignSource(
                String foreignSourceName) {
            return Lists.newArrayList();
        }

        @Override
        public void updateNodeScanStamp(Integer nodeId, Date scanStamp) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void deleteObsoleteInterfaces(Integer nodeId, Date scanStamp) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public OnmsIpInterface setIsPrimaryFlag(Integer nodeId, String ipAddress) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public OnmsIpInterface getPrimaryInterfaceForNode(OnmsNode node) {
            return nodeDao.get(node.getId()).getIpInterfaces().iterator().next();
        }

        @Override
        public OnmsNode createUndiscoveredNode(String ipAddress,
                String foreignSource) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public OnmsNode getNode(Integer nodeId) {
            return nodeDao.get(nodeId);
        }

        @Override
        public HostnameResolver getHostnameResolver() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setHostnameResolver(HostnameResolver resolver) {
            // TODO Auto-generated method stub
            
        }
        
    }
}
