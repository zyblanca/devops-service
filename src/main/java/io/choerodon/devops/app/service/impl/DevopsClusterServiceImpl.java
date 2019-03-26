package io.choerodon.devops.app.service.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.choerodon.core.convertor.ConvertHelper;
import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.api.dto.*;
import io.choerodon.devops.app.service.ClusterNodeInfoService;
import io.choerodon.devops.app.service.DevopsClusterService;
import io.choerodon.devops.domain.application.entity.*;
import io.choerodon.devops.domain.application.repository.*;
import io.choerodon.devops.infra.common.util.EnvUtil;
import io.choerodon.devops.infra.common.util.FileUtil;
import io.choerodon.devops.infra.common.util.GenerateUUID;
import io.choerodon.devops.infra.dataobject.DevopsEnvPodContainerDO;
import io.choerodon.devops.infra.dataobject.iam.ProjectDO;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.websocket.helper.EnvListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class DevopsClusterServiceImpl implements DevopsClusterService {

    @Value("${agent.version}")
    private String agentExpectVersion;

    @Value("${agent.serviceUrl}")
    private String agentServiceUrl;

    @Value("${agent.repoUrl}")
    private String agentRepoUrl;

    @Autowired
    private DevopsClusterRepository devopsClusterRepository;
    @Autowired
    private DevopsClusterProPermissionRepository devopsClusterProPermissionRepository;
    @Autowired
    private IamRepository iamRepository;
    @Autowired
    private EnvUtil envUtil;
    @Autowired
    private EnvListener envListener;
    @Autowired
    private DevopsEnvironmentRepository devopsEnvironmentRepository;
    @Autowired
    private ClusterNodeInfoService clusterNodeInfoService;
    @Autowired
    private DevopsEnvPodContainerRepository devopsEnvPodContainerRepository;


    @Override
    @Transactional
    public String createCluster(Long organizationId, DevopsClusterReqDTO devopsClusterReqDTO) {
        // 插入记录
        DevopsClusterE devopsClusterE = new DevopsClusterE();
        BeanUtils.copyProperties(devopsClusterReqDTO, devopsClusterE);
        devopsClusterE.setToken(GenerateUUID.generateUUID());
        devopsClusterE.setOrganizationId(organizationId);
        devopsClusterE = devopsClusterRepository.create(devopsClusterE);
        if (!devopsClusterE.getSkipCheckProjectPermission() && devopsClusterReqDTO.getProjects() != null) {
            for (Long projectId : devopsClusterReqDTO.getProjects()) {
                DevopsClusterProPermissionE devopsClusterProPermissionE = new DevopsClusterProPermissionE();
                devopsClusterProPermissionE.setClusterId(devopsClusterE.getId());
                devopsClusterProPermissionE.setProjectId(projectId);
                devopsClusterProPermissionRepository.insert(devopsClusterProPermissionE);
            }
        }

        // 渲染激活环境的命令参数
        InputStream inputStream = this.getClass().getResourceAsStream("/shell/cluster.sh");
        Map<String, String> params = new HashMap<>();
        params.put("{VERSION}", agentExpectVersion);
        params.put("{NAME}", "choerodon-cluster-agent-" + devopsClusterE.getCode());
        params.put("{SERVICEURL}", agentServiceUrl);
        params.put("{TOKEN}", devopsClusterE.getToken());
        params.put("{CHOERODONID}", devopsClusterE.getChoerodonId());
        params.put("{REPOURL}", agentRepoUrl);
        params.put("{CLUSTERID}", devopsClusterE
                .getId().toString());
        return FileUtil.replaceReturnString(inputStream, params);
    }

    @Override
    @Transactional
    public void updateCluster(Long clusterId, DevopsClusterReqDTO devopsClusterReqDTO) {
        List<Long> projects = devopsClusterReqDTO.getProjects();
        Boolean skipCheckPro = devopsClusterReqDTO.getSkipCheckProjectPermission();
        List<Long> addProjects = new ArrayList<>();
        DevopsClusterE devopsClusterE = devopsClusterRepository.query(clusterId);
        //以前不跳过项目权限校验,但是现在跳过，情况集群对应的项目集群校验表
        if (skipCheckPro && !devopsClusterE.getSkipCheckProjectPermission()) {
            devopsClusterProPermissionRepository.deleteByClusterId(clusterId);
        } else {
            //操作集群项目权限校验表记录
            List<Long> projectIds = devopsClusterProPermissionRepository.listByClusterId(clusterId)
                    .stream().map(DevopsClusterProPermissionE::getProjectId).collect(Collectors.toList());

            projects.forEach(projectId -> {
                if (!projectIds.contains(projectId)) {
                    addProjects.add(projectId);
                } else {
                    projectIds.remove(projectId);
                }
            });
            addProjects.forEach(addProject -> {
                DevopsClusterProPermissionE devopsClusterProPermissionE = new DevopsClusterProPermissionE();
                devopsClusterProPermissionE.setClusterId(clusterId);
                devopsClusterProPermissionE.setProjectId(addProject);
                devopsClusterProPermissionRepository.insert(devopsClusterProPermissionE);
            });
            projectIds.forEach(deleteProject -> {
                DevopsClusterProPermissionE devopsClusterProPermissionE = new DevopsClusterProPermissionE();
                devopsClusterProPermissionE.setClusterId(clusterId);
                devopsClusterProPermissionE.setProjectId(deleteProject);
                devopsClusterProPermissionRepository.delete(devopsClusterProPermissionE);
            });
        }
        devopsClusterE = ConvertHelper.convert(devopsClusterReqDTO, DevopsClusterE.class);
        devopsClusterE.setId(clusterId);
        devopsClusterRepository.update(devopsClusterE);
    }

    @Override
    public void checkName(Long organizationId, String name) {
        DevopsClusterE devopsClusterE = new DevopsClusterE();
        devopsClusterE.setOrganizationId(organizationId);
        devopsClusterE.setName(name);
        devopsClusterRepository.checkName(devopsClusterE);
    }

    @Override
    public Page<ProjectDTO> listProjects(Long organizationId, Long clusterId, PageRequest pageRequest,
                                         String[] params) {
        Page<ProjectDO> projects = iamRepository
                .queryProjectByOrgId(organizationId, pageRequest.getPage(), pageRequest.getSize(), null, params);
        Page<ProjectDTO> pageProjectDTOS = new Page<>();
        List<ProjectDTO> projectDTOS = new ArrayList<>();
        if (projects.getContent() != null) {
            BeanUtils.copyProperties(projects, pageProjectDTOS);
            List<Long> projectIds;
            if (clusterId != null) {
                projectIds = devopsClusterProPermissionRepository.listByClusterId(clusterId).stream()
                        .map(DevopsClusterProPermissionE::getProjectId).collect(Collectors.toList());
            } else {
                projectIds = new ArrayList<>();
            }
            projects.getContent().forEach(projectDO -> {
                ProjectDTO projectDTO = new ProjectDTO(projectDO.getId(), projectDO.getName(), projectDO.getCode(), projectIds.contains(projectDO.getId()));
                projectDTOS.add(projectDTO);
            });
        }
        pageProjectDTOS.setContent(projectDTOS);
        return pageProjectDTOS;
    }

    @Override
    public String queryShell(Long clusterId) {
        DevopsClusterE devopsClusterE = getDevopsClusterEStatus(clusterId);
        InputStream inputStream;
        if (devopsClusterE.getUpgrade()) {
            inputStream = this.getClass().getResourceAsStream("/shell/cluster-upgrade.sh");
        } else {
            inputStream = this.getClass().getResourceAsStream("/shell/cluster.sh");
        }
        Map<String, String> params = new HashMap<>();
        params.put("{VERSION}", agentExpectVersion);
        params.put("{NAME}", "choerodon-cluster-agent-" + devopsClusterE.getCode());
        params.put("{SERVICEURL}", agentServiceUrl);
        params.put("{TOKEN}", devopsClusterE.getToken());
        params.put("{REPOURL}", agentRepoUrl);
        params.put("{CHOERODONID}", devopsClusterE.getChoerodonId());
        params.put("{CLUSTERID}", devopsClusterE
                .getId().toString());
        return FileUtil.replaceReturnString(inputStream, params);
    }

    private DevopsClusterE getDevopsClusterEStatus(Long clusterId) {
        DevopsClusterE devopsClusterE = devopsClusterRepository.query(clusterId);
        List<Long> connectedEnvList = envUtil.getConnectedEnvList(envListener);
        List<Long> updatedEnvList = envUtil.getUpdatedEnvList(envListener);
        setClusterStatus(connectedEnvList, updatedEnvList, devopsClusterE);
        return devopsClusterE;
    }

    @Override
    public void checkCode(Long organizationId, String code) {
        DevopsClusterE devopsClusterE = new DevopsClusterE();
        devopsClusterE.setOrganizationId(organizationId);
        devopsClusterE.setCode(code);
        devopsClusterRepository.checkName(devopsClusterE);
    }

    @Override
    public Page<ClusterWithNodesDTO> pageClusters(Long organizationId, Boolean doPage, PageRequest pageRequest, String params) {
        Page<DevopsClusterE> devopsClusterEPage = devopsClusterRepository
                .pageClusters(organizationId, doPage, pageRequest, params);
        Page<ClusterWithNodesDTO> devopsClusterRepDTOPage = new Page<>();
        BeanUtils.copyProperties(devopsClusterEPage, devopsClusterRepDTOPage);
        List<Long> connectedEnvList = envUtil.getConnectedEnvList(envListener);
        List<Long> updatedEnvList = envUtil.getUpdatedEnvList(envListener);
        devopsClusterEPage.getContent().forEach(devopsClusterE ->
                setClusterStatus(connectedEnvList, updatedEnvList, devopsClusterE));
        devopsClusterRepDTOPage.setContent(fromClusterE2ClusterWithNodesDTO(devopsClusterEPage.getContent(), organizationId));
        return devopsClusterRepDTOPage;
    }

    /**
     * convert cluster entity to instances of {@link ClusterWithNodesDTO}
     *
     * @param clusterEList   the cluster entities
     * @param organizationId the organization id
     * @return the instances of the return type
     */
    private List<ClusterWithNodesDTO> fromClusterE2ClusterWithNodesDTO(List<DevopsClusterE> clusterEList, Long organizationId) {
        // default three records of nodes in the instance
        PageRequest pageRequest = new PageRequest(0, 3);

        return clusterEList.stream().map(cluster -> {
            ClusterWithNodesDTO clusterWithNodesDTO = new ClusterWithNodesDTO();
            BeanUtils.copyProperties(cluster, clusterWithNodesDTO);
            if (Boolean.TRUE.equals(clusterWithNodesDTO.getConnect())) {
                clusterWithNodesDTO.setNodes(clusterNodeInfoService.pageQueryClusterNodeInfo(cluster.getId(), organizationId, pageRequest));
            }
            return clusterWithNodesDTO;
        }).collect(Collectors.toList());
    }


    @Override
    public List<ProjectDTO> listClusterProjects(Long organizationId, Long clusterId) {
        return devopsClusterProPermissionRepository.listByClusterId(clusterId).stream()
                .map(devopsClusterProPermissionE -> {
                    ProjectE projectE = iamRepository.queryIamProject(devopsClusterProPermissionE.getProjectId());
                    return new ProjectDTO(devopsClusterProPermissionE.getProjectId(), projectE.getName(), projectE.getCode(), null);
                }).collect(Collectors.toList());
    }

    private void setClusterStatus(List<Long> connectedEnvList, List<Long> updatedEnvList, DevopsClusterE t) {
        if (connectedEnvList.contains(t.getId())) {
            if (updatedEnvList.contains(t.getId())) {
                t.initUpgrade(false);
                t.initConnect(true);
            } else {
                t.initUpgrade(true);
                t.initConnect(false);
                t.setUpgradeMessage("Version is too low, please upgrade!");
            }
        } else {
            t.initUpgrade(false);
            t.initConnect(false);
        }
    }

    @Override
    public void deleteCluster(Long clusterId) {
        devopsClusterRepository.delete(clusterId);
    }

    @Override
    public Boolean IsClusterRelatedEnvs(Long clusterId) {
        List<Long> connectedEnvList = envUtil.getConnectedEnvList(envListener);
        List<DevopsEnvironmentE> devopsEnvironmentES = devopsEnvironmentRepository.listByClusterId(clusterId);
        if (connectedEnvList.contains(clusterId) || !devopsEnvironmentES.isEmpty()) {
            throw new CommonException("error.cluster.delete");
        }
        return true;
    }


    @Override
    public DevopsClusterRepDTO getCluster(Long clusterId) {
        return ConvertHelper.convert(devopsClusterRepository.query(clusterId), DevopsClusterRepDTO.class);
    }

    @Override
    public Page<DevopsClusterPodDTO> pageQueryPodsByNodeName(Long clusterId, String nodeName, PageRequest pageRequest, String searchParam) {
        Page<DevopsEnvPodE> ePage = devopsClusterRepository.pageQueryPodsByNodeName(clusterId, nodeName, pageRequest, searchParam);
        Page<DevopsClusterPodDTO> clusterPodDTOPage = new Page<>();
        BeanUtils.copyProperties(ePage, clusterPodDTOPage, "content");
        clusterPodDTOPage.setContent(ePage.getContent().stream().map(this::podE2ClusterPodDTO).collect(Collectors.toList()));
        return clusterPodDTOPage;
    }

    /**
     * pod entity to cluster pod dto
     *
     * @param pod pod entity
     * @return the cluster pod dto
     */
    private DevopsClusterPodDTO podE2ClusterPodDTO(DevopsEnvPodE pod) {
        DevopsClusterPodDTO devopsEnvPodDTO = new DevopsClusterPodDTO();
        BeanUtils.copyProperties(pod, devopsEnvPodDTO);
        devopsEnvPodDTO.setContainersForLogs(
                devopsEnvPodContainerRepository.list(new DevopsEnvPodContainerDO(pod.getId()))
                        .stream()
                        .map(container -> new DevopsEnvPodContainerLogDTO(pod.getName(), container.getContainerName()))
                        .collect(Collectors.toList())
        );
        return devopsEnvPodDTO;
    }
}
