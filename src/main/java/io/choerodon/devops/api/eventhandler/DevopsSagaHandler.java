package io.choerodon.devops.api.eventhandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.choerodon.asgard.saga.SagaDefinition;
import io.choerodon.asgard.saga.annotation.SagaTask;
import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.api.dto.*;
import io.choerodon.devops.api.validator.ApplicationValidator;
import io.choerodon.devops.app.service.ApplicationInstanceService;
import io.choerodon.devops.app.service.ApplicationService;
import io.choerodon.devops.app.service.ApplicationTemplateService;
import io.choerodon.devops.app.service.DevopsEnvironmentService;
import io.choerodon.devops.app.service.DevopsGitService;
import io.choerodon.devops.app.service.DevopsGitlabPipelineService;
import io.choerodon.devops.domain.application.entity.*;
import io.choerodon.devops.domain.application.entity.gitlab.GitlabGroupE;
import io.choerodon.devops.domain.application.entity.gitlab.GitlabMemberE;
import io.choerodon.devops.domain.application.event.*;
import io.choerodon.devops.domain.application.factory.ApplicationFactory;
import io.choerodon.devops.domain.application.repository.*;
import io.choerodon.devops.domain.application.valueobject.Organization;
import io.choerodon.devops.domain.service.UpdateUserPermissionService;
import io.choerodon.devops.domain.service.impl.UpdateAppUserPermissionServiceImpl;
import io.choerodon.devops.infra.common.util.GitUserNameUtil;
import io.choerodon.devops.infra.common.util.TypeUtil;
import io.choerodon.devops.infra.common.util.enums.AccessLevel;
import io.choerodon.devops.infra.common.util.enums.GitPlatformType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Creator: Runge
 * Date: 2018/7/27
 * Time: 10:06
 * Description: Saga msg by DevOps self
 */
@Component
public class DevopsSagaHandler {
    private Logger logger = LoggerFactory.getLogger(DevopsSagaHandler.class);
    private static final String TEMPLATE = "template";
    private static final String APPLICATION = "application";
    private static final String STATUS_FIN = "finished";
    private static final String STATUS_FAILED = "failed";
    private static final Logger LOGGER = LoggerFactory.getLogger(DevopsSagaHandler.class);

    private final Gson gson = new Gson();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final DevopsEnvironmentService devopsEnvironmentService;
    private final DevopsGitService devopsGitService;
    private final ApplicationTemplateService applicationTemplateService;
    private final ApplicationService applicationService;
    private final DevopsGitlabPipelineService devopsGitlabPipelineService;
    private final ApplicationRepository applicationRepository;
    private final ApplicationTemplateRepository applicationTemplateRepository;
    private final DevopsEnvironmentRepository devopsEnvironmentRepository;
    private final DevopsAutoDeployRecordRepository devopsAutoDeployRecordRepository;
    private final DevopsAutoDeployRepository devopsAutoDeployRepository;
    private final ApplicationInstanceService applicationInstanceService;
    private final GitlabRepository gitlabRepository;

    @Value("${services.gitlab.url}")
    private String gitlabUrl;

    @Autowired
    private IamRepository iamRepository;

    @Autowired
    private UserAttrRepository userAttrRepository;


    @Autowired
    public DevopsSagaHandler(DevopsEnvironmentService devopsEnvironmentService,
                             DevopsGitService devopsGitService,
                             ApplicationTemplateService applicationTemplateService,
                             ApplicationService applicationService,
                             DevopsGitlabPipelineService devopsGitlabPipelineService,
                             ApplicationRepository applicationRepository,
                             ApplicationTemplateRepository applicationTemplateRepository,
                             DevopsEnvironmentRepository devopsEnvironmentRepository,
                             DevopsAutoDeployRecordRepository devopsAutoDeployRecordRepository,
                             DevopsAutoDeployRepository devopsAutoDeployRepository,
                             GitlabRepository gitlabRepository,
                             ApplicationInstanceService applicationInstanceService) {
        this.devopsEnvironmentService = devopsEnvironmentService;
        this.devopsGitService = devopsGitService;
        this.applicationTemplateService = applicationTemplateService;
        this.applicationService = applicationService;
        this.devopsGitlabPipelineService = devopsGitlabPipelineService;
        this.applicationRepository = applicationRepository;
        this.applicationTemplateRepository = applicationTemplateRepository;
        this.devopsEnvironmentRepository = devopsEnvironmentRepository;
        this.devopsAutoDeployRecordRepository = devopsAutoDeployRecordRepository;
        this.devopsAutoDeployRepository = devopsAutoDeployRepository;
        this.gitlabRepository = gitlabRepository;
        this.applicationInstanceService = applicationInstanceService;
    }

