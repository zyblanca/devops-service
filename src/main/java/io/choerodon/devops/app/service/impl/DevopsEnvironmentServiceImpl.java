package io.choerodon.devops.app.service.impl;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONArray;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.choerodon.asgard.saga.annotation.Saga;
import io.choerodon.asgard.saga.dto.StartInstanceDTO;
import io.choerodon.asgard.saga.feign.SagaClient;
import io.choerodon.core.convertor.ConvertHelper;
import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.devops.api.dto.*;
import io.choerodon.devops.api.dto.gitlab.MemberDTO;
import io.choerodon.devops.api.dto.iam.UserDTO;
import io.choerodon.devops.api.validator.DevopsEnvironmentValidator;
import io.choerodon.devops.app.service.DevopsEnvironmentService;
import io.choerodon.devops.domain.application.entity.*;
import io.choerodon.devops.domain.application.entity.gitlab.GitlabGroupE;
import io.choerodon.devops.domain.application.entity.gitlab.GitlabMemberE;
import io.choerodon.devops.domain.application.entity.iam.UserE;
import io.choerodon.devops.domain.application.event.GitlabProjectPayload;
import io.choerodon.devops.domain.application.factory.DevopsEnvironmentFactory;
import io.choerodon.devops.domain.application.repository.*;
import io.choerodon.devops.domain.application.valueobject.Organization;
import io.choerodon.devops.domain.application.valueobject.ProjectHook;
import io.choerodon.devops.domain.service.DeployService;
import io.choerodon.devops.domain.service.UpdateUserPermissionService;
import io.choerodon.devops.domain.service.impl.UpdateEnvUserPermissionServiceImpl;
import io.choerodon.devops.infra.common.util.*;
import io.choerodon.devops.infra.common.util.enums.InstanceStatus;
import io.choerodon.devops.infra.dataobject.gitlab.GitlabProjectDO;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.websocket.helper.EnvListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by younger on 2018/4/9.
 */
@Service
public class DevopsEnvironmentServiceImpl implements DevopsEnvironmentService {

    private static final Gson gson = new Gson();
    private static final String MASTER = "master";
    private static final String README = "README.md";
    private static final String README_CONTENT =
            "# This is gitops env repository!";
    private static final String ENV = "ENV";
    private static final String PROJECT_OWNER = "role/project/default/project-owner";
    private static final String PROJECT_MEMBER = "role/project/default/project-member";
    private Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
    private ObjectMapper objectMapper = new ObjectMapper();

    @Value("${agent.version}")
    private String agentExpectVersion;

    @Value("${agent.serviceUrl}")
    private String agentServiceUrl;

    @Value("${agent.repoUrl}")
    private String agentRepoUrl;

    @Value("${services.gateway.url}")
    private String gatewayUrl;

    @Value("${services.gitlab.sshUrl}")
    private String gitlabSshUrl;

    @Value("${services.gitlab.url}")
    private String gitlabUrl;

    @Autowired
    private IamRepository iamRepository;
    @Autowired
    private DevopsEnvironmentRepository devopsEnviromentRepository;
    @Autowired
    private DevopsEnvUserPermissionRepository devopsEnvUserPermissionRepository;
    @Autowired
    private EnvListener envListener;
    @Autowired
    private DevopsServiceRepository devopsServiceRepository;
    @Autowired
    private ApplicationInstanceRepository applicationInstanceRepository;
    @Autowired
    private DevopsEnvironmentValidator devopsEnvironmentValidator;
    @Autowired
    private EnvUtil envUtil;
    @Autowired
    private GitUtil gitUtil;
    @Autowired
    private SagaClient sagaClient;
    @Autowired
    private DevopsProjectRepository devopsProjectRepository;
    @Autowired
    private UserAttrRepository userAttrRepository;
    @Autowired
    private GitlabRepository gitlabRepository;
    @Autowired
    private DevopsEnvCommitRepository devopsEnvCommitRepository;
    @Autowired
    private DevopsEnvGroupRepository devopsEnvGroupRepository;
    @Autowired
    private DevopsIngressRepository devopsIngressRepository;
    @Autowired
    private DevopsClusterRepository devopsClusterRepository;
    @Autowired
    private DeployService deployService;
    @Autowired
    private GitlabProjectRepository gitlabProjectRepository;

