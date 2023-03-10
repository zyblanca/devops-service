package io.choerodon.devops.api.controller.v1

import io.choerodon.core.domain.Page
import io.choerodon.devops.DependencyInjectUtil
import io.choerodon.devops.ExportOctetStream2HttpMessageConverter
import io.choerodon.devops.IntegrationTestConfiguration
import io.choerodon.devops.api.dto.AppMarketDownloadDTO
import io.choerodon.devops.api.dto.AppMarketTgzDTO
import io.choerodon.devops.api.dto.AppMarketVersionDTO
import io.choerodon.devops.api.dto.ApplicationReleasingDTO
import io.choerodon.devops.domain.application.entity.ProjectE
import io.choerodon.devops.domain.application.entity.UserAttrE
import io.choerodon.devops.domain.application.repository.IamRepository
import io.choerodon.devops.domain.application.valueobject.Organization
import io.choerodon.devops.infra.common.util.FileUtil
import io.choerodon.devops.infra.dataobject.*
import io.choerodon.devops.infra.dataobject.iam.OrganizationDO
import io.choerodon.devops.infra.dataobject.iam.ProjectDO
import io.choerodon.devops.infra.feign.IamServiceClient
import io.choerodon.devops.infra.mapper.*
import io.choerodon.mybatis.pagehelper.domain.PageRequest
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.core.io.FileSystemResource
import org.springframework.http.*
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.Subject

import static org.mockito.Matchers.*
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

