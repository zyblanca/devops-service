package io.choerodon.devops.app.service.impl;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import io.choerodon.devops.domain.application.event.DevOpsAppImportPayload;
import io.choerodon.devops.infra.common.util.enums.GitPlatformType;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import io.choerodon.asgard.saga.annotation.Saga;
import io.choerodon.asgard.saga.dto.StartInstanceDTO;
import io.choerodon.asgard.saga.feign.SagaClient;
import io.choerodon.core.convertor.ConvertHelper;
import io.choerodon.core.convertor.ConvertPageHelper;
import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.core.oauth.DetailsHelper;
import io.choerodon.devops.api.dto.*;
import io.choerodon.devops.api.dto.gitlab.MemberDTO;
import io.choerodon.devops.api.validator.ApplicationValidator;
import io.choerodon.devops.app.service.ApplicationService;
import io.choerodon.devops.domain.application.entity.*;
import io.choerodon.devops.domain.application.entity.gitlab.CommitE;
import io.choerodon.devops.domain.application.entity.gitlab.GitlabGroupE;
import io.choerodon.devops.domain.application.entity.gitlab.GitlabMemberE;
import io.choerodon.devops.domain.application.entity.gitlab.GitlabUserE;
import io.choerodon.devops.domain.application.entity.iam.UserE;
import io.choerodon.devops.domain.application.event.DevOpsAppPayload;
import io.choerodon.devops.domain.application.event.DevOpsUserPayload;
import io.choerodon.devops.domain.application.factory.ApplicationFactory;
import io.choerodon.devops.domain.application.repository.*;
import io.choerodon.devops.domain.application.valueobject.Organization;
import io.choerodon.devops.domain.application.valueobject.ProjectHook;
import io.choerodon.devops.domain.application.valueobject.Variable;
import io.choerodon.devops.infra.common.util.*;
import io.choerodon.devops.infra.common.util.enums.AccessLevel;
import io.choerodon.devops.infra.dataobject.gitlab.BranchDO;
import io.choerodon.devops.infra.dataobject.gitlab.GitlabProjectDO;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;

/**
 * Created by younger on 2018/3/28.
 */
@Service
public class ApplicationServiceImpl implements ApplicationService {
    private static final Pattern REPOSITORY_URL_PATTERN = Pattern.compile("^http.*\\.git");
    private static final String GITLAB_CI_FILE = ".gitlab-ci.yml";
    private static final String DOCKER_FILE = "src/main/docker/Dockerfile";
    private static final String CHART_DIR = "charts";

    public static final Logger LOGGER = LoggerFactory.getLogger(ApplicationServiceImpl.class);
    private static final String MASTER = "master";
    private static final String APPLICATION = "application";
    private static final String ERROR_UPDATE_APP = "error.application.update";
    private Gson gson = new Gson();

    @Value("${services.gitlab.url}")
    private String gitlabUrl;
    @Value("${spring.application.name}")
    private String applicationName;
    @Value("${services.sonarqube.url}")
    private String sonarqubeUrl;
    @Value("${services.gateway.url}")
    private String gatewayUrl;

    @Autowired
    private GitlabRepository gitlabRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private IamRepository iamRepository;
    @Autowired
    private ApplicationTemplateRepository applicationTemplateRepository;
    @Autowired
    private DevopsProjectRepository devopsProjectRepository;
    @Autowired
    private GitUtil gitUtil;
    @Autowired
    private GitlabUserRepository gitlabUserRepository;
    @Autowired
    private UserAttrRepository userAttrRepository;
    @Autowired
    private GitlabGroupMemberRepository gitlabGroupMemberRepository;
    @Autowired
    private DevopsGitRepository devopsGitRepository;
    @Autowired
    private SagaClient sagaClient;
    @Autowired
    private ApplicationMarketRepository applicationMarketRepository;
    @Autowired
    private AppUserPermissionRepository appUserPermissionRepository;
    @Autowired
    private GitlabProjectRepository gitlabProjectRepository;

    @Override
    @Saga(code = "devops-create-gitlab-project",
            description = "Devops创建gitlab项目", inputSchema = "{}")
    public ApplicationRepDTO create(Long projectId, ApplicationReqDTO applicationReqDTO) {
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
        devOpsAppPayload.setType(APPLICATION);
        devOpsAppPayload.setPath(applicationReqDTO.getCode());
        devOpsAppPayload.setOrganizationId(organization.getId());
        devOpsAppPayload.setUserId(TypeUtil.objToInteger(userAttrE.getGitlabUserId()));
        devOpsAppPayload.setGroupId(TypeUtil.objToInteger(gitlabGroupE.getDevopsAppGroupId()));
        devOpsAppPayload.setUserIds(applicationReqDTO.getUserIds());
        devOpsAppPayload.setSkipCheckPermission(applicationReqDTO.getIsSkipCheckPermission());
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
        sagaClient.startSaga("devops-create-gitlab-project", new StartInstanceDTO(input, "", "", ResourceLevel.PROJECT.value(), projectId));

        return ConvertHelper.convert(applicationRepository.queryByCode(applicationE.getCode(),
                applicationE.getProjectE().getId()), ApplicationRepDTO.class);
    }

    @Override
    public ApplicationRepDTO query(Long projectId, Long applicationId) {
        ProjectE projectE = iamRepository.queryIamProject(projectId);
        Organization organization = iamRepository.queryOrganizationById(projectE.getOrganization().getId());
        ApplicationE applicationE = applicationRepository.query(applicationId);
        String urlSlash = gitlabUrl.endsWith("/") ? "" : "/";
        applicationE.initGitlabProjectEByUrl(gitlabUrl + urlSlash
                + organization.getCode() + "-" + projectE.getCode() + "/"
                + applicationE.getCode() + ".git");
        ApplicationRepDTO applicationRepDTO = ConvertHelper.convert(applicationE, ApplicationRepDTO.class);
        if (applicationE.getIsSkipCheckPermission()) {
            applicationRepDTO.setPermission(true);
        } else {
            applicationRepDTO.setPermission(false);
        }
        return applicationRepDTO;
    }

