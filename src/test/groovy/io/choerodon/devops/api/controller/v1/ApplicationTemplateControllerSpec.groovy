package io.choerodon.devops.api.controller.v1

import io.choerodon.asgard.saga.dto.SagaInstanceDTO
import io.choerodon.asgard.saga.feign.SagaClient
import io.choerodon.core.domain.Page
import io.choerodon.core.exception.CommonException
import io.choerodon.core.exception.ExceptionResponse
import io.choerodon.devops.DependencyInjectUtil
import io.choerodon.devops.IntegrationTestConfiguration
import io.choerodon.devops.api.dto.ApplicationTemplateDTO
import io.choerodon.devops.api.dto.ApplicationTemplateRepDTO
import io.choerodon.devops.api.dto.ApplicationTemplateUpdateDTO
import io.choerodon.devops.app.service.ApplicationTemplateService
import io.choerodon.devops.app.service.DevopsGitService
import io.choerodon.devops.domain.application.entity.UserAttrE
import io.choerodon.devops.domain.application.entity.gitlab.GitlabGroupE
import io.choerodon.devops.domain.application.repository.GitlabRepository
import io.choerodon.devops.domain.application.repository.IamRepository
import io.choerodon.devops.domain.application.valueobject.Organization
import io.choerodon.devops.domain.application.valueobject.ProjectHook
import io.choerodon.devops.infra.common.util.enums.Visibility
import io.choerodon.devops.infra.dataobject.ApplicationTemplateDO
import io.choerodon.devops.infra.dataobject.gitlab.GroupDO
import io.choerodon.devops.infra.dataobject.iam.OrganizationDO
import io.choerodon.devops.infra.feign.GitlabServiceClient
import io.choerodon.devops.infra.feign.IamServiceClient
import io.choerodon.devops.infra.mapper.ApplicationTemplateMapper
import io.choerodon.devops.infra.persistence.impl.ApplicationTemplateRepositoryImpl
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
 * Date: 2018/9/11
 * Time: 10:30
 * Description: 
 */

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(IntegrationTestConfiguration)
@Subject(ApplicationTemplateController)
@Stepwise
class ApplicationTemplateControllerSpec extends Specification {

    @Autowired
    TestRestTemplate restTemplate
    @Autowired
    private DevopsGitService devopsGitService
    @Autowired
    private ApplicationTemplateMapper applicationTemplateMapper
    @Autowired
    private ApplicationTemplateService applicationTemplateService
    @Autowired
    private ApplicationTemplateRepositoryImpl applicationTemplateRepository
    @Autowired
    private IamRepository iamRepository
    @Autowired
    private GitlabRepository gitlabRepository

    SagaClient sagaClient = Mockito.mock(SagaClient.class)
    IamServiceClient iamServiceClient = Mockito.mock(IamServiceClient.class)
    GitlabServiceClient gitlabServiceClient = Mockito.mock(GitlabServiceClient.class)

    @Shared
    Organization organization = new Organization()
    @Shared
    OrganizationDO organizationDO = new OrganizationDO()
    @Shared
    UserAttrE userAttrE = new UserAttrE()
    @Shared
    GitlabGroupE gitlabGroupE = new GitlabGroupE()
    @Shared
    Map<String, Object> searchParam = new HashMap<>();
    @Shared
    Long org_id = 1L
    @Shared
    Long init_id = 1L
    @Shared
    Long template_id = 4L

    // ?????????????????????
    def setupSpec() {
        given:
        organization.setId(init_id)
        organization.setCode("org")

        organizationDO.setCode("orgDO")

        gitlabGroupE.setName("org_template")
        gitlabGroupE.setPath("org_template")
        gitlabGroupE.setVisibility(Visibility.PUBLIC)

        userAttrE.setIamUserId(init_id)
        userAttrE.setGitlabUserId(init_id)

        Map<String, Object> params = new HashMap<>();
        params.put("name", [])
        params.put("code", ["code"])
        searchParam.put("searchParam", params)
        searchParam.put("param", "")
    }

