package io.choerodon.devops.app.service.impl;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.zaxxer.hikari.util.UtilityElf;
import feign.FeignException;
import io.choerodon.asgard.saga.annotation.Saga;
import io.choerodon.asgard.saga.dto.StartInstanceDTO;
import io.choerodon.asgard.saga.feign.SagaClient;
import io.choerodon.core.domain.Page;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.devops.api.dto.gitlab.MemberDTO;
import io.choerodon.devops.api.dto.iam.UserWithRoleDTO;
import io.choerodon.devops.app.service.*;
import io.choerodon.devops.domain.application.entity.*;
import io.choerodon.devops.domain.application.entity.gitlab.*;
import io.choerodon.devops.domain.application.entity.iam.UserE;
import io.choerodon.devops.domain.application.event.GitlabProjectPayload;
import io.choerodon.devops.domain.application.event.IamAppPayLoad;
import io.choerodon.devops.domain.application.repository.*;
import io.choerodon.devops.domain.application.valueobject.*;
import io.choerodon.devops.infra.common.util.*;
import io.choerodon.devops.infra.common.util.enums.InstanceStatus;
import io.choerodon.devops.infra.common.util.enums.ResourceType;
import io.choerodon.devops.infra.common.util.enums.ServiceStatus;
import io.choerodon.devops.infra.dataobject.*;
import io.choerodon.devops.infra.dataobject.gitlab.BranchDO;
import io.choerodon.devops.infra.dataobject.gitlab.CommitDO;
import io.choerodon.devops.infra.dataobject.gitlab.CommitStatuseDO;
import io.choerodon.devops.infra.dataobject.gitlab.GroupDO;
import io.choerodon.devops.infra.feign.GitlabServiceClient;
import io.choerodon.devops.infra.mapper.*;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.models.*;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;

@Service
public class DevopsCheckLogServiceImpl implements DevopsCheckLogService {

    private static final String SONARQUBE = "sonarqube";
    private static final String TWELVE_VERSION = "0.12.0";
    private static final String APP = "app: ";
    private static final Integer ADMIN = 1;
    private static final String ENV = "ENV";
    private static final String SERVICE_LABEL = "choerodon.io/network";
    private static final String PROJECT_OWNER = "role/project/default/project-owner";
    private static final String SERVICE = "service";
    private static final String SUCCESS = "success";
    private static final String FAILED = "failed: ";
    private static final String SERIAL_STRING = " serializable to yaml";
    private static final String APPLICATION = "application";
    private static final String ERROR_UPDATE_APP = "error.application.update";
    private static final String DEVELOPMENT = "development-application";
    private static final String TEST = "test-application";
    private static final String YAML_FILE = ".yaml";

    private static final Logger LOGGER = LoggerFactory.getLogger(DevopsCheckLogServiceImpl.class);
    private static final ExecutorService executorService = new ThreadPoolExecutor(0, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(), new UtilityElf.DefaultThreadFactory("devops-upgrade", false));
    private static io.kubernetes.client.JSON json = new io.kubernetes.client.JSON();
    private static final String SERVICE_PATTERN = "[a-zA-Z0-9_\\.][a-zA-Z0-9_\\-\\.]*[a-zA-Z0-9_\\-]|[a-zA-Z0-9_]";
    private Gson gson = new Gson();

    @Value("${services.gateway.url}")
    private String gatewayUrl;
    @Value("${services.helm.url}")
    private String helmUrl;

    @Autowired
    private ApplicationMapper applicationMapper;
    @Autowired
    private DevopsEnvironmentMapper devopsEnvironmentMapper;
    @Autowired
    private GitlabRepository gitlabRepository;
    @Autowired
    private UserAttrRepository userAttrRepository;
    @Autowired
    private DevopsCheckLogRepository devopsCheckLogRepository;
    @Autowired
    private GitlabServiceClient gitlabServiceClient;
    @Autowired
    private DevopsGitRepository devopsGitRepository;
    @Autowired
    private IamRepository iamRepository;
    @Autowired
    private DevopsProjectRepository devopsProjectRepository;
    @Autowired
    private DevopsEnvironmentRepository devopsEnvironmentRepository;
    @Autowired
    private DevopsEnvironmentService devopsEnvironmentService;
    @Autowired
    private ApplicationInstanceRepository applicationInstanceRepository;
    @Autowired
    private ApplicationVersionRepository applicationVersionRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private ApplicationInstanceService applicationInstanceService;
    @Autowired
    private DevopsServiceRepository devopsServiceRepository;
    @Autowired
    private DevopsIngressRepository devopsIngressRepository;
    @Autowired
    private DevopsIngressService devopsIngressService;
    @Autowired
    private SagaClient sagaClient;
    @Autowired
    private DevopsEnvResourceDetailRepository devopsEnvResourceDetailRepository;
    @Autowired
    private DevopsEnvResourceRepository devopsEnvResourceRepository;
    @Autowired
    private DevopsServiceInstanceRepository devopsServiceInstanceRepository;
    @Autowired
    private GitlabProjectRepository gitlabProjectRepository;
    @Autowired
    private DevopsGitlabCommitRepository devopsGitlabCommitRepository;
    @Autowired
    private DevopsGitlabPipelineRepository devopsGitlabPipelineRepository;
    @Autowired
    private DevopsGitlabPipelineMapper devopsGitlabPipelineMapper;
    @Autowired
    private DevopsGitlabCommitMapper devopsGitlabCommitMapper;
    @Autowired
    private DevopsProjectMapper devopsProjectMapper;
    @Autowired
    private GitlabGroupMemberRepository gitlabGroupMemberRepository;
    @Autowired
    private GitlabUserRepository gitlabUserRepository;
    @Autowired
    private DevopsEnvPodMapper devopsEnvPodMapper;
    @Autowired
    private GitUtil gitUtil;
    @Autowired
    private EnvUtil envUtil;
    @Autowired
    private ApplicationVersionMapper applicationVersionMapper;
    @Autowired
    private DevopsProjectConfigRepository devopsProjectConfigRepository;
    @Autowired
    private ApplicationService applicationService;

    @Override
    public void checkLog(String version) {
        LOGGER.info("start upgrade task");
        executorService.submit(new UpgradeTask(version));
    }