    @Override
    public void delete(Long projectId, Long applicationId) {
        ProjectE projectE = iamRepository.queryIamProject(projectId);
        Organization organization = iamRepository.queryOrganizationById(projectE.getOrganization().getId());
        ApplicationE applicationE = applicationRepository.query(applicationId);
        UserAttrE userAttrE = userAttrRepository.queryById(DetailsHelper.getUserDetails().getUserId());
        gitlabRepository.deleteDevOpsApp(organization.getCode() + "-" + projectE.getCode(),
                applicationE.getCode(), userAttrE.getGitlabUserId().intValue());
        applicationRepository.delete(applicationId);
    }

    @Saga(code = "devops-update-gitlab-users",
            description = "Devops更新gitlab用户", inputSchema = "{}")
    @Override
    public Boolean update(Long projectId, ApplicationUpdateDTO applicationUpdateDTO) {
        ApplicationE applicationE = ConvertHelper.convert(applicationUpdateDTO, ApplicationE.class);
        applicationE.setIsSkipCheckPermission(applicationUpdateDTO.getIsSkipCheckPermission());
        applicationE.initProjectE(projectId);

        Long appId = applicationUpdateDTO.getId();
        ApplicationE oldApplicationE = applicationRepository.query(appId);
        if (!oldApplicationE.getName().equals(applicationUpdateDTO.getName())) {
            applicationRepository.checkName(applicationE.getProjectE().getId(), applicationE.getName());
        }
        if (applicationRepository.update(applicationE) != 1) {
            throw new CommonException(ERROR_UPDATE_APP);
        }

        // 创建gitlabUserPayload
        DevOpsUserPayload devOpsAppPayload = new DevOpsUserPayload();
        devOpsAppPayload.setIamProjectId(projectId);
        devOpsAppPayload.setAppId(appId);
        devOpsAppPayload.setGitlabProjectId(oldApplicationE.getGitlabProjectE().getId());
        devOpsAppPayload.setIamUserIds(applicationUpdateDTO.getUserIds());

        if (oldApplicationE.getIsSkipCheckPermission() && applicationUpdateDTO.getIsSkipCheckPermission()) {
            return true;
        } else if (oldApplicationE.getIsSkipCheckPermission() && !applicationUpdateDTO.getIsSkipCheckPermission()) {
            applicationUpdateDTO.getUserIds().forEach(e -> appUserPermissionRepository.create(e, appId));
            devOpsAppPayload.setOption(1);
        } else if (!oldApplicationE.getIsSkipCheckPermission() && applicationUpdateDTO.getIsSkipCheckPermission()) {
            appUserPermissionRepository.deleteByAppId(appId);
            devOpsAppPayload.setOption(2);
        } else {
            appUserPermissionRepository.deleteByAppId(appId);
            applicationUpdateDTO.getUserIds().forEach(e -> appUserPermissionRepository.create(e, appId));
            devOpsAppPayload.setOption(3);
        }
        String input = gson.toJson(devOpsAppPayload);
        sagaClient.startSaga("devops-update-gitlab-users", new StartInstanceDTO(input, "app", appId.toString(), ResourceLevel.PROJECT.value(), projectId));

        return true;
    }

    @Override
    public Boolean active(Long applicationId, Boolean active) {
        if (!active) {
            applicationRepository.checkAppCanDisable(applicationId);
        }
        ApplicationE applicationE = applicationRepository.query(applicationId);
        applicationE.initActive(active);
        if (applicationRepository.update(applicationE) != 1) {
            throw new CommonException("error.application.active");
        }
        return true;
    }

    @Override
    public Page<ApplicationRepDTO> listByOptions(Long projectId, Boolean isActive, Boolean hasVersion,
                                                 String type, Boolean doPage,
                                                 PageRequest pageRequest, String params) {
        Page<ApplicationE> applicationES =
                applicationRepository.listByOptions(projectId, isActive, hasVersion, type, doPage, pageRequest, params);
        UserAttrE userAttrE = userAttrRepository.queryById(TypeUtil.objToLong(GitUserNameUtil.getUserId()));
        ProjectE projectE = iamRepository.queryIamProject(projectId);
        Organization organization = iamRepository.queryOrganizationById(projectE.getOrganization().getId());
        String urlSlash = gitlabUrl.endsWith("/") ? "" : "/";
        applicationES.getContent().forEach(t -> {
                    if (t.getGitlabProjectE() != null && t.getGitlabProjectE().getId() != null) {
                        t.initGitlabProjectEByUrl(
                                gitlabUrl + urlSlash + organization.getCode() + "-" + projectE.getCode() + "/" +
                                        t.getCode() + ".git");
                        getSonarUrl(projectE, organization, t);
                    }
                }
        );
        Page<ApplicationRepDTO> resultDTOPage = ConvertPageHelper.convertPage(applicationES, ApplicationRepDTO.class);
        resultDTOPage.setContent(setApplicationRepDTOPermission(applicationES.getContent(), userAttrE, projectE));
        return resultDTOPage;
    }