    @Override
    @Saga(code = "devops-create-env", description = "????????????", inputSchema = "{}")
    public void create(Long projectId, DevopsEnviromentDTO devopsEnviromentDTO) {
        DevopsEnvironmentE devopsEnvironmentE = ConvertHelper.convert(devopsEnviromentDTO, DevopsEnvironmentE.class);
        devopsEnvironmentE.initProjectE(projectId);
        checkCode(projectId, devopsEnviromentDTO.getClusterId(), devopsEnviromentDTO.getCode());
        devopsEnvironmentE.initActive(true);
        devopsEnvironmentE.initConnect(false);
        devopsEnvironmentE.initSynchro(false);
        devopsEnvironmentE.initFailed(false);
        devopsEnvironmentE.initDevopsClusterEById(devopsEnviromentDTO.getClusterId());
        devopsEnvironmentE.initToken(GenerateUUID.generateUUID());
        devopsEnvironmentE.initProjectE(projectId);
        ProjectE projectE = iamRepository.queryIamProject(projectId);
        Organization organization = iamRepository.queryOrganizationById(projectE.getOrganization().getId());
        List<DevopsEnvironmentE> devopsEnvironmentES = devopsEnviromentRepository
                .queryByprojectAndActive(projectId, true);
        //?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        if (devopsEnviromentDTO.getDevopsEnvGroupId() == null) {
            devopsEnvironmentE.initSequence(devopsEnvironmentES.stream().filter(devopsEnvironmentE1 ->
                    devopsEnvironmentE1.getDevopsEnvGroupId() == null).collect(Collectors.toList()));
        } else {
            devopsEnvironmentE.initSequence(devopsEnvironmentES.stream().filter(devopsEnvironmentE1 ->
                    devopsEnviromentDTO.getDevopsEnvGroupId().equals(devopsEnvironmentE1.getDevopsEnvGroupId()))
                    .collect(Collectors.toList()));
        }
        List<String> sshKeys = FileUtil.getSshKey(
                organization.getCode() + "/" + projectE.getCode() + "/" + devopsEnviromentDTO.getCode());
        devopsEnvironmentE.setEnvIdRsa(sshKeys.get(0));
        devopsEnvironmentE.setEnvIdRsaPub(sshKeys.get(1));
        Long envId = devopsEnviromentRepository.create(devopsEnvironmentE).getId();
        devopsEnvironmentE.setId(envId);
        devopsEnvironmentE.setGitlabEnvProjectPath(devopsEnviromentDTO.getCode() + "-" + envId);
        devopsEnviromentRepository.update(devopsEnvironmentE);

        GitlabGroupE gitlabGroupE = devopsProjectRepository.queryDevopsProject(projectId);
        UserAttrE userAttrE = userAttrRepository.queryById(TypeUtil.objToLong(GitUserNameUtil.getUserId()));
        GitlabProjectPayload gitlabProjectPayload = new GitlabProjectPayload();
        gitlabProjectPayload.setGroupId(TypeUtil.objToInteger(gitlabGroupE.getDevopsEnvGroupId()));
        gitlabProjectPayload.setUserId(TypeUtil.objToInteger(userAttrE.getGitlabUserId()));
        gitlabProjectPayload.setPath(devopsEnviromentDTO.getCode());
        gitlabProjectPayload.setGitlabProjectPath(devopsEnvironmentE.getGitlabEnvProjectPath());
        gitlabProjectPayload.setOrganizationId(null);
        gitlabProjectPayload.setType(ENV);
        UserE userE = iamRepository.queryUserByUserId(userAttrE.getIamUserId());
        gitlabProjectPayload.setLoginName(userE.getLoginName());
        gitlabProjectPayload.setRealName(userE.getRealName());
        gitlabProjectPayload.setClusterId(devopsEnviromentDTO.getClusterId());
        gitlabProjectPayload.setIamProjectId(projectId);

        // ?????????????????????????????????????????????payload?????????saga??????
        gitlabProjectPayload.setUserIds(devopsEnviromentDTO.getUserIds());