    // ???????????????????????????
    def "createTemplate"() {
        given: "???????????????"
        ApplicationTemplateDTO applicationTemplateDTO = new ApplicationTemplateDTO()
        applicationTemplateDTO.setId(4L)
        applicationTemplateDTO.setCode("code")
        applicationTemplateDTO.setName("app")
        applicationTemplateDTO.setDescription("des")
        applicationTemplateDTO.setOrganizationId(1L)

        and: 'mock saga'
        applicationTemplateService.initMockService(sagaClient)
        Mockito.doReturn(new SagaInstanceDTO()).when(sagaClient).startSaga(anyString(), anyObject())

        and: 'mock ????????????'
        DependencyInjectUtil.setAttribute(iamRepository, "iamServiceClient", iamServiceClient)
        Mockito.doReturn(new ResponseEntity<OrganizationDO>(organizationDO, HttpStatus.OK)).when(iamServiceClient).queryOrganizationById(anyLong())

        and: 'mock ??????gitlab???'
        applicationTemplateRepository.initMockService(iamServiceClient)
        DependencyInjectUtil.setAttribute(gitlabRepository, "gitlabServiceClient", gitlabServiceClient)
        GroupDO groupDO = null
        ResponseEntity<GroupDO> responseEntity = new ResponseEntity<>(groupDO, HttpStatus.OK)
        Mockito.when(gitlabServiceClient.queryGroupByName(anyString(), anyInt())).thenReturn(responseEntity)

        and: 'mock ??????gitlab???'
        GroupDO newGroupDO = new GroupDO()
        newGroupDO.setId(1)
        ResponseEntity<GroupDO> newResponseEntity = new ResponseEntity<>(newGroupDO, HttpStatus.OK)
        Mockito.when(gitlabServiceClient.createGroup(any(GroupDO.class), anyInt())).thenReturn(newResponseEntity)

        when: '???????????????????????????'
        def entity = restTemplate.postForEntity("/v1/organizations/1/app_templates", applicationTemplateDTO, ApplicationTemplateRepDTO.class)

        then: '?????????????????????'
        entity.statusCode.is2xxSuccessful()

        expect: '???????????????????????????'
        ApplicationTemplateDO applicationTemplateDO = applicationTemplateMapper.selectByPrimaryKey(4L)
        applicationTemplateDO["code"] == "code"
    }

    // ???????????????????????????
    def "updateTemplate"() {
        given: '?????????????????????dto???'
        ApplicationTemplateUpdateDTO applicationTemplateUpdateDTO = new ApplicationTemplateUpdateDTO()
        applicationTemplateUpdateDTO.setId(4L)
        applicationTemplateUpdateDTO.setName("updateName")
        applicationTemplateUpdateDTO.setDescription("des")

        when: '???????????????????????????'
        restTemplate.put("/v1/organizations/1/app_templates", applicationTemplateUpdateDTO, ApplicationTemplateRepDTO.class)

        then: '?????????'
        ApplicationTemplateDO applicationTemplateDO = applicationTemplateMapper.selectByPrimaryKey(4L)

        expect: '??????????????????'
        applicationTemplateMapper.selectAll().get(3)["name"] == "updateName"
    }

    // ?????????????????????????????????
    def "queryByAppTemplateId"() {
        when: '?????????????????????????????????'
        def object = restTemplate.getForObject("/v1/organizations/{org_id}/app_templates/{template_id}", ApplicationTemplateRepDTO.class, org_id, template_id)

        then: '??????????????????'
        object["code"] == "code"
    }

    // ?????????????????????????????????
    def "listByOptions"() {
        when: '?????????????????????????????????'
        def page = restTemplate.postForObject("/v1/organizations/{org_id}/app_templates/list_by_options", searchParam, Page.class, org_id)

        then: '??????????????????'
        page.size() == 1

        expect: '??????????????????'
        page.get(0)["code"] == "code"
    }

    // ?????????????????????????????????
    def "listByOrgId"() {
        when: '?????????????????????????????????'
        def list = restTemplate.getForObject("/v1/organizations/{org_id}/app_templates", List.class, org_id)

        then: '??????????????????'
        list.size() == 4

        expect: '??????????????????'
        list.get(3)["code"] == "code"
    }

    // ????????????????????????????????????
    def "checkName"() {
        when: '????????????????????????????????????'
        def exception = restTemplate.getForEntity("/v1/organizations/{org_id}/app_templates/check_name?name={name}", ExceptionResponse.class, org_id, "name")

        then: '?????????????????????????????????'
        exception.statusCode.is2xxSuccessful()
        notThrown(CommonException)
    }

    // ????????????????????????????????????
    def "checkCode"() {
        when: '????????????????????????????????????'
        def entity = restTemplate.getForEntity("/v1/organizations/{org_id}/app_templates/check_code?code={code}", Object.class, org_id, "testCode")

        then: '?????????????????????????????????'
        entity.statusCode.is2xxSuccessful()
        entity.getBody() == null

        when: '????????????????????????????????????'
        def exception = restTemplate.getForEntity("/v1/organizations/{org_id}/app_templates/check_code?code={code}", ExceptionResponse.class, org_id, "code")

        then: '??????????????????????????????'
        exception.statusCode.is2xxSuccessful()
        exception.getBody()["code"] == "error.code.exist"
    }

    // ???????????????????????????
    def "deleteTemplate"() {
        given: 'mock ??????gitlab??????'
        DependencyInjectUtil.setAttribute(gitlabRepository, "gitlabServiceClient", gitlabServiceClient)

        ProjectHook projectHook = new ProjectHook()
        ResponseEntity<ProjectHook> responseEntity = new ResponseEntity<>(projectHook, HttpStatus.OK)
        Mockito.when(gitlabServiceClient.deleteProject(anyInt(), anyInt())).thenReturn(responseEntity)

        when: '???????????????????????????'
        restTemplate.delete("/v1/organizations/{project_id}/app_templates/{template_id}", org_id, 4L)

        then: '?????????'
        ApplicationTemplateDO applicationTemplateDO = applicationTemplateMapper.selectByPrimaryKey(4L)

        expect: '??????????????????'
        applicationTemplateDO == null
    }
}