    private void getSonarUrl(ProjectE projectE, Organization organization, ApplicationE t) {
        if (!sonarqubeUrl.equals("")) {
            Integer result;
            try {
                result = HttpClientUtil.getSonar(sonarqubeUrl.endsWith("/")
                        ? sonarqubeUrl
                        : String
                        .format("%s/api/project_links/search?projectKey=%s-%s:%s", sonarqubeUrl, organization.getCode(),
                                projectE.getCode(), t.getCode()));
                if (result.equals(HttpStatus.OK.value())) {
                    t.initSonarUrl(sonarqubeUrl.endsWith("/") ? sonarqubeUrl : sonarqubeUrl + "/"
                            + "dashboard?id=" + organization.getCode() + "-" + projectE.getCode() + ":" + t.getCode());
                }
            } catch (Exception e) {
                t.initSonarUrl(null);
            }
        }
    }

    @Override
    public Page<ApplicationRepDTO> listCodeRepository(Long projectId, PageRequest pageRequest, String params) {

        UserAttrE userAttrE = userAttrRepository.queryById(TypeUtil.objToLong(GitUserNameUtil.getUserId()));
        ProjectE projectE = iamRepository.queryIamProject(projectId);
        Boolean isProjectOwner = iamRepository.isProjectOwner(userAttrE.getIamUserId(), projectE);
        Organization organization = iamRepository.queryOrganizationById(projectE.getOrganization().getId());

        Page<ApplicationE> applicationES = applicationRepository
                .listCodeRepository(projectId, pageRequest, params, isProjectOwner, userAttrE.getIamUserId());
        String urlSlash = gitlabUrl.endsWith("/") ? "" : "/";
        applicationES.forEach(t -> {
                    if (t.getGitlabProjectE() != null && t.getGitlabProjectE().getId() != null) {
                        t.initGitlabProjectEByUrl(gitlabUrl + urlSlash
                                + organization.getCode() + "-" + projectE.getCode() + "/" + t.getCode() + ".git");
                        getSonarUrl(projectE, organization, t);
                    }
                }
        );
        return ConvertPageHelper.convertPage(applicationES, ApplicationRepDTO.class);
    }

    @Override
    public List<ApplicationRepDTO> listByActive(Long projectId) {
        List<ApplicationE> applicationEList = applicationRepository.listByActive(projectId);
        UserAttrE userAttrE = userAttrRepository.queryById(TypeUtil.objToLong(GitUserNameUtil.getUserId()));
        ProjectE projectE = iamRepository.queryIamProject(projectId);
        Organization organization = iamRepository.queryOrganizationById(projectE.getOrganization().getId());
        String urlSlash = gitlabUrl.endsWith("/") ? "" : "/";
        applicationEList.forEach(t -> {
                    if (t.getGitlabProjectE() != null && t.getGitlabProjectE().getId() != null) {
                        t.initGitlabProjectEByUrl(gitlabUrl + urlSlash
                                + organization.getCode() + "-" + projectE.getCode() + "/" + t.getCode() + ".git");
                        getSonarUrl(projectE, organization, t);
                    }
                }
        );
        return setApplicationRepDTOPermission(applicationEList, userAttrE, projectE);
    }

    private List<ApplicationRepDTO> setApplicationRepDTOPermission(List<ApplicationE> applicationEList,
                                                                   UserAttrE userAttrE, ProjectE projectE) {
        List<ApplicationRepDTO> resultDTOList = ConvertHelper.convertList(applicationEList, ApplicationRepDTO.class);
        if (userAttrE == null) {
            throw new CommonException("error.gitlab.user.sync.failed");
        }
        if (!iamRepository.isProjectOwner(userAttrE.getIamUserId(), projectE)) {
            List<Long> appIds = appUserPermissionRepository.listByUserId(userAttrE.getIamUserId()).stream()
                    .map(AppUserPermissionE::getAppId).collect(Collectors.toList());
            resultDTOList.stream().filter(e -> e != null && !e.getPermission()).forEach(e -> {
                if (appIds.contains(e.getId())) {
                    e.setPermission(true);
                }
            });
        } else {
            resultDTOList.stream().filter(Objects::nonNull).forEach(e -> e.setPermission(true));
        }
        return resultDTOList;
    }

    @Override
    public List<ApplicationRepDTO> listAll(Long projectId) {
        return ConvertHelper.convertList(applicationRepository.listAll(projectId), ApplicationRepDTO.class);
    }

    @Override
    public void checkName(Long projectId, String name) {
        applicationRepository.checkName(projectId, name);
    }

    @Override
    public void checkCode(Long projectId, String code) {
        ApplicationE applicationE = ApplicationFactory.createApplicationE();
        applicationE.initProjectE(projectId);
        applicationE.setCode(code);
        applicationRepository.checkCode(applicationE);
    }

    @Override
    public List<ApplicationTemplateRepDTO> listTemplate(Long projectId) {
        ProjectE projectE = iamRepository.queryIamProject(projectId);
        return ConvertHelper.convertList(applicationTemplateRepository.list(projectE.getOrganization().getId()),
                ApplicationTemplateRepDTO.class).stream()
                .filter(ApplicationTemplateRepDTO::getSynchro).collect(Collectors.toList());
    }

