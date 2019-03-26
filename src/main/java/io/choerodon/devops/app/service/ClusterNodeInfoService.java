package io.choerodon.devops.app.service;

import io.choerodon.core.domain.Page;
import io.choerodon.devops.api.dto.AgentNodeInfoDTO;
import io.choerodon.devops.api.dto.ClusterNodeInfoDTO;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;

import java.util.List;

/**
 * @author zmf
 */
public interface ClusterNodeInfoService {
    /**
     * get redis cluster key to get node information
     *
     * @param clusterId the cluster id
     * @return the redis key according to the cluster id
     */
    String getRedisClusterKey(Long clusterId);

    /**
     * get redis cluster key to get node information
     *
     * @param clusterId      the cluster id
     * @param organizationId the organization id
     * @return the redis key according to the cluster id
     */
    String getRedisClusterKey(Long clusterId, Long organizationId);

    /**
     * set the node information for the redis key.
     * The previous value will be discarded.
     *
     * @param redisClusterKey        the key
     * @param agentNodeInfoDTOS the information of nodes.
     */
    void setValueForKey(String redisClusterKey, List<AgentNodeInfoDTO> agentNodeInfoDTOS);

    /**
     * page query the node information of the cluster
     *
     * @param clusterId      the cluster id
     * @param organizationId the organization id
     * @param pageRequest    the page parameters
     * @return a page of nodes
     */
    Page<ClusterNodeInfoDTO> pageQueryClusterNodeInfo(Long clusterId, Long organizationId, PageRequest pageRequest);

    /**
     * get cluster node information by cluster id and node name
     * There is a requirement of organization because the organization id is
     * available in front end and this can save a query in database for organization id.
     *
     * @param organizationId organization id
     * @param clusterId      the cluster id
     * @param nodeName       the node name
     * @return the node information
     */
    ClusterNodeInfoDTO getNodeInfo(Long organizationId, Long clusterId, String nodeName);
}
