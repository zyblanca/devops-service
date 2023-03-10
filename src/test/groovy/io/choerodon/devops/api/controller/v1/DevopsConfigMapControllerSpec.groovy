package io.choerodon.devops.api.controller.v1

import io.choerodon.core.domain.Page
import io.choerodon.devops.DependencyInjectUtil
import io.choerodon.devops.IntegrationTestConfiguration
import io.choerodon.devops.api.dto.DevopsConfigMapDTO
import io.choerodon.devops.api.dto.DevopsConfigMapRepDTO
import io.choerodon.devops.api.dto.iam.ProjectWithRoleDTO
import io.choerodon.devops.api.dto.iam.RoleDTO
import io.choerodon.devops.app.service.DevopsEnvironmentService
import io.choerodon.devops.app.service.impl.DevopsConfigMapServiceImpl
import io.choerodon.devops.domain.application.entity.DevopsEnvironmentE
import io.choerodon.devops.domain.application.repository.GitlabGroupMemberRepository
import io.choerodon.devops.domain.application.repository.GitlabRepository
import io.choerodon.devops.domain.application.repository.IamRepository
import io.choerodon.devops.domain.application.valueobject.RepositoryFile
import io.choerodon.devops.infra.common.util.EnvUtil
import io.choerodon.devops.infra.common.util.FileUtil
import io.choerodon.devops.infra.common.util.enums.AccessLevel
import io.choerodon.devops.infra.dataobject.DevopsConfigMapDO
import io.choerodon.devops.infra.dataobject.DevopsEnvFileResourceDO
import io.choerodon.devops.infra.dataobject.DevopsEnvironmentDO
import io.choerodon.devops.infra.dataobject.gitlab.MemberDO
import io.choerodon.devops.infra.dataobject.iam.ProjectDO
import io.choerodon.devops.infra.feign.GitlabServiceClient
import io.choerodon.devops.infra.feign.IamServiceClient
import io.choerodon.devops.infra.mapper.DevopsConfigMapMapper
import io.choerodon.devops.infra.mapper.DevopsEnvFileResourceMapper
import io.choerodon.devops.infra.mapper.DevopsEnvironmentMapper
import io.choerodon.websocket.helper.EnvListener
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
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

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(IntegrationTestConfiguration)
@Subject(ApplicationController)
@Stepwise
class DevopsConfigMapControllerSpec extends Specification {

    private static final String MAPPING = "/v1/projects/{project_id}/config_maps"

    @Autowired
    private TestRestTemplate restTemplate
    @Autowired
    private DevopsEnvironmentMapper devopsEnvironmentMapper
    @Autowired
    private DevopsConfigMapServiceImpl devopsConfigMapServiceImpl
    @Autowired
    private DevopsConfigMapMapper devopsConfigMapMapper
    @Autowired
    private DevopsEnvFileResourceMapper devopsEnvFileResourceMapper
    @Autowired
    private IamRepository iamRepository
    @Autowired
    private GitlabRepository gitlabRepository
    @Autowired
    private GitlabGroupMemberRepository gitlabGroupMemberRepository


    @Autowired
    @Qualifier("mockEnvUtil")
    private EnvUtil envUtil

    DevopsEnvironmentService devopsEnvironmentService = Mockito.mock(DevopsEnvironmentService.class)
    IamServiceClient iamServiceClient = Mockito.mock(IamServiceClient.class)
    GitlabServiceClient gitlabServiceClient = Mockito.mock(GitlabServiceClient.class)

    @Shared
    private DevopsEnvironmentDO devopsEnvironmentDO = new DevopsEnvironmentDO()
    @Shared
    private DevopsEnvFileResourceDO devopsEnvFileResourceDO = new DevopsEnvFileResourceDO()
    @Shared
    private boolean isToInit = true
    @Shared
    private boolean isToClean = false


    void setup() {
        if (!isToInit) {
            return
        }

        DependencyInjectUtil.setAttribute(iamRepository, "iamServiceClient", iamServiceClient)
        DependencyInjectUtil.setAttribute(gitlabRepository, "gitlabServiceClient", gitlabServiceClient)
        DependencyInjectUtil.setAttribute(gitlabGroupMemberRepository, "gitlabServiceClient", gitlabServiceClient)

        ProjectDO projectDO = new ProjectDO()
        projectDO.setName("pro")
        projectDO.setOrganizationId(1L)
        ResponseEntity<ProjectDO> responseEntity = new ResponseEntity<>(projectDO, HttpStatus.OK)
        Mockito.when(iamServiceClient.queryIamProject(anyLong())).thenReturn(responseEntity)

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

        MemberDO memberDO = new MemberDO()
        memberDO.setAccessLevel(AccessLevel.OWNER)
        ResponseEntity<MemberDO> responseEntity1 = new ResponseEntity<>(memberDO, HttpStatus.OK)
        Mockito.when(gitlabServiceClient.getUserMemberByUserId(anyInt(), anyInt())).thenReturn(responseEntity1)

        RepositoryFile file = new RepositoryFile()
        file.setFilePath("filePath")
        ResponseEntity<RepositoryFile> responseEntity2 = new ResponseEntity<>(file, HttpStatus.OK)
        Mockito.when(gitlabServiceClient.createFile(anyInt(), anyString(), anyString(), anyString(), anyInt())).thenReturn(responseEntity2)

        Mockito.when(gitlabServiceClient.updateFile(anyInt(), anyString(), anyString(), anyString(), anyInt())).thenReturn(responseEntity2)
    }