    @Override
    public void operationApplication(DevOpsAppPayload gitlabProjectPayload) {
        GitlabGroupE gitlabGroupE = devopsProjectRepository.queryByGitlabGroupId(
                TypeUtil.objToInteger(gitlabProjectPayload.getGroupId()));
        ApplicationE applicationE = applicationRepository.queryByCode(gitlabProjectPayload.getPath(),
                gitlabGroupE.getProjectE().getId());
        ProjectE projectE = iamRepository.queryIamProject(gitlabGroupE.getProjectE().getId());
        Organization organization = iamRepository.queryOrganizationById(projectE.getOrganization().getId());
        GitlabProjectDO gitlabProjectDO = gitlabRepository
                .getProjectByName(organization.getCode() + "-" + projectE.getCode(), applicationE.getCode(),
                        gitlabProjectPayload.getUserId());
        Integer gitlabProjectId = gitlabProjectDO.getId();
        if (gitlabProjectId == null) {
            gitlabProjectDO = gitlabRepository.createProject(gitlabProjectPayload.getGroupId(),
                    gitlabProjectPayload.getPath(),
                    gitlabProjectPayload.getUserId(), false);
        }
        gitlabProjectPayload.setGitlabProjectId(gitlabProjectDO.getId());

        // 为项目下的成员分配对于此gitlab项目的权限
        operateGitlabMemberPermission(gitlabProjectPayload);

        if (applicationE.getApplicationTemplateE() != null) {
            ApplicationTemplateE applicationTemplateE = applicationTemplateRepository.query(
                    applicationE.getApplicationTemplateE().getId());
            //拉取模板
            String applicationDir = APPLICATION + System.currentTimeMillis();
            Git git = cloneTemplate(applicationTemplateE, applicationDir);
            //渲染模板里面的参数
            replaceParams(applicationE, projectE, organization, applicationDir);

            UserAttrE userAttrE = userAttrRepository.queryByGitlabUserId(TypeUtil.objToLong(gitlabProjectPayload.getUserId()));

            // 获取push代码所需的access token
            String accessToken = getToken(gitlabProjectPayload, applicationDir, userAttrE);

            String repoUrl = !gitlabUrl.endsWith("/") ? gitlabUrl + "/" : gitlabUrl;
            applicationE.initGitlabProjectEByUrl(repoUrl + organization.getCode()
                    + "-" + projectE.getCode() + "/" + applicationE.getCode() + ".git");
            GitlabUserE gitlabUserE = gitlabUserRepository.getGitlabUserByUserId(gitlabProjectPayload.getUserId());

            BranchDO branchDO = devopsGitRepository.getBranch(gitlabProjectDO.getId(), MASTER);
            if (branchDO.getName() == null) {
                gitUtil.push(git, applicationDir, applicationE.getGitlabProjectE().getRepoURL(),
                        gitlabUserE.getUsername(), accessToken);
                branchDO = devopsGitRepository.getBranch(gitlabProjectDO.getId(), MASTER);
                //解决push代码之后gitlab给master分支设置保护分支速度和程序运行速度不一致
                if (!branchDO.getProtected()) {
                    try {
                        gitlabRepository.createProtectBranch(gitlabProjectPayload.getGitlabProjectId(), MASTER,
                                AccessLevel.MASTER.toString(), AccessLevel.MASTER.toString(),
                                gitlabProjectPayload.getUserId());
                    } catch (CommonException e) {
                        branchDO = devopsGitRepository.getBranch(gitlabProjectDO.getId(), MASTER);
                        if (!branchDO.getProtected()) {
                            throw new CommonException(e);
                        }
                    }
                }
            } else {
                if (!branchDO.getProtected()) {
                    gitlabRepository.createProtectBranch(gitlabProjectPayload.getGitlabProjectId(), MASTER,
                            AccessLevel.MASTER.toString(), AccessLevel.MASTER.toString(),
                            gitlabProjectPayload.getUserId());
                }
            }
            initMasterBranch(gitlabProjectPayload, applicationE);
        }
        try {
            String applicationToken = getApplicationToken(gitlabProjectDO.getId(), gitlabProjectPayload.getUserId());
            applicationE.setToken(applicationToken);
            applicationE.initGitlabProjectE(TypeUtil.objToInteger(gitlabProjectPayload.getGitlabProjectId()));
            applicationE.initSynchro(true);

            // set project hook id for application
            setProjectHook(applicationE, gitlabProjectDO.getId(), applicationToken, gitlabProjectPayload.getUserId());

            // 更新并校验
            if (applicationRepository.update(applicationE) != 1) {
                throw new CommonException(ERROR_UPDATE_APP);
            }
        } catch (Exception e) {
            throw new CommonException(e.getMessage(), e);
        }
    }

    /**
     * get application token (set a token if there is not one in gitlab)
     *
     * @param projectId gitlab project id
     * @param userId    gitlab user id
     * @return the application token that is stored in gitlab variables
     */
    private String getApplicationToken(Integer projectId, Integer userId) {
        List<Variable> variables = gitlabRepository.getVariable(projectId, userId);
        if (variables.isEmpty()) {
            String token = GenerateUUID.generateUUID();
            gitlabRepository.addVariable(projectId, "Token", token, false, userId);
            return token;
        } else {
            return variables.get(0).getValue();
        }
    }

