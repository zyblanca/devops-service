package io.choerodon.devops.api.controller.v1

import io.choerodon.core.domain.Page
import io.choerodon.devops.DependencyInjectUtil
import io.choerodon.devops.IntegrationTestConfiguration
import io.choerodon.devops.api.dto.ApplicationVersionRepDTO
import io.choerodon.devops.api.dto.DeployVersionDTO
import io.choerodon.devops.api.dto.iam.ProjectWithRoleDTO
import io.choerodon.devops.api.dto.iam.RoleDTO
import io.choerodon.devops.domain.application.repository.IamRepository
import io.choerodon.devops.infra.dataobject.*
import io.choerodon.devops.infra.dataobject.iam.OrganizationDO
import io.choerodon.devops.infra.dataobject.iam.ProjectDO
import io.choerodon.devops.infra.dataobject.iam.UserDO
import io.choerodon.devops.infra.feign.IamServiceClient
import io.choerodon.devops.infra.mapper.*
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.Subject

import static org.mockito.Matchers.anyInt
import static org.mockito.Matchers.anyLong
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

/**
 * Created by n!Ck
 * Date: 2018/9/17
 * Time: 13:43
 * Description:
 */

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(IntegrationTestConfiguration)
@Subject(ApplicationVersionController)
@Stepwise
class ApplicationVersionControllerSpec extends Specification {

    private static final String mapping = "/v1/projects/{project_id}/app_versions"

    @Autowired
    private TestRestTemplate restTemplate
    @Autowired
    private ApplicationMapper applicationMapper
    @Autowired
    private DevopsEnvCommandMapper devopsEnvCommandMapper
    @Autowired
    private DevopsEnvironmentMapper devopsEnvironmentMapper
    @Autowired
    private AppUserPermissionMapper appUserPermissionMapper
    @Autowired
    private ApplicationVersionMapper applicationVersionMapper
    @Autowired
    private ApplicationInstanceMapper applicationInstanceMapper
    @Autowired
    private ApplicationVersionValueMapper applicationVersionValueMapper
    @Autowired
    private DevopsGitlabPipelineMapper devopsGitlabPipelineMapper
    @Autowired
    private DevopsGitlabCommitMapper devopsGitlabCommitMapper

    @Autowired
    private IamRepository iamRepository

    @Shared
    Long project_id = 1L
    @Shared
    Long init_id = 1L
    @Shared
    Map<String, Object> searchParam = new HashMap<>()
    @Shared
    DevopsEnvCommandDO devopsEnvCommandDO = new DevopsEnvCommandDO()
    @Shared
    ApplicationVersionValueDO applicationVersionValueDO = new ApplicationVersionValueDO()
    @Shared
    AppUserPermissionDO appUserPermissionDO = new AppUserPermissionDO()
    @Shared
    DevopsEnvironmentDO devopsEnvironmentDO = new DevopsEnvironmentDO()
    @Shared
    ApplicationInstanceDO applicationInstanceDO = new ApplicationInstanceDO()
    @Shared
    ApplicationVersionDO applicationVersionDO = new ApplicationVersionDO()
    @Shared
    DevopsGitlabPipelineDO devopsGitlabPipelineDO = new DevopsGitlabPipelineDO()
    @Shared
    DevopsGitlabCommitDO devopsGitlabCommitDO = new DevopsGitlabCommitDO()
    @Shared
    ApplicationDO applicationDO = new ApplicationDO()

    IamServiceClient iamServiceClient = Mockito.mock(IamServiceClient.class)

    def setup() {
        DependencyInjectUtil.setAttribute(iamRepository, "iamServiceClient", iamServiceClient)

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

        Page<ProjectWithRoleDTO> page = new Page<>()
        page.setTotalPages(1)
        List<ProjectWithRoleDTO> list = new ArrayList<>()
        List<RoleDTO> roleDTOList = new ArrayList<>()
        RoleDTO roleDTO = new RoleDTO()
        roleDTO.setId(44)
        roleDTO.setCode("role/project/default/project-owner")
        roleDTOList.add(roleDTO)
        ProjectWithRoleDTO projectWithRoleDTO = new ProjectWithRoleDTO()
        projectWithRoleDTO.setName("test-name")
        projectWithRoleDTO.setRoles(roleDTOList)
        list.add(projectWithRoleDTO)
        page.setContent(list)
        ResponseEntity<Page<ProjectWithRoleDTO>> responseEntity2 = new ResponseEntity<>(page, HttpStatus.OK)
        Mockito.when(iamServiceClient.listProjectWithRole(anyLong(), anyInt(), anyInt())).thenReturn(responseEntity2)
        List<UserDO> userDOList = new ArrayList<>()
        UserDO userDO1 = new UserDO()
        userDO1.setLoginName("loginName")
        userDO1.setRealName("realName")
        userDOList.add(userDO1)
        ResponseEntity<List<UserDO>> responseEntity3 = new ResponseEntity<>(userDOList, HttpStatus.OK)
        Mockito.doReturn(responseEntity3).when(iamServiceClient).listUsersByIds(1L)
    }

