package io.choerodon.devops.api.controller.v1

import io.choerodon.asgard.saga.dto.SagaInstanceDTO
import io.choerodon.asgard.saga.dto.StartInstanceDTO
import io.choerodon.asgard.saga.feign.SagaClient
import io.choerodon.core.domain.Page
import io.choerodon.core.exception.CommonException
import io.choerodon.core.exception.ExceptionResponse
import io.choerodon.devops.DependencyInjectUtil
import io.choerodon.devops.IntegrationTestConfiguration
import io.choerodon.devops.api.dto.ApplicationImportDTO
import io.choerodon.devops.api.dto.ApplicationRepDTO
import io.choerodon.devops.api.dto.ApplicationReqDTO
import io.choerodon.devops.api.dto.ApplicationUpdateDTO
import io.choerodon.devops.api.dto.iam.ProjectWithRoleDTO
import io.choerodon.devops.api.dto.iam.RoleDTO
import io.choerodon.devops.app.service.ApplicationService
import io.choerodon.devops.app.service.DevopsGitService
import io.choerodon.devops.domain.application.entity.ProjectE
import io.choerodon.devops.domain.application.entity.UserAttrE
import io.choerodon.devops.domain.application.event.IamAppPayLoad
import io.choerodon.devops.domain.application.repository.*
import io.choerodon.devops.domain.application.valueobject.Organization
import io.choerodon.devops.infra.common.util.enums.AccessLevel
import io.choerodon.devops.infra.dataobject.*
import io.choerodon.devops.infra.dataobject.gitlab.MemberDO
import io.choerodon.devops.infra.dataobject.iam.OrganizationDO
import io.choerodon.devops.infra.dataobject.iam.ProjectDO
import io.choerodon.devops.infra.feign.GitlabServiceClient
import io.choerodon.devops.infra.feign.IamServiceClient
import io.choerodon.devops.infra.mapper.*
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.Subject

import static org.mockito.Matchers.*
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

