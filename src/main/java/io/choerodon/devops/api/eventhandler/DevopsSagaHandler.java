package io.choerodon.devops.api.eventhandler;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.choerodon.devops.domain.application.entity.*;
import io.choerodon.devops.domain.application.event.*;
import io.choerodon.devops.domain.application.repository.*;
import io.choerodon.devops.domain.application.valueobject.Organization;
import io.choerodon.devops.infra.common.util.TypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.choerodon.asgard.saga.SagaDefinition;
import io.choerodon.asgard.saga.annotation.SagaTask;
import io.choerodon.devops.api.dto.PipelineWebHookDTO;
import io.choerodon.devops.api.dto.PushWebHookDTO;
import io.choerodon.devops.app.service.*;
import io.choerodon.devops.domain.service.UpdateUserPermissionService;
import io.choerodon.devops.domain.service.impl.UpdateAppUserPermissionServiceImpl;

/**
 * Creator: Runge
 * Date: 2018/7/27
 * Time: 10:06
 * Description: Saga msg by DevOps self
 */
@Component
public class DevopsSagaHandler {
    private static final String TEMPLATE = "template";
    private static final String APPLICATION = "application";
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
                             DevopsEnvironmentRepository devopsEnvironmentRepository) {
        this.devopsEnvironmentService = devopsEnvironmentService;
        this.devopsGitService = devopsGitService;
        this.applicationTemplateService = applicationTemplateService;
        this.applicationService = applicationService;
        this.devopsGitlabPipelineService = devopsGitlabPipelineService;
        this.applicationRepository = applicationRepository;
        this.applicationTemplateRepository = applicationTemplateRepository;
        this.devopsEnvironmentRepository = devopsEnvironmentRepository;
    }

    /**
     * devops创建环境
     */
    @SagaTask(code = "devopsCreateEnv",
            description = "devops创建环境",
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
     * 环境创建失败
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
     * GitOps 事件处理
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

    /**
     * GitOps 事件处理
     */
    @SagaTask(code = "devopsOperationGitlabProject",
            description = "devops create GitLab project",
            sagaCode = "devops-create-gitlab-project",
            maxRetryCount = 3,
            seq = 1)
    public String createApp(String data) {
        DevOpsAppPayload devOpsAppPayload = gson.fromJson(data, DevOpsAppPayload.class);

        DevOpsAppPayloadDevKit devOpsAppPayloadDevKit       = gson.fromJson(data, DevOpsAppPayloadDevKit.class);

        if (devOpsAppPayload.getType().equals(APPLICATION)) {
            try {
                applicationService.operationApplication(devOpsAppPayload);
            } catch (Exception e) {
                applicationService.setAppErrStatus(data, devOpsAppPayload.getIamProjectId());
                throw e;
            }
            ApplicationE applicationE = applicationRepository.query(devOpsAppPayload.getAppId());
            if (applicationE.getFailed() != null && applicationE.getFailed()) {
                applicationE.setFailed(false);
                if (1 != applicationRepository.update(applicationE)) {
                    LOGGER.error("update application set create success status error");
                }
            }

            //===== 对接DevKit,创建新应用通知到DevKit =====
            BeanUtils.copyProperties(devOpsAppPayload, devOpsAppPayloadDevKit);
            // 新应用名称
            devOpsAppPayloadDevKit.setItemName(applicationE.getName());

            // 新应用Git地址, gitlabUrl + urlSlash + 组织Code + 项目Code + / + 应用Code + .git
            String urlSlash = gitlabUrl.endsWith("/") ? "" : "/";
            LOGGER.error("=============== Begin =================");
            ProjectE projectE = iamRepository.queryIamProject(applicationE.getProjectE().getId());
            LOGGER.error("项目编码:" + projectE.getCode());
            Organization organization = iamRepository.queryOrganizationById(projectE.getOrganization().getId());
            LOGGER.error("组织编码:" + projectE.getOrganization().getCode());
            devOpsAppPayloadDevKit.setGitAddress(gitlabUrl + urlSlash + organization.getCode() + "-" + projectE.getCode() + "/" + applicationE.getCode() + ".git");

            // 新应用用户名称
            UserAttrE userAttrE = userAttrRepository.queryByGitlabUserId(TypeUtil.objToLong(devOpsAppPayload.getUserId()));
            String    loginName = iamRepository.queryById(userAttrE.getIamUserId()).getLoginName();


            LOGGER.error("用户名称:" + loginName);
            devOpsAppPayloadDevKit.setUserLogin(loginName);

            // Gitlab的Token
            devOpsAppPayloadDevKit.setToken(applicationE.getToken());

            LOGGER.error("=============== End =================");

        }

        data = gson.toJson(devOpsAppPayloadDevKit);
        LOGGER.error("data:" + data);
        //===== 对接DevKit,创建新应用通知到DevKit =====

        return data;
    }

    /**
     * GitOps 事件处理
     */
    @SagaTask(code = "devopsCreateGitlabProject",
            description = "Devops从外部代码平台导入到gitlab项目",
            sagaCode = "devops-import-gitlab-project",
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

            //===== 对接DevKit,创建新应用通知到DevKit =====
            DevOpsAppImportPayloadDevKit devOpsAppImportPayloadDevKit = new DevOpsAppImportPayloadDevKit();

            BeanUtils.copyProperties(devOpsAppImportPayload, devOpsAppImportPayloadDevKit);
            // 新应用名称
            devOpsAppImportPayloadDevKit.setItemName(applicationE.getName());

            // 新应用Git地址, gitlabUrl + urlSlash + 组织Code + 项目Code + / + 应用Code + .git
            String urlSlash = gitlabUrl.endsWith("/") ? "" : "/";
            LOGGER.error("=============== Begin =================");
            ProjectE projectE = iamRepository.queryIamProject(applicationE.getProjectE().getId());
            LOGGER.error("项目编码:" + projectE.getCode());
            Organization organization = iamRepository.queryOrganizationById(projectE.getOrganization().getId());
            LOGGER.error("组织编码:" + projectE.getOrganization().getCode());
            devOpsAppImportPayloadDevKit.setGitAddress(gitlabUrl + urlSlash + organization.getCode() + "-" + projectE.getCode() + "/" + applicationE.getCode() + ".git");

            // 新应用用户名称
            UserAttrE userAttrE = userAttrRepository.queryByGitlabUserId(TypeUtil.objToLong(devOpsAppImportPayload.getUserId()));
            String    loginName = iamRepository.queryById(userAttrE.getIamUserId()).getLoginName();
            LOGGER.error("用户名称:" + loginName);
            devOpsAppImportPayloadDevKit.setUserLogin(loginName);

            // 新应用的组织编码
            devOpsAppImportPayloadDevKit.setOrganizationCode(organization.getCode());

            // Gitlab的Token
            devOpsAppImportPayloadDevKit.setToken(applicationE.getToken());

            data = gson.toJson(devOpsAppImportPayloadDevKit);
            LOGGER.error("data:" + data);
        }

        return data;
    }

    /**
     * GitOps 用户权限分配处理
     */
    @SagaTask(code = "devopsUpdateGitlabUsers",
            description = "devops update gitlab users",
            sagaCode = "devops-update-gitlab-users",
            maxRetryCount = 3,
            seq = 1)
    public String updateGitlabUser(String data) {
        DevOpsUserPayload devOpsUserPayload = gson.fromJson(data, DevOpsUserPayload.class);

        // 如果仅修改应用名称就不需要更新权限
        DevOpsUserPayloadDevKit devOpsUserPayloadDevKit = gson.fromJson(data, DevOpsUserPayloadDevKit.class);
        if(devOpsUserPayloadDevKit.getOnlyModifyApplication()){
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
     * GitOps 应用创建失败处理
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
     * GitOps 应用模板创建失败处理
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
     * GitOps 模板事件处理
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
     * GitOps 事件处理
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
}