    /**
     * devops????????????
     */
    @SagaTask(code = "devopsCreateEnv",
            description = "devops????????????",
            sagaCode = "devops-create-env",
            maxRetryCount = 3,
            seq = 1)
    public String devopsCreateEnv(String data) {
        GitlabProjectPayload gitlabProjectPayload = gson.fromJson(data, GitlabProjectPayload.class);
        try {
            devopsEnvironmentService.handleCreateEnvSaga(gitlabProjectPayload);
        } catch (Exception e) {
            devopsEnvironmentService.setEnvErrStatus(data, gitlabProjectPayload.getIamProjectId());
            throw e;
        }
        DevopsEnvironmentE devopsEnvironmentE = devopsEnvironmentRepository
                .queryByClusterIdAndCode(gitlabProjectPayload.getClusterId(), gitlabProjectPayload.getPath());
        if (devopsEnvironmentE.getFailed() != null && devopsEnvironmentE.getFailed()) {
            devopsEnvironmentE.initFailed(false);
            devopsEnvironmentRepository.update(devopsEnvironmentE);
        }
        return data;
    }

    /**
     * ??????????????????
     */
    @SagaTask(code = "devopsCreateEnvError",
            description = "set  DevOps app status error",
            sagaCode = "devops-set-env-err",
            maxRetryCount = 3,
            seq = 1)
    public String setEnvErr(String data) {
        GitlabProjectPayload gitlabProjectPayload = gson.fromJson(data, GitlabProjectPayload.class);
        DevopsEnvironmentE devopsEnvironmentE = devopsEnvironmentRepository
                .queryByClusterIdAndCode(gitlabProjectPayload.getClusterId(), gitlabProjectPayload.getPath());
        devopsEnvironmentE.initFailed(true);
        devopsEnvironmentRepository.update(devopsEnvironmentE);
        return data;
    }

    /**
     * GitOps ????????????
     */
    @SagaTask(code = "devopsGitOps",
            description = "gitops",
            sagaCode = "devops-sync-gitops",
            concurrentLimitNum = 1,
            maxRetryCount = 3,
            concurrentLimitPolicy = SagaDefinition.ConcurrentLimitPolicy.TYPE_AND_ID,
            seq = 1)
    public String gitops(String data) {
        PushWebHookDTO pushWebHookDTO = null;
        try {
            pushWebHookDTO = objectMapper.readValue(data, PushWebHookDTO.class);
        } catch (IOException e) {
            LOGGER.info(e.getMessage());
        }
        devopsGitService.fileResourceSync(pushWebHookDTO);
        return data;
    }

    @SagaTask(code = "syncSteamDataToGitlab",
            description = "??????Gitlab???????????????????????????????????????Gitlab",
            sagaCode = "devops-ci-sync-steam-data-gitlab",
            maxRetryCount = 2,
            seq = 1)
    public String syncSteamDataToGitlab(String data) {
        logger.info("?????????????????????Gitlab, data={}", data);
        DevOpsAppPayload gitlabProjectPayload = gson.fromJson(data, DevOpsAppPayload.class);
        applicationService.syncSteamDataToGitlab(gitlabProjectPayload);
        return data;
    }