    def cleanup() {
        if (!isToClean) {
            return
        }

        DependencyInjectUtil.restoreDefaultDependency(iamRepository, "iamServiceClient")
        DependencyInjectUtil.restoreDefaultDependency(gitlabRepository, "gitlabServiceClient")
        DependencyInjectUtil.restoreDefaultDependency(gitlabGroupMemberRepository, "gitlabServiceClient")
//        DependencyInjectUtil.restoreDefaultDependency(devopsConfigMapServiceImpl, "devopsEnvironmentService")

        // ??????secret
        devopsConfigMapMapper.selectAll().forEach{devopsConfigMapMapper.delete(it)}
        // ??????envFileResource
        devopsEnvFileResourceMapper.selectAll().forEach{devopsEnvFileResourceMapper.delete(it)}
        // ??????env
        devopsEnvironmentMapper.selectAll().forEach{devopsEnvironmentMapper.delete(it)}
        // ??????gitops
        FileUtil.deleteDirectory(new File("gitops"))
    }

    def setupSpec() {
        devopsEnvironmentDO.setId(1L)
        devopsEnvironmentDO.setProjectId(1L)
        devopsEnvironmentDO.setGitlabEnvProjectId(1L)

        devopsEnvFileResourceDO.setId(1L)
        devopsEnvFileResourceDO.setEnvId(1L)
        devopsEnvFileResourceDO.setResourceId(1L)
        devopsEnvFileResourceDO.setFilePath("configMap-test.yaml")
        devopsEnvFileResourceDO.setResourceType("ConfigMap")
    }

    def "Create"() {

        given: '???????????????'
        isToInit = false

        FileUtil.copyFile("src/test/gitops/testConfigMap/configMap-test.yaml", "gitops/testConfigMap")
        devopsEnvironmentMapper.insert(devopsEnvironmentDO)
        devopsEnvFileResourceMapper.insert(devopsEnvFileResourceDO)

        and: '?????????DTO'
        DevopsConfigMapDTO devopsConfigMapDTO = new DevopsConfigMapDTO()
        devopsConfigMapDTO.setId(1L)
        devopsConfigMapDTO.setName("asdasdqqqq")
        devopsConfigMapDTO.setEnvId(1L)
        devopsConfigMapDTO.setDescription("ggggg")
        devopsConfigMapDTO.setType("create")
        Map<String, String> valueMap = new HashMap<>()
        valueMap.put("xxxx", "xxxxx")
        devopsConfigMapDTO.setValue(valueMap)

        and: 'mock envUtil'
        envUtil.checkEnvConnection(_ as Long, _ as EnvListener) >> null

        when: '??????'
        restTemplate.postForObject(MAPPING, devopsConfigMapDTO, Object.class, 1L)

        then: '????????????'
        devopsConfigMapMapper.selectAll().get(0).getDescription() == "ggggg"

        when: '????????????key-value?????????'
        devopsConfigMapDTO.setId(devopsConfigMapMapper.selectAll().get(0).getId())
        devopsConfigMapDTO.setType("update")
        devopsConfigMapDTO.setDescription("gggggnew")
        restTemplate.postForObject(MAPPING, devopsConfigMapDTO, Object.class, 1L)

        then: '????????????'
        devopsConfigMapMapper.selectAll().get(0).getDescription() == "gggggnew"

        when: '??????????????????key-value'
        valueMap.put("xxxxnew", "xxxxxnew")
        devopsConfigMapDTO.setValue(valueMap)
        restTemplate.postForObject(MAPPING, devopsConfigMapDTO, Object.class, 1L)

        then: '????????????'
        devopsConfigMapMapper.selectAll().get(0).getValue().contains("xxxxnew")
    }

    def "CheckName"() {
        when: '?????????????????????'
        restTemplate.getForEntity(MAPPING + "/check_name?envId=1&configMapName=asdasd", Object.class, 1L)

        then: '????????????'
        noExceptionThrown()
    }

    def "Query"() {
        when: '??????id??????'
        def dto = restTemplate.getForEntity(MAPPING + "/1", DevopsConfigMapRepDTO.class, 1L)

        then: '????????????'
        dto.getBody()["name"] == "asdasdqqqq"
    }

    def "ListByEnv"() {
        given: '????????????'
        String params = "{\"searchParam\":{},\"param\":\"\"}"

        when: '????????????'
        def page = restTemplate.postForEntity(MAPPING + "/1/listByEnv?page=0&size=10", params, Page.class, 1L)

        then: '????????????'
        page.getBody().get(0)["name"] == "asdasdqqqq"
    }

    def "Delete"() {
        given: 'mock envUtil'
        envUtil.checkEnvConnection(_ as Long, _ as EnvListener) >> null

        when: '????????????'
        restTemplate.delete(MAPPING + "/1/delete", 1L)

        then: '????????????'
        noExceptionThrown()
    }

    def "clean data"() {
        given: "????????????"
        isToClean = true
    }
}