    private void createGitFile(String repoPath, Git git, String relativePath, String content) {
        GitUtil newGitUtil = new GitUtil();
        try {
            newGitUtil.createFileInRepo(repoPath, git, relativePath, content, null);
        } catch (IOException e) {
            LOGGER.info("error.file.open: " + relativePath, e);
        } catch (GitAPIException e) {
            LOGGER.info("error.git.commit: " + relativePath, e);
        }

    }

    private String getObjectYaml(Object object) {
        Tag tag = new Tag(object.getClass().toString());
        SkipNullRepresenterUtil skipNullRepresenter = new SkipNullRepresenterUtil();
        skipNullRepresenter.addClassTag(object.getClass(), tag);
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setAllowReadOnlyProperties(true);
        Yaml yaml = new Yaml(skipNullRepresenter, options);
        return yaml.dump(object).replace("!<" + tag.getValue() + ">", "---");
    }


    private void updateWebHook(List<CheckLog> logs) {
        List<ApplicationDO> applications = applicationMapper.selectAll();
        applications.stream()
                .filter(applicationDO ->
                        applicationDO.getHookId() != null)
                .forEach(applicationDO -> {
                    CheckLog checkLog = new CheckLog();
                    checkLog.setContent(APP + applicationDO.getName() + "update gitlab webhook");
                    try {
                        gitlabRepository.updateWebHook(applicationDO.getGitlabProjectId(), TypeUtil.objToInteger(applicationDO.getHookId()), ADMIN);
                        checkLog.setResult(SUCCESS);
                    } catch (Exception e) {
                        checkLog.setResult(FAILED + e.getMessage());
                    }
                    logs.add(checkLog);
                });
    }

    private void syncCommit(List<CheckLog> logs) {
        List<ApplicationDO> applications = applicationMapper.selectAll();
        applications.stream().filter(applicationDO -> applicationDO.getGitlabProjectId() != null)
                .forEach(applicationDO -> {
                            CheckLog checkLog = new CheckLog();
                            checkLog.setContent(APP + applicationDO.getName() + "sync gitlab commit");
                            try {
                                List<CommitDO> commitDOS = gitlabProjectRepository.listCommits(applicationDO.getGitlabProjectId(), ADMIN);
                                commitDOS.forEach(commitDO -> {
                                    DevopsGitlabCommitE devopsGitlabCommitE = new DevopsGitlabCommitE();
                                    devopsGitlabCommitE.setAppId(applicationDO.getId());
                                    devopsGitlabCommitE.setCommitContent(commitDO.getMessage());
                                    devopsGitlabCommitE.setCommitSha(commitDO.getId());
                                    devopsGitlabCommitE.setUrl(commitDO.getUrl());
                                    if ("root".equals(commitDO.getAuthorName())) {
                                        devopsGitlabCommitE.setUserId(1L);
                                    } else {
                                        UserE userE = iamRepository.queryByEmail(applicationDO.getProjectId(),
                                                commitDO.getAuthorEmail());
                                        if (userE != null) {
                                            devopsGitlabCommitE.setUserId(userE.getId());
                                        }
                                    }
                                    devopsGitlabCommitE.setCommitDate(commitDO.getCommittedDate());
                                    devopsGitlabCommitRepository.create(devopsGitlabCommitE);

                                });
                                logs.add(checkLog);

                            } catch (Exception e) {
                                checkLog.setResult(FAILED + e.getMessage());
                            }
                        }
                );
    }