    /**
     * 处理当前项目成员对于此gitlab应用的权限
     *
     * @param devOpsAppPayload 此次操作相关信息
     */
    private void operateGitlabMemberPermission(DevOpsAppPayload devOpsAppPayload) {
        // 不跳过权限检查，则为gitlab项目分配项目成员权限
        if (!devOpsAppPayload.getSkipCheckPermission()) {
            if (!devOpsAppPayload.getUserIds().isEmpty()) {
                List<Long> gitlabUserIds = userAttrRepository.listByUserIds(devOpsAppPayload.getUserIds()).stream()
                        .map(UserAttrE::getGitlabUserId).collect(Collectors.toList());
                gitlabUserIds.forEach(e -> {
                    GitlabMemberE gitlabMemberE = gitlabProjectRepository
                            .getProjectMember(devOpsAppPayload.getGitlabProjectId(), TypeUtil.objToInteger(e));
                    if (gitlabMemberE == null || gitlabMemberE.getId() == null) {
                        gitlabRepository.addMemberIntoProject(devOpsAppPayload.getGitlabProjectId(),
                                new MemberDTO(TypeUtil.objToInteger(e), 40, ""));
                    }
                });
            }
        }
        // 跳过权限检查，项目下所有成员自动分配权限
        else {
            List<Long> iamUserIds = iamRepository.getAllMemberIdsWithoutOwner(devOpsAppPayload.getIamProjectId());
            List<Integer> gitlabUserIds = userAttrRepository.listByUserIds(iamUserIds).stream()
                    .map(UserAttrE::getGitlabUserId).map(TypeUtil::objToInteger).collect(Collectors.toList());

            gitlabUserIds.forEach(e -> {
                        GitlabMemberE gitlabMemberE = gitlabProjectRepository.getProjectMember(devOpsAppPayload.getGitlabProjectId(), TypeUtil.objToInteger(e));
                        if (gitlabMemberE == null || gitlabMemberE.getId() == null) {
                            gitlabRepository.addMemberIntoProject(devOpsAppPayload.getGitlabProjectId(),
                                    new MemberDTO(TypeUtil.objToInteger(e), 40, ""));
                        }
                    }
            );
        }
    }

    /**
     * 拉取模板库到本地
     *
     * @param applicationTemplateE 模板库的信息
     * @param applicationDir       本地库地址
     * @return 本地库的git实例
     */
    private Git cloneTemplate(ApplicationTemplateE applicationTemplateE, String applicationDir) {
        String repoUrl = applicationTemplateE.getRepoUrl();
        String type = applicationTemplateE.getCode();
        if (applicationTemplateE.getOrganization().getId() != null) {
            repoUrl = repoUrl.startsWith("/") ? repoUrl.substring(1) : repoUrl;
            repoUrl = !gitlabUrl.endsWith("/") ? gitlabUrl + "/" + repoUrl : gitlabUrl + repoUrl;
            type = MASTER;
        }
        return gitUtil.clone(applicationDir, type, repoUrl);
    }

    /**
     * set project hook id for application
     *
     * @param applicationE the application entity
     * @param projectId    the gitlab project id
     * @param token        the token for project hook
     * @param userId       the gitlab user id
     */
    private void setProjectHook(ApplicationE applicationE, Integer projectId, String token, Integer userId) {
        ProjectHook projectHook = ProjectHook.allHook();
        projectHook.setEnableSslVerification(true);
        projectHook.setProjectId(projectId);
        projectHook.setToken(token);
        String uri = !gatewayUrl.endsWith("/") ? gatewayUrl + "/" : gatewayUrl;
        uri += "devops/webhook";
        projectHook.setUrl(uri);
        List<ProjectHook> projectHooks = gitlabRepository
                .getHooks(projectId, userId);
        if (projectHooks == null) {
            applicationE.initHookId(TypeUtil.objToLong(gitlabRepository.createWebHook(
                    projectId, userId, projectHook)
                    .getId()));
        } else {
            applicationE.initHookId(TypeUtil.objToLong(projectHooks.get(0).getId()));
        }
    }