    def setupSpec() {
        given: '???????????????????????????'
        Map<String, Object> params = new HashMap<>()
        params.put("version", [])
        params.put("appName", [])
        params.put("appCode", ["app"])
        searchParam.put("searchParam", params)
        searchParam.put("param", "")

        appUserPermissionDO.setAppId(1L)
        appUserPermissionDO.setIamUserId(1L)

        devopsEnvironmentDO.setId(init_id)
        devopsEnvironmentDO.setCode("spock-test")
        devopsEnvironmentDO.setGitlabEnvProjectId(init_id)
        devopsEnvironmentDO.setHookId(init_id)
        devopsEnvironmentDO.setDevopsEnvGroupId(init_id)
        devopsEnvironmentDO.setProjectId(init_id)

        applicationInstanceDO.setId(init_id)
        applicationInstanceDO.setCode("spock-test")
        applicationInstanceDO.setStatus("running")
        applicationInstanceDO.setAppId(init_id)
        applicationInstanceDO.setAppVersionId(init_id)
        applicationInstanceDO.setEnvId(init_id)
        applicationInstanceDO.setCommandId(init_id)

        applicationDO.setId(init_id)
        applicationDO.setName("app_name")
        applicationDO.setCode("app_code")
        applicationDO.setProjectId(project_id)
        applicationDO.setAppTemplateId(init_id)
        applicationDO.setGitlabProjectId(1)
        applicationDO.setIsSkipCheckPermission(true)

        applicationVersionDO.setId(init_id)
        applicationVersionDO.setValueId(1L)
        applicationVersionDO.setIsPublish(1)
        applicationVersionDO.setCommit("test")
        applicationVersionDO.setAppId(init_id)
        applicationVersionDO.setVersion("0.1.0-dev.20180521111826")

        devopsEnvCommandDO.setId(1L)
        devopsEnvCommandDO.setObjectVersionId(1L)

        applicationVersionValueDO.setId(1L)
        applicationVersionValueDO.setValue("test-value")

        devopsGitlabPipelineDO.setId(init_id)
        devopsGitlabPipelineDO.setAppId(init_id)
        devopsGitlabPipelineDO.setCommitId(init_id)
        devopsGitlabPipelineDO.setPipelineId(init_id)

        devopsGitlabCommitDO.setAppId(init_id)
        devopsGitlabCommitDO.setId(init_id)
        devopsGitlabCommitDO.setUserId(init_id)
        devopsGitlabCommitDO.setRef("0.1.0-dev.20180521111826")
        devopsGitlabCommitDO.setCommitSha("test")
    }

    // ????????????????????????
    def "PageByOptions"() {
        given: '???????????????'
        applicationMapper.insert(applicationDO)
        applicationVersionMapper.insert(applicationVersionDO)
        applicationInstanceMapper.insert(applicationInstanceDO)
        devopsEnvironmentMapper.insert(devopsEnvironmentDO)
        appUserPermissionMapper.insert(appUserPermissionDO)
        devopsEnvCommandMapper.insert(devopsEnvCommandDO)
        applicationVersionValueMapper.insert(applicationVersionValueDO)
        devopsGitlabPipelineMapper.insert(devopsGitlabPipelineDO)
        devopsGitlabCommitMapper.insert(devopsGitlabCommitDO)

        when: '????????????????????????'
        def page = restTemplate.postForObject(mapping + "/list_by_options?page=0&size=0&appId={app_id}", searchParam, Page.class, project_id, init_id)

        then: '?????????'
        page.size() == 1

        expect: '??????????????????'
        page.get(0).version == "0.1.0-dev.20180521111826"
    }

    // ?????????????????????????????????
    def "QueryByAppId"() {
        given:
        String version = "0.1.0-dev.20180521111826";
        when: '?????????????????????????????????'
        def page = restTemplate.getForObject(mapping + "/list_by_app/{app_id}?is_publish=true&page=0&size=10&version={version}", Page.class, project_id, init_id,version)

        then: '?????????'
        page.size() == 1

        expect: '??????????????????'
        page.get(0).version == "0.1.0-dev.20180521111826"
    }

    // ??????????????????????????????????????????
    def "QueryDeployedByAppId"() {
        when: '??????????????????????????????????????????'
        def list = restTemplate.getForObject(mapping + "/list_deployed_by_app/{app_id}", List.class, project_id, init_id)

        then: '?????????'
        list.size() == 1

        expect: '??????????????????'
        list.get(0).version == "0.1.0-dev.20180521111826"
    }

    // ????????????????????????????????????????????????
    def "QueryByAppIdAndEnvId"() {
        when: '????????????????????????????????????????????????'
        def list = restTemplate.getForObject(mapping + "/app/{app_id}/env/{envId}/query", List.class, 1L, 1L, 1L)

        then: '?????????'
        list.size() == 1

        expect: '??????????????????'
        list.get(0).version == "0.1.0-dev.20180521111826"
    }

