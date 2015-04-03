package org.opennms.tools;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.opennms.core.criteria.Criteria;
import org.opennms.netmgt.dao.api.NodeDao;
import org.opennms.netmgt.model.OnmsCategory;
import org.opennms.netmgt.model.OnmsCriteria;
import org.opennms.netmgt.model.OnmsDistPoller;
import org.opennms.netmgt.model.OnmsIpInterface;
import org.opennms.netmgt.model.OnmsMonitoredService;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsServiceType;
import org.opennms.netmgt.model.SurveillanceStatus;

import com.google.common.collect.Sets;

public class MyNodeDao implements NodeDao {
    private final List<InetAddress> ipAddrs;
    
    public MyNodeDao(List<InetAddress> ipAddrs) {
        this.ipAddrs = ipAddrs;
    }

    @Override
    public OnmsNode get(Integer id) {
        OnmsIpInterface ipInterface = new OnmsIpInterface();
        ipInterface.setIpAddress(ipAddrs.get(id % ipAddrs.size()));
        OnmsServiceType snmpSvcType = new OnmsServiceType();
        snmpSvcType.setName("SNMP");
        OnmsMonitoredService snmpSvc = new OnmsMonitoredService();
        snmpSvc.setServiceType(snmpSvcType);
        
        ipInterface.setMonitoredServices(Sets.newHashSet(snmpSvc));

        OnmsNode node = new OnmsNode();
        node.setId(id);
        node.setIpInterfaces(Sets.newHashSet(ipInterface));

        return node;
    }
    
    @Override
    public OnmsNode get(String nodeId) {
        return get(Integer.valueOf(nodeId));
    }

    @Override
    public List<OnmsNode> findMatching(OnmsCriteria criteria) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int countMatching(OnmsCriteria onmsCrit) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void lock() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void initialize(Object obj) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void flush() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int countAll() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void delete(OnmsNode enetity) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void delete(Integer key) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public List<OnmsNode> findMatching(Criteria criteria) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int countMatching(Criteria onmsCrit) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public OnmsNode load(Integer id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void save(OnmsNode entity) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void saveOrUpdate(OnmsNode entity) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void update(OnmsNode entity) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public Map<Integer, String> getAllLabelsById() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getLabelForId(Integer id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<OnmsNode> findByLabel(String label) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<OnmsNode> findNodes(OnmsDistPoller dp) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OnmsNode getHierarchy(Integer id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Integer> getForeignIdToNodeIdMap(String foreignSource) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<OnmsNode> findAllByVarCharAssetColumn(String columnName,
            String columnValue) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<OnmsNode> findAllByVarCharAssetColumnCategoryList(
            String columnName, String columnValue,
            Collection<OnmsCategory> categories) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<OnmsNode> findByCategory(OnmsCategory category) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<OnmsNode> findAllByCategoryList(
            Collection<OnmsCategory> categories) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<OnmsNode> findAllByCategoryLists(
            Collection<OnmsCategory> rowCatNames,
            Collection<OnmsCategory> colCatNames) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<OnmsNode> findAll() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<OnmsNode> findByForeignSource(String foreignSource) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OnmsNode findByForeignId(String foreignSource, String foreignId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<OnmsNode> findByIpAddressAndService(InetAddress ipAddress,
            String serviceName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getNodeCountForForeignSource(String groupName) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public List<OnmsNode> findAllProvisionedNodes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<OnmsIpInterface> findObsoleteIpInterfaces(Integer nodeId,
            Date scanStamp) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteObsoleteInterfaces(Integer nodeId, Date scanStamp) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void updateNodeScanStamp(Integer nodeId, Date scanStamp) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Collection<Integer> getNodeIds() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<OnmsNode> findByForeignSourceAndIpAddress(String foreignSource,
            String ipAddress) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SurveillanceStatus findSurveillanceStatusByCategoryLists(
            Collection<OnmsCategory> rowCategories,
            Collection<OnmsCategory> columnCategories) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Integer getNextNodeId(Integer nodeId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Integer getPreviousNodeId(Integer nodeId) {
        // TODO Auto-generated method stub
        return null;
    }

}