    @Override
    public void operationApplicationImport(DevOpsAppImportPayload devOpsAppImportPayload) {
        // 准备相关的数据
        GitlabGroupE gitlabGroupE = devopsProjectRepository.queryByGitlabGroupId(
                TypeUtil.objToInteger(devOpsAppImportPayload.getGroupId()));
        ApplicationE applicationE = applicationRepository.queryByCode(devOpsAppImportPayload.getPath(),
                gitlabGroupE.getProjectE().getId());
        ProjectE projectE = iamRepository.queryIamProject(gitlabGroupE.getProjectE().getId());
        Organization organization = iamRepository.queryOrganizationById(projectE.getOrganization().getId());
        GitlabProjectDO gitlabProjectDO = gitlabRepository
                .getProjectByName(organization.getCode() + "-" + projectE.getCode(), applicationE.getCode(),
                        devOpsAppImportPayload.getUserId());
        if (gitlabProjectDO.getId() == null) {
            gitlabProjectDO = gitlabRepository.createProject(devOpsAppImportPayload.getGroupId(),
                    devOpsAppImportPayload.getPath(),
                    devOpsAppImportPayload.getUserId(), false);
        }
        devOpsAppImportPayload.setGitlabProjectId(gitlabProjectDO.getId());

        // 为项目下的成员分配对于此gitlab项目的权限
        operateGitlabMemberPermission(devOpsAppImportPayload);

        if (applicationE.getApplicationTemplateE() != null) {
            UserAttrE userAttrE = userAttrRepository.queryByGitlabUserId(TypeUtil.objToLong(devOpsAppImportPayload.getUserId()));
            ApplicationTemplateE applicationTemplateE = applicationTemplateRepository.query(
                    applicationE.getApplicationTemplateE().getId());
            // 拉取模板
            String templateDir = APPLICATION + System.currentTimeMillis();
            Git templateGit = cloneTemplate(applicationTemplateE, templateDir);
            // 渲染模板里面的参数
            replaceParams(applicationE, projectE, organization, templateDir);

            // clone外部代码仓库
            String applicationDir = APPLICATION + System.currentTimeMillis();
            Git repositoryGit = gitUtil.cloneRepository(applicationDir, devOpsAppImportPayload.getRepositoryUrl(), devOpsAppImportPayload.getAccessToken());

            // 将模板库中文件复制到代码库中
            File templateWorkDir = new File(gitUtil.getWorkingDirectory(templateDir));
            File applicationWorkDir = new File(gitUtil.getWorkingDirectory(applicationDir));
            mergeTemplateToApplication(templateWorkDir, applicationWorkDir);

            // 获取push代码所需的access token
            String accessToken = getToken(devOpsAppImportPayload, applicationDir, userAttrE);

            // 设置Application对应的gitlab项目的仓库地址
            String repoUrl = !gitlabUrl.endsWith("/") ? gitlabUrl + "/" : gitlabUrl;
            applicationE.initGitlabProjectEByUrl(repoUrl + organization.getCode()
                    + "-" + projectE.getCode() + "/" + applicationE.getCode() + ".git");

            BranchDO branchDO = devopsGitRepository.getBranch(gitlabProjectDO.getId(), MASTER);
            if (branchDO.getName() == null) {
                try {
                    // 提交并推代码
                    gitUtil.commitAndPush(repositoryGit, applicationE.getGitlabProjectE().getRepoURL(), accessToken);
                } catch (CommonException e) {
                    releaseResources(templateWorkDir, applicationWorkDir, templateGit, repositoryGit);
                    throw e;
                } finally {
                    releaseResources(templateWorkDir, applicationWorkDir, templateGit, repositoryGit);
                }

                branchDO = devopsGitRepository.getBranch(gitlabProjectDO.getId(), MASTER);
                //解决push代码之后gitlab给master分支设置保护分支速度和程序运行速度不一致
                if (!branchDO.getProtected()) {
                    try {
                        gitlabRepository.createProtectBranch(devOpsAppImportPayload.getGitlabProjectId(), MASTER, AccessLevel.MASTER.toString(), AccessLevel.MASTER.toString(), devOpsAppImportPayload.getUserId());
                    } catch (CommonException e) {
                        if (!devopsGitRepository.getBranch(gitlabProjectDO.getId(), MASTER).getProtected()) {
                            throw new CommonException(e);
                        }
                    }
                }
            } else {
                if (!branchDO.getProtected()) {
                    gitlabRepository.createProtectBranch(devOpsAppImportPayload.getGitlabProjectId(), MASTER,
                            AccessLevel.MASTER.toString(), AccessLevel.MASTER.toString(),
                            devOpsAppImportPayload.getUserId());
                }
            }
            initMasterBranch(devOpsAppImportPayload, applicationE);
        }
        try {
            // 设置appliation的属性
            String applicationToken = getApplicationToken(gitlabProjectDO.getId(), devOpsAppImportPayload.getUserId());
            applicationE.initGitlabProjectE(TypeUtil.objToInteger(devOpsAppImportPayload.getGitlabProjectId()));
            applicationE.setToken(applicationToken);
            applicationE.setSynchro(true);

            // set project hook id for application
            setProjectHook(applicationE, gitlabProjectDO.getId(), applicationToken, devOpsAppImportPayload.getUserId());

            // 更新并校验
            if (applicationRepository.update(applicationE) != 1) {
                throw new CommonException(ERROR_UPDATE_APP);
            }
        } catch (Exception e) {
            throw new CommonException(e.getMessage(), e);
        }
    }


    /**
     * 释放资源
     */
    private void releaseResources(File templateWorkDir, File applicationWorkDir, Git templateGit, Git repositoryGit) {
        FileUtil.deleteDirectory(templateWorkDir);
        FileUtil.deleteDirectory(applicationWorkDir);
        if (templateGit != null) {
            templateGit.close();
        }
        if (repositoryGit != null) {
            repositoryGit.close();
        }
    }

    /**
     * 将模板库中的chart包，dockerfile，gitlab-ci文件复制到导入的代码仓库中
     * 复制文件前会判断文件是否存在，如果存在则不复制
     *
     * @param templateWorkDir    模板库工作目录
     * @param applicationWorkDir 应用库工作目录
     */
    private void mergeTemplateToApplication(File templateWorkDir, File applicationWorkDir) {
        // ci 文件
        File appGitlabCiFile = new File(applicationWorkDir, GITLAB_CI_FILE);
        File templateGitlabCiFile = new File(templateWorkDir, GITLAB_CI_FILE);
        if (!appGitlabCiFile.exists() && templateGitlabCiFile.exists()) {
            FileUtil.copyFile(templateGitlabCiFile, appGitlabCiFile);
        }

        // Dockerfile 文件
        File appDockerFile = new File(applicationWorkDir, DOCKER_FILE);
        File templateDockerFile = new File(templateWorkDir, DOCKER_FILE);
        if (!appDockerFile.exists() && templateDockerFile.exists()) {
            FileUtil.copyFile(templateDockerFile, appDockerFile);
        }

        // chart文件夹
        File appChartDir = new File(applicationWorkDir, CHART_DIR);
        File templateChartDir = new File(templateWorkDir, CHART_DIR);
        if (!appChartDir.exists() && templateChartDir.exists()) {
            FileUtil.copyDir(templateChartDir, appChartDir);
        }
    }

    @Override
    @Saga(code = "devops-set-app-err",
            description = "Devops设置application状态为创建失败(devops set app status create err)", inputSchema = "{}")
    public void setAppErrStatus(String input, Long projectId) {
        sagaClient.startSaga("devops-set-app-err", new StartInstanceDTO(input, "", "", ResourceLevel.PROJECT.value(), projectId));
    }