    /**
     * GitOps ????????????
     */
    @SagaTask(code = "devopsOperationGitlabProject",
            description = "devops create GitLab project",
            sagaCode = "devops-create-gitlab-application",
            maxRetryCount = 3,
            seq = 1)
    public String createApp(String data) {
        DevOpsAppPayload devOpsAppPayload = gson.fromJson(data, DevOpsAppPayload.class);

        DevOpsAppPayloadDevKit devOpsAppPayloadDevKit = gson.fromJson(data, DevOpsAppPayloadDevKit.class);

        if (devOpsAppPayload.getType().equals(APPLICATION)) {
            try {
                applicationService.operationApplication(devOpsAppPayload);
            } catch (Exception e) {
                applicationService.setAppErrStatus(data, devOpsAppPayload.getIamProjectId());
                throw e;
            }

            //===== ??????DevKit,????????????????????????DevKit =====
            BeanUtils.copyProperties(devOpsAppPayload, devOpsAppPayloadDevKit);
            // ???????????????
            ApplicationE applicationE = applicationRepository.query(devOpsAppPayload.getAppId());
            devOpsAppPayloadDevKit.setItemName(applicationE.getName());
            // ?????????Git??????, gitlabUrl + urlSlash + ??????Code + ??????Code + / + ??????Code + .git
            String urlSlash = gitlabUrl.endsWith("/") ? "" : "/";
            LOGGER.error("=============== Begin =================");
            ProjectE projectE = iamRepository.queryIamProject(applicationE.getProjectE().getId());
            LOGGER.error("????????????:" + projectE.getCode());
            Organization organization = iamRepository.queryOrganizationById(projectE.getOrganization().getId());
            LOGGER.error("????????????:" + projectE.getOrganization().getCode());
            devOpsAppPayloadDevKit.setGitAddress(gitlabUrl + urlSlash + organization.getCode() + "-" + projectE.getCode() + "/" + applicationE.getCode() + ".git");
            // Gitlab???Token
            devOpsAppPayloadDevKit.setToken(applicationE.getToken());

            LOGGER.error("=============== End =================");

        }

        data = gson.toJson(devOpsAppPayloadDevKit);
        LOGGER.error("data:" + data);
        //===== ??????DevKit,????????????????????????DevKit =====
        return data;
    }

    /**
     * GitOps ????????????
     */
    @SagaTask(code = "devopsCreateGitlabProject",
            description = "Devops??????????????????????????????gitlab??????",
            sagaCode = "devops-import-gitlab-application",
            maxRetryCount = 3,
            seq = 1)
    public String importApp(String data) {
        DevOpsAppImportPayload devOpsAppImportPayload = gson.fromJson(data, DevOpsAppImportPayload.class);
        if (devOpsAppImportPayload.getType().equals(APPLICATION)) {
            try {
                applicationService.operationApplicationImport(devOpsAppImportPayload);
            } catch (Exception e) {
                applicationService.setAppErrStatus(data, devOpsAppImportPayload.getIamProjectId());
                throw e;
            }
            ApplicationE applicationE = applicationRepository.query(devOpsAppImportPayload.getAppId());
            if (applicationE.getFailed() != null && applicationE.getFailed()) {
                applicationE.setFailed(false);
                if (1 != applicationRepository.update(applicationE)) {
                    LOGGER.error("update application set create success status error");
                }
            }

            //===== ??????DevKit,????????????????????????DevKit =====
            DevOpsAppImportPayloadDevKit devOpsAppImportPayloadDevKit = new DevOpsAppImportPayloadDevKit();
            BeanUtils.copyProperties(devOpsAppImportPayload, devOpsAppImportPayloadDevKit);
            // ???????????????
            devOpsAppImportPayloadDevKit.setItemName(applicationE.getName());
            // ?????????Git??????, gitlabUrl + urlSlash + ??????Code + ??????Code + / + ??????Code + .git
            String urlSlash = gitlabUrl.endsWith("/") ? "" : "/";
            LOGGER.error("=============== Begin =================");
            ProjectE projectE = iamRepository.queryIamProject(applicationE.getProjectE().getId());
            LOGGER.error("????????????:" + projectE.getCode());
            Organization organization = iamRepository.queryOrganizationById(projectE.getOrganization().getId());
            LOGGER.error("????????????:" + projectE.getOrganization().getCode());
            devOpsAppImportPayloadDevKit.setGitAddress(gitlabUrl + urlSlash + organization.getCode() + "-" + projectE.getCode() + "/" + applicationE.getCode() + ".git");
            // ????????????????????????
            devOpsAppImportPayloadDevKit.setOrganizationCode(organization.getCode());
            // Gitlab???Token
            devOpsAppImportPayloadDevKit.setToken(applicationE.getToken());
            data = gson.toJson(devOpsAppImportPayloadDevKit);
//            gitlabRepository.batchAddVariable(applicationE.getGitlabProjectE().getId(), TypeUtil.objToInteger(devOpsAppImportPayload.getGitlabUserId()),
//                    applicationService.setVariableDTO(applicationE.getHarborConfigE().getId(),applicationE.getChartConfigE().getId()));
        }
        return data;
    }


