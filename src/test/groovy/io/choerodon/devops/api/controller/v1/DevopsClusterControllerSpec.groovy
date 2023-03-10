package io.choerodon.devops.api.controller.v1

import com.alibaba.fastjson.JSONObject
import io.choerodon.core.domain.Page
import io.choerodon.core.exception.CommonException
import io.choerodon.core.exception.ExceptionResponse
import io.choerodon.devops.DependencyInjectUtil
import io.choerodon.devops.IntegrationTestConfiguration
import io.choerodon.devops.api.dto.ClusterNodeInfoDTO
import io.choerodon.devops.api.dto.DevopsClusterRepDTO
import io.choerodon.devops.api.dto.DevopsClusterReqDTO
import io.choerodon.devops.app.service.impl.ClusterNodeInfoServiceImpl
import io.choerodon.devops.domain.application.repository.IamRepository
import io.choerodon.devops.infra.common.util.EnvUtil
import io.choerodon.devops.infra.dataobject.ApplicationInstanceDO
import io.choerodon.devops.infra.dataobject.DevopsClusterDO
import io.choerodon.devops.infra.dataobject.DevopsEnvPodDO
import io.choerodon.devops.infra.dataobject.DevopsEnvironmentDO
import io.choerodon.devops.infra.dataobject.iam.OrganizationDO
import io.choerodon.devops.infra.dataobject.iam.ProjectDO
import io.choerodon.devops.infra.feign.IamServiceClient
import io.choerodon.devops.infra.mapper.*
import io.choerodon.websocket.helper.EnvListener
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.ListOperations
import org.springframework.data.redis.core.StringRedisTemplate
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
 * Date: 2018/11/13
 * Time: 14:03
 * Description: 
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(IntegrationTestConfiguration)
@Subject(DevopsClusterController)
@Stepwise
class DevopsClusterControllerSpec extends Specification {

    private static final String MAPPING = "/v1/organizations/{organization_id}/clusters"
    private static Long ID

    @Autowired
    private TestRestTemplate restTemplate

    @Autowired
    private IamRepository iamRepository
    @Autowired
    private DevopsClusterMapper devopsClusterMapper
    @Autowired
    private DevopsClusterProPermissionMapper devopsClusterProPermissionMapper

    @Autowired
    private DevopsEnvironmentMapper devopsEnvironmentMapper
    @Autowired
    private DevopsEnvPodMapper devopsEnvPodMapper
    @Autowired
    private ApplicationInstanceMapper applicationInstanceMapper

    @Autowired
    private ClusterNodeInfoServiceImpl clusterNodeInfoService

    @Autowired
    @Qualifier("mockEnvUtil")
    private EnvUtil envUtil

    IamServiceClient iamServiceClient = Mockito.mock(IamServiceClient.class)
    StringRedisTemplate mockStringRedisTemplate = Mockito.mock(StringRedisTemplate)

    @Shared
    private DevopsClusterDO devopsClusterDO = new DevopsClusterDO()
    @Shared
    private DevopsEnvironmentDO devopsEnvironmentDO = new DevopsEnvironmentDO()
    @Shared
    private ApplicationInstanceDO applicationInstanceDO = new ApplicationInstanceDO()
    @Shared
    private DevopsEnvPodDO devopsEnvPodDO = new DevopsEnvPodDO()

    @Shared
    private boolean isToInit = true
    @Shared
    private boolean isToClean = false