    private void initMasterBranch(DevOpsAppPayload gitlabProjectPayload, ApplicationE applicationE) {
        CommitE commitE;
        try {
            commitE = devopsGitRepository.getCommit(
                    gitlabProjectPayload.getGitlabProjectId(), MASTER, gitlabProjectPayload.getUserId());
        } catch (Exception e) {
            commitE = new CommitE();
        }
        DevopsBranchE devopsBranchE = new DevopsBranchE();
        devopsBranchE.setUserId(TypeUtil.objToLong(gitlabProjectPayload.getUserId()));
        devopsBranchE.setApplicationE(applicationE);
        devopsBranchE.setBranchName(MASTER);
        devopsBranchE.setCheckoutCommit(commitE.getId());
        devopsBranchE.setCheckoutDate(commitE.getCommittedDate());
        devopsBranchE.setLastCommitUser(TypeUtil.objToLong(gitlabProjectPayload.getUserId()));
        devopsBranchE.setLastCommitMsg(commitE.getMessage());
        devopsBranchE.setLastCommitDate(commitE.getCommittedDate());
        devopsBranchE.setLastCommit(commitE.getId());
        devopsGitRepository.createDevopsBranch(devopsBranchE);
    }

    private void replaceParams(ApplicationE applicationE, ProjectE projectE, Organization organization,
                               String applicationDir) {
        try {
            File file = new File(gitUtil.getWorkingDirectory(applicationDir));
            Map<String, String> params = new HashMap<>();
            params.put("{{group.name}}", organization.getCode() + "-" + projectE.getCode());
            params.put("{{service.code}}", applicationE.getCode());
            FileUtil.replaceReturnFile(file, params);
        } catch (Exception e) {
            //删除模板
            gitUtil.deleteWorkingDirectory(applicationDir);
            throw new CommonException(e.getMessage(), e);
        }
    }

    private String getToken(DevOpsAppPayload gitlabProjectPayload, String applicationDir, UserAttrE userAttrE) {
        String accessToken = userAttrE.getGitlabToken();
        if (accessToken == null) {
            accessToken = gitlabRepository.createToken(gitlabProjectPayload.getGitlabProjectId(),
                    applicationDir, gitlabProjectPayload.getUserId());
            userAttrE.setGitlabToken(accessToken);
            userAttrRepository.update(userAttrE);
        }
        return accessToken;
    }

    @Override
    public Boolean applicationExist(String uuid) {
        return applicationRepository.applicationExist(uuid);
    }


    @Override
    public String queryFile(String token, String type) {
        ApplicationE applicationE = applicationRepository.queryByToken(token);
        if (applicationE == null) {
            return null;
        }
        try {
            ProjectE projectE = iamRepository.queryIamProject(applicationE.getProjectE().getId());
            Organization organization = iamRepository.queryOrganizationById(projectE.getOrganization().getId());
            InputStream inputStream;
            if (type == null) {
                inputStream = this.getClass().getResourceAsStream("/shell/ci.sh");
            } else {
                inputStream = this.getClass().getResourceAsStream("/shell/" + type + ".sh");
            }
            Map<String, String> params = new HashMap<>();
            params.put("{{ GROUP_NAME }}", organization.getCode() + "-" + projectE.getCode());
            params.put("{{ PROJECT_NAME }}", applicationE.getCode());
            return FileUtil.replaceReturnString(inputStream, params);
        } catch (CommonException e) {
            return null;
        }
    }

    @Override
    public List<ApplicationCodeDTO> listByEnvId(Long projectId, Long envId, String status, Long appId) {
        List<ApplicationCodeDTO> applicationCodeDTOS = ConvertHelper
                .convertList(applicationRepository.listByEnvId(projectId, envId, status),
                        ApplicationCodeDTO.class);
        if (appId != null) {
            ApplicationE applicationE = applicationRepository.query(appId);
            ApplicationCodeDTO applicationCodeDTO = new ApplicationCodeDTO();
            BeanUtils.copyProperties(applicationE, applicationCodeDTO);
            ApplicationMarketE applicationMarketE = applicationMarketRepository.queryByAppId(appId);
            if (applicationMarketE != null) {
                applicationCodeDTO.setPublishLevel(applicationMarketE.getPublishLevel());
                applicationCodeDTO.setContributor(applicationMarketE.getContributor());
                applicationCodeDTO.setDescription(applicationMarketE.getDescription());
            }
            for (int i = 0; i < applicationCodeDTOS.size(); i++) {
                if (applicationCodeDTOS.get(i).getId().equals(applicationE.getId())) {
                    applicationCodeDTOS.remove(applicationCodeDTOS.get(i));
                }
            }
            applicationCodeDTOS.add(0, applicationCodeDTO);
        }
        return applicationCodeDTOS;
    }

    @Override
    public Page<ApplicationCodeDTO> pageByEnvId(Long projectId, Long envId, PageRequest pageRequest) {
        return ConvertPageHelper.convertPage(applicationRepository.pageByEnvId(projectId, envId, pageRequest),
                ApplicationCodeDTO.class);
    }

    @Override
    public Page<ApplicationReqDTO> listByActiveAndPubAndVersion(Long projectId, PageRequest pageRequest,
                                                                String params) {
        return ConvertPageHelper.convertPage(applicationRepository
                        .listByActiveAndPubAndVersion(projectId, true, pageRequest, params),
                ApplicationReqDTO.class);
    }

    @Override
    public List<AppUserPermissionRepDTO> listAllUserPermission(Long appId) {
        List<Long> userIds = appUserPermissionRepository.listAll(appId).stream().map(AppUserPermissionE::getIamUserId)
                .collect(Collectors.toList());
        List<UserE> userEList = iamRepository.listUsersByIds(userIds);
        List<AppUserPermissionRepDTO> resultList = new ArrayList<>();
        userEList.forEach(
                e -> resultList.add(new AppUserPermissionRepDTO(e.getId(), e.getLoginName(), e.getRealName())));
        return resultList;
    }

