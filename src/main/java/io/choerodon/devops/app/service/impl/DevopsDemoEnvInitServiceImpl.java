package io.choerodon.devops.app.service.impl;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import io.choerodon.core.convertor.ConvertHelper;
import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.api.dto.*;
import io.choerodon.devops.api.eventhandler.DevopsSagaHandler;
import io.choerodon.devops.api.validator.ApplicationValidator;
import io.choerodon.devops.app.service.*;
import io.choerodon.devops.domain.application.entity.ApplicationE;
import io.choerodon.devops.domain.application.entity.ProjectE;
import io.choerodon.devops.domain.application.entity.UserAttrE;
import io.choerodon.devops.domain.application.entity.gitlab.GitlabGroupE;
import io.choerodon.devops.domain.application.entity.gitlab.GitlabMemberE;
import io.choerodon.devops.domain.application.event.DevOpsAppPayload;
import io.choerodon.devops.domain.application.event.OrganizationRegisterEventPayload;
import io.choerodon.devops.domain.application.repository.*;
import io.choerodon.devops.domain.application.valueobject.MockMultipartFile;
import io.choerodon.devops.domain.application.valueobject.Organization;
import io.choerodon.devops.infra.common.util.GitUserNameUtil;
import io.choerodon.devops.infra.common.util.TypeUtil;
import io.choerodon.devops.infra.common.util.enums.AccessLevel;
import io.choerodon.devops.infra.dataobject.gitlab.MergeRequestDO;
import io.choerodon.devops.infra.feign.GitlabServiceClient;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

/**
 * 为搭建Demo环境初始化项目中的一些数据，包含应用，分支，提交，版本，应用市场等
 * 目前实现不选择程序中进行循环等待等待ci生成版本，而是手动生成版本，但是该版本可以看
 * 但是会部署失败，显示找不到Chart.
 *
 * @author zmf
 */
