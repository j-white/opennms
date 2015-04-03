package org.opennms.tools;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.opennms.netmgt.events.api.EventForwarder;
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
import org.springframework.core.io.Resource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class MyProvisioner {
    
    public Provisioner createAndStart() throws Exception {
        ProvisioningAdapterManager proAdaMan = new ProvisioningAdapterManager();
        proAdaMan.setAdapters(new HashSet<ProvisioningAdapter>());
        
        Scheduler scheduler = new MyScheduler();
        ImportScheduler importScheduler = new ImportScheduler(scheduler);

        ProvisionService provisionService = new MyProvisionService();
        
        ScheduledExecutorService scheduledExector = Executors.newScheduledThreadPool(10);

        DefaultTaskCoordinator taskCoordinator = new DefaultTaskCoordinator("Tasks", Executors.newFixedThreadPool(10));

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
    
    /*
    public static void main(String[] args) throws Exception {
        ProvisioningAdapterManager proAdaMan = new ProvisioningAdapterManager();
        proAdaMan.setAdapters(new HashSet<ProvisioningAdapter>());
        
        Scheduler scheduler = new MyScheduler();
        ImportScheduler importScheduler = new ImportScheduler(scheduler);

        ProvisionService provisionService = new MyProvisionService();
        
        ScheduledExecutorService scheduledExector = Executors.newScheduledThreadPool(10);

        DefaultTaskCoordinator taskCoordinator = new DefaultTaskCoordinator("Tasks", Executors.newFixedThreadPool(10));

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

        Event e = new Event();
        e.setNodeid(1L);
        provisioner.handleForceRescan(e);

        Thread.sleep(1000);
        provisioner.destroy();
    }
    */
    
    private static final SnmpAgentConfigFactory snmpAgentConfigFactory = new SnmpAgentConfigFactory() {
        @Override
        public SnmpAgentConfig getAgentConfig(InetAddress address) {
            final SnmpAgentConfig agent = new SnmpAgentConfig(address);
            agent.setTimeout(1800);
            agent.setVersion(2);
            return agent;
        }
    };
    
    private static class MyEventForwarder implements EventForwarder {
        @Override
        public void sendNow(Event event) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void sendNow(Log eventLog) {
            // TODO Auto-generated method stub
            
        }
    }

    private static class MyProvisionService implements ProvisionService {

        @Override
        public boolean isRequisitionedEntityDeletionEnabled() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isDiscoveryEnabled() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void clearCache() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public OnmsDistPoller createDistPollerIfNecessary(String dpName,
                String dpAddr) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void updateNode(OnmsNode node, String rescanExisting) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public OnmsNode updateNodeAttributes(OnmsNode node) {
            // TODO Auto-generated method stub
            return null;
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
            return Lists.newArrayList();
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
            OnmsIpInterface ipInterface = new OnmsIpInterface();
            try {
                ipInterface.setIpAddress(InetAddress.getLocalHost());
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            
            OnmsServiceType snmpSvcType = new OnmsServiceType();
            snmpSvcType.setName("SNMP");
            OnmsMonitoredService snmpSvc = new OnmsMonitoredService();
            snmpSvc.setServiceType(snmpSvcType);
            
            ipInterface.setMonitoredServices(Sets.newHashSet(snmpSvc));

            // getMonitoredServiceByServiceType
            return ipInterface;
        }

        @Override
        public OnmsNode createUndiscoveredNode(String ipAddress,
                String foreignSource) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public OnmsNode getNode(Integer nodeId) {
            OnmsIpInterface ipInterface = new OnmsIpInterface();
            try {
                ipInterface.setIpAddress(InetAddress.getLocalHost());
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }

            OnmsNode node = new OnmsNode();
            node.setId(nodeId);
            node.setIpInterfaces(Sets.newHashSet(ipInterface));
            return node;
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