    def setup() {
        if (!isToInit) {
            return
        }

        DependencyInjectUtil.setAttribute(iamRepository, "iamServiceClient", iamServiceClient)
        DependencyInjectUtil.setAttribute(clusterNodeInfoService, "stringRedisTemplate", mockStringRedisTemplate)

        ProjectDO projectDO = new ProjectDO()
        projectDO.setId(1L)
        projectDO.setCode("pro")
        projectDO.setOrganizationId(1L)
        ResponseEntity<ProjectDO> responseEntity = new ResponseEntity<>(projectDO, HttpStatus.OK)
        Mockito.doReturn(responseEntity).when(iamServiceClient).queryIamProject(anyLong())

        OrganizationDO organizationDO = new OrganizationDO()
        organizationDO.setId(1L)
        organizationDO.setCode("org")
        ResponseEntity<OrganizationDO> responseEntity1 = new ResponseEntity<>(organizationDO, HttpStatus.OK)
        Mockito.doReturn(responseEntity1).when(iamServiceClient).queryOrganizationById(anyLong())

        Page<ProjectDO> projectDOPage = new Page<>()
        List<ProjectDO> projectDOList = new ArrayList<>()
        projectDOList.add(projectDO)
        projectDOPage.setContent(projectDOList)
        ResponseEntity<Page<ProjectDO>> projectDOPageResponseEntity = new ResponseEntity<>(projectDOPage, HttpStatus.OK)
        Mockito.when(iamServiceClient.queryProjectByOrgId(anyLong(), anyInt(), anyInt(), anyString(), any(String[].class))).thenReturn(projectDOPageResponseEntity)

        devopsClusterDO.setCode("uat")
        devopsClusterDO.setId(1000L)
        devopsClusterDO.setName("uat")
        devopsClusterDO.setOrganizationId(1L)
        devopsClusterMapper.insert(devopsClusterDO)

        devopsEnvironmentDO.setName("env")
        devopsEnvironmentDO.setCode("env")
        devopsEnvironmentDO.setClusterId(devopsClusterDO.getId())
        devopsEnvironmentMapper.insert(devopsEnvironmentDO)

        applicationInstanceDO.setAppId(1000L)
        applicationInstanceDO.setAppName("app")
        applicationInstanceDO.setEnvId(devopsEnvironmentDO.getId())
        applicationInstanceMapper.insert(applicationInstanceDO)

        devopsEnvPodDO.setAppInstanceId(applicationInstanceDO.getId())
        devopsEnvPodDO.setNodeName("uat01")
        devopsEnvPodDO.setRestartCount(11L)
        devopsEnvPodDO.setName("pod01")
        devopsEnvPodMapper.insert(devopsEnvPodDO)

        ListOperations<String, String> mockListOperations = Mockito.mock(ListOperations)
        Mockito.when(mockStringRedisTemplate.opsForList()).thenReturn(mockListOperations)
        Mockito.when(mockListOperations.size(anyString())).thenReturn(1L)

        ClusterNodeInfoDTO clusterNodeInfoDTO = new ClusterNodeInfoDTO()
        clusterNodeInfoDTO.setNodeName("uat01")

        Mockito.when(mockListOperations.range(anyString(), anyLong(), anyLong())).thenReturn(Arrays.asList(JSONObject.toJSONString(clusterNodeInfoDTO)))
    }

    def cleanup() {
        if (!isToClean) {
            return
        }
        DependencyInjectUtil.restoreDefaultDependency(iamRepository, "iamServiceClient")
        DependencyInjectUtil.restoreDefaultDependency(clusterNodeInfoService, "stringRedisTemplate")

        // ??????cluster
        devopsClusterMapper.selectAll().forEach{ devopsClusterMapper.delete(it) }

        // ??????clusterProRel
        devopsClusterProPermissionMapper.selectAll().forEach{ devopsClusterProPermissionMapper.delete(it) }
        devopsEnvironmentMapper.selectAll().forEach{ devopsEnvironmentMapper.delete(it) }
        devopsEnvPodMapper.selectAll().forEach{ devopsEnvPodMapper.delete(it) }
        applicationInstanceMapper.selectAll().forEach{ applicationInstanceMapper.delete(it) }

    }

    def "Create"() {
        given: '?????????DTO'
        isToInit = false
        DevopsClusterReqDTO devopsClusterReqDTO = new DevopsClusterReqDTO()
        List<Long> projectIds = new ArrayList<>()
        projectIds.add(1L)
        devopsClusterReqDTO.setCode("cluster")
        devopsClusterReqDTO.setName("cluster")
        devopsClusterReqDTO.setProjects(projectIds)
        devopsClusterReqDTO.setSkipCheckProjectPermission(false)

        when: '?????????????????????'
        def str = restTemplate.postForObject(MAPPING, devopsClusterReqDTO, String.class, 1L)

        then: '???????????????'
        str != null
    }

    def "Update"() {
        given: '?????????DTO'
        def searchCondition = new DevopsClusterDO()
        searchCondition.setCode("cluster")
        searchCondition.setName("cluster")
        ID = devopsClusterMapper.selectOne(searchCondition).getId()

        DevopsClusterReqDTO devopsClusterReqDTO = new DevopsClusterReqDTO()
        List<Long> projectIds = new ArrayList<>()
        projectIds.add(2L)
        devopsClusterReqDTO.setCode("cluster")
        devopsClusterReqDTO.setProjects(projectIds)
        devopsClusterReqDTO.setName("updateCluster")
        devopsClusterReqDTO.setSkipCheckProjectPermission(false)

        searchCondition.setName("updateCluster")

        when: '????????????????????????'
        restTemplate.put(MAPPING + "?clusterId=" + ID, devopsClusterReqDTO, 2L)

        then: '??????????????????'
        devopsClusterMapper.selectOne(searchCondition).getName() == "updateCluster"
    }