@Service
public class DevopsDemoEnvInitServiceImpl implements DevopsDemoEnvInitService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${demo.data.file.path:demo/demo-data.json}")
    private String demoDataFilePath;
    @Value("${demo.tgz.file.path:demo/code-i.tgz}")
    private String tgzFilePath;

    @Autowired
    private DevopsProjectConfigService projectConfigService;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private DevopsGitService devopsGitService;
    @Autowired
    private ApplicationMarketService applicationMarketService;
    @Autowired
    private ApplicationVersionService applicationVersionService;
    @Autowired
    private ApplicationTemplateService applicationTemplateService;
    @Autowired
    private GitlabServiceClient gitlabServiceClient;
    @Autowired
    private UserAttrRepository userAttrRepository;
    @Autowired
    private IamRepository iamRepository;
    @Autowired
    private DevopsProjectRepository devopsProjectRepository;
    @Autowired
    private GitlabGroupMemberRepository gitlabGroupMemberRepository;
    @Autowired
    private AppUserPermissionRepository appUserPermissionRepository;
    @Autowired
    private DevopsSagaHandler devopsSagaHandler;

    private Gson gson = new Gson();

    private DemoDataDTO demoDataDTO;
    private Integer gitlabUserId;

    @PostConstruct
    public void loadData() {
        try {
            String content = StreamUtils.copyToString(this.getClass().getClassLoader().getResourceAsStream(demoDataFilePath), Charset.forName("UTF-8"));
            demoDataDTO = JSONObject.parseObject(content, DemoDataDTO.class);
        } catch (Exception e) {
            logger.error("Load content of demo data failed. exception is: {}", e);
        }
    }

    @Override
    public void initialDemoEnv(OrganizationRegisterEventPayload organizationRegisterEventPayload) {
        Long projectId = organizationRegisterEventPayload.getProject().getId();
//        1. 创建应用
        ApplicationReqDTO app = demoDataDTO.getApplicationInfo();
        app.setApplicationTemplateId(getMicroServiceTemplateId());
        app.setIsSkipCheckPermission(Boolean.TRUE);

        ApplicationRepDTO applicationRepDTO = createDemoApp(projectId, app);

        gitlabUserId = TypeUtil.objToInteger(userAttrRepository.queryById(organizationRegisterEventPayload.getUser().getId()).getGitlabUserId());

        if (applicationRepository.query(applicationRepDTO.getId()).getGitlabProjectE().getId() == null) {
            throw new CommonException("Creating gitlab project for app {} failed.", applicationRepDTO.getId());
        }

        Integer gitlabProjectId = applicationRepository.query(applicationRepDTO.getId()).getGitlabProjectE().getId();

//        2. 创建分支
        DevopsBranchDTO devopsBranchDTO = new DevopsBranchDTO();
        devopsBranchDTO.setAppId(applicationRepDTO.getId());
        devopsBranchDTO.setBranchName(demoDataDTO.getBranchInfo().getBranchName());
        devopsBranchDTO.setOriginBranch(demoDataDTO.getBranchInfo().getOriginBranch());
        devopsGitService.createBranch(projectId, applicationRepDTO.getId(), devopsBranchDTO);

//        3. 提交代码
        newCommit(gitlabProjectId);

//        4. 持续集成
//           - 使用pipeline接口查询pipeline状态，状态完成之后进行下一步操作
//        目前选择不等待

//        5. 创建合并请求并确认
        mergeBranch(gitlabProjectId);

//        6. 创建标记，由于选择人工造版本数据而不是通过ci，此处tag-name不使用正确的。
        devopsGitService.createTag(projectId, applicationRepDTO.getId(), demoDataDTO.getTagInfo().getTag() + "-alpha.1", demoDataDTO.getTagInfo().getRef(), demoDataDTO.getTagInfo().getMsg(), demoDataDTO.getTagInfo().getReleaseNotes());

        createFakeApplicationVersion(applicationRepDTO.getId());

//        7. 发布应用
        ApplicationReleasingDTO applicationReleasingDTO = demoDataDTO.getApplicationRelease();
        applicationReleasingDTO.setAppId(applicationRepDTO.getId());
        applicationReleasingDTO.setAppVersions(Collections.singletonList(getApplicationVersion(projectId, applicationRepDTO.getId())));
        applicationMarketService.release(projectId, applicationReleasingDTO);
    }

    /**
     * 自己重写创建应用方法，在创建应用之后直接调用创建gitlab项目的方法，而不是使用saga.
     * 避免事务之间的值的隔离。
     *
     * @param projectId         项目id
     * @param applicationReqDTO 应用创建的数据
     * @return 应用创建的纪录
     */
    private ApplicationRepDTO createDemoApp(Long projectId, ApplicationReqDTO applicationReqDTO) {
        UserAttrE userAttrE = userAttrRepository.queryById(TypeUtil.objToLong(GitUserNameUtil.getUserId()));
        ApplicationValidator.checkApplication(applicationReqDTO);
        ProjectE projectE = iamRepository.queryIamProject(projectId);
        Organization organization = iamRepository.queryOrganizationById(projectE.getOrganization().getId());
        ApplicationE applicationE = ConvertHelper.convert(applicationReqDTO, ApplicationE.class);
        applicationE.initProjectE(projectId);
        applicationRepository.checkName(applicationE.getProjectE().getId(), applicationE.getName());
        applicationRepository.checkCode(applicationE);
        applicationE.initActive(true);
        applicationE.initSynchro(false);
        applicationE.setIsSkipCheckPermission(applicationReqDTO.getIsSkipCheckPermission());

        // 查询创建应用所在的gitlab应用组
        GitlabGroupE gitlabGroupE = devopsProjectRepository.queryDevopsProject(applicationE.getProjectE().getId());
        GitlabMemberE gitlabMemberE = gitlabGroupMemberRepository.getUserMemberByUserId(
                TypeUtil.objToInteger(gitlabGroupE.getDevopsAppGroupId()),
                TypeUtil.objToInteger(userAttrE.getGitlabUserId()));
        if (gitlabMemberE == null || gitlabMemberE.getAccessLevel() != AccessLevel.OWNER.toValue()) {
            throw new CommonException("error.user.not.owner");
        }
        // 创建saga payload
        DevOpsAppPayload devOpsAppPayload = new DevOpsAppPayload();
        devOpsAppPayload.setType("application");
        devOpsAppPayload.setPath(applicationReqDTO.getCode());
        devOpsAppPayload.setOrganizationId(organization.getId());
        devOpsAppPayload.setUserId(TypeUtil.objToInteger(userAttrE.getGitlabUserId()));
        devOpsAppPayload.setGroupId(TypeUtil.objToInteger(gitlabGroupE.getDevopsAppGroupId()));
        devOpsAppPayload.setUserIds(applicationReqDTO.getUserIds());
        devOpsAppPayload.setSkipCheckPermission(applicationReqDTO.getIsSkipCheckPermission());

        //设置仓库Id
        List<DevopsProjectConfigDTO> configDTOS1 = projectConfigService.queryByIdAndType(null,"harbor");
        List<DevopsProjectConfigDTO> configDTOS2 = projectConfigService.queryByIdAndType(null,"chart");
        applicationE.initHarborConfig(configDTOS1.get(0).getId());
        applicationE.initChartConfig(configDTOS2.get(0).getId());

        applicationE = applicationRepository.create(applicationE);
        devOpsAppPayload.setAppId(applicationE.getId());
        devOpsAppPayload.setIamProjectId(projectId);
        Long appId = applicationE.getId();
        if (appId == null) {
            throw new CommonException("error.application.create.insert");
        }
        // 如果不跳过权限检查
        List<Long> userIds = applicationReqDTO.getUserIds();
        if (!applicationReqDTO.getIsSkipCheckPermission() && userIds != null && !userIds.isEmpty()) {
            userIds.forEach(e -> appUserPermissionRepository.create(e, appId));
        }

        String input = gson.toJson(devOpsAppPayload);

        devopsSagaHandler.createApp(input);

        return ConvertHelper.convert(applicationRepository.queryByCode(applicationE.getCode(),
                applicationE.getProjectE().getId()), ApplicationRepDTO.class);
    }


    /**
     * get template id of template 'MicroService'
     *
     * @return the id
     * @throws CommonException if there isn't a template named 'MicroService'
     */
    private Long getMicroServiceTemplateId() throws CommonException {
        List<ApplicationTemplateRepDTO> template = applicationTemplateService.listByOptions(new PageRequest(0, 1), null, demoDataDTO.getTemplateSearchParam());

        if (template != null && !template.isEmpty()) {
            return template.get(0).getId();
        }
        throw new CommonException("Can not get template named 'Micro Service'.");
    }


    /**
     * add a new commit to the certain branch
     *
     * @param gitlabProjectId gitlab project id
     */
    private void newCommit(Integer gitlabProjectId) {
        gitlabServiceClient.createFile(gitlabProjectId, "newFile" + UUID.randomUUID().toString().replaceAll("-", ""), "a new commit.", "[ADD] a new file", gitlabUserId, demoDataDTO.getBranchInfo().getBranchName());
    }


    /**
     * create a new merge request and confirm.
     *
     * @param gitlabProjectId gitlab project id.
     */
    private void mergeBranch(Integer gitlabProjectId) {
        try {
            // 创建merge request
            MergeRequestDO mergeRequest = gitlabServiceClient.createMergeRequest(gitlabProjectId, demoDataDTO.getBranchInfo().getBranchName(), "master", "a new merge request", "[ADD] add instant push", gitlabUserId).getBody();

            // 确认merge request
            gitlabServiceClient.acceptMergeRequest(gitlabProjectId, mergeRequest.getId(), "", Boolean.FALSE, Boolean.TRUE, gitlabUserId);
        } catch (Exception e) {
            logger.error("Error occurred when merge request. Exception is {}", e);
        }
    }


    /**
     * get the application version named "0.1.0"
     *
     * @param projectId     the project id
     * @param applicationId the application id
     * @return the version
     */
    private AppMarketVersionDTO getApplicationVersion(Long projectId, Long applicationId) {
        PageRequest pageRequest = new PageRequest(0, 1);
        Page<ApplicationVersionRepDTO> versions = applicationVersionService.listApplicationVersionInApp(projectId, applicationId, pageRequest, demoDataDTO.getAppVersionSearchParam());
        if (!versions.isEmpty()) {
            AppMarketVersionDTO appMarketVersionDTO = new AppMarketVersionDTO();
            BeanUtils.copyProperties(versions.getContent().get(0), appMarketVersionDTO);
            return appMarketVersionDTO;
        } else {
            logger.error("Error: can not find a version with name {}", demoDataDTO.getTagInfo().getTag());
            return null;
        }
    }

    /**
     * 手动制造一个应用版本
     *
     * @param appId application id
     */
    private void createFakeApplicationVersion(Long appId) {
        try {
            byte[] bytes = StreamUtils.copyToByteArray(this.getClass().getClassLoader().getResourceAsStream(tgzFilePath));
            MockMultipartFile multipartFile = new MockMultipartFile("code-i.tgz", "code-i.tgz", "application/tgz", bytes);
            applicationVersionService.create(demoDataDTO.getAppVersion().getImage(), applicationRepository.query(appId).getToken(), demoDataDTO.getAppVersion().getVersion(), demoDataDTO.getAppVersion().getCommit(), multipartFile);
        } catch (IOException e) {
            logger.error("can not find file {}", tgzFilePath);
            throw new CommonException(e);
        }
    }
}