        String input;
        try {
            input = objectMapper.writeValueAsString(gitlabProjectPayload);
            sagaClient.startSaga("devops-create-env", new StartInstanceDTO(input, "", "", ResourceLevel.PROJECT.value(), projectId));
            deployService.initEnv(devopsEnvironmentE, devopsEnviromentDTO.getClusterId());
        } catch (JsonProcessingException e) {
            throw new CommonException(e.getMessage(), e);
        }
    }

    @Override
    public List<DevopsEnvGroupEnvsDTO> listDevopsEnvGroupEnvs(Long projectId, Boolean active) {
        List<DevopsEnvGroupEnvsDTO> devopsEnvGroupEnvsDTOS = new ArrayList<>();
        List<Long> connectedClusterList = envUtil.getConnectedEnvList(envListener);
        List<Long> upgradeClusterList = envUtil.getUpdatedEnvList(envListener);
        List<DevopsEnvironmentE> devopsEnvironmentES = devopsEnviromentRepository
                .queryByprojectAndActive(projectId, active).stream().peek(t ->
                        setEnvStatus(connectedClusterList, upgradeClusterList, t)
                )
                .sorted(Comparator.comparing(DevopsEnvironmentE::getSequence))
                .collect(Collectors.toList());
        List<DevopsEnviromentRepDTO> devopsEnviromentRepDTOS = ConvertHelper.convertList(devopsEnvironmentES, DevopsEnviromentRepDTO.class);
        if (!active) {
            DevopsEnvGroupEnvsDTO devopsEnvGroupEnvsDTO = new DevopsEnvGroupEnvsDTO();
            devopsEnvGroupEnvsDTO.setDevopsEnviromentRepDTOs(devopsEnviromentRepDTOS);
            devopsEnvGroupEnvsDTOS.add(devopsEnvGroupEnvsDTO);
            return devopsEnvGroupEnvsDTOS;
        }
        List<DevopsEnvGroupE> devopsEnvGroupES = devopsEnvGroupRepository.listByProjectId(projectId);
        devopsEnviromentRepDTOS.forEach(devopsEnviromentRepDTO -> {
            DevopsClusterE devopsClusterE = devopsClusterRepository.query(devopsEnviromentRepDTO.getClusterId());
            devopsEnviromentRepDTO.setClusterName(devopsClusterE == null ? null : devopsClusterE.getName());
            if (devopsEnviromentRepDTO.getDevopsEnvGroupId() == null) {
                devopsEnviromentRepDTO.setDevopsEnvGroupId(0L);
            }
        });
        //?????????????????????????????????????????????????????????????????????????????????????????????
        Map<Long, List<DevopsEnviromentRepDTO>> resultMaps = devopsEnviromentRepDTOS.stream()
                .collect(Collectors.groupingBy(DevopsEnviromentRepDTO::getDevopsEnvGroupId));
        List<Long> envGroupIds = new ArrayList<>();
        resultMaps.forEach((key, value) -> {
            envGroupIds.add(key);
            DevopsEnvGroupEnvsDTO devopsEnvGroupEnvsDTO = new DevopsEnvGroupEnvsDTO();
            DevopsEnvGroupE devopsEnvGroupE = new DevopsEnvGroupE();
            if (key != 0) {
                devopsEnvGroupE = devopsEnvGroupRepository.query(key);
            }
            devopsEnvGroupEnvsDTO.setDevopsEnvGroupId(devopsEnvGroupE.getId());
            devopsEnvGroupEnvsDTO.setDevopsEnvGroupName(devopsEnvGroupE.getName());
            devopsEnvGroupEnvsDTO.setDevopsEnviromentRepDTOs(value);
            devopsEnvGroupEnvsDTOS.add(devopsEnvGroupEnvsDTO);
        });
        devopsEnvGroupES.forEach(devopsEnvGroupE -> {
            if (!envGroupIds.contains(devopsEnvGroupE.getId())) {
                DevopsEnvGroupEnvsDTO devopsEnvGroupEnvsDTO = new DevopsEnvGroupEnvsDTO();
                devopsEnvGroupEnvsDTO.setDevopsEnvGroupId(devopsEnvGroupE.getId());
                devopsEnvGroupEnvsDTO.setDevopsEnvGroupName(devopsEnvGroupE.getName());
                devopsEnvGroupEnvsDTOS.add(devopsEnvGroupEnvsDTO);
            }
        });
        return devopsEnvGroupEnvsDTOS;
    }

    @Override
    public List<DevopsEnviromentRepDTO> listByProjectIdAndActive(Long projectId, Boolean active) {

        // ?????????????????????????????????
        List<Long> permissionEnvIds = devopsEnvUserPermissionRepository
                .listByUserId(TypeUtil.objToLong(GitUserNameUtil.getUserId())).stream()
                .filter(DevopsEnvUserPermissionE::getPermitted)
                .map(DevopsEnvUserPermissionE::getEnvId).collect(Collectors.toList());
        ProjectE projectE = iamRepository.queryIamProject(projectId);
        // ??????????????????????????????????????????
        Boolean isProjectOwner = iamRepository
                .isProjectOwner(TypeUtil.objToLong(GitUserNameUtil.getUserId()), projectE);

        List<Long> connectedClusterList = envUtil.getConnectedEnvList(envListener);
        List<Long> upgradeClusterList = envUtil.getUpdatedEnvList(envListener);
        List<DevopsEnvironmentE> devopsEnvironmentES = devopsEnviromentRepository
                .queryByprojectAndActive(projectId, active).stream()
                .filter(devopsEnvironmentE -> !devopsEnvironmentE.getFailed()).peek(t -> {
                    setEnvStatus(connectedClusterList, upgradeClusterList, t);
                    // ?????????????????????????????????????????????????????????????????????????????????
                    setPermission(t, permissionEnvIds, isProjectOwner);
                })
                .sorted(Comparator.comparing(DevopsEnvironmentE::getSequence))
                .collect(Collectors.toList());
        return ConvertHelper.convertList(devopsEnvironmentES, DevopsEnviromentRepDTO.class);
    }

    @Override
    public List<DevopsEnviromentRepDTO> listDeployed(Long projectId) {
        List<Long> envList = devopsServiceRepository.selectDeployedEnv();
        return listByProjectIdAndActive(projectId, true).stream().filter(t ->
                envList.contains(t.getId())).collect(Collectors.toList());
    }

    @Override
    public Boolean activeEnvironment(Long projectId, Long environmentId, Boolean active) {
        DevopsEnvironmentE devopsEnvironmentE = devopsEnviromentRepository.queryById(environmentId);

        List<Long> upgradeClusterList = envUtil.getUpdatedEnvList(envListener);
        List<Long> connectedClusterList = envUtil.getConnectedEnvList(envListener);
        setEnvStatus(connectedClusterList, upgradeClusterList, devopsEnvironmentE);
        if (!active && devopsEnvironmentE.getConnect()) {
            devopsEnvironmentValidator.checkEnvCanDisabled(environmentId);
        }
        List<DevopsEnvironmentE> devopsEnvironmentES = devopsEnviromentRepository
                .queryByprojectAndActive(projectId, true);
        devopsEnvironmentE.setActive(active);
        //??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        if (active) {
            DevopsEnvGroupE devopsEnvGroupE = devopsEnvGroupRepository.query(devopsEnvironmentE.getDevopsEnvGroupId());
            if (devopsEnvironmentE.getDevopsEnvGroupId() == null || devopsEnvGroupE == null) {
                devopsEnvironmentE.setDevopsEnvGroupId(null);
                devopsEnviromentRepository.update(devopsEnvironmentE);
                devopsEnvironmentE.initSequence(devopsEnvironmentES.stream().filter(devopsEnvironmentE1 ->
                        devopsEnvironmentE1.getDevopsEnvGroupId() == null).collect(Collectors.toList()));
            } else {
                devopsEnvironmentE.initSequence(devopsEnvironmentES.stream().filter(devopsEnvironmentE1 ->
                        (devopsEnvironmentE.getDevopsEnvGroupId()).equals(devopsEnvironmentE1.getDevopsEnvGroupId()))
                        .collect(Collectors.toList()));
            }
        } else {
            // ???????????????????????????????????????sequence????????????
            List<Long> environmentIds;
            if (devopsEnvironmentE.getDevopsEnvGroupId() == null) {
                environmentIds = devopsEnvironmentES.stream()
                        .filter(devopsEnvironmentE1 -> devopsEnvironmentE1.getDevopsEnvGroupId() == null)
                        .sorted(Comparator.comparing(DevopsEnvironmentE::getSequence))
                        .collect(Collectors.toList()).stream()
                        .map(DevopsEnvironmentE::getId)
                        .collect(Collectors.toList());
            } else {
                environmentIds = devopsEnvironmentES.stream()
                        .filter(devopsEnvironmentE1 -> (devopsEnvironmentE.getDevopsEnvGroupId())
                                .equals(devopsEnvironmentE1.getDevopsEnvGroupId()))
                        .sorted(Comparator.comparing(DevopsEnvironmentE::getSequence))
                        .collect(Collectors.toList()).stream()
                        .map(DevopsEnvironmentE::getId)
                        .collect(Collectors.toList());
            }
            environmentIds.remove(environmentId);
            Long[] ids = new Long[environmentIds.size()];
            sort(environmentIds.toArray(ids));
        }
        devopsEnviromentRepository.update(devopsEnvironmentE);
        return true;
    }

    @Override
    public DevopsEnvironmentUpdateDTO query(Long environmentId) {
        return ConvertHelper.convert(devopsEnviromentRepository
                .queryById(environmentId), DevopsEnvironmentUpdateDTO.class);
    }

    @Override
    public DevopsEnvironmentUpdateDTO update(DevopsEnvironmentUpdateDTO devopsEnvironmentUpdateDTO, Long projectId) {
        DevopsEnvironmentE devopsEnvironmentE = ConvertHelper.convert(
                devopsEnvironmentUpdateDTO, DevopsEnvironmentE.class);
        Long clusterId = devopsEnviromentRepository.queryById(devopsEnvironmentUpdateDTO.getId()).getClusterE().getId();
        devopsEnvironmentE.initDevopsClusterEById(clusterId);
        devopsEnvironmentE.initProjectE(projectId);
        List<DevopsEnvironmentE> devopsEnvironmentES = devopsEnviromentRepository
                .queryByprojectAndActive(projectId, true);
        DevopsEnvironmentE beforeDevopsEnvironmentE = devopsEnviromentRepository
                .queryById(devopsEnvironmentUpdateDTO.getId());
        List<Long> ids = new ArrayList<>();
        //????????????,?????????????????????,??????????????????sequence????????????,????????????????????????????????????sequence??????
        if (devopsEnvironmentUpdateDTO.getDevopsEnvGroupId() != null) {
            if (!devopsEnvironmentUpdateDTO.getDevopsEnvGroupId().equals(beforeDevopsEnvironmentE.getDevopsEnvGroupId())) {
                if (beforeDevopsEnvironmentE.getDevopsEnvGroupId() == null) {
                    ids = devopsEnvironmentES.stream().filter(devopsEnvironmentE1 ->
                            devopsEnvironmentE1.getDevopsEnvGroupId() == null)
                            .sorted(Comparator.comparing(DevopsEnvironmentE::getSequence)).map(
                                    DevopsEnvironmentE::getId)
                            .collect(Collectors.toList());
                }
                //????????????,?????????????????????,????????????????????????sequence????????????,????????????????????????????????????sequence??????
                else if (beforeDevopsEnvironmentE.getDevopsEnvGroupId() != null) {
                    ids = devopsEnvironmentES.stream().filter(devopsEnvironmentE1 ->
                            beforeDevopsEnvironmentE.getDevopsEnvGroupId()
                                    .equals(devopsEnvironmentE1.getDevopsEnvGroupId()))
                            .sorted(Comparator.comparing(DevopsEnvironmentE::getSequence)).map(
                                    DevopsEnvironmentE::getId)
                            .collect(Collectors.toList());
                }
                ids.remove(devopsEnvironmentUpdateDTO.getId());
                sort(ids.toArray(new Long[ids.size()]));
                devopsEnvironmentE.initSequence(devopsEnvironmentES.stream().filter(devopsEnvironmentE1 ->
                        (devopsEnvironmentUpdateDTO.getDevopsEnvGroupId())
                                .equals(devopsEnvironmentE1.getDevopsEnvGroupId())).collect(Collectors.toList()));
            }
        } else {
            //????????????,?????????????????????
            if (beforeDevopsEnvironmentE.getDevopsEnvGroupId() != null) {
                ids = devopsEnvironmentES.stream().filter(devopsEnvironmentE1 ->
                        beforeDevopsEnvironmentE.getDevopsEnvGroupId()
                                .equals(devopsEnvironmentE1.getDevopsEnvGroupId()))
                        .sorted(Comparator.comparing(DevopsEnvironmentE::getSequence)).map(
                                DevopsEnvironmentE::getId)
                        .collect(Collectors.toList());
                ids.remove(devopsEnvironmentUpdateDTO.getId());
                //????????????????????????sequence????????????
                sort(ids.toArray(new Long[ids.size()]));
                //??????????????????????????????sequence??????
                devopsEnvironmentE.initSequence(devopsEnvironmentES.stream().filter(devopsEnvironmentE1 ->
                        devopsEnvironmentE1.getDevopsEnvGroupId() == null).collect(Collectors.toList()));
            }
        }
        if (devopsEnvironmentUpdateDTO.getClusterId() != null && !devopsEnvironmentUpdateDTO.getClusterId()
                .equals(beforeDevopsEnvironmentE.getClusterE().getId())) {
            deployService.initCluster(devopsEnvironmentUpdateDTO.getClusterId());
        }
        return ConvertHelper.convert(devopsEnviromentRepository.update(
                devopsEnvironmentE), DevopsEnvironmentUpdateDTO.class);
    }

    @Override
    public DevopsEnvGroupEnvsDTO sort(Long[] environmentIds) {
        DevopsEnvGroupEnvsDTO devopsEnvGroupEnvsDTO = new DevopsEnvGroupEnvsDTO();
        List<DevopsEnvironmentE> devopsEnvironmentES = Arrays.stream(environmentIds)
                .map(id -> devopsEnviromentRepository.queryById(id))
                .collect(Collectors.toList());
        long sequence = 1L;
        for (DevopsEnvironmentE devopsEnvironmentE : devopsEnvironmentES) {
            devopsEnvironmentE.setSequence(sequence);
            devopsEnviromentRepository.update(devopsEnvironmentE);
            sequence = sequence + 1;
        }
        List<Long> connectedEnvList = envUtil.getConnectedEnvList(envListener);
        List<Long> upgradeClusterList = envUtil.getUpdatedEnvList(envListener);

        devopsEnvironmentES.forEach(t ->
                setEnvStatus(connectedEnvList, upgradeClusterList, t)
        );
        if (!devopsEnvironmentES.isEmpty()) {
            DevopsEnvGroupE devopsEnvGroupE = new DevopsEnvGroupE();
            if (devopsEnvironmentES.get(0).getDevopsEnvGroupId() != null) {
                devopsEnvGroupE = devopsEnvGroupRepository.query(devopsEnvironmentES.get(0).getDevopsEnvGroupId());
            }
            devopsEnvGroupEnvsDTO.setDevopsEnviromentRepDTOs(ConvertHelper.convertList(devopsEnvironmentES,
                    DevopsEnviromentRepDTO.class));
            devopsEnvGroupEnvsDTO.setDevopsEnvGroupName(devopsEnvGroupE.getName());
            devopsEnvGroupEnvsDTO.setDevopsEnvGroupId(devopsEnvGroupE.getId());
        }
        return devopsEnvGroupEnvsDTO;
    }

    private void setEnvStatus(List<Long> connectedEnvList, List<Long> upgradeEnvList, DevopsEnvironmentE t) {
        if (connectedEnvList.contains(t.getClusterE().getId()) && upgradeEnvList.contains(t.getClusterE().getId())) {
            t.initConnect(true);
        } else {
            t.initConnect(false);
        }
    }

    @Override
    public void checkCode(Long projectId, Long clusterId, String code) {
        DevopsEnvironmentE devopsEnvironmentE = DevopsEnvironmentFactory.createDevopsEnvironmentE();
        DevopsClusterE devopsClusterE = devopsClusterRepository.query(clusterId);
        devopsEnvironmentE.initProjectE(projectId);
        devopsEnvironmentE.initDevopsClusterEById(clusterId);
        devopsEnvironmentE.setCode(code);
        if (devopsClusterE.getNamespaces() != null) {
            JSONArray.parseArray(devopsClusterE.getNamespaces(), String.class).forEach(namespace -> {
                if (namespace.equals(code)) {
                    throw new CommonException("error.code.exist");
                }
            });
        }
        devopsEnviromentRepository.checkCode(devopsEnvironmentE);
    }

    @Override
    public List<DevopsEnviromentRepDTO> listByProjectId(Long projectId, Long appId) {
        List<DevopsEnviromentRepDTO> devopsEnviromentRepDTOList = listByProjectIdAndActive(projectId, true);

        if (appId == null) {
            return devopsEnviromentRepDTOList.stream().filter(t ->
                    applicationInstanceRepository.selectByEnvId(t.getId()).stream()
                            .anyMatch(applicationInstanceE ->
                                    applicationInstanceE.getStatus().equals(InstanceStatus.RUNNING.getStatus())))
                    .collect(Collectors.toList());
        } else {
            return devopsEnviromentRepDTOList.stream().filter(t ->
                    applicationInstanceRepository.selectByEnvId(t.getId()).stream()
                            .anyMatch(applicationInstanceE ->
                                    applicationInstanceE.getStatus().equals(InstanceStatus.RUNNING.getStatus())
                                            && applicationInstanceE.getApplicationE().getId().equals(appId)))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void handleCreateEnvSaga(GitlabProjectPayload gitlabProjectPayload) {
        GitlabGroupE gitlabGroupE = devopsProjectRepository.queryByEnvGroupId(
                TypeUtil.objToInteger(gitlabProjectPayload.getGroupId()));
        DevopsEnvironmentE devopsEnvironmentE = devopsEnviromentRepository
                .queryByClusterIdAndCode(gitlabProjectPayload.getClusterId(), gitlabProjectPayload.getPath());
        ProjectE projectE = iamRepository.queryIamProject(gitlabGroupE.getProjectE().getId());
        Organization organization = iamRepository.queryOrganizationById(projectE.getOrganization().getId());

        GitlabProjectDO gitlabProjectDO = gitlabRepository.getProjectByName(organization.getCode()
                + "-" + projectE.getCode() + "-gitops", devopsEnvironmentE.getGitlabEnvProjectPath(), gitlabProjectPayload.getUserId());
        if (gitlabProjectDO.getId() == null) {
            gitlabProjectDO = gitlabRepository.createProject(
                    gitlabProjectPayload.getGroupId(),
                    devopsEnvironmentE.getGitlabEnvProjectPath(),
                    gitlabProjectPayload.getUserId(),
                    false);
        }
        devopsEnvironmentE.initGitlabEnvProjectId(TypeUtil.objToLong(gitlabProjectDO.getId()));
        if (gitlabRepository.getDeployKeys(gitlabProjectDO.getId(), gitlabProjectPayload.getUserId()).isEmpty()) {
            gitlabRepository.createDeployKey(
                    gitlabProjectDO.getId(),
                    devopsEnvironmentE.getGitlabEnvProjectPath(),
                    devopsEnvironmentE.getEnvIdRsaPub(),
                    true,
                    gitlabProjectPayload.getUserId());
        }
        ProjectHook projectHook = ProjectHook.allHook();
        projectHook.setEnableSslVerification(true);
        projectHook.setProjectId(gitlabProjectDO.getId());
        projectHook.setToken(devopsEnvironmentE.getToken());
        String uri = !gatewayUrl.endsWith("/") ? gatewayUrl + "/" : gatewayUrl;
        uri += "devops/webhook/git_ops";
        projectHook.setUrl(uri);
        List<ProjectHook> projectHooks = gitlabRepository.getHooks(gitlabProjectDO.getId(),
                gitlabProjectPayload.getUserId());
        if (projectHooks == null) {
            devopsEnvironmentE.initHookId(TypeUtil.objToLong(gitlabRepository.createWebHook(
                    gitlabProjectDO.getId(), gitlabProjectPayload.getUserId(), projectHook).getId()));
        } else {
            devopsEnvironmentE.initHookId(TypeUtil.objToLong(projectHooks.get(0).getId()));
        }
        if (!gitlabRepository.getFile(gitlabProjectDO.getId(), MASTER, README)) {
            gitlabRepository.createFile(gitlabProjectDO.getId(),
                    README, README_CONTENT, "ADD README", gitlabProjectPayload.getUserId());
        }
        // ?????????????????????????????????????????????gitlab?????????devops?????????????????????
        initUserPermissionWhenCreatingEnv(gitlabProjectPayload, devopsEnvironmentE.getId(),
                TypeUtil.objToLong(gitlabProjectDO.getId()), projectE.getId());
        devopsEnvironmentE.initSynchro(true);
        devopsEnviromentRepository.update(devopsEnvironmentE);
    }

    private void initUserPermissionWhenCreatingEnv(GitlabProjectPayload gitlabProjectPayload, Long envId,
                                                   Long gitlabProjectId, Long projectId) {

        List<Long> userIds = gitlabProjectPayload.getUserIds();
        // ?????????????????????????????????
        Page<UserDTO> allProjectMemberPage = getMembersFromProject(new PageRequest(), projectId, "");
        // ?????????????????????????????????
        if (userIds != null && !userIds.isEmpty()) {
            allProjectMemberPage.getContent().stream().filter(e -> userIds.contains(e.getId())).forEach(e -> {
                Long userId = e.getId();
                String loginName = e.getLoginName();
                String realName = e.getRealName();
                UserAttrE userAttrE = userAttrRepository.queryById(userId);
                Long gitlabUserId = userAttrE.getGitlabUserId();

                // ???????????????????????????????????????(?????????????????????????????????????????????)?????????gitlab????????????
                GitlabMemberE gitlabMemberE = gitlabProjectRepository.getProjectMember(gitlabProjectId.intValue(), TypeUtil.objToInteger(gitlabUserId));
                if (gitlabMemberE == null || gitlabMemberE.getId() == null) {
                    MemberDTO memberDTO = new MemberDTO(TypeUtil.objToInteger(gitlabUserId), 40, "");
                    gitlabRepository.addMemberIntoProject(TypeUtil.objToInteger(gitlabProjectId), memberDTO);
                }
                // ??????devops???????????????
                devopsEnvUserPermissionRepository
                        .create(new DevopsEnvUserPermissionE(loginName, userId, realName, envId, true));
            });
        }
    }

    @Override
    public EnvSyncStatusDTO queryEnvSyncStatus(Long projectId, Long envId) {
        ProjectE projectE = iamRepository.queryIamProject(projectId);
        Organization organization = iamRepository.queryOrganizationById(projectE.getOrganization().getId());
        DevopsEnvironmentE devopsEnvironmentE = devopsEnviromentRepository.queryById(envId);
        EnvSyncStatusDTO envSyncStatusDTO = new EnvSyncStatusDTO();
        if (devopsEnvironmentE.getAgentSyncCommit() != null) {
            envSyncStatusDTO.setAgentSyncCommit(devopsEnvCommitRepository
                    .query(devopsEnvironmentE.getAgentSyncCommit()).getCommitSha());
        }
        if (devopsEnvironmentE.getDevopsSyncCommit() != null) {
            envSyncStatusDTO.setDevopsSyncCommit(devopsEnvCommitRepository
                    .query(devopsEnvironmentE.getDevopsSyncCommit())
                    .getCommitSha());
        }
        if (devopsEnvironmentE.getSagaSyncCommit() != null) {
            envSyncStatusDTO.setSagaSyncCommit(devopsEnvCommitRepository
                    .query(devopsEnvironmentE.getSagaSyncCommit()).getCommitSha());
        }

        gitlabUrl = gitlabUrl.endsWith("/") ? gitlabUrl.substring(0, gitlabUrl.length() - 1) : gitlabUrl;
        envSyncStatusDTO.setCommitUrl(String.format("%s/%s-%s-gitops/%s/commit/",
                gitlabUrl, organization.getCode(), projectE.getCode(),
                devopsEnvironmentE.getGitlabEnvProjectPath()));
        return envSyncStatusDTO;
    }

    @Override
    public Page<DevopsEnvUserPermissionDTO> listUserPermissionByEnvId(Long projectId, PageRequest pageRequest,
                                                                      String searchParams, Long envId) {
        if (envId == null) {
            // ??????????????????id????????????????????????????????????
            Page<UserDTO> allProjectMemberPage = getMembersFromProject(pageRequest, projectId, searchParams);

            List<DevopsEnvUserPermissionDTO> allProjectMemberList = new ArrayList<>();
            Page<DevopsEnvUserPermissionDTO> devopsEnvUserPermissionDTOPage = new Page<>();
            allProjectMemberPage.getContent().forEach(e -> {
                DevopsEnvUserPermissionDTO devopsEnvUserPermissionDTO = new DevopsEnvUserPermissionDTO();
                devopsEnvUserPermissionDTO.setIamUserId(e.getId());
                devopsEnvUserPermissionDTO.setLoginName(e.getLoginName());
                devopsEnvUserPermissionDTO.setRealName(e.getRealName());
                allProjectMemberList.add(devopsEnvUserPermissionDTO);
            });
            BeanUtils.copyProperties(allProjectMemberPage, devopsEnvUserPermissionDTOPage);
            devopsEnvUserPermissionDTOPage.setContent(allProjectMemberList);
            return devopsEnvUserPermissionDTOPage;
        } else {
            List<DevopsEnvUserPermissionDTO> retureUsersDTOList = new ArrayList<>();
            // ????????????????????????iam?????????????????????????????????iam?????????????????????????????????????????????????????????????????????
            Page<UserDTO> allProjectMemberPage = getMembersFromProject(pageRequest, projectId, searchParams);
            allProjectMemberPage.getContent().forEach(e -> {
                DevopsEnvUserPermissionDTO devopsEnvUserPermissionDTO = new DevopsEnvUserPermissionDTO();
                devopsEnvUserPermissionDTO.setIamUserId(e.getId());
                devopsEnvUserPermissionDTO.setLoginName(e.getLoginName());
                devopsEnvUserPermissionDTO.setRealName(e.getRealName());
                retureUsersDTOList.add(devopsEnvUserPermissionDTO);
            });
            Page<DevopsEnvUserPermissionDTO> devopsEnvUserPermissionDTOPage = new Page<>();
            BeanUtils.copyProperties(allProjectMemberPage, devopsEnvUserPermissionDTOPage);
            devopsEnvUserPermissionDTOPage.setContent(retureUsersDTOList);
            return devopsEnvUserPermissionDTOPage;
        }
    }

    @Override
    public List<DevopsEnvUserPermissionDTO> listAllUserPermission(Long envId) {
        return devopsEnvUserPermissionRepository.listALlUserPermission(envId);
    }

    @Override
    public Boolean updateEnvUserPermission(Long envId, List<Long> userIds) {
        UpdateUserPermissionService updateEnvUserPermissionService = new UpdateEnvUserPermissionServiceImpl();
        return updateEnvUserPermissionService.updateUserPermission(null, envId, userIds, null);
    }

    private Page<UserDTO> getMembersFromProject(PageRequest pageRequest, Long projectId, String searchParams) {
        RoleAssignmentSearchDTO roleAssignmentSearchDTO = new RoleAssignmentSearchDTO();
        if (searchParams != null && !"".equals(searchParams)) {
            Map maps = gson.fromJson(searchParams, Map.class);
            Map<String, Object> searchParamMap = TypeUtil.cast(maps.get(TypeUtil.SEARCH_PARAM));
            String param = TypeUtil.cast(maps.get(TypeUtil.PARAM));
            roleAssignmentSearchDTO.setParam(new String[]{param});
            if (searchParamMap.get("loginName") != null) {
                String loginName = TypeUtil.objToString(searchParamMap.get("loginName"));
                String subLogin = loginName.substring(1, loginName.length() - 1);
                roleAssignmentSearchDTO.setLoginName(subLogin);
            }
            if (searchParamMap.get("realName") != null) {
                String realName = TypeUtil.objToString(searchParamMap.get("realName"));
                String subReal = realName.substring(1, realName.length() - 1);
                roleAssignmentSearchDTO.setRealName(subReal);
            }
        }
        // ???????????????????????????id?????????
        Long ownerId = iamRepository.queryRoleIdByCode(PROJECT_OWNER);
        // ??????????????????id
        Long memberId = iamRepository.queryRoleIdByCode(PROJECT_MEMBER);
        // ???????????????????????????????????????????????????????????????????????????
        Page<UserDTO> allMemberWithOtherUsersPage = iamRepository
                .pagingQueryUsersByRoleIdOnProjectLevel(pageRequest, roleAssignmentSearchDTO,
                        memberId, projectId, true);
        // ????????????????????????????????????????????????????????????
        if (allMemberWithOtherUsersPage.getContent().isEmpty()) {
            return allMemberWithOtherUsersPage;
        }
        // ?????????????????????
        Page<UserDTO> allOwnerUsersPage = iamRepository
                .pagingQueryUsersByRoleIdOnProjectLevel(new PageRequest(), roleAssignmentSearchDTO,
                        ownerId, projectId, false);
        // ???????????????????????????????????????????????????????????????????????????
        if (allOwnerUsersPage.getContent().isEmpty()) {
            return allMemberWithOtherUsersPage;
        } else {
            // ??????????????????????????????????????????????????????
            List<UserDTO> returnUserDTOList = allMemberWithOtherUsersPage.stream()
                    .filter(e -> !allOwnerUsersPage.getContent().contains(e)).collect(Collectors.toList());
            // ????????????????????????????????????
            allMemberWithOtherUsersPage.setContent(returnUserDTOList);
            return allMemberWithOtherUsersPage;
        }
    }

    private void setPermission(DevopsEnvironmentE devopsEnvironmentE, List<Long> permissionEnvIds,
                               Boolean isProjectOwner) {
        if (permissionEnvIds.contains(devopsEnvironmentE.getId()) || isProjectOwner) {
            devopsEnvironmentE.setPermission(true);
        } else {
            devopsEnvironmentE.setPermission(false);
        }
    }

    @Override
    @Transactional
    public void deleteDeactivatedEnvironment(Long envId) {
        DevopsEnvironmentE devopsEnvironmentE = devopsEnviromentRepository.queryById(envId);
        // ???????????????????????????
        applicationInstanceRepository.deleteAppInstanceByEnvId(envId);
        // ??????????????????????????????????????????
        devopsIngressRepository.deleteIngressAndIngressPathByEnvId(envId);
        // ??????????????????????????????????????????
        devopsServiceRepository.deleteServiceAndInstanceByEnvId(envId);
        // ????????????
        devopsEnviromentRepository.deleteById(envId);
        // ??????gitlab???, ??????????????????????????????
        if (devopsEnvironmentE.getGitlabEnvProjectId() != null) {
            Integer gitlabProjectId = TypeUtil.objToInt(devopsEnvironmentE.getGitlabEnvProjectId());
            GitlabProjectDO gitlabProjectDO = gitlabRepository.getProjectById(gitlabProjectId);
            if (gitlabProjectDO != null && gitlabProjectDO.getId() != null) {
                UserAttrE userAttrE = userAttrRepository.queryById(TypeUtil.objToLong(GitUserNameUtil.getUserId()));
                Integer gitlabUserId = TypeUtil.objToInt(userAttrE.getGitlabUserId());
                gitlabRepository.deleteProject(gitlabProjectId, gitlabUserId);
            }
        }
        // ????????????????????????
        if (devopsEnvironmentE.getClusterE().getId() != null) {
            deployService.deleteEnv(envId, devopsEnvironmentE.getCode(), devopsEnvironmentE.getClusterE().getId());
        }
    }

    @Override
    public List<DevopsClusterRepDTO> listDevopsCluster(Long projectId) {
        ProjectE projectE = iamRepository.queryIamProject(projectId);
        List<DevopsClusterRepDTO> devopsClusterRepDTOS = ConvertHelper.convertList(devopsClusterRepository.listByProjectId(projectId, projectE.getOrganization().getId()), DevopsClusterRepDTO.class);
        List<Long> connectedClusterList = envUtil.getConnectedEnvList(envListener);
        List<Long> upgradeClusterList = envUtil.getUpdatedEnvList(envListener);
        devopsClusterRepDTOS.forEach(t -> {
            if (connectedClusterList.contains(t.getId()) && upgradeClusterList.contains(t.getId())) {
                t.setConnect(true);
            } else {
                t.setConnect(false);
            }
        });
        return devopsClusterRepDTOS;
    }

    @Override
    @Saga(code = "devops-set-env-err",
            description = "devops??????????????????(devops set env status create err)", inputSchema = "{}")
    public void setEnvErrStatus(String data, Long projectId) {
        sagaClient.startSaga("devops-set-env-err", new StartInstanceDTO(data, "", "", ResourceLevel.PROJECT.value(), projectId));
    }

    @Override
    public DevopsEnviromentRepDTO queryByCode(Long clusterId, String code) {
        return ConvertHelper.convert(devopsEnviromentRepository.queryByClusterIdAndCode(clusterId, code), DevopsEnviromentRepDTO.class);
    }

    @Override
    public void initMockService(SagaClient sagaClient) {
        this.sagaClient = sagaClient;
    }
}