    @Override
    public Boolean validateRepositoryUrlAndToken(GitPlatformType gitPlatformType, String repositoryUrl, String accessToken) {
        if (!REPOSITORY_URL_PATTERN.matcher(repositoryUrl).matches()) {
            return Boolean.FALSE;
        }

        // 当不存在access_token时，默认将仓库识别为公开的
        return GitUtil.validRepositoryUrl(repositoryUrl, accessToken);
    }

    /**
     * ensure the repository url and access token are valid.
     *
     * @param gitPlatformType git platform type
     * @param repositoryUrl   repository url
     * @param accessToken    access token (Nullable)
     */
    private void checkRepositoryUrlAndToken(GitPlatformType gitPlatformType, String repositoryUrl, String accessToken) {
        Boolean validationResult = validateRepositoryUrlAndToken(gitPlatformType, repositoryUrl, accessToken);
        if (Boolean.FALSE.equals(validationResult)) {
            throw new CommonException("error.repository.token.invalid");
        } else if (validationResult == null) {
            throw new CommonException("error.repository.empty");
        }
    }

    @Override
    @Saga(code = "devops-import-gitlab-project", description = "Devops从外部代码平台导入到gitlab项目", inputSchema = "{}")
    public ApplicationRepDTO importApplicationFromGitPlatform(Long projectId, ApplicationImportDTO applicationImportDTO) {
        // 获取当前操作的用户的信息
        UserAttrE userAttrE = userAttrRepository.queryById(TypeUtil.objToLong(GitUserNameUtil.getUserId()));

        // 校验application信息的格式
        ApplicationValidator.checkApplication(applicationImportDTO);

        // 校验名称唯一性
        applicationRepository.checkName(projectId, applicationImportDTO.getName());

        // 校验code唯一性
        applicationRepository.checkCode(projectId, applicationImportDTO.getCode());

        // 校验repository（和token） 地址是否有效
        GitPlatformType gitPlatformType = GitPlatformType.from(applicationImportDTO.getPlatformType());
        checkRepositoryUrlAndToken(gitPlatformType, applicationImportDTO.getRepositoryUrl(), applicationImportDTO.getAccessToken());

        ProjectE projectE = iamRepository.queryIamProject(projectId);
        Organization organization = iamRepository.queryOrganizationById(projectE.getOrganization().getId());

        ApplicationE applicationE = fromImportDtoToEntity(applicationImportDTO);

        applicationE.initProjectE(projectId);


        applicationE.initActive(true);
        applicationE.initSynchro(false);
        applicationE.setIsSkipCheckPermission(applicationImportDTO.getIsSkipCheckPermission());

        // 查询创建应用所在的gitlab应用组
        GitlabGroupE gitlabGroupE = devopsProjectRepository.queryDevopsProject(applicationE.getProjectE().getId());
        GitlabMemberE gitlabMemberE = gitlabGroupMemberRepository.getUserMemberByUserId(
                TypeUtil.objToInteger(gitlabGroupE.getDevopsAppGroupId()),
                TypeUtil.objToInteger(userAttrE.getGitlabUserId()));

        // 校验用户的gitlab权限
        if (gitlabMemberE == null || gitlabMemberE.getAccessLevel() != AccessLevel.OWNER.toValue()) {
            throw new CommonException("error.user.not.owner");
        }

        // 创建应用
        applicationE = applicationRepository.create(applicationE);

        // 校验创建成功与否
        Long appId = applicationE.getId();
        if (appId == null) {
            throw new CommonException("error.application.create.insert");
        }

        // 创建saga payload
        DevOpsAppImportPayload devOpsAppImportPayload = new DevOpsAppImportPayload();
        devOpsAppImportPayload.setType(APPLICATION);
        devOpsAppImportPayload.setPath(applicationImportDTO.getCode());
        devOpsAppImportPayload.setOrganizationId(organization.getId());
        devOpsAppImportPayload.setUserId(TypeUtil.objToInteger(userAttrE.getGitlabUserId()));
        devOpsAppImportPayload.setGroupId(TypeUtil.objToInteger(gitlabGroupE.getDevopsAppGroupId()));
        devOpsAppImportPayload.setUserIds(applicationImportDTO.getUserIds());
        devOpsAppImportPayload.setSkipCheckPermission(applicationImportDTO.getIsSkipCheckPermission());
        devOpsAppImportPayload.setAppId(appId);
        devOpsAppImportPayload.setIamProjectId(projectId);
        devOpsAppImportPayload.setPlatformType(gitPlatformType);
        devOpsAppImportPayload.setRepositoryUrl(applicationImportDTO.getRepositoryUrl());
        devOpsAppImportPayload.setAccessToken(applicationImportDTO.getAccessToken());

        // 如果不跳过权限检查
        List<Long> userIds = applicationImportDTO.getUserIds();
        if (!applicationImportDTO.getIsSkipCheckPermission() && userIds != null && !userIds.isEmpty()) {
            userIds.forEach(e -> appUserPermissionRepository.create(e, appId));
        }

        String input = gson.toJson(devOpsAppImportPayload);
        sagaClient.startSaga("devops-import-gitlab-project", new StartInstanceDTO(input, "", "", ResourceLevel.PROJECT.value(), projectId));

        return ConvertHelper.convert(applicationRepository.query(appId), ApplicationRepDTO.class);
    }

    private ApplicationE fromImportDtoToEntity(ApplicationImportDTO applicationImportDTO) {
        ApplicationE applicationE = ApplicationFactory.createApplicationE();
        applicationE.initProjectE(applicationImportDTO.getProjectId());
        BeanUtils.copyProperties(applicationImportDTO, applicationE);
        if (applicationImportDTO.getApplicationTemplateId() != null) {
            applicationE.initApplicationTemplateE(applicationImportDTO.getApplicationTemplateId());
        }
        return applicationE;
    }
}
