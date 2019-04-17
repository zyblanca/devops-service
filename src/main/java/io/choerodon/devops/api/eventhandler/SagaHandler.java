package io.choerodon.devops.api.eventhandler;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.choerodon.asgard.saga.SagaDefinition;
import io.choerodon.asgard.saga.annotation.SagaTask;
import io.choerodon.devops.api.dto.*;
import io.choerodon.devops.app.service.*;
import io.choerodon.devops.domain.application.entity.ApplicationE;
import io.choerodon.devops.domain.application.entity.UserAttrE;
import io.choerodon.devops.domain.application.entity.gitlab.GitlabGroupE;
import io.choerodon.devops.domain.application.event.*;
import io.choerodon.devops.domain.application.repository.ApplicationRepository;
import io.choerodon.devops.domain.application.repository.UserAttrRepository;
import io.choerodon.devops.infra.common.util.TypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Creator: Runge
 * Date: 2018/7/27
 * Time: 10:06
 * Description: External saga msg
 */
@Component
public class SagaHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SagaHandler.class);
    private final Gson gson = new Gson();

    @Autowired
    private GitlabGroupService gitlabGroupService;
    @Autowired
    private HarborService harborService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private GitlabGroupMemberService gitlabGroupMemberService;
    @Autowired
    private GitlabUserService gitlabUserService;
    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ProjectService projectService;

    private void loggerInfo(Object o) {
        LOGGER.info("data: {}", o);
    }


    @SagaTask(code = "devopsCreateGitLabGroup",
            description = "devops 创建 GitLab Group",
            sagaCode = "iam-create-project",
            maxRetryCount = 3,
            seq = 1)
    public String handleGitlabGroupEvent(String msg) {
        ProjectEvent projectEvent = gson.fromJson(msg, ProjectEvent.class);
        GitlabGroupPayload gitlabGroupPayload = new GitlabGroupPayload();
        BeanUtils.copyProperties(projectEvent, gitlabGroupPayload);
        loggerInfo(gitlabGroupPayload);
        gitlabGroupService.createGroup(gitlabGroupPayload, "");
        return msg;
    }

    /**
     * 创建组事件
     */
    @SagaTask(code = "devopsCreateGitOpsGroup",
            description = "devops 创建 GitOps Group",
            sagaCode = "iam-create-project",
            maxRetryCount = 3,
            seq = 2)
    public String handleGitOpsGroupEvent(String msg) {
        ProjectEvent projectEvent = gson.fromJson(msg, ProjectEvent.class);
        GitlabGroupPayload gitlabGroupPayload = new GitlabGroupPayload();
        BeanUtils.copyProperties(projectEvent, gitlabGroupPayload);
        loggerInfo(gitlabGroupPayload);
        gitlabGroupService.createGroup(gitlabGroupPayload, "-gitops");
        return msg;
    }


    @SagaTask(code = "devopsUpdateGitLabGroup",
            description = "devops  更新 GitLab Group",
            sagaCode = "iam-update-project",
            maxRetryCount = 3,
            seq = 1)
    public String handleUpdateGitlabGroupEvent(String msg) {
        ProjectEvent projectEvent = gson.fromJson(msg, ProjectEvent.class);
        GitlabGroupPayload gitlabGroupPayload = new GitlabGroupPayload();
        BeanUtils.copyProperties(projectEvent, gitlabGroupPayload);
        loggerInfo(gitlabGroupPayload);
        gitlabGroupService.updateGroup(gitlabGroupPayload, "");

        // 对接DevKit, 传递项目的GitlabGroupId值
        msg = gitlabGroupIdPayload(projectEvent);

        return msg;
    }

    @SagaTask(code = "devopsUpdateGitOpsGroup",
            description = "devops  更新 GitOps Group",
            sagaCode = "iam-update-project",
            maxRetryCount = 3,
            seq = 1)
    public String handleUpdateGitOpsGroupEvent(String msg) {
        ProjectEvent projectEvent = gson.fromJson(msg, ProjectEvent.class);
        GitlabGroupPayload gitlabGroupPayload = new GitlabGroupPayload();
        BeanUtils.copyProperties(projectEvent, gitlabGroupPayload);
        loggerInfo(gitlabGroupPayload);
        gitlabGroupService.updateGroup(gitlabGroupPayload, "-gitops");
        return msg;
    }


    /**
     * DevKit服务的项目启用监听
     */
    @SagaTask(code = "devKitEnableOrganization",
            description = "DevKit服务的项目启用监听",
            sagaCode = "iam-enable-project",
            maxRetryCount = 3,
            concurrentLimitNum = 2,
            concurrentLimitPolicy = SagaDefinition.ConcurrentLimitPolicy.NONE,
            seq = 10)
    public String handleProjectEnableEvent(String data) {
        ProjectEvent projectEvent = gson.fromJson(data, ProjectEvent.class);
        // 对接DevKit, 传递项目的GitlabGroupId值
        data = gitlabGroupIdPayload(projectEvent);
        LOGGER.error("DevKit服务的项目禁用监听:" + data);
        return data;
    }

    /**
     * DevKit服务的项目禁用监听
     */
    @SagaTask(code = "devKitDisableProject",
            description = "DevKit服务的项目禁用监听",
            sagaCode = "iam-disable-project",
            maxRetryCount = 3,
            concurrentLimitNum = 2,
            concurrentLimitPolicy = SagaDefinition.ConcurrentLimitPolicy.NONE,
            seq = 10)
    public String handleProjectDisableEvent(String data) {
        ProjectEvent projectEvent = gson.fromJson(data, ProjectEvent.class);
        // 对接DevKit, 传递项目的GitlabGroupId值
        data = gitlabGroupIdPayload(projectEvent);
        LOGGER.error("DevKit服务的项目禁用监听:" + data);
        return data;
    }

    /**
     * 封装GitLabGroupId到参数中
     * @param projectEvent
     * @return
     */
    private String gitlabGroupIdPayload(ProjectEvent  projectEvent) {
        GitlabGroupE       gitlabGroupE       = projectService.queryDevopsProject(projectEvent.getProjectId());
        ProjectEventDevKit projectEventDevKit = new ProjectEventDevKit();
        BeanUtils.copyProperties(projectEvent, projectEventDevKit);
        projectEventDevKit.setDevopsAppGroupId(gitlabGroupE.getDevopsAppGroupId());
        return gson.toJson(projectEventDevKit);
    }

    /**
     * 创建harbor项目事件
     */
    @SagaTask(code = "devopsCreateHarbor",
            description = "devops 创建 Harbor",
            sagaCode = "iam-create-project",
            maxRetryCount = 3,
            seq = 1)
    public String handleHarborEvent(String msg) {
        ProjectEvent projectEvent = gson.fromJson(msg, ProjectEvent.class);
        HarborPayload harborPayload = new HarborPayload(
                projectEvent.getProjectId(),
                projectEvent.getOrganizationCode() + "-" + projectEvent.getProjectCode()
        );
        loggerInfo(harborPayload);
        harborService.createHarbor(harborPayload);
        return msg;
    }

    /**
     * 创建组织事件
     */
    @SagaTask(code = "devopsCreateOrganization",
            description = "创建组织事件",
            sagaCode = "org-create-organization",
            maxRetryCount = 3,
            seq = 1)
    public String handleOrganizationCreateEvent(String payload) {
        OrganizationEventPayload organizationEventPayload = gson.fromJson(payload, OrganizationEventPayload.class);
        loggerInfo(organizationEventPayload);
        organizationService.create(organizationEventPayload);
        return payload;
    }


    /**
     * Iam创建应用事件
     */
    @SagaTask(code = "iamCreateApplication",
            description = "Iam创建应用事件",
            sagaCode = "iam-create-application",
            maxRetryCount = 3,
            seq = 1)
    public String handleIamCreateApplication(String payload) {
        IamAppPayLoad iamAppPayLoad = gson.fromJson(payload, IamAppPayLoad.class);
        loggerInfo(iamAppPayLoad);
        applicationService.createIamApplication(iamAppPayLoad);
        return payload;
    }


    /**
     * Iam更新应用事件
     */
    @SagaTask(code = "iamCreateApplication",
            description = "Iam更新应用事件",
            sagaCode = "iam-update-application",
            maxRetryCount = 3,
            seq = 1)
    public String handleIamUpdateApplication(String payload) {
        IamAppPayLoad iamAppPayLoad = gson.fromJson(payload, IamAppPayLoad.class);
        loggerInfo(iamAppPayLoad);
        applicationService.createIamApplication(iamAppPayLoad);
        return payload;
    }

    /**
     * Iam启用应用事件
     */
    @SagaTask(code = "iamEnableApplication",
            description = "Iam启用应用事件",
            sagaCode = "iam-enable-application",
            maxRetryCount = 3,
            seq = 1)
    public String handleIamEnableApplication(String payload) {
        IamAppPayLoad iamAppPayLoad = gson.fromJson(payload, IamAppPayLoad.class);
        loggerInfo(iamAppPayLoad);
        ApplicationE applicationE = applicationRepository.queryByCode(iamAppPayLoad.getCode(), iamAppPayLoad.getProjectId());
        applicationService.active(applicationE.getId(), true);
        return payload;
    }

    /**
     * Iam停用应用事件
     */
    @SagaTask(code = "iamDisableApplication",
            description = "Iam停用应用事件",
            sagaCode = "iam-disable-application",
            maxRetryCount = 3,
            seq = 1)
    public String handleIamDisableApplication(String payload) {
        IamAppPayLoad iamAppPayLoad = gson.fromJson(payload, IamAppPayLoad.class);
        loggerInfo(iamAppPayLoad);
        ApplicationE applicationE = applicationRepository.queryByCode(iamAppPayLoad.getCode(), iamAppPayLoad.getProjectId());
        applicationService.active(applicationE.getId(), false);
        return payload;
    }

    /**
     * 角色同步事件
     */
    @SagaTask(code = "devopsUpdateMemberRole", description = "角色同步事件",
            sagaCode = "iam-update-memberRole", maxRetryCount = 3,
            seq = 1)
    public List<GitlabGroupMemberDevKitDTO> handleGitlabGroupMemberEvent(String payload) {
        List<GitlabGroupMemberDTO> gitlabGroupMemberDTOList = gson.fromJson(payload,
                new TypeToken<List<GitlabGroupMemberDTO>>() {
                }.getType());
        loggerInfo(gitlabGroupMemberDTOList);
        gitlabGroupMemberService.createGitlabGroupMemberRole(gitlabGroupMemberDTOList);
        // 对接DevKit,传递devopsAppGroupId
        List<GitlabGroupMemberDevKitDTO> gitlabGroupMemberDevKitDTOList = new ArrayList<>();
        GitlabGroupMemberDevKitDTO gitlabGroupMemberDevKitDTO = new GitlabGroupMemberDevKitDTO();
        for (GitlabGroupMemberDTO gitlabGroupMemberDTO : gitlabGroupMemberDTOList) {
            if("project".equals(gitlabGroupMemberDTO.getResourceType())){
                BeanUtils.copyProperties(gitlabGroupMemberDTO, gitlabGroupMemberDevKitDTO);
                GitlabGroupE gitlabGroupE = projectService.queryDevopsProject(gitlabGroupMemberDTO.getResourceId());
                gitlabGroupMemberDevKitDTO.setDevopsAppGroupId(gitlabGroupE.getDevopsAppGroupId());
            }
            gitlabGroupMemberDevKitDTOList.add(gitlabGroupMemberDevKitDTO);
        }

        return gitlabGroupMemberDevKitDTOList;
        //return gitlabGroupMemberDTOList;
    }


    /**
     * 删除角色同步事件
     */
    @SagaTask(code = "devopsDeleteMemberRole", description = "删除角色同步事件",
            sagaCode = "iam-delete-memberRole", maxRetryCount = 3,
            seq = 1)
    public List<GitlabGroupMemberDevKitDTO> handleDeleteMemberRoleEvent(String payload) {
        List<GitlabGroupMemberDTO> gitlabGroupMemberDTOList = gson.fromJson(payload,
                new TypeToken<List<GitlabGroupMemberDTO>>() {
                }.getType());
        loggerInfo(gitlabGroupMemberDTOList);
        gitlabGroupMemberService.deleteGitlabGroupMemberRole(gitlabGroupMemberDTOList);
        // 对接DevKit,传递devopsAppGroupId
        List<GitlabGroupMemberDevKitDTO> gitlabGroupMemberDevKitDTOList = new ArrayList<>();
        GitlabGroupMemberDevKitDTO gitlabGroupMemberDevKitDTO = new GitlabGroupMemberDevKitDTO();
        for (GitlabGroupMemberDTO gitlabGroupMemberDTO : gitlabGroupMemberDTOList) {
            if("project".equals(gitlabGroupMemberDTO.getResourceType())){
                BeanUtils.copyProperties(gitlabGroupMemberDTO, gitlabGroupMemberDevKitDTO);
                GitlabGroupE gitlabGroupE = projectService.queryDevopsProject(gitlabGroupMemberDTO.getResourceId());
                gitlabGroupMemberDevKitDTO.setDevopsAppGroupId(gitlabGroupE.getDevopsAppGroupId());
                gitlabGroupMemberDevKitDTOList.add(gitlabGroupMemberDevKitDTO);
            }
        }

        return gitlabGroupMemberDevKitDTOList;
        //return gitlabGroupMemberDTOList;
    }

    /**
     * 用户创建事件
     */
    @SagaTask(code = "devopsCreateUser", description = "用户创建事件",
            sagaCode = "iam-create-user", maxRetryCount = 5,
            seq = 1)
    public List<GitlabUserDTO> handleCreateUserEvent(String payload) {
        List<GitlabUserDTO> gitlabUserDTO = gson.fromJson(payload, new TypeToken<List<GitlabUserDTO>>() {
        }.getType());
        loggerInfo(gitlabUserDTO);
        gitlabUserDTO.forEach(t -> {
            GitlabUserRequestDTO gitlabUserReqDTO = new GitlabUserRequestDTO();
            gitlabUserReqDTO.setProvider("oauth2_generic");
            gitlabUserReqDTO.setExternUid(t.getId());
            gitlabUserReqDTO.setSkipConfirmation(true);
            gitlabUserReqDTO.setUsername(t.getUsername());
            gitlabUserReqDTO.setEmail(t.getEmail());
            gitlabUserReqDTO.setName(t.getName());
            if (t.getName() == null) {
                gitlabUserReqDTO.setName(t.getUsername());
            }
            gitlabUserReqDTO.setCanCreateGroup(true);
            gitlabUserReqDTO.setProjectsLimit(100);

            gitlabUserService.createGitlabUser(gitlabUserReqDTO);
        });
        return gitlabUserDTO;
    }

    /**
     * 用户更新事件
     */
    @SagaTask(code = "devopsUpdateUser", description = "用户更新事件",
            sagaCode = "iam-update-user", maxRetryCount = 3,
            seq = 1)
    public String handleUpdateUserEvent(String payload) {
        GitlabUserDTO gitlabUserDTO = gson.fromJson(payload, GitlabUserDTO.class);
        loggerInfo(gitlabUserDTO);

        GitlabUserRequestDTO gitlabUserReqDTO = new GitlabUserRequestDTO();
        gitlabUserReqDTO.setProvider("oauth2_generic");
        gitlabUserReqDTO.setExternUid(gitlabUserDTO.getId());
        gitlabUserReqDTO.setSkipConfirmation(true);
        gitlabUserReqDTO.setUsername(gitlabUserDTO.getUsername());
        gitlabUserReqDTO.setEmail(gitlabUserDTO.getEmail());
        gitlabUserReqDTO.setName(gitlabUserDTO.getName());
        gitlabUserReqDTO.setCanCreateGroup(true);
        gitlabUserReqDTO.setProjectsLimit(100);

        gitlabUserService.updateGitlabUser(gitlabUserReqDTO);
        return payload;
    }

    /**
     * 用户启用事件
     */
    @SagaTask(code = "devopsEnableUser", description = "用户启用事件",
            sagaCode = "iam-enable-user", maxRetryCount = 3,
            seq = 1)
    public String handleIsEnabledUserEvent(String payload) {
        GitlabUserDTO gitlabUserDTO = gson.fromJson(payload, GitlabUserDTO.class);
        loggerInfo(gitlabUserDTO);

        gitlabUserService.isEnabledGitlabUser(TypeUtil.objToInteger(gitlabUserDTO.getId()));
        return payload;
    }

    /**
     * 用户禁用事件
     */
    @SagaTask(code = "devopsDisableUser", description = "用户禁用事件",
            sagaCode = "iam-disable-user", maxRetryCount = 3,
            seq = 1)
    public String handleDisEnabledUserEvent(String payload) {
        GitlabUserDTO gitlabUserDTO = gson.fromJson(payload, GitlabUserDTO.class);
        loggerInfo(gitlabUserDTO);

        gitlabUserService.disEnabledGitlabUser(TypeUtil.objToInteger(gitlabUserDTO.getId()));
        return payload;
    }

    /**
     * 注册组织事件
     */
    @SagaTask(code = "devopsOrgRegister", description = "注册组织",
            sagaCode = "org-register", maxRetryCount = 3,
            seq = 1)
    public String registerOrganization(String payload) {
        RegisterOrganizationDTO registerOrganizationDTO = gson.fromJson(payload, RegisterOrganizationDTO.class);
        loggerInfo(registerOrganizationDTO);

        organizationService.registerOrganization(registerOrganizationDTO);
        return payload;
    }
}