/**
 * Created by n!Ck
 * Date: 2018/10/23
 * Time: 21:34
 * Description: 
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(IntegrationTestConfiguration)
@Subject(ApplicationMarketController)
@Stepwise
class ApplicationMarketControllerSpec extends Specification {

    @Autowired
    private TestRestTemplate restTemplate

    @Autowired
    private ApplicationMapper applicationMapper
    @Autowired
    private DevopsEnvironmentMapper devopsEnvironmentMapper
    @Autowired
    private ApplicationMarketMapper applicationMarketMapper
    @Autowired
    private ApplicationVersionMapper applicationVersionMapper
    @Autowired
    private ApplicationInstanceMapper applicationInstanceMapper
    @Autowired
    private ApplicationVersionValueMapper applicationVersionValueMapper
    @Autowired
    private ApplicationVersionReadmeMapper applicationVersionReadmeMapper

    @Autowired
    private IamRepository iamRepository

    IamServiceClient iamServiceClient = Mockito.mock(IamServiceClient.class)

    @Shared
    ApplicationDO applicationDO = new ApplicationDO()
    @Shared
    DevopsEnvironmentDO devopsEnvironmentDO = new DevopsEnvironmentDO()
    @Shared
    ApplicationVersionDO applicationVersionDO = new ApplicationVersionDO()
    @Shared
    ApplicationInstanceDO applicationInstanceDO = new ApplicationInstanceDO()

    @Shared
    Organization organization = new Organization()
    @Shared
    ProjectE projectE = new ProjectE()
    @Shared
    UserAttrE userAttrE = new UserAttrE()
    @Shared
    Map<String, Object> searchParam = new HashMap<>()
    @Shared
    PageRequest pageRequest = new PageRequest()
    @Shared
    Long project_id = 1L
    @Shared
    Long init_id = 1L
    @Shared
    String fileCode

    def setupSpec() {
        organization.setId(init_id)
        organization.setCode("org")

        projectE.setId(init_id)
        projectE.setCode("pro")
        projectE.setOrganization(organization)

        userAttrE.setIamUserId(init_id)
        userAttrE.setGitlabUserId(init_id)

        Map<String, Object> xxx = new HashMap<>()
        xxx.put("name", [])
        xxx.put("code", ["app"])
        searchParam.put("searchParam", xxx)
        searchParam.put("param", "")

        pageRequest.size = 10
        pageRequest.page = 0

        // da
        applicationDO.setId(1L)
        applicationDO.setActive(true)
        applicationDO.setProjectId(1L)
        applicationDO.setCode("appCode")
        applicationDO.setName("appName")

        // dav
        applicationVersionDO.setId(1L)
        applicationVersionDO.setAppId(1L)
        applicationVersionDO.setIsPublish(1L)
        applicationVersionDO.setVersion("0.0")
        applicationVersionDO.setReadmeValueId(1L)
        applicationVersionDO.setRepository("http://helm-charts.saas.hand-china.com/ystest/ystest/")

        // dai
        applicationInstanceDO.setId(1L)
        applicationInstanceDO.setEnvId(1L)
        applicationInstanceDO.setAppId(1L)

        // de
        devopsEnvironmentDO.setId(1L)
        devopsEnvironmentDO.setProjectId(2L)
    }

    def setup() {
        DependencyInjectUtil.setAttribute(iamRepository, "iamServiceClient", iamServiceClient)

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
    }

    def "Create"() {
        given: '????????????'
        applicationMapper.insert(applicationDO)
        applicationVersionMapper.insert(applicationVersionDO)

        and: '??????DTO'
        ApplicationReleasingDTO applicationReleasingDTO = new ApplicationReleasingDTO()
        applicationReleasingDTO.setAppId(1L)
        applicationReleasingDTO.setImgUrl("imgUrl")
        applicationReleasingDTO.setCategory("category")
        applicationReleasingDTO.setContributor("contributor")
        applicationReleasingDTO.setDescription("description")
        applicationReleasingDTO.setPublishLevel("public")

        and: '????????????'
        List<AppMarketVersionDTO> appVersions = new ArrayList<>()
        AppMarketVersionDTO appMarketVersionDTO = new AppMarketVersionDTO()
        appMarketVersionDTO.setId(1L)
        appVersions.add(appMarketVersionDTO)
        applicationReleasingDTO.setAppVersions(appVersions)

        when: '????????????'
        def marketId = restTemplate.postForObject("/v1/projects/1/apps_market", applicationReleasingDTO, Long.class)

        then: '??????????????????id'
        applicationMarketMapper.selectAll().get(0)["id"] == marketId
    }

    def "PageListMarketAppsByProjectId"() {
        given: '????????????'
        devopsEnvironmentMapper.insert(devopsEnvironmentDO)
        applicationInstanceMapper.insert(applicationInstanceDO)

        when: '??????????????????????????????????????????'
        def page = restTemplate.postForObject("/v1/projects/1/apps_market/list", searchParam, Page.class)

        then: '???????????????'
        page.getContent().get(0)["name"] == "appName"
    }

    def "ListAllApp"() {
        when: '??????????????????????????????????????????????????????????????????????????????'
        def page = restTemplate.postForObject("/v1/projects/1/apps_market/list_all", searchParam, Page.class)

        then: '???????????????'
        page.getContent().get(0)["name"] == "appName"
    }

    def "QueryAppInProject"() {
        when: '????????????????????????????????????????????????'
        def dto = restTemplate.getForObject("/v1/projects/1/apps_market/{app_market_id}/detail", ApplicationReleasingDTO.class
                , applicationMarketMapper.selectAll().get(0).getId())

        then: '???????????????'
        dto["code"] == "appCode"
    }

    def "QueryApp"() {
        when: '????????????????????????????????????????????????'
        def dto = restTemplate.getForObject("/v1/projects/1/apps_market/{app_market_id}", ApplicationReleasingDTO.class,
                applicationMarketMapper.selectAll().get(0).getId())

        then: '???????????????'
        dto["code"] == "appCode"
    }

    def "QueryAppVersionsInProject"() {
        when: '???????????????????????????????????????????????????'
        def list = restTemplate.getForObject("/v1/projects/1/apps_market/{app_market_id}/versions", List.class,
                applicationMarketMapper.selectAll().get(0).getId())

        then: '???????????????'
        list.get(0)["version"] == "0.0"
    }

    def "QueryAppVersionsInProjectByPage"() {
        when: '?????????????????????????????????????????????????????????'
        def page = restTemplate.postForObject("/v1/projects/1/apps_market/{app_market_id}/versions", searchParam, Page.class,
                applicationMarketMapper.selectAll().get(0).getId())

        then:
        page.getContent().get(0)["version"] == "0.0"
    }

    def "QueryAppVersionReadme"() {
        given: '??????App Version Readme'
        ApplicationVersionReadmeDO applicationVersionReadmeDO = new ApplicationVersionReadmeDO()
        applicationVersionReadmeDO.setId(1L)
        applicationVersionReadmeDO.setReadme("readme")
        applicationVersionReadmeMapper.insert(applicationVersionReadmeDO)

        when: '????????????????????????????????????????????????README'
        def str = restTemplate.getForObject("/v1/projects/1/apps_market/{app_market_id}/versions/{version_id}/readme",
                String.class, applicationMarketMapper.selectAll().get(0).getId(), applicationVersionMapper.selectAll().get(0).getId())

        then: '???????????????'
        str == "readme"
    }

    def "Update"() {
        given: '?????????DTO'
        Long appMarketId = applicationMarketMapper.selectAll().get(0).getId()
        ApplicationReleasingDTO applicationReleasingDTO = new ApplicationReleasingDTO()
        applicationReleasingDTO.setId(appMarketId)
        applicationReleasingDTO.setContributor("newContributor")
        applicationReleasingDTO.setPublishLevel("public")
        when: '?????????????????????????????????'
        restTemplate.put("/v1/projects/1/apps_market/{app_market_id}", applicationReleasingDTO, appMarketId)

        then: '???w???????????????contributor??????'
        applicationMarketMapper.selectAll().get(0)["contributor"] == "newContributor"
    }

    def "UpdateVersions"() {
        given: '??????dotList'
        AppMarketVersionDTO appMarketVersionDTO = new AppMarketVersionDTO()
        appMarketVersionDTO.setId(1L)
        List<AppMarketVersionDTO> dtoList = new ArrayList<>()
        dtoList.add(appMarketVersionDTO)

        when: '?????????????????????????????????'
        restTemplate.put("/v1/projects/1/apps_market/{app_market_id}/versions", dtoList,
                applicationMarketMapper.selectAll().get(0).getId())

        then: '???????????????'
        applicationMarketMapper.selectAll().get(0)["contributor"] == "newContributor"
    }

    def "UploadApps"() {
        given: '??????multipartFile'
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.parseMediaType("multipart/form-data"))

        MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>()
        FileSystemResource fileSystemResource = new FileSystemResource("src/test/resources/charts.zip")
        map.add("file", fileSystemResource)
        map.add("filename", fileSystemResource.getFilename())

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<MultiValueMap<String, Object>>(map, headers)

        when: '??????????????????????????????'
        def dto = restTemplate.postForObject("/v1/projects/1/apps_market/upload", requestEntity, AppMarketTgzDTO.class)
        fileCode = dto.fileCode

        then: '???????????????'
        dto.getAppMarketList().get(0)["id"] == 27
    }

    def "ImportApps"() {
        when: '????????????????????????'
        def bool = restTemplate.postForObject("/v1/projects/1/apps_market/import?file_name=" + fileCode + "&public=true",
                null, Boolean.class)

        then: '???????????????'
        bool
        applicationMarketMapper.selectAll().get(1)["contributor"] == "Choerodon"
        applicationMarketMapper.selectAll().get(0)["contributor"] == "newContributor"
    }

    def "DeleteZip"() {
        when: '??????????????????????????????'
        restTemplate.postForObject("/v1/projects/1/apps_market/import_cancel?file_name=", null, Object.class)

        then: '???????????????'
        File file = new File("tmp/org")
        file.listFiles().size() == 0
    }

    def "ExportFile"() {
        given: '??????dto list'
        List<AppMarketDownloadDTO> dtoList = new ArrayList<>()
        AppMarketDownloadDTO appMarketDownloadDTO = new AppMarketDownloadDTO()
        appMarketDownloadDTO.setAppMarketId(applicationMarketMapper.selectAll().get(0).getId())
        List<Long> appVersionList = new ArrayList<>()
        appVersionList.add(1L)
        appMarketDownloadDTO.setAppVersionIds(appVersionList)
        dtoList.add(appMarketDownloadDTO)

        and: '??????http?????????????????????'
        restTemplate.getRestTemplate().getMessageConverters().add(new ExportOctetStream2HttpMessageConverter())
        HttpHeaders headers = new HttpHeaders()
        List headersList = new ArrayList<>()
        headersList.add(MediaType.APPLICATION_OCTET_STREAM)
        headers.setAccept(headersList)

        HttpEntity<List<AppMarketDownloadDTO>> reqHttpEntity = new HttpEntity<>(dtoList)

        when: '??????????????????????????????'
        ResponseEntity<byte[]> responseEntity = restTemplate.exchange("/v1/projects/1/apps_market/export?fileName=testChart", HttpMethod.POST, reqHttpEntity, byte[].class)

        then: '???????????????'
        responseEntity.getHeaders().get("Content-Length").get(0).toString().toInteger() != 0

        // ??????app
        List<ApplicationDO> list = applicationMapper.selectAll()
        if (list != null && !list.isEmpty()) {
            for (ApplicationDO e : list) {
                applicationMapper.delete(e)
            }
        }
        // ??????appVersion
        List<ApplicationVersionDO> list1 = applicationVersionMapper.selectAll()
        if (list1 != null && !list1.isEmpty()) {
            for (ApplicationVersionDO e : list1) {
                applicationVersionMapper.delete(e)
            }
        }
        // ??????env
        List<DevopsEnvironmentDO> list2 = devopsEnvironmentMapper.selectAll()
        if (list2 != null && !list2.isEmpty()) {
            for (DevopsEnvironmentDO e : list2) {
                devopsEnvironmentMapper.delete(e)
            }
        }
        // ??????appVersionReadme
        List<ApplicationVersionReadmeDO> list3 = applicationVersionReadmeMapper.selectAll()
        if (list3 != null && !list3.isEmpty()) {
            for (ApplicationVersionReadmeDO e : list3) {
                applicationVersionReadmeMapper.delete(e)
            }
        }
        // ??????appVersionValue
        List<ApplicationVersionValueDO> list4 = applicationVersionValueMapper.selectAll()
        if (list4 != null && !list4.isEmpty()) {
            for (ApplicationVersionValueDO e : list4) {
                applicationVersionValueMapper.delete(e)
            }
        }
        // ??????appMarket
        List<DevopsAppMarketDO> list5 = applicationMarketMapper.selectAll()
        if (list5 != null && !list5.isEmpty()) {
            for (DevopsAppMarketDO e : list5) {
                applicationMarketMapper.delete(e)
            }
        }
        // ??????appInstance
        List<ApplicationInstanceDO> list6 = applicationInstanceMapper.selectAll()
        if (list6 != null && !list6.isEmpty()) {
            for (ApplicationInstanceDO e : list6) {
                applicationInstanceMapper.delete(e)
            }
        }
    }

    // ??????????????????
    def cleanupSpec() {
        FileUtil.deleteDirectory(new File("Charts"))
        FileUtil.deleteDirectory(new File("devops-service"))
        FileUtil.deleteDirectory(new File("tmp"))
    }
}