/**
 * Created by n!Ck
 * Date: 2018/9/3
 * Time: 20:27
 * Description:
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(IntegrationTestConfiguration)
@Subject(ApplicationController)
@Stepwise
class ApplicationControllerSpec extends Specification {

    private static final String MAPPING = "/v1/projects/{project_id}/apps"

    @Autowired
    private TestRestTemplate restTemplate
    @Autowired
    private DevopsGitService devopsGitService
    @Autowired
    private ApplicationMapper applicationMapper
    @Autowired
    private ApplicationService applicationService
    @Autowired
    private UserAttrRepository userAttrRepository
    @Autowired
    private DevopsEnvPodMapper devopsEnvPodMapper
    @Autowired
    private DevopsProjectMapper devopsProjectMapper
    @Autowired
    private AppUserPermissionMapper appUserPermissionMapper
    @Autowired
    private ApplicationMarketMapper applicationMarketMapper
    @Autowired
    private DevopsProjectRepository devopsProjectRepository
    @Autowired
    protected DevopsEnvironmentMapper devopsEnvironmentMapper
    @Autowired
    private ApplicationVersionMapper applicationVersionMapper
    @Autowired
    private ApplicationInstanceMapper applicationInstanceMapper
    @Autowired
    private ApplicationTemplateMapper applicationTemplateMapper
    @Autowired
    private ApplicationInstanceRepository applicationInstanceRepository

    @Autowired
    private IamRepository iamRepository
    @Autowired
    private GitlabRepository gitlabRepository
    @Autowired
    private GitlabGroupMemberRepository gitlabGroupMemberRepository

    SagaClient sagaClient = Mockito.mock(SagaClient.class)
    IamServiceClient iamServiceClient = Mockito.mock(IamServiceClient.class)
    GitlabServiceClient gitlabServiceClient = Mockito.mock(GitlabServiceClient.class)

    @Shared
    Organization organization = new Organization()
    @Shared
    ProjectE projectE = new ProjectE()
    @Shared
    UserAttrE userAttrE = new UserAttrE()
    @Shared
    Map<String, Object> searchParam = new HashMap<>()
    @Shared
    Long project_id = 1L
    @Shared
    Long init_id = 1L
    @Shared
    DevopsAppMarketDO devopsAppMarketDO = new DevopsAppMarketDO()
    @Shared
    DevopsEnvPodDO devopsEnvPodDO = new DevopsEnvPodDO()
    @Shared
    ApplicationTemplateDO applicationTemplateDO = new ApplicationTemplateDO()
    @Shared
    private boolean isToInit = true
    @Shared
    private boolean isToClean = false
    @Shared
    Long harborConfigId = 1L
    @Shared
    Long chartConfigId = 2L

    def setupSpec() {
        given:
        organization.setId(init_id)
        organization.setCode("org")

        projectE.setId(init_id)
        projectE.setCode("pro")
        projectE.setOrganization(organization)

        userAttrE.setIamUserId(init_id)
        userAttrE.setGitlabUserId(init_id)

        Map<String, Object> params = new HashMap<>()
        params.put("name", [])
        params.put("code", ["app"])
        searchParam.put("searchParam", params)
        searchParam.put("param", "")

        devopsAppMarketDO.setId(1L)
        devopsAppMarketDO.setAppId(2L)
        devopsAppMarketDO.setPublishLevel("pub")
        devopsAppMarketDO.setContributor("con")
        devopsAppMarketDO.setDescription("des")

        devopsEnvPodDO.setId(1L)
        devopsEnvPodDO.setAppInstanceId(1L)
    }

    def setup() {

        if (isToInit) {
            DependencyInjectUtil.setAttribute(iamRepository, "iamServiceClient", iamServiceClient)
            DependencyInjectUtil.setAttribute(gitlabRepository, "gitlabServiceClient", gitlabServiceClient)
            DependencyInjectUtil.setAttribute(gitlabGroupMemberRepository, "gitlabServiceClient", gitlabServiceClient)
            DependencyInjectUtil.setAttribute(applicationService, "sagaClient", sagaClient)

            // ??????app
            applicationMapper.selectAll().forEach { applicationMapper.delete(it) }

            ProjectDO projectDO = new ProjectDO()
            projectDO.setName("pro")
            projectDO.setOrganizationId(1L)
            ResponseEntity<ProjectDO> responseEntity = new ResponseEntity<>(projectDO, HttpStatus.OK)
            Mockito.doReturn(responseEntity).when(iamServiceClient).queryIamProject(1L)
            OrganizationDO organizationDO = new OrganizationDO()
            organizationDO.setId(1L)
            organizationDO.setCode("testOrganization")
            ResponseEntity<OrganizationDO> responseEntity1 = new ResponseEntity<>(organizationDO, HttpStatus.OK)
            Mockito.doReturn(responseEntity1).when(iamServiceClient).queryOrganizationById(1L)

            List<RoleDTO> roleDTOList = new ArrayList<>()
            RoleDTO roleDTO = new RoleDTO()
            roleDTO.setCode("role/project/default/project-owner")
            roleDTOList.add(roleDTO)
            List<ProjectWithRoleDTO> projectWithRoleDTOList = new ArrayList<>()
            ProjectWithRoleDTO projectWithRoleDTO = new ProjectWithRoleDTO()
            projectWithRoleDTO.setName("pro")
            projectWithRoleDTO.setRoles(roleDTOList)
            projectWithRoleDTOList.add(projectWithRoleDTO)
            Page<ProjectWithRoleDTO> projectWithRoleDTOPage = new Page<>()
            projectWithRoleDTOPage.setContent(projectWithRoleDTOList)
            projectWithRoleDTOPage.setTotalPages(2)
            ResponseEntity<Page<ProjectWithRoleDTO>> pageResponseEntity = new ResponseEntity<>(projectWithRoleDTOPage, HttpStatus.OK)
            Mockito.doReturn(pageResponseEntity).when(iamServiceClient).listProjectWithRole(anyLong(), anyInt(), anyInt())
        }
    }

    def cleanup() {
        if (isToClean) {
            DependencyInjectUtil.restoreDefaultDependency(iamRepository, "iamServiceClient")
            DependencyInjectUtil.restoreDefaultDependency(gitlabRepository, "gitlabServiceClient")
            DependencyInjectUtil.restoreDefaultDependency(gitlabGroupMemberRepository, "gitlabServiceClient")
            DependencyInjectUtil.restoreDefaultDependency(applicationService, "sagaClient")

            // ??????appInstance
            applicationInstanceMapper.selectAll().forEach { applicationInstanceMapper.delete(it) }
            // ??????env
            devopsEnvironmentMapper.selectAll().forEach { devopsEnvironmentMapper.delete(it) }
            // ??????app
            applicationMapper.selectAll().forEach { applicationMapper.delete(it) }
            // ??????appVersion
            applicationVersionMapper.selectAll().forEach { applicationVersionMapper.delete(it) }
            // ??????appMarket
            applicationMarketMapper.selectAll().forEach { applicationMarketMapper.delete(it) }
            // ??????appTemplate
            applicationTemplateMapper.delete(applicationTemplateDO)
            // ??????appUserPermission
            appUserPermissionMapper.selectAll().forEach { appUserPermissionMapper.delete(it) }
            // ??????envPod
            devopsEnvPodMapper.selectAll().forEach { devopsEnvPodMapper.delete(it) }
        }
    }

    // ?????????????????????
    def "create"() {
        given: '??????issueDTO'
        isToInit = false
        ApplicationReqDTO applicationDTO = new ApplicationReqDTO()

        and: '??????'
        applicationDTO.setId(init_id)
        applicationDTO.setName("appName")
        applicationDTO.setCode("appCode")
        applicationDTO.setType("normal")
        applicationDTO.setProjectId(project_id)
        applicationDTO.setApplicationTemplateId(init_id)
        applicationDTO.setIsSkipCheckPermission(true)
        applicationDTO.setHarborConfigId(harborConfigId)
        applicationDTO.setChartConfigId(chartConfigId)
        List<Long> userList = new ArrayList<>()
        userList.add(2L)
        applicationDTO.setUserIds(userList)

        and: 'mock??????gitlab??????'
        MemberDO memberDO = new MemberDO()
        memberDO.setId(1)
        memberDO.setAccessLevel(AccessLevel.OWNER)
        ResponseEntity<MemberDO> memberDOResponseEntity = new ResponseEntity<>(memberDO, HttpStatus.OK)
        Mockito.when(gitlabServiceClient.getUserMemberByUserId(anyInt(), anyInt())).thenReturn(memberDOResponseEntity)

        and: 'mock iam????????????'
        IamAppPayLoad iamAppPayLoad =new IamAppPayLoad()
        iamAppPayLoad.setProjectId(init_id)
        iamAppPayLoad.setOrganizationId(init_id)
        ResponseEntity<IamAppPayLoad> iamAppPayLoadResponseEntity = new ResponseEntity<>(iamAppPayLoad, HttpStatus.OK)
        Mockito.when(iamServiceClient.createIamApplication(anyLong(), any(IamAppPayLoad))).thenReturn(iamAppPayLoadResponseEntity)

        and: 'mock??????sagaClient'
        Mockito.doReturn(new SagaInstanceDTO()).when(sagaClient).startSaga(anyString(), any(StartInstanceDTO))

        when: '??????????????????'
        def entity = restTemplate.postForEntity(MAPPING, applicationDTO, ApplicationRepDTO.class, project_id)

        then: '????????????'
        entity.statusCode.is2xxSuccessful()
        entity.getBody().getId() == 1L
        ApplicationDO applicationDO = applicationMapper.selectByPrimaryKey(init_id)

        expect: '??????????????????'
        applicationDO["code"] == "appCode"
    }

    // ?????????????????????????????????
    def "queryByAppId"() {
        when:
        def entity = restTemplate.getForEntity(MAPPING + "/{app_id}/detail", ApplicationRepDTO.class, project_id, 1L)

        then: '????????????'
        entity.getBody()["code"] == "appCode"
    }

    // ???????????????????????????
    def "update"() {
        given: '??????applicationUpdateDTO???'
        ApplicationUpdateDTO applicationUpdateDTO = new ApplicationUpdateDTO()
        applicationUpdateDTO.setId(init_id)
        applicationUpdateDTO.setName("updatename")
        applicationUpdateDTO.setIsSkipCheckPermission(true)

        and: 'mock??????sagaClient'
        Mockito.doReturn(new SagaInstanceDTO()).when(sagaClient).startSaga(anyString(), any(StartInstanceDTO))

        when: '???????????????????????????????????????????????????true????????????????????????????????????'
        restTemplate.put(MAPPING, applicationUpdateDTO, project_id)
        then: '????????????'
        List<AppUserPermissionDO> permissionResult = appUserPermissionMapper.selectAll()
        ApplicationDO appResult = applicationMapper.selectByPrimaryKey(1L)
        permissionResult.size() == 0
        appResult.getIsSkipCheckPermission()

        when: '???????????????????????????????????????????????????????????????????????????'
        applicationUpdateDTO.setIsSkipCheckPermission(false)
        List<Long> userIds = new ArrayList<>()
        userIds.add(2L)
        applicationUpdateDTO.setUserIds(userIds)
        restTemplate.put(MAPPING, applicationUpdateDTO, project_id)
        then: '????????????'
        List<AppUserPermissionDO> permissionResult1 = appUserPermissionMapper.selectAll()
        ApplicationDO appResult1 = applicationMapper.selectByPrimaryKey(1L)
        permissionResult1.size() == 1
        permissionResult1.get(0).getAppId() == 1L
        !appResult1.getIsSkipCheckPermission()

        when: '?????????????????????????????????????????????????????????????????????????????????'
        applicationUpdateDTO.setIsSkipCheckPermission(false)
        restTemplate.put(MAPPING, applicationUpdateDTO, project_id)
        then: '????????????'
        List<AppUserPermissionDO> permissionResult2 = appUserPermissionMapper.selectAll()
        ApplicationDO appResult2 = applicationMapper.selectByPrimaryKey(1L)
        permissionResult2.size() == 1
        permissionResult2.get(0).getAppId() == 1L
        !appResult2.getIsSkipCheckPermission()

        when: '???????????????????????????????????????????????????????????????????????????'
        applicationUpdateDTO.setIsSkipCheckPermission(true)
        restTemplate.put(MAPPING, applicationUpdateDTO, project_id)
        then: '????????????'
        List<AppUserPermissionDO> permissionResult3 = appUserPermissionMapper.selectAll()
        ApplicationDO appResult3 = applicationMapper.selectByPrimaryKey(1L)
        permissionResult3.size() == 0
        appResult3.getIsSkipCheckPermission()
    }

    // ????????????
    def "disableApp"() {
        when:
        restTemplate.put(MAPPING + "/{init_id}?active=false", Boolean, 1L, init_id)
        applicationMapper.selectAll().forEach { println(it.getId() + it.getName() + it.getCode() + it.getActive()) }

        then: '??????????????????'
        !applicationMapper.selectByPrimaryKey(init_id).getActive()
    }

    // ????????????
    def "enableApp"() {
        when:
        restTemplate.put(MAPPING + "/1?active=true", Boolean.class, 1L)

        then: '?????????'
        ApplicationDO applicationDO = applicationMapper.selectByPrimaryKey(init_id)

        expect: '??????????????????'
        applicationDO["isActive"] == true
    }

    // ????????????
    def "deleteByAppId"() {
        given: 'mock??????git??????'
        ResponseEntity responseEntity2 = new ResponseEntity(HttpStatus.OK)
        Mockito.when(gitlabServiceClient.deleteProjectByProjectName(anyString(), anyString(), anyInt())).thenReturn(responseEntity2)

        when:
        restTemplate.delete(MAPPING + "/1", 1L)

        then: '??????????????????'
        applicationMapper.selectAll().isEmpty()

        and: '????????????????????????'
        ApplicationDO applicationDO = new ApplicationDO()
        applicationDO.setId(1L)
        applicationDO.setProjectId(1L)
        applicationDO.setName("appName")
        applicationDO.setCode("appCode")
        applicationDO.setActive(true)
        applicationDO.setSynchro(true)
        applicationDO.setType("normal")
        applicationDO.setGitlabProjectId(1)
        applicationDO.setAppTemplateId(1L)
        applicationDO.setIsSkipCheckPermission(true)
        applicationMapper.insert(applicationDO)
    }

    // ???????????????????????????
    def "pageByOptions"() {
        when:
        def app = restTemplate.postForObject(MAPPING + "/list_by_options?active=true", searchParam, Page.class, 1L)

        then: '?????????'
        app.size() == 1

        expect: '???????????????'
        app.getContent().get(0)["code"] == "appCode"
    }

    // ????????????id????????????????????????????????????????????????
    def "pageByEnvIdAndStatus"() {
        given: '????????????????????????'
        ApplicationInstanceDO applicationInstanceDO = new ApplicationInstanceDO()
        applicationInstanceDO.setId(init_id)
        applicationInstanceDO.setCode("spock-test")
        applicationInstanceDO.setStatus("running")
        applicationInstanceDO.setAppId(init_id)
        applicationInstanceDO.setAppVersionId(init_id)
        applicationInstanceDO.setEnvId(init_id)
        applicationInstanceDO.setCommandId(init_id)
        applicationInstanceMapper.insert(applicationInstanceDO)

        and: '??????env'
        DevopsEnvironmentDO devopsEnvironmentDO = new DevopsEnvironmentDO()
        devopsEnvironmentDO.setId(init_id)
        devopsEnvironmentDO.setCode("spock-test")
        devopsEnvironmentDO.setGitlabEnvProjectId(init_id)
        devopsEnvironmentDO.setHookId(init_id)
        devopsEnvironmentDO.setDevopsEnvGroupId(init_id)
        devopsEnvironmentDO.setProjectId(init_id)
        devopsEnvironmentMapper.insert(devopsEnvironmentDO)

        and: '?????????appMarket??????'
        applicationMarketMapper.insert(devopsAppMarketDO)

        and: '?????????envPod??????'
        devopsEnvPodMapper.insert(devopsEnvPodDO)

        when:
        def applicationPage = restTemplate.getForObject(MAPPING + "/pages?env_id={env_id}", Page.class, project_id, 1)

        then: '?????????'
        applicationPage.size() == 1

        expect: '???????????????'
        applicationPage.getContent().get(0)["code"] == "appCode"
    }

    // ????????????id??????????????????????????????????????????
    def "listByEnvIdAndStatus"() {
        when:
        def applicationList = restTemplate.getForObject(MAPPING + "/options?envId=1&status=running&appId=1", List.class, 1L)

        then: '?????????'
        applicationList.size() == 1

        expect: '???????????????'
        applicationList.get(0)["code"] == "appCode"
    }

    // ??????????????????????????????????????????
    def "listByActive"() {
        when:
        def applicationList = restTemplate.getForObject(MAPPING, List.class, project_id)

        then: '?????????'
        applicationList.size() == 1

        expect: '???????????????'
        applicationList.get(0)["code"] == "appCode"
    }

    // ??????????????????????????????????????????
    def "listAll"() {
        when:
        def applicationList = restTemplate.getForObject(MAPPING + "/list_all", List.class, project_id)

        then: '?????????'
        applicationList.size() == 1

        expect: '???????????????'
        applicationList.get(0)["code"] == "appCode"
    }

    // ????????????????????????????????????
    def "checkName"() {
        when: '????????????????????????????????????'
        def exception = restTemplate.getForEntity(MAPPING + "/check_name?name=testName", ExceptionResponse.class, 1L)

        then: '??????????????????????????????'
        exception.statusCode.is2xxSuccessful()
        notThrown(CommonException)
    }

    // ????????????????????????????????????
    def "checkCode"() {
        when: '????????????????????????????????????'
        def exception = restTemplate.getForEntity(MAPPING + "/check_code?code=testCode", ExceptionResponse.class, 1L)

        then: '??????????????????????????????'
        exception.statusCode.is2xxSuccessful()
        notThrown(CommonException)
    }

    // ????????????????????????
    def "listTemplate"() {
        given: '?????????appTemplateDO???'
        applicationTemplateDO.setId(4L)
        applicationTemplateDO.setName("tempname")
        applicationTemplateDO.setCode("tempcode")
        applicationTemplateDO.setOrganizationId(init_id)
        applicationTemplateDO.setDescription("tempdes")
        applicationTemplateDO.setCopyFrom(init_id)
        applicationTemplateDO.setRepoUrl("tempurl")
        applicationTemplateDO.setType(null)
        applicationTemplateDO.setUuid("tempuuid")
        applicationTemplateDO.setSynchro(true)
        applicationTemplateDO.setGitlabProjectId(init_id)
        applicationTemplateMapper.insert(applicationTemplateDO)

        when:
        def templateList = restTemplate.getForObject(MAPPING + "/template", List.class, 1L)

        then: '?????????'
        templateList.size() == 1

        expect: '???????????????'
        templateList.get(0)["code"] == "tempcode"
    }

    // ????????????????????????????????????????????????????????????????????????
    def "listByActiveAndPubAndVersion"() {
        given: '?????????appVersionDO???'
        ApplicationVersionDO applicationVersionDO = new ApplicationVersionDO()
        applicationVersionDO.setId(init_id)
        applicationVersionDO.setVersion("0.1.0")
        applicationVersionDO.setAppId(init_id)
        applicationVersionMapper.insert(applicationVersionDO)

        when:
        def entity = restTemplate.postForObject(MAPPING + "/list_unpublish", searchParam, Page.class, 1L)

        then: '???????????????'
        entity.get(0)["code"] == "appCode"
    }

    // ?????????????????????????????????
    def "listCodeRepository"() {
        when:
        def entity = restTemplate.postForObject("/v1/projects/{project_id}/apps/list_code_repository", searchParam, Page.class, project_id)

        then:
        entity.get(0)["code"] == "appCode"

    }

    // ?????????????????????????????????
    def "listAllUserPermission"() {
        when:
        def permissionList = restTemplate.getForObject(MAPPING + "/{appId}/list_all", List.class, 100L, 100L)

        then:
        permissionList.isEmpty()
    }

    // validate repository url and token
    def "validateUrlAndAccessToken"() {
        given: "????????????"
        def url = MAPPING + "/url_validation?platform_type={platform_type}&access_token={access_token}&url={url}"

        when: "??????github????????????ssh??????"
        def result = restTemplate.getForEntity(url, String, 1L, "github", "", "git@github.com:git/git.git")

        then:
        result.getBody() == "false"

        when: "??????github????????????"
        result = restTemplate.getForEntity(url, String, 1L, "github", "", "https://github.com/git/git.git")

        then:
        result.getBody() == "true"

        when: "??????gitlab, ???token???????????????"
        result = restTemplate.getForEntity(url, String, 1L, "gitlab", "", "http://git.staging.saas.hand-china.com/code-x-code-x/code-i.git")

        then:
        result.getBody() == "false"

        when: "??????gitlab, ???token???????????????"
        result = restTemplate.getForEntity(url, String, 1L, "gitlab", "munijNHhNBEh7BRNhwrV", "http://git.staging.saas.hand-china.com/code-x-code-x/code-i.git")

        then:
        result.getBody() == "true"

        when: "??????gitlab, ???token??????????????????"
        result = restTemplate.getForEntity(url, String, 1L, "gitlab", "munijNHhNBEh7BRNhwrV", "http://git.staging.saas.hand-china.com/code-x-code-x/test-empty.git")

        then:
        result.getBody() == "null"
    }

    // ?????????????????????
    def "import Application"() {
        given: '??????issueDTO'
        def url = MAPPING + "/import"
        ApplicationImportDTO applicationDTO = new ApplicationImportDTO()
        applicationDTO.setName("test-import-github")
        applicationDTO.setCode("test-import-gitlab")
        applicationDTO.setType("normal")
        applicationDTO.setProjectId(project_id)
        applicationDTO.setApplicationTemplateId(init_id)
        applicationDTO.setIsSkipCheckPermission(true)
        applicationDTO.setRepositoryUrl("https://github.com/choerodon/choerodon-microservice-template.git")
        applicationDTO.setPlatformType("github")
        applicationDTO.setHarborConfigId(harborConfigId)
        applicationDTO.setChartConfigId(chartConfigId)

        def searchCondition = new ApplicationDO()
        searchCondition.setCode(applicationDTO.getCode())

        when: '????????????github??????'
        def entity = restTemplate.postForEntity(url, applicationDTO, ApplicationRepDTO.class, project_id)

        then: '????????????'
        entity.statusCode.is2xxSuccessful()
        applicationMapper.selectOne(searchCondition) != null
        applicationMapper.delete(searchCondition)

        when: '????????????????????????????????????'
        applicationDTO.setName("test-import-invalid")
        applicationDTO.setCode("test-import-invalid")
        applicationDTO.setRepositoryUrl("https://github.com/choerodon/choerodon-microservice-template.gi")
        entity = restTemplate.postForEntity(url, applicationDTO, ExceptionResponse.class, project_id)

        then: '????????????'
        entity.getStatusCode().is2xxSuccessful()
        entity.getBody().getCode() == "error.repository.token.invalid"

        when: '????????????????????????'
        applicationDTO.setName("test-import-empty")
        applicationDTO.setCode("test-import-empty")
        applicationDTO.setRepositoryUrl("http://git.staging.saas.hand-china.com/code-x-code-x/test-empty.git")
        applicationDTO.setPlatformType("gitlab")
        applicationDTO.setAccessToken("munijNHhNBEh7BRNhwrV")
        entity = restTemplate.postForEntity(url, applicationDTO, ApplicationRepDTO.class, project_id)

        then: '????????????'
        entity.getStatusCode().is2xxSuccessful()
        entity.getBody().getCode() == "error.repository.empty"

        when: '????????????gitlab???????????????'
        applicationDTO.setName("test-import-gitlab")
        applicationDTO.setCode("test-import-gitlab")
        applicationDTO.setRepositoryUrl("http://git.staging.saas.hand-china.com/code-x-code-x/code-i.git")
        applicationDTO.setPlatformType("gitlab")
        applicationDTO.setAccessToken("munijNHhNBEh7BRNhwrV")
        searchCondition.setCode(applicationDTO.getCode())
        entity = restTemplate.postForEntity(url, applicationDTO, ApplicationRepDTO.class, project_id)

        then: '????????????'
        entity.getStatusCode().is2xxSuccessful()
        applicationMapper.selectOne(searchCondition) != null
        applicationMapper.delete(searchCondition)
    }

    // ??????????????????
    def "cleanupData"() {
        given:
        isToClean = true
    }
}