    @SagaTask(code = "devopsCreateApplication",
            description = "devops-ci??????gitlab??????",
            sagaCode = "devops-ci-create-application",
            maxRetryCount = 3,
            seq = 1)
    public String createApplication(String data) {

        ApplicationReqDTO applicationReqDTO = gson.fromJson(data, ApplicationReqDTO.class);

        //??????????????????
        List<Long> userIds = iamRepository.getAllMemberIdsWithoutOwner(applicationReqDTO.getProjectId());

        applicationReqDTO.setUserIds(userIds);

        applicationService.create(applicationReqDTO.getProjectId(), applicationReqDTO);

        return data;
    }

    /**
     * GitOps ????????????????????????
     */
    @SagaTask(code = "devopsUpdateGitlabUsers",
            description = "devops update gitlab users",
            sagaCode = "devops-update-gitlab-users",
            maxRetryCount = 3,
            seq = 1)
    public String updateGitlabUser(String data) {
        DevOpsUserPayload devOpsUserPayload = gson.fromJson(data, DevOpsUserPayload.class);

        // ???????????????????????????????????????????????????
        DevOpsUserPayloadDevKit devOpsUserPayloadDevKit = gson.fromJson(data, DevOpsUserPayloadDevKit.class);
        if (devOpsUserPayloadDevKit.getOnlyModifyApplication()) {
            return data;
        }

        try {
            UpdateUserPermissionService updateUserPermissionService = new UpdateAppUserPermissionServiceImpl();
            updateUserPermissionService
                    .updateUserPermission(devOpsUserPayload.getIamProjectId(), devOpsUserPayload.getAppId(),
                            devOpsUserPayload.getIamUserIds(), devOpsUserPayload.getOption());
        } catch (Exception e) {
            LOGGER.error("update gitlab users {} error", devOpsUserPayload.getIamUserIds());
            throw e;
        }
        return data;
    }

    /**
     * GitOps ????????????????????????
     */
    @SagaTask(code = "devopsCreateGitlabProjectErr",
            description = "set  DevOps app status error",
            sagaCode = "devops-set-app-err",
            maxRetryCount = 3,
            seq = 1)
    public String setAppErr(String data) {
        DevOpsAppPayload devOpsAppPayload = gson.fromJson(data, DevOpsAppPayload.class);
        ApplicationE applicationE = applicationRepository.query(devOpsAppPayload.getAppId());
        applicationE.setFailed(true);
        if (1 != applicationRepository.update(applicationE)) {
            LOGGER.error("update application {} set create failed status error", applicationE.getCode());
        }
        return data;
    }

    /**
     * GitOps ??????????????????????????????
     */
    @SagaTask(code = "devopsCreateGitlabProjectTemplateErr",
            description = "set  DevOps app template status error",
            sagaCode = "devops-set-appTemplate-err",
            maxRetryCount = 3,
            seq = 1)
    public String setAppTemplateErr(String data) {
        DevOpsAppPayload devOpsAppPayload = gson.fromJson(data, DevOpsAppPayload.class);
        ApplicationTemplateE applicationTemplateE = applicationTemplateRepository.queryByCode(
                devOpsAppPayload.getOrganizationId(), devOpsAppPayload.getPath());
        applicationTemplateE.setFailed(true);
        applicationTemplateRepository.update(applicationTemplateE);
        return data;
    }