    private void syncPipelines(List<CheckLog> logs) {
        List<ApplicationDO> applications = applicationMapper.selectAll();
        applications.stream().filter(applicationDO -> applicationDO.getGitlabProjectId() != null)
                .forEach(applicationDO -> {
                    CheckLog checkLog = new CheckLog();
                    checkLog.setContent(APP + applicationDO.getName() + "sync gitlab pipeline");
                    try {
                        List<GitlabPipelineE> pipelineDOS = gitlabProjectRepository
                                .listPipeline(applicationDO.getGitlabProjectId(), ADMIN);
                        pipelineDOS.forEach(pipelineE -> {
                            GitlabPipelineE gitlabPipelineE = gitlabProjectRepository
                                    .getPipeline(applicationDO.getGitlabProjectId(), pipelineE.getId(), ADMIN);
                            DevopsGitlabPipelineE devopsGitlabPipelineE = new DevopsGitlabPipelineE();
                            devopsGitlabPipelineE.setAppId(applicationDO.getId());
                            Long userId = userAttrRepository
                                    .queryUserIdByGitlabUserId(TypeUtil.objToLong(gitlabPipelineE.getUser()
                                            .getId()));
                            devopsGitlabPipelineE.setPipelineCreateUserId(userId);
                            devopsGitlabPipelineE.setPipelineId(TypeUtil.objToLong(gitlabPipelineE.getId()));
                            if (gitlabPipelineE.getStatus().toString().equals(SUCCESS)) {
                                devopsGitlabPipelineE.setStatus("passed");
                            } else {
                                devopsGitlabPipelineE.setStatus(gitlabPipelineE.getStatus().toString());
                            }
                            try {
                                devopsGitlabPipelineE
                                        .setPipelineCreationDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                                .parse(gitlabPipelineE.getCreatedAt()));
                            } catch (ParseException e) {
                                checkLog.setResult(FAILED + e.getMessage());
                            }
                            DevopsGitlabCommitE devopsGitlabCommitE = devopsGitlabCommitRepository
                                    .queryByShaAndRef(gitlabPipelineE.getSha(), gitlabPipelineE.getRef());
                            if (devopsGitlabCommitE != null) {
                                devopsGitlabCommitE.setRef(gitlabPipelineE.getRef());
                                devopsGitlabCommitRepository.update(devopsGitlabCommitE);
                                devopsGitlabPipelineE.initDevopsGitlabCommitEById(devopsGitlabCommitE.getId());
                            }
                            List<Stage> stages = new ArrayList<>();
                            List<String> stageNames = new ArrayList<>();
                            List<Integer> gitlabJobIds = gitlabProjectRepository
                                    .listJobs(applicationDO.getGitlabProjectId(),
                                            TypeUtil.objToInteger(devopsGitlabPipelineE.getPipelineId()), ADMIN)
                                    .stream().map(GitlabJobE::getId).collect(Collectors.toList());

                            gitlabProjectRepository
                                    .getCommitStatuse(applicationDO.getGitlabProjectId(), gitlabPipelineE.getSha(),
                                            ADMIN)
                                    .forEach(commitStatuseDO -> {
                                        if (gitlabJobIds.contains(commitStatuseDO.getId())) {
                                            Stage stage = getPipelineStage(commitStatuseDO);
                                            stages.add(stage);
                                        } else if (commitStatuseDO.getName().equals(SONARQUBE) && !stageNames
                                                .contains(SONARQUBE) && !stages.isEmpty()) {
                                            Stage stage = getPipelineStage(commitStatuseDO);
                                            stages.add(stage);
                                            stageNames.add(commitStatuseDO.getName());
                                        }
                                    });
                            devopsGitlabPipelineE.setStage(JSONArray.toJSONString(stages));
                            devopsGitlabPipelineRepository.create(devopsGitlabPipelineE);
                        });
                    } catch (Exception e) {
                        checkLog.setResult(FAILED + e.getMessage());
                    }
                    logs.add(checkLog);
                });
        devopsGitlabPipelineRepository.deleteWithoutCommit();
    }

    private void fixPipelines(List<CheckLog> logs) {
        List<DevopsGitlabPipelineDO> gitlabPipelineES = devopsGitlabPipelineMapper.selectAll();
        gitlabPipelineES.forEach(devopsGitlabPipelineDO -> {
            CheckLog checkLog = new CheckLog();
            checkLog.setContent(APP + devopsGitlabPipelineDO.getPipelineId() + "fix pipeline");
            try {
                ApplicationDO applicationDO = applicationMapper.selectByPrimaryKey(devopsGitlabPipelineDO.getAppId());
                if (applicationDO.getGitlabProjectId() != null) {
                    DevopsGitlabCommitDO devopsGitlabCommitDO = devopsGitlabCommitMapper
                            .selectByPrimaryKey(devopsGitlabPipelineDO.getCommitId());
                    if (devopsGitlabCommitDO != null) {
                        GitlabPipelineE gitlabPipelineE = gitlabProjectRepository
                                .getPipeline(applicationDO.getGitlabProjectId(),
                                        TypeUtil.objToInteger(devopsGitlabPipelineDO.getPipelineId()), ADMIN);
                        List<Stage> stages = new ArrayList<>();
                        List<String> stageNames = new ArrayList<>();
                        List<Integer> gitlabJobIds = gitlabProjectRepository
                                .listJobs(applicationDO.getGitlabProjectId(),
                                        TypeUtil.objToInteger(devopsGitlabPipelineDO.getPipelineId()),
                                        ADMIN).stream().map(GitlabJobE::getId).collect(Collectors.toList());

                        gitlabProjectRepository.getCommitStatuse(applicationDO.getGitlabProjectId(),
                                devopsGitlabCommitDO.getCommitSha(), ADMIN)
                                .forEach(commitStatuseDO -> {
                                    if (gitlabJobIds.contains(commitStatuseDO.getId())) {
                                        Stage stage = getPipelineStage(commitStatuseDO);
                                        stages.add(stage);
                                    } else if (commitStatuseDO.getName().equals(SONARQUBE) && !stageNames
                                            .contains(SONARQUBE) && !stages.isEmpty()) {
                                        Stage stage = getPipelineStage(commitStatuseDO);
                                        stages.add(stage);
                                        stageNames.add(commitStatuseDO.getName());
                                    }
                                });
                        devopsGitlabPipelineDO.setStatus(gitlabPipelineE.getStatus().toString());
                        devopsGitlabPipelineDO.setStage(JSONArray.toJSONString(stages));
                        devopsGitlabPipelineMapper.updateByPrimaryKeySelective(devopsGitlabPipelineDO);
                    }
                }
                checkLog.setResult(SUCCESS);
            } catch (Exception e) {
                checkLog.setResult(FAILED + e.getMessage());
            }
            logs.add(checkLog);
        });

    }

    private Stage getPipelineStage(CommitStatuseDO commitStatuseDO) {
        Stage stage = new Stage();
        stage.setDescription(commitStatuseDO.getDescription());
        stage.setId(commitStatuseDO.getId());
        stage.setName(commitStatuseDO.getName());
        stage.setStatus(commitStatuseDO.getStatus());
        if (commitStatuseDO.getFinishedAt() != null) {
            stage.setFinishedAt(commitStatuseDO.getFinishedAt());
        }
        if (commitStatuseDO.getStartedAt() != null) {
            stage.setStartedAt(commitStatuseDO.getStartedAt());
        }
        return stage;
    }


    private void syncCommandId() {
        devopsCheckLogRepository.syncCommandId();
    }

    private void syncCommandVersionId() {
        devopsCheckLogRepository.syncCommandVersionId();
    }

    private void syncGitOpsUserAccess(List<CheckLog> logs, String version) {
        List<Long> projectIds = devopsProjectMapper.selectAll().stream().
                filter(devopsProjectDO -> devopsProjectDO.getDevopsEnvGroupId() != null && devopsProjectDO
                        .getDevopsAppGroupId() != null).map(DevopsProjectDO::getIamProjectId)
                .collect(Collectors.toList());
        projectIds.forEach(projectId -> {
            Page<UserWithRoleDTO> allProjectUser = iamRepository
                    .queryUserPermissionByProjectId(projectId, new PageRequest(), false);
            if (!allProjectUser.getContent().isEmpty()) {
                allProjectUser.forEach(userWithRoleDTO -> {
                    // 如果是项目成员
                    if (userWithRoleDTO.getRoles().stream().noneMatch(roleDTO -> roleDTO.getCode().equals(PROJECT_OWNER))) {
                        CheckLog checkLog = new CheckLog();
                        checkLog.setContent(userWithRoleDTO.getLoginName() + ": remove env permission");
                        try {
                            UserAttrE userAttrE = userAttrRepository.queryById(userWithRoleDTO.getId());
                            if (userAttrE != null) {
                                Integer gitlabUserId = TypeUtil.objToInteger(userAttrE.getGitlabUserId());
                                GitlabGroupE gitlabGroupE = devopsProjectRepository.queryDevopsProject(projectId);
                                GitlabMemberE envgroupMemberE = gitlabGroupMemberRepository.getUserMemberByUserId(
                                        TypeUtil.objToInteger(gitlabGroupE.getDevopsEnvGroupId()), gitlabUserId);
                                GitlabMemberE appgroupMemberE = gitlabGroupMemberRepository.getUserMemberByUserId(
                                        TypeUtil.objToInteger(gitlabGroupE.getDevopsAppGroupId()), gitlabUserId);
                                if (version.equals(TWELVE_VERSION)) {
                                    if (appgroupMemberE != null && appgroupMemberE.getId() != null) {
                                        gitlabGroupMemberRepository.deleteMember(
                                                TypeUtil.objToInteger(gitlabGroupE.getDevopsAppGroupId()), gitlabUserId);
                                    }
                                } else {
                                    if (envgroupMemberE != null && envgroupMemberE.getId() != null) {
                                        gitlabGroupMemberRepository.deleteMember(
                                                TypeUtil.objToInteger(gitlabGroupE.getDevopsEnvGroupId()), gitlabUserId);
                                    }
                                }
                            }
                            checkLog.setResult(SUCCESS);
                            LOGGER.info(SUCCESS);
                        } catch (Exception e) {
                            LOGGER.info(FAILED + e.getMessage());
                            checkLog.setResult(FAILED + e.getMessage());
                        }
                        logs.add(checkLog);
                    }
                });
            }
        });
    }

    private void syncGitlabUserName(List<CheckLog> logs) {
        userAttrRepository.list().stream().filter(userAttrE -> userAttrE.getGitlabUserId() != null).forEach(userAttrE ->
                {
                    CheckLog checkLog = new CheckLog();
                    try {
                        UserE userE = iamRepository.queryUserByUserId(userAttrE.getIamUserId());
                        if (Pattern.matches(SERVICE_PATTERN, userE.getLoginName())) {
                            userAttrE.setGitlabUserName(userE.getLoginName());
                            if (userE.getLoginName().equals("admin") || userE.getLoginName().equals("admin1")) {
                                userAttrE.setGitlabUserName("root");
                            }
                        } else {
                            GitlabUserE gitlabUserE = gitlabUserRepository.getGitlabUserByUserId(TypeUtil.objToInteger(userAttrE.getGitlabUserId()));
                            userAttrE.setGitlabUserName(gitlabUserE.getUsername());
                        }
                        userAttrRepository.update(userAttrE);
                        LOGGER.info(SUCCESS);
                        checkLog.setResult(SUCCESS);
                        checkLog.setContent(userAttrE.getGitlabUserId() + " : init Name Succeed");
                    } catch (Exception e) {
                        LOGGER.info(e.getMessage());
                        checkLog.setResult(FAILED);
                        checkLog.setContent(userAttrE.getGitlabUserId() + " : init Name Failed");
                    }
                    logs.add(checkLog);
                }
        );
    }

    private class SyncInstanceByEnv {
        private List<CheckLog> logs;
        private DevopsEnvironmentE env;
        private String filePath;
        private Git git;

        SyncInstanceByEnv(List<CheckLog> logs, DevopsEnvironmentE env, String filePath, Git git) {
            this.logs = logs;
            this.env = env;
            this.filePath = filePath;
            this.git = git;
        }

        void invoke() {
            applicationInstanceRepository.selectByEnvId(env.getId()).stream()
                    .filter(a -> !InstanceStatus.DELETED.getStatus().equals(a.getStatus()))
                    .forEach(applicationInstanceE -> {
                        CheckLog checkLog = new CheckLog();
                        try {
                            checkLog.setContent("instance: " + applicationInstanceE.getCode() + SERIAL_STRING);
                            String fileRelativePath = "release-" + applicationInstanceE.getCode() + YAML_FILE;
                            if (!new File(filePath + File.separator + fileRelativePath).exists()) {
                                createGitFile(filePath, git, fileRelativePath,
                                        getObjectYaml(getC7NHelmRelease(applicationInstanceE)));
                                checkLog.setResult(SUCCESS);
                            }
                            LOGGER.info(checkLog.toString());
                        } catch (Exception e) {
                            LOGGER.info("{}:{} instance/{} sync failed {}",
                                    env.getCode(), env.getId(), applicationInstanceE.getCode(), e);
                            checkLog.setResult(FAILED + e.getMessage());
                        }
                        logs.add(checkLog);
                    });
        }


        private C7nHelmRelease getC7NHelmRelease(ApplicationInstanceE applicationInstanceE) {
            ApplicationVersionE applicationVersionE = applicationVersionRepository
                    .query(applicationInstanceE.getApplicationVersionE().getId());
            ApplicationE applicationE = applicationRepository.query(applicationInstanceE.getApplicationE().getId());
            C7nHelmRelease c7nHelmRelease = new C7nHelmRelease();
            c7nHelmRelease.getMetadata().setName(applicationInstanceE.getCode());
            c7nHelmRelease.getSpec().setRepoUrl(applicationVersionE.getRepository());
            c7nHelmRelease.getSpec().setChartName(applicationE.getCode());
            c7nHelmRelease.getSpec().setChartVersion(applicationVersionE.getVersion());
            c7nHelmRelease.getSpec().setValues(applicationInstanceService.getReplaceResult(
                    applicationVersionRepository.queryValue(applicationVersionE.getId()),
                    applicationInstanceRepository.queryValueByEnvIdAndAppId(
                            applicationInstanceE.getDevopsEnvironmentE().getId(),
                            applicationE.getId())).getDeltaYaml().trim());
            return c7nHelmRelease;
        }
    }

    private class SynServiceByEnv {
        private List<CheckLog> logs;
        private DevopsEnvironmentE env;
        private String filePath;
        private Git git;

        SynServiceByEnv(List<CheckLog> logs, DevopsEnvironmentE env, String filePath, Git git) {
            this.logs = logs;
            this.env = env;
            this.filePath = filePath;
            this.git = git;
        }

        void invoke() {
            devopsServiceRepository.selectByEnvId(env.getId()).stream()
                    .filter(t -> !ServiceStatus.DELETED.getStatus().equals(t.getStatus()))
                    .forEach(devopsServiceE -> {
                        CheckLog checkLog = new CheckLog();
                        try {
                            checkLog.setContent("service: " + devopsServiceE.getName() + SERIAL_STRING);
                            String fileRelativePath = "svc-" + devopsServiceE.getName() + YAML_FILE;
                            if (!new File(filePath + File.separator + fileRelativePath).exists()) {
                                createGitFile(filePath, git, fileRelativePath,
                                        getObjectYaml(getService(devopsServiceE)));
                                checkLog.setResult(SUCCESS);
                            }
                            LOGGER.info(checkLog.toString());
                        } catch (Exception e) {
                            LOGGER.info("{}:{} service/{} sync failed {}",
                                    env.getCode(), env.getId(), devopsServiceE.getName(), e);
                            checkLog.setResult(FAILED + e.getMessage());
                        }
                        logs.add(checkLog);
                    });
        }

        private V1Service getService(DevopsServiceE devopsServiceE) {
            V1Service service = new V1Service();
            service.setKind("Service");
            service.setApiVersion("v1");

            // metadata
            V1ObjectMeta metadata = new V1ObjectMeta();
            metadata.setName(devopsServiceE.getName());
            // metadata / labels
            Map<String, String> label = new HashMap<>();
            label.put(SERVICE_LABEL, SERVICE);
            metadata.setLabels(label);
            // metadata / annotations
            if (devopsServiceE.getAnnotations() != null) {
                Map<String, String> annotations = gson.fromJson(
                        devopsServiceE.getAnnotations(), new TypeToken<Map<String, String>>() {
                        }.getType());
                metadata.setAnnotations(annotations);
            }
            // set metadata
            service.setMetadata(metadata);

            V1ServiceSpec spec = new V1ServiceSpec();
            // spec / ports
            spec.setPorts(getServicePort(devopsServiceE));

            // spec / selector
            if (devopsServiceE.getLabels() != null) {
                Map<String, String> selector = gson.fromJson(
                        devopsServiceE.getLabels(), new TypeToken<Map<String, String>>() {
                        }.getType());
                spec.setSelector(selector);
            }

            // spec / externalIps
            if (!StringUtils.isEmpty(devopsServiceE.getExternalIp())) {
                List<String> externalIps = new ArrayList<>(Arrays.asList(devopsServiceE.getExternalIp().split(",")));
                spec.setExternalIPs(externalIps);
            }

            spec.setSessionAffinity("None");
            spec.type("ClusterIP");
            service.setSpec(spec);

            return service;
        }

        private List<V1ServicePort> getServicePort(DevopsServiceE devopsServiceE) {
            final Integer[] serialNumber = {0};
            List<V1ServicePort> ports;
            if (devopsServiceE.getPorts() == null) {
                List<DevopsServiceAppInstanceE> devopsServiceAppInstanceES =
                        devopsServiceInstanceRepository.selectByServiceId(devopsServiceE.getId());
                if (!devopsServiceAppInstanceES.isEmpty()) {
                    DevopsEnvResourceE devopsEnvResourceE = devopsEnvResourceRepository.queryResource(
                            devopsServiceAppInstanceES.get(0).getAppInstanceId(),
                            null, null,
                            ResourceType.SERVICE.getType(),
                            devopsServiceE.getName());
                    DevopsEnvResourceDetailE devopsEnvResourceDetailE = devopsEnvResourceDetailRepository
                            .query(devopsEnvResourceE.getDevopsEnvResourceDetailE().getId());
                    ports = getV1ServicePortsWithNoPorts(devopsServiceE, devopsEnvResourceDetailE);
                    devopsServiceRepository.update(devopsServiceE);
                } else {
                    ports = null;
                }
            } else {
                ports = getV1ServicePortsWithPorts(devopsServiceE, serialNumber);
            }
            return ports;
        }

        private List<V1ServicePort> getV1ServicePortsWithPorts(DevopsServiceE devopsServiceE, Integer[] serialNumber) {
            return devopsServiceE.getPorts().stream()
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
                        v1ServicePort.setName(t.getName() != null ? t.getName() : "http" + serialNumber[0]++);
                        v1ServicePort.setProtocol(t.getProtocol() != null ? t.getProtocol() : "TCP");
                        return v1ServicePort;
                    }).collect(Collectors.toList());
        }

        private List<V1ServicePort> getV1ServicePortsWithNoPorts(DevopsServiceE devopsServiceE, DevopsEnvResourceDetailE devopsEnvResourceDetailE) {
            List<V1ServicePort> ports;
            V1Service v1Service = json.deserialize(devopsEnvResourceDetailE.getMessage(),
                    V1Service.class);
            String port = TypeUtil.objToString(v1Service.getSpec().getPorts().get(0).getPort());
            if (port == null) {
                port = "<none>";
            }
            String targetPort = TypeUtil.objToString(v1Service.getSpec().getPorts().get(0).getTargetPort());
            if (targetPort == null) {
                targetPort = "<none>";
            }
            String name = v1Service.getSpec().getPorts().get(0).getName();
            String protocol = v1Service.getSpec().getPorts().get(0).getProtocol();
            List<PortMapE> portMapES = new ArrayList<>();
            PortMapE portMapE = new PortMapE();
            portMapE.setName(name);
            portMapE.setPort(TypeUtil.objToLong(port));
            portMapE.setProtocol(protocol);
            portMapE.setTargetPort(targetPort);
            portMapES.add(portMapE);
            ports = portMapES.stream()
                    .map(t -> {
                        V1ServicePort v1ServicePort = new V1ServicePort();
                        if (t.getPort() != null) {
                            v1ServicePort.setPort(TypeUtil.objToInteger(t.getPort()));
                        }
                        if (t.getTargetPort() != null) {
                            v1ServicePort.setTargetPort(new IntOrString(t.getTargetPort()));
                        }
                        v1ServicePort.setName(t.getName());
                        v1ServicePort.setProtocol(t.getProtocol());
                        return v1ServicePort;
                    }).collect(Collectors.toList());
            devopsServiceE.setPorts(portMapES);
            return ports;
        }
    }

    private class SyncIngressByEnv {
        private List<CheckLog> logs;
        private DevopsEnvironmentE env;
        private String filePath;
        private Git git;

        SyncIngressByEnv(List<CheckLog> logs, DevopsEnvironmentE env, String filePath, Git git) {
            this.logs = logs;
            this.env = env;
            this.filePath = filePath;
            this.git = git;
        }

        void invoke() {
            devopsIngressRepository.listByEnvId(env.getId())
                    .forEach(devopsIngressE -> {
                        CheckLog checkLog = new CheckLog();
                        try {
                            checkLog.setContent("ingress: " + devopsIngressE.getName() + SERIAL_STRING);
                            String fileRelativePath = "ing-" + devopsIngressE.getName() + YAML_FILE;
                            if (!new File(filePath + File.separator + fileRelativePath).exists()) {
                                createGitFile(filePath, git, fileRelativePath,
                                        getObjectYaml(getV1beta1Ingress(devopsIngressE)));
                                checkLog.setResult(SUCCESS);
                            }
                            LOGGER.info(checkLog.toString());
                        } catch (Exception e) {
                            LOGGER.info("{}:{} ingress/{} sync failed {}",
                                    env.getCode(), env.getId(), devopsIngressE.getName(), e);
                            checkLog.setResult(FAILED + e.getMessage());
                        }
                        logs.add(checkLog);
                    });
        }

        private V1beta1Ingress getV1beta1Ingress(DevopsIngressE devopsIngressE) {
            V1beta1Ingress v1beta1Ingress = devopsIngressService.initV1beta1Ingress(
                    devopsIngressE.getDomain(), devopsIngressE.getName(), devopsIngressE.getCertName());
            List<DevopsIngressPathE> devopsIngressPathES =
                    devopsIngressRepository.selectByIngressId(devopsIngressE.getId());
            devopsIngressPathES.forEach(devopsIngressPathE ->
                    v1beta1Ingress.getSpec().getRules().get(0).getHttp()
                            .addPathsItem(devopsIngressService.createPath(
                                    devopsIngressPathE.getPath(),
                                    devopsIngressPathE.getServiceName(),
                                    devopsIngressPathE.getServicePort())));

            return v1beta1Ingress;
        }
    }

    class UpgradeTask implements Runnable {
        private String version;
        private Long env;

        UpgradeTask(String version) {
            this.version = version;
        }


        UpgradeTask(String version, Long env) {
            this.version = version;
            this.env = env;
        }

        @Override
        public void run() {
            DevopsCheckLogE devopsCheckLogE = new DevopsCheckLogE();
            List<CheckLog> logs = new ArrayList<>();
            devopsCheckLogE.setBeginCheckDate(new Date());
            if ("0.8".equals(version)) {
                LOGGER.info("Start to execute upgrade task 0.8");
                List<ApplicationDO> applications = applicationMapper.selectAll();
                applications.stream()
                        .filter(applicationDO ->
                                applicationDO.getGitlabProjectId() != null && applicationDO.getHookId() == null)
                        .forEach(applicationDO -> syncWebHook(applicationDO, logs));
                applications.stream()
                        .filter(applicationDO ->
                                applicationDO.getGitlabProjectId() != null)
                        .forEach(applicationDO -> syncBranches(applicationDO, logs));
            } else if ("0.9".equals(version)) {
                LOGGER.info("Start to execute upgrade task 0.9");
                syncNonEnvGroupProject(logs);
                gitOpsUserAccess();
                syncEnvProject(logs);
                syncObjects(logs, this.env);
            } else if ("0.10.0".equals(version)) {
                LOGGER.info("Start to execute upgrade task 1.0");
                updateWebHook(logs);
                syncCommit(logs);
                syncPipelines(logs);
            } else if ("0.10.4".equals(version)) {
                fixPipelines(logs);
            } else if ("0.11.0".equals(version)) {
                syncGitOpsUserAccess(logs, "0.11.0");
                updateWebHook(logs);
            } else if (TWELVE_VERSION.equals(version)) {
                syncGitOpsUserAccess(logs, TWELVE_VERSION);
                syncGitlabUserName(logs);
            } else if ("0.11.2".equals(version)) {
                syncCommandId();
                syncCommandVersionId();
            } else if ("0.14.0".equals(version)) {
                syncDevopsEnvPodNodeNameAndRestartCount();
            } else if ("0.15.0".equals(version)) {
                syncAppToIam();
                syncAppVersion();
                syncCiVariableAndRole(logs);
            } else {
                LOGGER.info("version not matched");
            }
            devopsCheckLogE.setLog(JSON.toJSONString(logs));
            devopsCheckLogE.setEndCheckDate(new Date());
            devopsCheckLogRepository.create(devopsCheckLogE);
        }

        /**
         * 为devops_env_pod表的遗留数据的新增的node_name和restart_count字段同步数据
         */
        private void syncDevopsEnvPodNodeNameAndRestartCount() {
            List<DevopsEnvPodDO> pods = devopsEnvPodMapper.selectAll();
            pods.forEach(pod -> {
                try {
                    if (StringUtils.isEmpty(pod.getNodeName())) {
                        String message = devopsEnvResourceRepository.getResourceDetailByNameAndTypeAndInstanceId(pod.getAppInstanceId(), pod.getName(), ResourceType.POD);
                        V1Pod v1Pod = json.deserialize(message, V1Pod.class);
                        pod.setNodeName(v1Pod.getSpec().getNodeName());
                        pod.setRestartCount(K8sUtil.getRestartCountForPod(v1Pod));
                        devopsEnvPodMapper.updateByPrimaryKey(pod);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Processing node name and restart count for pod with name {} failed. \n exception is: {}", pod.getName(), e);
                }
            });
        }

        /**
         * 同步devops应用表数据到iam应用表数据
         */
        @Saga(code = "devops-sync-application",
                description = "Devops同步应用到iam", inputSchema = "{}")
        private void syncAppToIam() {
            List<ApplicationDO> applicationDOS = applicationMapper.selectAll().stream().filter(applicationDO -> applicationDO.getGitlabProjectId() != null).collect(Collectors.toList());
            List<IamAppPayLoad> iamAppPayLoads = applicationDOS.stream().map(applicationDO -> {
                ProjectE projectE = iamRepository.queryIamProject(applicationDO.getProjectId());
                Organization organization = iamRepository.queryOrganizationById(projectE.getOrganization().getId());
                IamAppPayLoad iamAppPayLoad = new IamAppPayLoad();
                iamAppPayLoad.setOrganizationId(organization.getId());
                iamAppPayLoad.setApplicationCategory(APPLICATION);
                iamAppPayLoad.setApplicationType(applicationDO.getType());
                iamAppPayLoad.setCode(applicationDO.getCode());
                iamAppPayLoad.setName(applicationDO.getName());
                iamAppPayLoad.setEnabled(true);
                iamAppPayLoad.setProjectId(applicationDO.getProjectId());
                return iamAppPayLoad;

            }).collect(Collectors.toList());
            String input = JSONArray.toJSONString(iamAppPayLoads);
            sagaClient.startSaga("devops-sync-application", new StartInstanceDTO(input, "", "", "", null));
        }

        private void syncCiVariableAndRole(List<CheckLog> logs) {
            List<Integer> gitlabProjectIds = applicationMapper.selectAll().stream()
                    .filter(applicationDO -> applicationDO.getGitlabProjectId() != null)
                    .map(ApplicationDO::getGitlabProjectId).collect(Collectors.toList());
            List<Integer> envGitlabProjectIds = devopsEnvironmentMapper.selectAll().stream()
                    .filter(environmentDO -> environmentDO.getGitlabEnvProjectId() != null)
                    .map(t -> TypeUtil.objToInteger(t.getGitlabEnvProjectId())).collect(Collectors.toList());
            //changRole
            gitlabProjectIds.forEach(t -> {
                CheckLog checkLog = new CheckLog();
                try {
                    checkLog.setContent("gitlabProjectId: " + t + " sync gitlab variable and role");
                    List<MemberDTO> memberDTOS = gitlabProjectRepository.getAllMemberByProjectId(t).stream().filter(m -> m.getAccessLevel() == 40).map(memberE ->
                            new MemberDTO(memberE.getId(), 30)).collect(Collectors.toList());
                    if (!memberDTOS.isEmpty()) {
                        gitlabRepository.updateMemberIntoProject(t, memberDTOS);
                    }
                    LOGGER.info("update project member maintainer to developer success");
//                    gitlabRepository.batchAddVariable(t, null, applicationService.setVariableDTO(devopsProjectConfigRepository.queryByIdAndType(null, ProjectConfigType.HARBOR.getType()).get(0).getId(),
//                            devopsProjectConfigRepository.queryByIdAndType(null, ProjectConfigType.CHART.getType()).get(0).getId()));
//                    LOGGER.info("the project add Variable success,gitlabProjectId: " + t);
                    checkLog.setResult(SUCCESS);
                } catch (Exception e) {
                    LOGGER.info("gitlab.project.is.not.exist,gitlabProjectId: " + t, e);
                    checkLog.setResult(FAILED + e.getMessage());
                }
                LOGGER.info(checkLog.toString());
                logs.add(checkLog);
            });
        }

        private void syncAppVersion() {
            List<ApplicationVersionDO> applicationVersionDOS = applicationVersionMapper.selectAll();
            if (!applicationVersionDOS.isEmpty()) {
                if (!applicationVersionDOS.get(0).getRepository().contains(helmUrl)) {
                    if (helmUrl.endsWith("/")) {
                        helmUrl = helmUrl.substring(0, helmUrl.length() - 1);
                    }
                    applicationVersionMapper.updateRepository(helmUrl);
                }
            }
        }

        private void syncObjects(List<CheckLog> logs, Long envId) {
            List<DevopsEnvironmentE> devopsEnvironmentES;
            if (envId != null) {
                devopsEnvironmentES = new ArrayList<>();
                devopsEnvironmentES.add(devopsEnvironmentRepository.queryById(envId));
            } else {
                devopsEnvironmentES = devopsEnvironmentRepository.list();
            }
            LOGGER.info("begin to sync env objects for {}  env", devopsEnvironmentES.size());
            devopsEnvironmentES.forEach(devopsEnvironmentE -> {
                gitUtil.setSshKey(devopsEnvironmentE.getEnvIdRsa());
                if (devopsEnvironmentE.getGitlabEnvProjectId() != null) {
                    LOGGER.info("{}:{}  begin to upgrade!", devopsEnvironmentE.getCode(), devopsEnvironmentE.getId());
                    String filePath;
                    try {
                        filePath = envUtil.handDevopsEnvGitRepository(devopsEnvironmentE);
                    } catch (Exception e) {
                        LOGGER.info("clone git  env repo error {}", e);
                        return;
                    }
                    syncFiles(logs, devopsEnvironmentE, gitUtil, filePath);
                }
            });
        }

        private void syncEnvProject(List<CheckLog> logs) {
            LOGGER.info("start to sync env project");
            List<DevopsEnvironmentE> devopsEnvironmentES = devopsEnvironmentRepository.list();
            devopsEnvironmentES
                    .stream()
                    .filter(devopsEnvironmentE -> devopsEnvironmentE.getGitlabEnvProjectId() == null)
                    .forEach(devopsEnvironmentE -> {
                        CheckLog checkLog = new CheckLog();
                        try {
                            //generate git project code
                            checkLog.setContent("env: " + devopsEnvironmentE.getName() + " create gitops project");
                            ProjectE projectE = iamRepository.queryIamProject(devopsEnvironmentE.getProjectE().getId());
                            Organization organization = iamRepository
                                    .queryOrganizationById(projectE.getOrganization().getId());
                            //generate rsa key
                            List<String> sshKeys = FileUtil.getSshKey(String.format("%s/%s/%s",
                                    organization.getCode(), projectE.getCode(), devopsEnvironmentE.getCode()));
                            devopsEnvironmentE.setEnvIdRsa(sshKeys.get(0));
                            devopsEnvironmentE.setEnvIdRsaPub(sshKeys.get(1));
                            devopsEnvironmentRepository.update(devopsEnvironmentE);
                            GitlabProjectPayload gitlabProjectPayload = new GitlabProjectPayload();
                            GitlabGroupE gitlabGroupE = devopsProjectRepository.queryDevopsProject(projectE.getId());
                            gitlabProjectPayload.setGroupId(TypeUtil.objToInteger(gitlabGroupE.getDevopsEnvGroupId()));
                            gitlabProjectPayload.setUserId(ADMIN);
                            gitlabProjectPayload.setPath(devopsEnvironmentE.getCode());
                            gitlabProjectPayload.setOrganizationId(null);
                            gitlabProjectPayload.setType(ENV);
                            devopsEnvironmentService.handleCreateEnvSaga(gitlabProjectPayload);
                            checkLog.setResult(SUCCESS);
                        } catch (Exception e) {
                            LOGGER.info("create env git project error", e);
                            checkLog.setResult(FAILED + e.getMessage());
                        }
                        LOGGER.info(checkLog.toString());
                        logs.add(checkLog);
                    });
        }

        private void syncFiles(List<CheckLog> logs, DevopsEnvironmentE env, GitUtil gitUtil, String filePath) {
            try (Git git = Git.open(new File(filePath))) {
                new SyncInstanceByEnv(logs, env, filePath, git).invoke();
                new SynServiceByEnv(logs, env, filePath, git).invoke();
                new SyncIngressByEnv(logs, env, filePath, git).invoke();

                if (git.tagList().call().stream().map(Ref::getName).noneMatch("agent-sync"::equals)) {
                    git.tag().setName("agent-sync").call();
                }

                gitUtil.gitPush(git);
                gitUtil.gitPushTag(git);
                LOGGER.info("{}:{} finish to upgrade", env.getCode(), env.getId());
            } catch (IOException e) {
                LOGGER.info("error.git.open: " + filePath, e);
            } catch (GitAPIException e) {
                LOGGER.info("error.git.push: {},{}", filePath, e.getMessage());
            }
        }

        private void syncWebHook(ApplicationDO applicationDO, List<CheckLog> logs) {
            CheckLog checkLog = new CheckLog();
            checkLog.setContent(APP + applicationDO.getName() + " create gitlab webhook");
            try {
                ProjectHook projectHook = ProjectHook.allHook();
                projectHook.setEnableSslVerification(true);
                projectHook.setProjectId(applicationDO.getGitlabProjectId());
                projectHook.setToken(applicationDO.getToken());
                String uri = !gatewayUrl.endsWith("/") ? gatewayUrl + "/" : gatewayUrl;
                uri += "devops/webhook";
                projectHook.setUrl(uri);
                applicationDO.setHookId(TypeUtil.objToLong(gitlabRepository
                        .createWebHook(applicationDO.getGitlabProjectId(), ADMIN, projectHook).getId()));
                applicationMapper.updateByPrimaryKey(applicationDO);
                checkLog.setResult(SUCCESS);
            } catch (Exception e) {
                checkLog.setResult(FAILED + e.getMessage());
            }
            logs.add(checkLog);
        }

        private void syncBranches(ApplicationDO applicationDO, List<CheckLog> logs) {
            CheckLog checkLog = new CheckLog();
            checkLog.setContent(APP + applicationDO.getName() + " sync branches");
            try {
                Optional<List<BranchDO>> branchDOS = Optional.ofNullable(
                        devopsGitRepository.listBranches(applicationDO.getGitlabProjectId(), ADMIN));
                List<String> branchNames =
                        devopsGitRepository.listDevopsBranchesByAppId(applicationDO.getId()).stream()
                                .map(DevopsBranchE::getBranchName).collect(Collectors.toList());
                branchDOS.ifPresent(branchDOS1 -> branchDOS1.stream()
                        .filter(branchDO -> !branchNames.contains(branchDO.getName()))
                        .forEach(branchDO -> {
                            DevopsBranchE newDevopsBranchE = new DevopsBranchE();
                            newDevopsBranchE.initApplicationE(applicationDO.getId());
                            newDevopsBranchE.setLastCommitDate(branchDO.getCommit().getCommittedDate());
                            newDevopsBranchE.setLastCommit(branchDO.getCommit().getId());
                            newDevopsBranchE.setBranchName(branchDO.getName());
                            newDevopsBranchE.setCheckoutCommit(branchDO.getCommit().getId());
                            newDevopsBranchE.setCheckoutDate(branchDO.getCommit().getCommittedDate());
                            newDevopsBranchE.setLastCommitMsg(branchDO.getCommit().getMessage());
                            UserAttrE userAttrE = userAttrRepository.queryByGitlabUserName(branchDO.getCommit().getAuthorName());
                            newDevopsBranchE.setLastCommitUser(userAttrE.getIamUserId());
                            devopsGitRepository.createDevopsBranch(newDevopsBranchE);
                            checkLog.setResult(SUCCESS);
                        }));
            } catch (Exception e) {
                checkLog.setResult(FAILED + e.getMessage());
            }
            logs.add(checkLog);
        }


        @Saga(code = "devops-upgrade-0.9",
                description = "Devops平滑升级到0.9", inputSchema = "{}")
        private void gitOpsUserAccess() {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(" saga start");
            }
            sagaClient.startSaga("devops-upgrade-0.9", new StartInstanceDTO("{}", "", "", ResourceLevel.SITE.value(), 0L));
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(" saga start success");
            }
        }

        private void syncNonEnvGroupProject(List<CheckLog> logs) {
            List<DevopsProjectDO> projectDOList = devopsCheckLogRepository.queryNonEnvGroupProject();
            LOGGER.info("{} projects need to upgrade", projectDOList.size());
            final String groupCodeSuffix = "gitops";
            projectDOList.forEach(t -> {
                CheckLog checkLog = new CheckLog();
                try {
                    Long projectId = t.getIamProjectId();
                    ProjectE projectE = iamRepository.queryIamProject(projectId);
                    checkLog.setContent("project: " + projectE.getName() + " create gitops group");
                    Organization organization = iamRepository
                            .queryOrganizationById(projectE.getOrganization().getId());
                    //创建gitlab group
                    GroupDO group = new GroupDO();
                    // name: orgName-projectName
                    group.setName(String.format("%s-%s-%s",
                            organization.getName(), projectE.getName(), groupCodeSuffix));
                    // path: orgCode-projectCode
                    group.setPath(String.format("%s-%s-%s",
                            organization.getCode(), projectE.getCode(), groupCodeSuffix));
                    ResponseEntity<GroupDO> responseEntity;
                    try {
                        responseEntity = gitlabServiceClient.createGroup(group, ADMIN);
                        group = responseEntity.getBody();
                        DevopsProjectDO devopsProjectDO = new DevopsProjectDO(projectId);
                        devopsProjectDO.setDevopsEnvGroupId(TypeUtil.objToLong(group.getId()));
                        devopsProjectRepository.updateProjectAttr(devopsProjectDO);
                        checkLog.setResult(SUCCESS);
                    } catch (FeignException e) {
                        checkLog.setResult(e.getMessage());
                    }
                } catch (Exception e) {
                    LOGGER.info("create project GitOps group error");
                    checkLog.setResult(FAILED + e.getMessage());
                }
                LOGGER.info(checkLog.toString());
                logs.add(checkLog);
            });
        }
    }
}
