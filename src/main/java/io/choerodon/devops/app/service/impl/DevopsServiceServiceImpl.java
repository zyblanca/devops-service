package io.choerodon.devops.app.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import io.choerodon.core.convertor.ConvertHelper;
import io.choerodon.core.convertor.ConvertPageHelper;
import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.api.dto.DevopsServiceDTO;
import io.choerodon.devops.api.dto.DevopsServiceReqDTO;
import io.choerodon.devops.api.validator.DevopsServiceValidator;
import io.choerodon.devops.app.service.DevopsEnvironmentService;
import io.choerodon.devops.app.service.DevopsServiceService;
import io.choerodon.devops.app.service.GitlabGroupMemberService;
import io.choerodon.devops.domain.application.entity.*;
import io.choerodon.devops.domain.application.handler.CheckOptionsHandler;
import io.choerodon.devops.domain.application.handler.ObjectOperation;
import io.choerodon.devops.domain.application.repository.*;
import io.choerodon.devops.domain.application.valueobject.DevopsServiceV;
import io.choerodon.devops.infra.common.util.EnvUtil;
import io.choerodon.devops.infra.common.util.GitUserNameUtil;
import io.choerodon.devops.infra.common.util.TypeUtil;
import io.choerodon.devops.infra.common.util.enums.CommandStatus;
import io.choerodon.devops.infra.common.util.enums.CommandType;
import io.choerodon.devops.infra.common.util.enums.ObjectType;
import io.choerodon.devops.infra.common.util.enums.ServiceStatus;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.websocket.helper.EnvListener;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.models.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by Zenger on 2018/4/13.
 */
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class DevopsServiceServiceImpl implements DevopsServiceService {

    public static final String ENDPOINTS = "Endpoints";
    public static final String LOADBALANCER = "LoadBalancer";
    public static final String SERVICE = "Service";
    public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";
    private static final String SERVICE_LABLE = "choerodon.io/network";
    private static final String SERVICE_LABLE_VALUE = "service";
    private Gson gson = new Gson();
    @Value("${services.gitlab.sshUrl}")
    private String gitlabSshUrl;

    @Autowired
    private DevopsServiceRepository devopsServiceRepository;
    @Autowired
    private DevopsEnvironmentRepository devopsEnviromentRepository;
    @Autowired
    private ApplicationInstanceRepository applicationInstanceRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private DevopsServiceInstanceRepository devopsServiceInstanceRepository;
    @Autowired
    private EnvListener envListener;
    @Autowired
    private EnvUtil envUtil;
    @Autowired
    private UserAttrRepository userAttrRepository;
    @Autowired
    private DevopsEnvironmentRepository environmentRepository;
    @Autowired
    private DevopsEnvFileResourceRepository devopsEnvFileResourceRepository;
    @Autowired
    private GitlabRepository gitlabRepository;
    @Autowired
    private GitlabGroupMemberService gitlabGroupMemberService;
    @Autowired
    private DevopsEnvironmentService devopsEnvironmentService;
    @Autowired
    private DevopsEnvCommandRepository devopsEnvCommandRepository;
    @Autowired
    private DevopsEnvUserPermissionRepository devopsEnvUserPermissionRepository;
    @Autowired
    private CheckOptionsHandler checkOptionsHandler;


    @Override
    public Boolean checkName(Long envId, String name) {
        return devopsServiceRepository.checkName(envId, name);
    }


    @Override
    public Page<DevopsServiceDTO> listByEnv(Long projectId, Long envId, PageRequest pageRequest, String searchParam) {
        Page<DevopsServiceV> devopsServiceByPage = devopsServiceRepository.listDevopsServiceByPage(
                projectId, envId, pageRequest, searchParam);
        List<Long> connectedEnvList = envUtil.getConnectedEnvList(envListener);
        List<Long> updatedEnvList = envUtil.getUpdatedEnvList(envListener);
        devopsServiceByPage.forEach(devopsServiceV -> {
            DevopsEnvironmentE devopsEnvironmentE = devopsEnviromentRepository.queryById(devopsServiceV.getEnvId());
            if (connectedEnvList.contains(devopsEnvironmentE.getClusterE().getId())
                    && updatedEnvList.contains(devopsEnvironmentE.getClusterE().getId())) {
                devopsServiceV.setEnvStatus(true);
            }
        });
        return ConvertPageHelper.convertPage(devopsServiceByPage, DevopsServiceDTO.class);
    }

    @Override
    public DevopsServiceDTO queryByName(Long envId, String serviceName) {
        return ConvertHelper.convert(devopsServiceRepository.selectByNameAndEnvId(serviceName,envId), DevopsServiceDTO.class);
    }

    @Override
    public List<DevopsServiceDTO> listDevopsService(Long envId) {
        return ConvertHelper.convertList(
                devopsServiceRepository.listDevopsService(envId), DevopsServiceDTO.class);
    }

    @Override
    public DevopsServiceDTO query(Long id) {
        List<DevopsServiceAppInstanceE> devopsServiceAppInstanceES = devopsServiceInstanceRepository.selectByServiceId(id);
        //???????????????????????????????????????????????????????????????
        if (!devopsServiceAppInstanceES.isEmpty()) {
            for (DevopsServiceAppInstanceE devopsServiceAppInstanceE : devopsServiceAppInstanceES) {
                ApplicationInstanceE applicationInstanceE = applicationInstanceRepository.selectById(devopsServiceAppInstanceE.getAppInstanceId());
                if (applicationInstanceE != null) {
                    ApplicationE applicationE = applicationRepository.query(applicationInstanceE.getApplicationE().getId());
                    DevopsServiceV devopsServiceV = devopsServiceRepository.selectById(id);
                    devopsServiceV.setAppId(applicationE.getId());
                    devopsServiceV.setAppName(applicationE.getName());
                    devopsServiceV.setAppProjectId(applicationE.getProjectE().getId());
                    return ConvertHelper.convert(devopsServiceV, DevopsServiceDTO.class);
                }
            }
        }
        return ConvertHelper.convert(devopsServiceRepository.selectById(id), DevopsServiceDTO.class);
    }

    @Override
    public Boolean insertDevopsService(Long projectId, DevopsServiceReqDTO devopsServiceReqDTO) {

        //????????????????????????????????????
        devopsEnvUserPermissionRepository.checkEnvDeployPermission(TypeUtil.objToLong(GitUserNameUtil.getUserId()), devopsServiceReqDTO.getEnvId());

        DevopsEnvironmentE devopsEnvironmentE = devopsEnviromentRepository.queryById(devopsServiceReqDTO.getEnvId());
        //????????????????????????
        envUtil.checkEnvConnection(devopsEnvironmentE.getClusterE().getId(), envListener);

        List<DevopsServiceAppInstanceE> devopsServiceAppInstanceES = new ArrayList<>();
        List<String> beforeDevopsServiceAppInstanceES = new ArrayList<>();
        //????????????service????????????
        DevopsServiceE devopsServiceE = handlerCreateService(devopsServiceReqDTO, projectId, devopsServiceAppInstanceES, beforeDevopsServiceAppInstanceES);

        DevopsEnvCommandE devopsEnvCommandE = initDevopsEnvCommandE(CREATE);

        //?????????V1Service??????
        V1Service v1Service = initV1Service(
                devopsServiceReqDTO,
                gson.fromJson(devopsServiceE.getAnnotations(), Map.class));
        V1Endpoints v1Endpoints = null;
        if (devopsServiceReqDTO.getEndPoints() != null) {
            v1Endpoints = initV1EndPoints(devopsServiceReqDTO);
        }
        //???gitops?????????service??????
        operateEnvGitLabFile(v1Service, v1Endpoints, true, devopsServiceE, devopsServiceAppInstanceES, beforeDevopsServiceAppInstanceES, devopsEnvCommandE);
        return true;
    }

    @Override
    public Boolean insertDevopsServiceByGitOps(Long projectId, DevopsServiceReqDTO devopsServiceReqDTO, Long userId) {
        //????????????????????????
        DevopsEnvironmentE devopsEnvironmentE = devopsEnviromentRepository.queryById(devopsServiceReqDTO.getEnvId());

        envUtil.checkEnvConnection(devopsEnvironmentE.getClusterE().getId(), envListener);
        List<DevopsServiceAppInstanceE> devopsServiceAppInstanceES = new ArrayList<>();
        List<String> beforeDevopsServiceAppInstanceES = new ArrayList<>();

        //????????????service????????????
        DevopsServiceE devopsServiceE = handlerCreateService(devopsServiceReqDTO, projectId, devopsServiceAppInstanceES, beforeDevopsServiceAppInstanceES);

        DevopsEnvCommandE devopsEnvCommandE = initDevopsEnvCommandE(CREATE);

        //??????service??????????????????
        devopsServiceE = devopsServiceRepository.insert(devopsServiceE);

        //??????service???instance????????????????????????
        Long serviceEId = devopsServiceE.getId();
        devopsEnvCommandE.setObjectId(serviceEId);
        devopsEnvCommandE.setCreatedBy(userId);
        devopsServiceE.setCommandId(devopsEnvCommandRepository.create(devopsEnvCommandE).getId());
        devopsServiceRepository.update(devopsServiceE);

        devopsServiceAppInstanceES.forEach(devopsServiceAppInstanceE -> {
            devopsServiceAppInstanceE.setServiceId(serviceEId);
            devopsServiceInstanceRepository.insert(devopsServiceAppInstanceE);
        });
        return true;
    }

    private DevopsServiceE handlerCreateService(DevopsServiceReqDTO devopsServiceReqDTO, Long projectId, List<DevopsServiceAppInstanceE> devopsServiceAppInstanceES, List<String> beforeDevopsServiceAppInstanceES) {

        //??????service????????????
        DevopsServiceValidator.checkService(devopsServiceReqDTO);

        initDevopsServicePorts(devopsServiceReqDTO);

        DevopsEnvironmentE devopsEnvironmentE =
                devopsEnviromentRepository.queryById(devopsServiceReqDTO.getEnvId());
        if (!devopsServiceRepository.checkName(devopsEnvironmentE.getId(), devopsServiceReqDTO.getName())) {
            throw new CommonException("error.service.name.exist");
        }

        //?????????DevopsService??????
        DevopsServiceE devopsServiceE = new DevopsServiceE();
        BeanUtils.copyProperties(devopsServiceReqDTO, devopsServiceE);
        return initDevopsService(devopsServiceE, devopsServiceReqDTO, devopsServiceAppInstanceES, beforeDevopsServiceAppInstanceES);

    }

    private DevopsServiceE initDevopsService(DevopsServiceE devopsServiceE, DevopsServiceReqDTO devopsServiceReqDTO, List<DevopsServiceAppInstanceE> devopsServiceAppInstanceES, List<String> beforeDevopsServiceAppInstanceES) {
        devopsServiceE.setAppId(devopsServiceReqDTO.getAppId());
        ApplicationE applicationE = applicationRepository.query(devopsServiceReqDTO.getAppId());
        if (devopsServiceReqDTO.getLabel() != null) {
            if (devopsServiceReqDTO.getLabel().size() == 1 && devopsServiceReqDTO.getLabel().containsKey(SERVICE_LABLE)) {
                devopsServiceRepository.setLablesToNull(devopsServiceE.getId());
                devopsServiceE.setLabels(null);
            } else {
                devopsServiceReqDTO.getLabel().remove(SERVICE_LABLE);
                devopsServiceE.setLabels(gson.toJson(devopsServiceReqDTO.getLabel()));
            }
        } else {
            devopsServiceRepository.setLablesToNull(devopsServiceE.getId());
            devopsServiceE.setLabels(null);
        }
        if (devopsServiceReqDTO.getEndPoints() != null) {
            devopsServiceE.setEndPoints(gson.toJson(devopsServiceReqDTO.getEndPoints()));
        } else {
            devopsServiceRepository.setEndPointToNull(devopsServiceE.getId());
            devopsServiceE.setEndPoints(null);
        }
        devopsServiceE.setPorts(devopsServiceReqDTO.getPorts());
        devopsServiceE.setType(devopsServiceReqDTO.getType() == null ? "ClusterIP" : devopsServiceReqDTO.getType());
        devopsServiceE.setExternalIp(devopsServiceReqDTO.getExternalIp());

        String serviceInstances = updateServiceInstanceAndGetCode(devopsServiceReqDTO, devopsServiceAppInstanceES, beforeDevopsServiceAppInstanceES);
        Map<String, String> annotations = new HashMap<>();
        if (!serviceInstances.isEmpty()) {
            annotations.put("choerodon.io/network-service-instances", serviceInstances);
            annotations.put("choerodon.io/network-service-app", applicationE.getCode());
        }

        devopsServiceE.setAnnotations(gson.toJson(annotations));
        devopsServiceE.setStatus(ServiceStatus.OPERATIING.getStatus());

        return devopsServiceE;

    }

    private DevopsServiceE handlerUpdateService(DevopsServiceReqDTO devopsServiceReqDTO, DevopsServiceE devopsServiceE, List<DevopsServiceAppInstanceE> devopsServiceAppInstanceES, List<String> beforeDevopsServiceAppInstanceES) {
        //service????????????
        DevopsServiceValidator.checkService(devopsServiceReqDTO);
        initDevopsServicePorts(devopsServiceReqDTO);

        if (!devopsServiceE.getEnvId().equals(devopsServiceReqDTO.getEnvId())) {
            throw new CommonException("error.env.notEqual");
        }
        String serviceName = devopsServiceReqDTO.getName();
        if (!serviceName.equals(devopsServiceE.getName())) {
            throw new CommonException("error.name.notEqual");
        }
        //???????????????????????????
        List<DevopsServiceAppInstanceE> devopsServiceInstanceEList =
                devopsServiceInstanceRepository.selectByServiceId(devopsServiceE.getId());
        //??????????????????????????????
        List<PortMapE> oldPort = devopsServiceE.getPorts();
        boolean isUpdate = false;
        if (devopsServiceReqDTO.getAppId() != null && devopsServiceE.getAppId() != null && devopsServiceReqDTO.getAppInstance() != null) {
            isUpdate = !devopsServiceReqDTO.getAppInstance().stream()
                    .sorted().collect(Collectors.toList())
                    .equals(devopsServiceInstanceEList.stream()
                            .map(DevopsServiceAppInstanceE::getAppInstanceId).sorted()
                            .collect(Collectors.toList()));
        }
        if ((devopsServiceReqDTO.getAppId() == null && devopsServiceE.getAppId() != null) || (devopsServiceReqDTO.getAppId() != null && devopsServiceE.getAppId() == null)) {
            isUpdate = true;
        }
        if (devopsServiceReqDTO.getAppId() == null && devopsServiceE.getAppId() == null) {
            if (devopsServiceReqDTO.getLabel() != null && devopsServiceE.getLabels() != null) {
                if (!gson.toJson(devopsServiceReqDTO.getLabel()).equals(devopsServiceE.getLabels())) {
                    isUpdate = true;
                }
            } else if (devopsServiceReqDTO.getEndPoints() != null && devopsServiceE.getEndPoints() != null) {
                if (!gson.toJson(devopsServiceReqDTO.getEndPoints()).equals(devopsServiceE.getEndPoints())) {
                    isUpdate = true;
                }
            } else {
                isUpdate = true;
            }
        }
        if (!isUpdate && oldPort.stream().sorted().collect(Collectors.toList())
                .equals(devopsServiceReqDTO.getPorts().stream().sorted().collect(Collectors.toList()))
                && !isUpdateExternalIp(devopsServiceReqDTO, devopsServiceE)) {
            return null;
        }


        //?????????DevopsService??????
        return initDevopsService(devopsServiceE, devopsServiceReqDTO, devopsServiceAppInstanceES, beforeDevopsServiceAppInstanceES);
    }


    @Override
    public Boolean updateDevopsService(Long projectId, Long id,
                                       DevopsServiceReqDTO devopsServiceReqDTO) {


        //????????????????????????????????????
        devopsEnvUserPermissionRepository.checkEnvDeployPermission(TypeUtil.objToLong(GitUserNameUtil.getUserId()), devopsServiceReqDTO.getEnvId());

        //????????????????????????
        DevopsEnvironmentE devopsEnvironmentE = devopsEnviromentRepository.queryById(devopsServiceReqDTO.getEnvId());

        envUtil.checkEnvConnection(devopsEnvironmentE.getClusterE().getId(), envListener);


        //???????????????????????????gitops?????????????????????,???????????????????????????????????????gitops???????????????????????????
        checkOptionsHandler.check(devopsEnvironmentE, id, devopsServiceReqDTO.getName(), SERVICE);

        DevopsEnvCommandE devopsEnvCommandE = initDevopsEnvCommandE(UPDATE);

        //????????????service????????????
        List<DevopsServiceAppInstanceE> devopsServiceAppInstanceES = new ArrayList<>();
        List<String> beforeDevopsServiceAppInstanceES = devopsServiceInstanceRepository
                .selectByServiceId(id).stream().map(DevopsServiceAppInstanceE::getCode).collect(Collectors.toList());
        DevopsServiceE devopsServiceE = devopsServiceRepository.query(id);
        devopsServiceE = handlerUpdateService(devopsServiceReqDTO, devopsServiceE, devopsServiceAppInstanceES, beforeDevopsServiceAppInstanceES);
        V1Endpoints v1Endpoints = null;
        if (devopsServiceE == null) {
            return false;
        } else {
            //?????????V1Service??????
            V1Service v1Service = initV1Service(
                    devopsServiceReqDTO,
                    gson.fromJson(devopsServiceE.getAnnotations(), Map.class));
            if (devopsServiceReqDTO.getEndPoints() != null) {
                v1Endpoints = initV1EndPoints(devopsServiceReqDTO);
            }
            //???gitops?????????service??????
            operateEnvGitLabFile(v1Service, v1Endpoints, false, devopsServiceE, devopsServiceAppInstanceES, beforeDevopsServiceAppInstanceES, devopsEnvCommandE);
        }
        return true;
    }


    @Override
    public Boolean updateDevopsServiceByGitOps(Long projectId, Long id,
                                               DevopsServiceReqDTO devopsServiceReqDTO, Long userId) {
        //????????????????????????
        DevopsEnvironmentE devopsEnvironmentE = devopsEnviromentRepository.queryById(devopsServiceReqDTO.getEnvId());

        envUtil.checkEnvConnection(devopsEnvironmentE.getClusterE().getId(), envListener);


        DevopsEnvCommandE devopsEnvCommandE = initDevopsEnvCommandE(UPDATE);

        //????????????service????????????
        List<DevopsServiceAppInstanceE> devopsServiceAppInstanceES = new ArrayList<>();
        List<String> beforeDevopsServiceAppInstanceES = devopsServiceInstanceRepository
                .selectByServiceId(id).stream().map(DevopsServiceAppInstanceE::getCode).collect(Collectors.toList());
        DevopsServiceE devopsServiceE = devopsServiceRepository.query(id);
        devopsServiceE = handlerUpdateService(devopsServiceReqDTO, devopsServiceE, devopsServiceAppInstanceES, beforeDevopsServiceAppInstanceES);
        if (devopsServiceE == null) {
            return false;
        }
        //??????service??????????????????
        devopsEnvCommandE.setObjectId(id);
        devopsEnvCommandE.setCreatedBy(userId);
        devopsServiceE.setCommandId(devopsEnvCommandRepository.create(devopsEnvCommandE).getId());
        devopsServiceRepository.update(devopsServiceE);


        //??????service???instance??????????????????????????????
        beforeDevopsServiceAppInstanceES.forEach(instanceCode ->
                devopsServiceInstanceRepository.deleteByOptions(id, instanceCode)
        );
        devopsServiceAppInstanceES.forEach(devopsServiceAppInstanceE -> {
            devopsServiceAppInstanceE.setServiceId(id);
            devopsServiceInstanceRepository.insert(devopsServiceAppInstanceE);
        });


        return true;
    }

    @Override
    public void deleteDevopsService(Long id) {
        DevopsServiceE devopsServiceE = getDevopsServiceE(id);

        //????????????????????????????????????
        devopsEnvUserPermissionRepository.checkEnvDeployPermission(TypeUtil.objToLong(GitUserNameUtil.getUserId()), devopsServiceE.getEnvId());

        //????????????????????????
        DevopsEnvironmentE devopsEnvironmentE = environmentRepository.queryById(devopsServiceE.getEnvId());
        envUtil.checkEnvConnection(devopsEnvironmentE.getClusterE().getId(), envListener);

        UserAttrE userAttrE = userAttrRepository.queryById(TypeUtil.objToLong(GitUserNameUtil.getUserId()));
        //??????gitops?????????????????????????????????????????????gitops????????????
        gitlabGroupMemberService.checkEnvProject(devopsEnvironmentE, userAttrE);

        DevopsEnvCommandE devopsEnvCommandE = initDevopsEnvCommandE(DELETE);

        devopsEnvCommandE.setObjectId(id);
        devopsServiceE.setStatus(ServiceStatus.OPERATIING.getStatus());
        devopsServiceE.setCommandId(devopsEnvCommandRepository.create(devopsEnvCommandE).getId());
        devopsServiceRepository.update(devopsServiceE);

        //??????????????????????????????????????????????????????gitops?????????????????????????????????
        String path = envUtil.handDevopsEnvGitRepository(devopsEnvironmentE);

        //??????????????????????????????????????????????????????
        DevopsEnvFileResourceE devopsEnvFileResourceE = devopsEnvFileResourceRepository
                .queryByEnvIdAndResource(devopsEnvironmentE.getId(), id, SERVICE);
        if (devopsEnvFileResourceE == null) {
            devopsServiceRepository.delete(id);
            if (gitlabRepository.getFile(TypeUtil.objToInteger(devopsEnvironmentE.getGitlabEnvProjectId()), "master",
                    "svc-" + devopsServiceE.getName() + ".yaml")) {
                gitlabRepository.deleteFile(
                        TypeUtil.objToInteger(devopsEnvironmentE.getGitlabEnvProjectId()),
                        "svc-" + devopsServiceE.getName() + ".yaml",
                        "DELETE FILE",
                        TypeUtil.objToInteger(userAttrE.getGitlabUserId()));
            }
            return;
        }
        List<DevopsEnvFileResourceE> devopsEnvFileResourceES = devopsEnvFileResourceRepository.queryByEnvIdAndPath(devopsEnvironmentE.getId(), devopsEnvFileResourceE.getFilePath());

        //??????????????????????????????????????????????????????????????????,????????????????????????????????????????????????
        if (devopsEnvFileResourceES.size() == 1) {
            if (gitlabRepository.getFile(TypeUtil.objToInteger(devopsEnvironmentE.getGitlabEnvProjectId()), "master",
                    devopsEnvFileResourceE.getFilePath())) {
                gitlabRepository.deleteFile(
                        TypeUtil.objToInteger(devopsEnvironmentE.getGitlabEnvProjectId()),
                        devopsEnvFileResourceE.getFilePath(),
                        "DELETE FILE",
                        TypeUtil.objToInteger(userAttrE.getGitlabUserId()));
            }
        } else {
            ObjectOperation<V1Service> objectOperation = new ObjectOperation<>();
            V1Service v1Service = new V1Service();
            V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
            v1ObjectMeta.setName(devopsServiceE.getName());
            v1Service.setMetadata(v1ObjectMeta);
            objectOperation.setType(v1Service);
            Integer projectId = TypeUtil.objToInteger(devopsEnvironmentE.getGitlabEnvProjectId());
            objectOperation.operationEnvGitlabFile(
                    "release-" + devopsServiceE.getName(),
                    projectId,
                    DELETE,
                    userAttrE.getGitlabUserId(),
                    devopsServiceE.getId(), SERVICE, null, devopsEnvironmentE.getId(), path);
        }

    }


    @Override
    public void deleteDevopsServiceByGitOps(Long id) {
        DevopsServiceE devopsServiceE = getDevopsServiceE(id);
        //????????????????????????
        DevopsEnvironmentE devopsEnvironmentE = devopsEnviromentRepository.queryById(devopsServiceE.getEnvId());
        envUtil.checkEnvConnection(devopsEnvironmentE.getClusterE().getId(), envListener);

        DevopsEnvCommandE devopsEnvCommandE = devopsEnvCommandRepository.query(devopsServiceE.getCommandId());

        //????????????
        devopsEnvCommandE.setStatus(CommandStatus.SUCCESS.getStatus());
        devopsEnvCommandRepository.update(devopsEnvCommandE);
        devopsServiceRepository.delete(id);
    }

    /**
     * ????????????
     *
     * @param devopsServiceReqDTO ????????????
     * @return String
     */
    private String updateServiceInstanceAndGetCode(DevopsServiceReqDTO devopsServiceReqDTO,
                                                   List<DevopsServiceAppInstanceE> addDevopsServiceAppInstanceES,
                                                   List<String> beforedevopsServiceAppInstanceES) {
        StringBuilder stringBuffer = new StringBuilder();
        List<String> appInstances = devopsServiceReqDTO.getAppInstance();
        if (appInstances != null) {
            appInstances.forEach(appInstance -> {
                ApplicationInstanceE applicationInstanceE =
                        applicationInstanceRepository.selectByCode(appInstance, devopsServiceReqDTO.getEnvId());
                stringBuffer.append(appInstance).append("+");
                if (beforedevopsServiceAppInstanceES.contains(appInstance)) {
                    beforedevopsServiceAppInstanceES.remove(appInstance);
                    return;
                }
                DevopsServiceAppInstanceE devopsServiceAppInstanceE = new DevopsServiceAppInstanceE();
                if (applicationInstanceE != null) {
                    devopsServiceAppInstanceE.setAppInstanceId(applicationInstanceE.getId());
                }
                devopsServiceAppInstanceE.setCode(appInstance);
                addDevopsServiceAppInstanceES.add(devopsServiceAppInstanceE);
            });
        }
        String instancesCode = stringBuffer.toString();
        if (instancesCode.endsWith("+")) {
            return instancesCode.substring(0, stringBuffer.toString().lastIndexOf('+'));
        }
        return instancesCode;
    }


    /**
     * ??????k8s service???yaml??????
     */
    private V1Service initV1Service(DevopsServiceReqDTO devopsServiceReqDTO, Map<String, String> annotations) {
        V1Service service = new V1Service();
        service.setKind(SERVICE);
        service.setApiVersion("v1");
        V1ObjectMeta metadata = new V1ObjectMeta();
        metadata.setName(devopsServiceReqDTO.getName());
        metadata.setAnnotations(annotations);
        Map<String, String> label = new HashMap<>();
        label.put(SERVICE_LABLE, SERVICE_LABLE_VALUE);
        metadata.setLabels(label);
        service.setMetadata(metadata);

        V1ServiceSpec spec = new V1ServiceSpec();
        spec.setType(devopsServiceReqDTO.getType() == null ? "ClusterIP" : devopsServiceReqDTO.getType());
        spec.setSelector(devopsServiceReqDTO.getLabel());
        final Integer[] serialNumber = {0};
        List<V1ServicePort> ports = devopsServiceReqDTO.getPorts().stream()
                .map(t -> {
                    V1ServicePort v1ServicePort = new V1ServicePort();
                    if (t.getNodePort() != null) {
                        v1ServicePort.setNodePort(t.getNodePort().intValue());
                    }
                    if (t.getPort() != null) {
                        v1ServicePort.setPort(t.getPort().intValue());
                    }
                    if (t.getTargetPort() != null) {
                        v1ServicePort.setTargetPort(new IntOrString(t.getTargetPort()));
                    }
                    v1ServicePort.setName(t.getName() == null ? "http" + serialNumber[0]++ : t.getName());
                    v1ServicePort.setProtocol(t.getProtocol() == null ? "TCP" : t.getProtocol());
                    return v1ServicePort;
                }).collect(Collectors.toList());

        if (!StringUtils.isEmpty(devopsServiceReqDTO.getExternalIp())) {
            List<String> externalIps = new ArrayList<>(
                    Arrays.asList(devopsServiceReqDTO.getExternalIp().split(",")));
            spec.setExternalIPs(externalIps);
        }

        spec.setPorts(ports);
        spec.setSessionAffinity("None");
        service.setSpec(spec);

        return service;
    }

    private V1Endpoints initV1EndPoints(DevopsServiceReqDTO devopsServiceReqDTO) {
        V1Endpoints v1Endpoints = new V1Endpoints();
        v1Endpoints.setApiVersion("v1");
        v1Endpoints.setKind(ENDPOINTS);
        V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
        v1ObjectMeta.setName(devopsServiceReqDTO.getName());
        v1Endpoints.setMetadata(v1ObjectMeta);
        List<V1EndpointSubset> v1EndpointSubsets = new ArrayList<>();
        V1EndpointSubset v1EndpointSubset = new V1EndpointSubset();
        devopsServiceReqDTO.getEndPoints().forEach((key, value) -> {
            List<String> ips = Arrays.asList(key.split(","));
            v1EndpointSubset.setAddresses(ips.stream().map(ip -> {
                V1EndpointAddress v1EndpointAddress = new V1EndpointAddress();
                v1EndpointAddress.setIp(ip);
                return v1EndpointAddress;

            }).collect(Collectors.toList()));
            final Integer[] serialNumber = {0};
            v1EndpointSubset.setPorts(value.stream().map(port -> {
                V1EndpointPort v1EndpointPort = new V1EndpointPort();
                v1EndpointPort.setPort(port.getPort());
                v1EndpointPort.setName("http" + serialNumber[0]++);
                return v1EndpointPort;
            }).collect(Collectors.toList()));
            v1EndpointSubsets.add(v1EndpointSubset);
        });
        v1Endpoints.setSubsets(v1EndpointSubsets);
        return v1Endpoints;
    }


    /**
     * ????????????ip????????????
     */
    private Boolean isUpdateExternalIp(DevopsServiceReqDTO devopsServiceReqDTO, DevopsServiceE devopsServiceE) {
        return !((StringUtils.isEmpty(devopsServiceReqDTO.getExternalIp())
                && StringUtils.isEmpty(devopsServiceE.getExternalIp()))
                || (!StringUtils.isEmpty(devopsServiceReqDTO.getExternalIp())
                && !StringUtils.isEmpty(devopsServiceE.getExternalIp())
                && devopsServiceReqDTO.getExternalIp().equals(devopsServiceE.getExternalIp())));
    }

    /**
     * ??????????????????
     */
    private DevopsServiceE getDevopsServiceE(Long id) {
        DevopsServiceE devopsServiceE = devopsServiceRepository.query(id);
        if (devopsServiceE == null) {
            throw new CommonException("error.service.query");
        }
        return devopsServiceE;
    }

    /**
     * ????????????
     *
     * @param id ??????id
     * @return app
     */
    public ApplicationE getApplicationE(long id) {
        ApplicationE applicationE = applicationRepository.query(id);
        if (applicationE == null) {
            throw new CommonException("error.application.query");
        }
        return applicationE;
    }


    private void operateEnvGitLabFile(V1Service service, V1Endpoints v1Endpoints, Boolean isCreate,
                                      DevopsServiceE devopsServiceE,
                                      List<DevopsServiceAppInstanceE> devopsServiceAppInstanceES,
                                      List<String> beforeDevopsServiceAppInstanceES,
                                      DevopsEnvCommandE devopsEnvCommandE) {

        DevopsEnvironmentE devopsEnvironmentE =
                devopsEnviromentRepository.queryById(devopsServiceE.getEnvId());
        UserAttrE userAttrE = userAttrRepository.queryById(TypeUtil.objToLong(GitUserNameUtil.getUserId()));
        //??????gitops?????????????????????????????????????????????gitops????????????
        gitlabGroupMemberService.checkEnvProject(devopsEnvironmentE, userAttrE);

        //???????????????????????????
        if (isCreate) {
            Long serviceId = devopsServiceRepository.insert(devopsServiceE).getId();
            devopsEnvCommandE.setObjectId(serviceId);
            devopsServiceE.setId(serviceId);
            devopsServiceE.setCommandId(devopsEnvCommandRepository.create(devopsEnvCommandE).getId());
            devopsServiceRepository.update(devopsServiceE);
            if (beforeDevopsServiceAppInstanceES != null) {
                beforeDevopsServiceAppInstanceES.forEach(instanCode ->
                        devopsServiceInstanceRepository.deleteByOptions(serviceId, instanCode)
                );
            }
            devopsServiceAppInstanceES.forEach(devopsServiceAppInstanceE -> {
                devopsServiceAppInstanceE.setServiceId(serviceId);
                devopsServiceInstanceRepository.insert(devopsServiceAppInstanceE);
            });
        } else {
            devopsEnvCommandE.setObjectId(devopsServiceE.getId());
            devopsServiceE.setCommandId(devopsEnvCommandRepository.create(devopsEnvCommandE).getId());
            devopsServiceRepository.update(devopsServiceE);
            Long serviceId = devopsServiceE.getId();
            if (beforeDevopsServiceAppInstanceES != null) {
                beforeDevopsServiceAppInstanceES.forEach(instanceCode ->
                        devopsServiceInstanceRepository.deleteByOptions(serviceId, instanceCode)
                );
            }
            devopsServiceAppInstanceES.forEach(devopsServiceAppInstanceE -> {
                devopsServiceAppInstanceE.setServiceId(serviceId);
                devopsServiceInstanceRepository.insert(devopsServiceAppInstanceE);
            });
        }

        //??????????????????????????????????????????????????????gitops?????????????????????????????????
        String path = envUtil.handDevopsEnvGitRepository(devopsEnvironmentE);

        //????????????
        ObjectOperation<V1Service> objectOperation = new ObjectOperation<>();
        objectOperation.setType(service);
        objectOperation.operationEnvGitlabFile("svc-" + devopsServiceE.getName(), TypeUtil.objToInteger(devopsEnvironmentE.getGitlabEnvProjectId()), isCreate ? CREATE : UPDATE,
                userAttrE.getGitlabUserId(), devopsServiceE.getId(), SERVICE, v1Endpoints, devopsServiceE.getEnvId(), path);


    }


    private void initDevopsServicePorts(DevopsServiceReqDTO devopsServiceReqDTO) {
        final Integer[] serialNumber = {0};
        devopsServiceReqDTO.setPorts(devopsServiceReqDTO.getPorts().stream()
                .map(t -> {
                    PortMapE portMapE = new PortMapE();
                    portMapE.setNodePort(t.getNodePort());
                    portMapE.setPort(t.getPort());
                    portMapE.setTargetPort(t.getTargetPort());
                    portMapE.setName(t.getName() == null ? "http" + ++serialNumber[0] : t.getName());
                    portMapE.setProtocol(t.getProtocol() == null ? "TCP" : t.getProtocol());
                    return portMapE;
                })
                .collect(Collectors.toList()));
    }


    private DevopsEnvCommandE initDevopsEnvCommandE(String type) {
        DevopsEnvCommandE devopsEnvCommandE = new DevopsEnvCommandE();
        if (type.equals(CREATE)) {
            devopsEnvCommandE.setCommandType(CommandType.CREATE.getType());
        } else if (type.equals(UPDATE)) {
            devopsEnvCommandE.setCommandType(CommandType.UPDATE.getType());
        } else {
            devopsEnvCommandE.setCommandType(CommandType.DELETE.getType());
        }
        devopsEnvCommandE.setObject(ObjectType.SERVICE.getType());
        devopsEnvCommandE.setStatus(CommandStatus.OPERATING.getStatus());
        return devopsEnvCommandE;
    }
}