    // ??????????????????????????????
    def "GetUpgradeAppVersion"() {
        given: '?????????????????????DO???'
        ApplicationVersionDO applicationVersionDO = new ApplicationVersionDO()
        applicationVersionDO.setId(2L)
        applicationVersionDO.setVersion("0.2.0-dev.20180521111826")
        applicationVersionDO.setAppId(init_id)
        applicationVersionMapper.insert(applicationVersionDO)

        when: '??????????????????????????????'
        def list = restTemplate.getForObject(mapping + "/version/{app_version_id}/upgrade_version", List.class, 1L, 1L)

        then: '?????????'
        list.size() == 1

        expect: '??????????????????'
        list.get(0).version == "0.2.0-dev.20180521111826"
    }

    // ??????????????????????????????????????????????????????????????????
    def "GetDeployVersions"() {
        when: '??????????????????????????????????????????????????????????????????'
        def dto = restTemplate.getForObject(mapping + "/app/{app_id}/deployVersions", DeployVersionDTO.class, 1L, 1L)

        then: '??????????????????'
        dto["latestVersion"] == "0.2.0-dev.20180521111826"
    }

    // ????????????id????????????values
    def "GetVersionValue"() {
        when: '????????????id????????????values'
        def str = restTemplate.getForObject(mapping + "/{app_verisonId}/queryValue", String.class, 1L, 1L)

        then: '???????????????'
        str == "test-value"
    }

    // ????????????id??????????????????
    def "GetAppversion"() {
        given: '??????????????????'
        HttpEntity<List<Long>> entity = new HttpEntity<>(Arrays.asList(applicationVersionDO.getId()))

        when: '????????????id??????????????????'
        def dto = restTemplate.postForEntity(mapping + "/list_by_appVersionIds", entity, List, project_id)

        then: '???????????????'
        dto.statusCode.is2xxSuccessful()
        dto.getBody().size() != 0
        ((LinkedHashMap)dto.getBody().get(0)).get("version") == applicationVersionDO.getVersion()
    }

    def "getAppversionByBranch"() {

         when:'???????????????????????????'
         def list = restTemplate.getForObject(mapping + "/list_by_branch?appId=1&branch=0.1.0-dev.20180521111826", List.class, 1L)

         then: '???????????????'
         list.size() == 1
    }

    def "queryByPipeline"() {

        when: '??????pipeline????????????????????????'
        def result = restTemplate.getForObject(mapping + "/query_by_pipeline?pipelineId=1&branch=0.1.0-dev.20180521111826", Boolean.class, 1L)


        then: '???????????????'
        result == true


    }

    // ??????????????????
    def "cleanupData"() {
        given:
        // ??????appInstance
        List<ApplicationInstanceDO> list = applicationInstanceMapper.selectAll()
        if (list != null && !list.isEmpty()) {
            for (ApplicationInstanceDO e : list) {
                applicationInstanceMapper.delete(e)
            }
        }
        // ??????env
        List<DevopsEnvironmentDO> list1 = devopsEnvironmentMapper.selectAll()
        if (list1 != null && !list1.isEmpty()) {
            for (DevopsEnvironmentDO e : list1) {
                devopsEnvironmentMapper.delete(e)
            }
        }
        // ??????app
        List<ApplicationDO> list2 = applicationMapper.selectAll()
        if (list2 != null && !list2.isEmpty()) {
            for (ApplicationDO e : list2) {
                applicationMapper.delete(e)
            }
        }
        // ??????appVersion
        List<ApplicationVersionDO> list3 = applicationVersionMapper.selectAll()
        if (list3 != null && !list3.isEmpty()) {
            for (ApplicationVersionDO e : list3) {
                applicationVersionMapper.delete(e)
            }
        }
        // ??????appUserPermission
        List<AppUserPermissionDO> list4 = appUserPermissionMapper.selectAll()
        if (list4 != null && !list4.isEmpty()) {
            for (AppUserPermissionDO e : list4) {
                appUserPermissionMapper.delete(e)
            }
        }
        // ??????appVersionValue
        List<ApplicationVersionValueDO> list5 = applicationVersionValueMapper.selectAll()
        if (list5 != null && !list5.isEmpty()) {
            for (ApplicationVersionValueDO e : list5) {
                applicationVersionValueMapper.delete(e)
            }
        }
        // ??????envCommand
        List<DevopsEnvCommandDO> list6 = devopsEnvCommandMapper.selectAll()
        if (list6 != null && !list6.isEmpty()) {
            for (DevopsEnvCommandDO e : list6) {
                devopsEnvCommandMapper.delete(e)
            }
        }

        //??????gitlabPipeline
        List<DevopsGitlabPipelineDO> list7 = devopsGitlabPipelineMapper.selectAll()
        if(list7!=null&&!list7.isEmpty()) {
            for(DevopsGitlabPipelineDO e:list7) {
                devopsGitlabPipelineMapper.delete(e)
            }
        }

        //??????gitlabCommit
        List<DevopsGitlabCommitDO> list8 = devopsGitlabCommitMapper.selectAll()
        if(list8 !=null&&!list8.isEmpty()) {
            for(DevopsGitlabCommitDO e:list8) {
                devopsGitlabCommitMapper.delete(e)
            }
        }

    }
}