    def "Query"() {
        when: '????????????????????????'
        def dto = restTemplate.getForObject(MAPPING + "/" + ID, DevopsClusterRepDTO.class, 1L)

        then: '???????????????'
        dto["name"] == "updateCluster"
    }

    def "CheckName"() {
        when: '????????????????????????'
        def exception = restTemplate.getForEntity(MAPPING + "/check_name?name=uniqueName", ExceptionResponse.class, 1L)

        then: '??????????????????????????????'
        exception.statusCode.is2xxSuccessful()
        notThrown(CommonException)
    }

    def "CheckCode"() {
        when: '???????????????????????????'
        def exception = restTemplate.getForEntity(MAPPING + "/check_code?code=uniqueCode", ExceptionResponse.class, 1L)

        then: '??????????????????????????????'
        exception.statusCode.is2xxSuccessful()
        notThrown(CommonException)
    }

    def "PageProjects"() {
        given: '??????????????????'
        String[] str = new String[1]
        str[0] = "{}"

        when: '????????????????????????'
        def e = restTemplate.postForEntity(MAPPING + "/page_projects?page=0&size=10&cluster_id=" + ID, str, Page.class, 1L)

        then: '???????????????'
        e.getBody().get(0)["code"] == "pro"
    }

    def "ListClusterProjects"() {
        when: '?????????????????????????????????'
        def e = restTemplate.getForEntity(MAPPING + "/list_cluster_projects/{clusterId}", List.class, 1L, ID)

        then: '???????????????'
        e.getBody().get(0)["code"] == "pro"
    }

    def "QueryShell"() {
        given: 'mock envUtil'
        List<Long> envList = new ArrayList<>()
        envList.add(1L)
        envList.add(2L)
        envUtil.getConnectedEnvList(_ as EnvListener) >> envList
        envUtil.getUpdatedEnvList(_ as EnvListener) >> envList

        when: '??????shell??????'
        def e = restTemplate.getForEntity(MAPPING + "/query_shell/{clusterId}", String.class, 1L, ID)

        then: '???????????????'
        e.getBody() != null
    }

    def "ListCluster"() {
        given: '????????????'
        String str = new String("{}")

        and: 'mock envUtil'
        List<Long> envList = new ArrayList<>()
        envList.add(1L)
        envList.add(2L)
        envUtil.getConnectedEnvList(_ as EnvListener) >> envList
        envUtil.getUpdatedEnvList(_ as EnvListener) >> envList

        when: '??????????????????'
        def e = restTemplate.postForEntity(MAPPING + "/page_cluster?page=0&size=10&doPage=true", str, Page.class, 1L)

        then: '???????????????'
        e.getBody().get(0)["code"] == "cluster"
    }

    def "get pods in node"() {
        given: '??????????????????'

        when: '????????????'
        def e = restTemplate.postForEntity(MAPPING + "/page_node_pods?page=0&size=10&cluster_id={clusterId}&node_name={nodeName}", "{}", Page.class, 1L, devopsClusterDO.getId(), devopsEnvPodDO.getNodeName())

        then: '????????????'
        e.getStatusCode().is2xxSuccessful()
        e.getBody().getContent().size() == 1
    }

    // ??????????????????????????????
    def "list cluster nodes"() {
        given: "????????????"
        def url = MAPPING + "/page_nodes?cluster_id={cluster_id}&page=0&size=10"

        when: "????????????"
        def res = restTemplate.getForEntity(url, Page, devopsClusterDO.getOrganizationId(), devopsClusterDO.getId())

        then: "????????????"
        res.getStatusCode().is2xxSuccessful()
        res.getBody().getContent().size() == 1
    }

    // ????????????id????????????????????????????????????
    def "get certain node information"() {
        given: "????????????"
        def url = MAPPING + "/nodes?cluster_id={clusterId}&node_name={nodeName}"

        when: "????????????"
        def res = restTemplate.getForEntity(url, ClusterNodeInfoDTO, devopsClusterDO.getOrganizationId(), devopsClusterDO.getId(), devopsEnvPodDO.getNodeName())

        then: "????????????"
        res.getStatusCode().is2xxSuccessful()
        res.getBody().getNodeName() == devopsEnvPodDO.getNodeName()
    }

    def "DeleteCluster"() {
        given: 'mock envUtil'
        List<Long> envList = new ArrayList<>()
        envList.add(999L)
        envUtil.getConnectedEnvList(_ as EnvListener) >> envList

        when: '????????????'
        restTemplate.delete(MAPPING + "/{clusterId}", 1L, devopsClusterMapper.selectByPrimaryKey(ID).getId())

        then: '???????????????'
        devopsClusterMapper.selectByPrimaryKey(ID) == null
    }

    def clean() {
        given:
        isToClean = true
    }
}