    /**
     * GitOps ??????????????????
     */
    @SagaTask(code = "devopsOperationGitlabTemplateProject",
            description = "devops create GitLab template project",
            sagaCode = "devops-create-gitlab-template-project",
            maxRetryCount = 3,
            seq = 1)
    public String createTemplate(String data) {
        GitlabProjectPayload gitlabProjectEventDTO = gson.fromJson(data, GitlabProjectPayload.class);
        if (gitlabProjectEventDTO.getType().equals(TEMPLATE)) {
            try {
                applicationTemplateService.operationApplicationTemplate(gitlabProjectEventDTO);
            } catch (Exception e) {
                applicationTemplateService.setAppTemplateErrStatus(data, gitlabProjectEventDTO.getOrganizationId());
                throw e;
            }
            ApplicationTemplateE applicationTemplateE = applicationTemplateRepository.queryByCode(
                    gitlabProjectEventDTO.getOrganizationId(), gitlabProjectEventDTO.getPath());
            if (applicationTemplateE.getFailed() != null && applicationTemplateE.getFailed()) {
                applicationTemplateE.setFailed(false);
                applicationTemplateRepository.update(applicationTemplateE);
            }
        }
        return data;
    }

    /**
     * GitOps ????????????
     */
    @SagaTask(code = "devopsGitlabPipeline",
            description = "gitlab-pipeline",
            sagaCode = "devops-gitlab-pipeline",
            maxRetryCount = 3,
            concurrentLimitPolicy = SagaDefinition.ConcurrentLimitPolicy.TYPE_AND_ID,
            seq = 1)
    public String gitlabPipeline(String data) {
        PipelineWebHookDTO pipelineWebHookDTO = null;
        try {
            pipelineWebHookDTO = objectMapper.readValue(data, PipelineWebHookDTO.class);
        } catch (IOException e) {
            LOGGER.info(e.getMessage());
        }
        devopsGitlabPipelineService.handleCreate(pipelineWebHookDTO);
        return data;
    }

    @SagaTask(code = "devops-auto-deploy-create-instance",
            description = "devops create auto deploy instance",
            sagaCode = "devops-create-auto-deploy-instance",
            concurrentLimitPolicy = SagaDefinition.ConcurrentLimitPolicy.TYPE_AND_ID,
            maxRetryCount = 3,
            seq = 1)
    public void createAutoDeployInstance(String data) {
        //?????????????????????
        ApplicationDeployDTO applicationDeployDTO = gson.fromJson(data, ApplicationDeployDTO.class);
        try {
            ApplicationInstanceDTO applicationInstanceDTO = applicationInstanceService.createOrUpdate(applicationDeployDTO);
            //???????????????????????????
            DevopsAutoDeployRecordE devopsAutoDeployRecordE = new DevopsAutoDeployRecordE(applicationDeployDTO.getRecordId(), STATUS_FIN,
                    applicationDeployDTO.getInstanceName(), applicationInstanceDTO.getId());
            devopsAutoDeployRecordRepository.createOrUpdate(devopsAutoDeployRecordE);
            if (devopsAutoDeployRepository.queryById(applicationDeployDTO.getAutoDeployId()).getInstanceId() == null) {
                devopsAutoDeployRepository.updateInstanceId(applicationDeployDTO.getAutoDeployId(), applicationInstanceDTO.getId());
            }
        } catch (Exception e) {
            //??????????????????,???????????????
            DevopsAutoDeployRecordE devopsAutoDeployRecordE = new DevopsAutoDeployRecordE(applicationDeployDTO.getRecordId(), STATUS_FAILED,
                    null, null);
            devopsAutoDeployRecordRepository.createOrUpdate(devopsAutoDeployRecordE);
            LOGGER.error("error create auto deploy instance {}", e.getMessage());
        }
    }

}
