package io.choerodon.devops.api.controller.v1

import io.choerodon.core.domain.Page
import io.choerodon.core.exception.CommonException
import io.choerodon.core.exception.ExceptionResponse
import io.choerodon.devops.DependencyInjectUtil
import io.choerodon.devops.IntegrationTestConfiguration
import io.choerodon.devops.api.dto.DevopsBranchDTO
import io.choerodon.devops.api.dto.iam.ProjectWithRoleDTO
import io.choerodon.devops.api.dto.iam.RoleDTO
import io.choerodon.devops.domain.application.entity.UserAttrE
import io.choerodon.devops.domain.application.entity.gitlab.CommitE
import io.choerodon.devops.domain.application.entity.iam.UserE
import io.choerodon.devops.domain.application.repository.*
import io.choerodon.devops.domain.application.valueobject.Issue
import io.choerodon.devops.infra.common.util.enums.AccessLevel
import io.choerodon.devops.infra.dataobject.ApplicationDO
import io.choerodon.devops.infra.dataobject.DevopsBranchDO
import io.choerodon.devops.infra.dataobject.DevopsMergeRequestDO
import io.choerodon.devops.infra.dataobject.gitlab.BranchDO
import io.choerodon.devops.infra.dataobject.gitlab.CommitDO
import io.choerodon.devops.infra.dataobject.gitlab.MemberDO
import io.choerodon.devops.infra.dataobject.gitlab.TagDO
import io.choerodon.devops.infra.dataobject.iam.OrganizationDO
import io.choerodon.devops.infra.dataobject.iam.ProjectDO
import io.choerodon.devops.infra.dataobject.iam.UserDO
import io.choerodon.devops.infra.feign.AgileServiceClient
import io.choerodon.devops.infra.feign.GitlabServiceClient
import io.choerodon.devops.infra.feign.IamServiceClient
import io.choerodon.devops.infra.mapper.ApplicationMapper
import io.choerodon.devops.infra.mapper.DevopsBranchMapper
import io.choerodon.devops.infra.mapper.DevopsMergeRequestMapper
import io.choerodon.devops.infra.mapper.UserAttrMapper
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

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(IntegrationTestConfiguration)
@Subject(DevopsGitController)
@Stepwise
class DevopsGitControllerSpec extends Specification {

    @Autowired
    private TestRestTemplate restTemplate
    @Autowired
    private ApplicationMapper applicationMapper
    @Autowired
    private DevopsGitRepository devopsGitRepository
    @Autowired
    private DevopsBranchMapper devopsBranchMapper
    @Autowired
    private AgileRepository agileRepository
    @Autowired
    private DevopsMergeRequestMapper devopsMergeRequestMapper
    @Autowired
    private UserAttrMapper userAttrMapper
    @Autowired
    private DevopsBranchRepository devopsBranchRepository

    @Autowired
    private IamRepository iamRepository
    @Autowired
    private UserAttrRepository userAttrRepository
    @Autowired
    private GitlabProjectRepository gitlabProjectRepository

    GitlabServiceClient gitlabServiceClient = Mockito.mock(GitlabServiceClient.class)
    AgileServiceClient agileServiceClient = Mockito.mock(AgileServiceClient.class)
    IamServiceClient iamServiceClient = Mockito.mock(IamServiceClient.class)

    @Shared
    ApplicationDO applicationDO = new ApplicationDO()

    def setupSpec() {
        applicationDO.setId(1L)
        applicationDO.setProjectId(1L)
        applicationDO.setCode("test")
        applicationDO.setName("test")
        applicationDO.setGitlabProjectId(1)
    }

    def setup() {
        DependencyInjectUtil.setAttribute(iamRepository, "iamServiceClient", iamServiceClient)
        DependencyInjectUtil.setAttribute(devopsGitRepository, "gitlabServiceClient", gitlabServiceClient)
        DependencyInjectUtil.setAttribute(gitlabProjectRepository, "gitlabServiceClient", gitlabServiceClient)


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

        UserDO userDO = new UserDO()
        userDO.setLoginName("test")
        userDO.setId(1L)
        userDO.setImageUrl("imageURL")
        ResponseEntity<UserDO> responseEntity2 = new ResponseEntity<>(userDO, HttpStatus.OK)
        Mockito.when(iamServiceClient.queryByLoginName(anyString())).thenReturn(responseEntity2)

        List<UserDO> userDOList = new ArrayList<>()
        userDOList.add(userDO)
        ResponseEntity<List<UserDO>> responseEntity3 = new ResponseEntity<>(userDOList, HttpStatus.OK)
        Mockito.when(iamServiceClient.listUsersByIds(any(Long[].class))).thenReturn(responseEntity3)

        Page<UserDO> page = new Page<>()
        page.setTotalPages(1)
        page.setTotalElements(5)
        page.setContent(userDOList)
        ResponseEntity<Page<UserDO>> responseEntityPage = new ResponseEntity<>(page, HttpStatus.OK)
        Mockito.when(iamServiceClient.listUsersByEmail(anyLong(), anyInt(), anyInt(), anyString())).thenReturn(responseEntityPage)

        TagDO tagDO = new TagDO()
        tagDO.setName("testTag")
        CommitDO commitDO = new CommitDO()
        commitDO.setId("DOcommitId")
        commitDO.setMessage("message")
        commitDO.setCommittedDate(new Date(2018, 11, 9, 0, 0, 0))
        commitDO.setAuthorName("testAuthorName")
        tagDO.setCommit(commitDO)
        ResponseEntity<TagDO> responseEntity4 = new ResponseEntity<>(tagDO, HttpStatus.OK)
        Mockito.when(gitlabServiceClient.updateTagRelease(anyInt(), anyString(), anyString(), anyInt())).thenReturn(responseEntity4)

        List<TagDO> tagDOList = new ArrayList<>()
        tagDOList.add(tagDO)
        ResponseEntity<List<TagDO>> responseEntity5 = new ResponseEntity<>(tagDOList, HttpStatus.OK)
        Mockito.when(gitlabServiceClient.getTags(anyInt(), anyInt())).thenReturn(responseEntity5)

        BranchDO branchDO = new BranchDO()
        CommitE commitE = new CommitE()
        commitE.setMessage("message")
        commitE.setId("EcommitId")
        commitE.setCommittedDate(new Date(2018, 11, 9, 0, 0, 0))
        commitE.setAuthorName("testAuthorName")
        branchDO.setCommit(commitE)
        ResponseEntity<BranchDO> responseEntity6 = new ResponseEntity<>(branchDO, HttpStatus.OK)
        Mockito.when(gitlabServiceClient.createBranch(anyInt(), anyString(), anyString(), anyInt())).thenReturn(responseEntity6)
    }

    def "GetUrl"() {
        given: '???????????????'
        applicationMapper.insert(applicationDO)

        when: '?????????????????????'
        def url = restTemplate.getForObject("/v1/projects/1/apps/1/git/url", String.class)

        then: '??????????????????'
        url != ""
    }

    def "CreateTag"() {
        given: 'mock gitlab??????tag'
        UserAttrE userAttrE = new UserAttrE()
        userAttrE.setIamUserId(1L)
        userAttrE.setGitlabUserId(1L)
        Mockito.doReturn(null).when(gitlabServiceClient).createTag(null, null, null, null, null, null)

        when: '????????????'
        restTemplate.postForEntity("/v1/projects/1/apps/1/git/tags?tag=test&ref=test&message=test", "test", Object.class)

        then: '??????'
        userAttrRepository.queryById(_ as Long) >> userAttrE
    }

    def "UpdateTagRelease"() {
        given: 'mock ??????tag'
        UserAttrE userAttrE = new UserAttrE()
        userAttrE.setIamUserId(1L)
        userAttrE.setGitlabUserId(1L)
        ResponseEntity<TagDO> responseEntity = new ResponseEntity<>(new TagDO(), HttpStatus.OK)
        Mockito.doReturn(responseEntity).when(gitlabServiceClient).updateTagRelease(1, "test", "test", 1)

        when: '????????????'
        restTemplate.put("/v1/projects/1/apps/1/git/tags?tag=test", "test", Object.class)

        then: '??????'
        userAttrRepository.queryById(_ as Long) >> userAttrE
    }

    def "GetTagByPage"() {
        given:
        UserAttrE userAttrE = new UserAttrE()
        userAttrE.setIamUserId(1L)
        userAttrE.setGitlabUserId(1L)

        List<TagDO> tagDOS = new ArrayList<>()
        TagDO tagDO = new TagDO()
        CommitDO commitDO = new CommitDO()
        commitDO.setId("test")
        commitDO.setAuthorName("test")
        tagDO.setCommit(commitDO)
        tagDOS.add(tagDO)
        ResponseEntity<List<TagDO>> tagResponseEntity = new ResponseEntity<>(tagDOS, HttpStatus.OK)
        Mockito.doReturn(tagResponseEntity).when(gitlabServiceClient).getTags(1, 1)

        and: '????????????'
        MemberDO memberDO = new MemberDO()
        memberDO.setAccessLevel(AccessLevel.OWNER)
        ResponseEntity<MemberDO> responseEntity2 = new ResponseEntity<>(memberDO, HttpStatus.OK)
        Mockito.when(gitlabServiceClient.getProjectMember(anyInt(), anyInt())).thenReturn(responseEntity2)

        when: '????????????????????????'
        def page = restTemplate.postForObject("/v1/projects/1/apps/1/git/tags_list_options?page=0&size=10", null, Page.class)

        then: '???????????????'
        page.size() == 1
    }

    def "GetTagList"() {
        given:
        UserAttrE userAttrE = new UserAttrE()
        userAttrE.setIamUserId(1L)
        userAttrE.setGitlabUserId(1L)
        List<TagDO> tagDOS = new ArrayList<>()
        TagDO tagDO = new TagDO()
        CommitDO commitDO = new CommitDO()
        commitDO.setId("test")
        commitDO.setAuthorName("test")
        tagDO.setCommit(commitDO)
        tagDOS.add(tagDO)
        ResponseEntity<List<TagDO>> tagResponseEntity = new ResponseEntity<>(tagDOS, HttpStatus.OK)
        Mockito.doReturn(tagResponseEntity).when(gitlabServiceClient).getTags(1, 1)
        userAttrRepository.queryById(_ as Long) >> userAttrE

        when: '??????????????????'
        def tags = restTemplate.getForObject("/v1/projects/1/apps/1/git/tag_list", List.class)

        then: '???????????????'
        tags.size() == 1
    }

    def "CheckTag"() {
        given: 'mock gitlab??????tag'
        UserAttrE userAttrE = new UserAttrE()
        userAttrE.setIamUserId(1L)
        userAttrE.setGitlabUserId(1L)
        List<TagDO> tagDOS = new ArrayList<>()
        TagDO tagDO = new TagDO()
        CommitDO commitDO = new CommitDO()
        commitDO.setId("test")
        commitDO.setAuthorName("test")
        tagDO.setCommit(commitDO)
        tagDOS.add(tagDO)
        ResponseEntity<List<TagDO>> tagResponseEntity = new ResponseEntity<>(tagDOS, HttpStatus.OK)
        Mockito.doReturn(tagResponseEntity).when(gitlabServiceClient).getTags(1, 1)
        userAttrRepository.queryById(_ as Long) >> userAttrE

        when: '??????????????????'
        def exist = restTemplate.getForObject("/v1/projects/1/apps/1/git/tags_check?tag_name=test", Boolean.class)

        then:
        exist
    }

    def "DeleteTag"() {
        given: 'mock gitlab??????tag'
        UserAttrE userAttrE = new UserAttrE()
        userAttrE.setIamUserId(1L)
        userAttrE.setGitlabUserId(1L)
        Mockito.doReturn(null).when(gitlabServiceClient).deleteTag(null, null, null)

        when: '????????????'
        restTemplate.delete("/v1/projects/1/apps/1/git/tags?tag=test")

        then: '?????????'
        userAttrRepository.queryById(_ as Long) >> userAttrE
    }

    def "CreateBranch"() {
        given: 'mock gitlab????????????'
        UserAttrE userAttrE = new UserAttrE()
        userAttrE.setIamUserId(1L)
        userAttrE.setGitlabUserId(1L)
        DevopsBranchDTO devopsBranchDTO = new DevopsBranchDTO()
        devopsBranchDTO.setAppName("test")
        devopsBranchDTO.setBranchName("test")
        devopsBranchDTO.setIssueId(1L)
        devopsBranchDTO.setAppId(1L)
        devopsBranchDTO.setOriginBranch("test")
        BranchDO branchDO = new BranchDO()
        CommitE commitE = new CommitE()
        commitE.setId("test")
        commitE.setCommittedDate(new Date())
        commitE.setMessage("test")
        branchDO.setCommit(commitE)
        ResponseEntity<BranchDO> branchDOResponseEntity = new ResponseEntity<>(branchDO, HttpStatus.OK)
        Mockito.doReturn(branchDOResponseEntity).when(gitlabServiceClient).createBranch(1, "test", "test", 1)
        userAttrRepository.queryById(_ as Long) >> userAttrE

        when: '????????????'
        restTemplate.postForObject("/v1/projects/1/apps/1/git/branch", devopsBranchDTO, Object.class)

        then: '???????????????'
        devopsBranchMapper.selectAll().get(0)["branchName"] == "test"
    }

    def "ListByAppId"() {
        given: 'mock gitlab??????issue'
        UserAttrE userAttrE = new UserAttrE()
        userAttrE.setIamUserId(1L)
        userAttrE.setGitlabUserId(1L)
        Issue issue = new Issue()
        ResponseEntity<Issue> issueResponseEntity = new ResponseEntity<>(issue, HttpStatus.OK)
        UserE userE = new UserE()
        userE.setLoginName("test")
        userE.setId(1L)
        userE.setRealName("test")
        userE.setImageUrl("test")
        agileRepository.initAgileServiceClient(agileServiceClient)
        Mockito.when(agileServiceClient.queryIssue(anyLong(), anyLong(), anyLong())).thenReturn(issueResponseEntity)

        and: '????????????'
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

        when: '??????????????????????????????'
        def branches = restTemplate.postForObject("/v1/projects/1/apps/1/git/branches?page=0&size=10", null, Page.class)


        then: '???????????????'
        branches.size() == 1
    }

    def "QueryByAppId"() {
        when: '??????????????????'
        def devopsBranch = restTemplate.getForObject("/v1/projects/1/apps/1/git/branch?branchName=test", DevopsBranchDTO.class)

        then: '???????????????'
        devopsBranch["branchName"] == "test"
    }

    def "Update"() {
        given: '?????????branchDTO???'
        DevopsBranchDTO devopsBranchDTO = new DevopsBranchDTO()
        devopsBranchDTO.setBranchName("test")
        devopsBranchDTO.setIssueId(2L)

        when: '???????????????????????????'
        restTemplate.put("/v1/projects/1/apps/1/git/branch", devopsBranchDTO)

        then: '???????????????'
        devopsBranchMapper.selectByPrimaryKey(devopsBranchMapper.selectAll().get(0).getId()).getIssueId() == 2L
    }

    def "Delete"() {
        given: 'mock gitlab????????????'
        UserAttrE userAttrE = new UserAttrE()
        userAttrE.setIamUserId(1L)
        userAttrE.setGitlabUserId(1L)
        ResponseEntity responseEntity = new ResponseEntity(new Object(), HttpStatus.OK)
        Mockito.when(gitlabServiceClient.deleteBranch(anyInt(), anyString(), anyInt())).thenReturn(responseEntity)

        and: 'mock gitlab????????????'
        List<BranchDO> branchDOList = new ArrayList<>()
        BranchDO branchDO = new BranchDO()
        branchDO.setName("test")
        branchDOList.add(branchDO)
        ResponseEntity branchResponse = new ResponseEntity(branchDOList, HttpStatus.OK)
        Mockito.when(gitlabServiceClient.listBranches(anyInt(), anyInt())).thenReturn(branchResponse)

        when: '????????????'
        restTemplate.delete("/v1/projects/{project_id}/apps/{application_id}/git/branch?branch_name=test", 1L, 1L)

        then: '???????????????'
        devopsBranchMapper.selectByPrimaryKey(devopsBranchMapper.selectAll().get(0).getId()).getDeleted()
    }

    def "GetMergeRequestList"() {
        given: 'mock ??????commits'
        UserE userE = new UserE()
        userE.setLoginName("test")
        userE.setId(1L)
        userE.setRealName("test")
        userE.setImageUrl("test")
        UserAttrE userAttrE = new UserAttrE()
        userAttrE.setIamUserId(1L)
        userAttrE.setGitlabUserId(1L)
        DevopsMergeRequestDO devopsMergeRequestDO = new DevopsMergeRequestDO()
        devopsMergeRequestDO.setId(1L)
        devopsMergeRequestDO.setState("merged")
        devopsMergeRequestDO.setProjectId(1L)
        devopsMergeRequestDO.setGitlabMergeRequestId(1L)
        devopsMergeRequestDO.setAuthorId(1L)
        devopsMergeRequestDO.setAssigneeId(1L)
        devopsMergeRequestMapper.insert(devopsMergeRequestDO)
        DevopsMergeRequestDO devopsMergeRequestDO1 = new DevopsMergeRequestDO()
        devopsMergeRequestDO1.setId(2L)
        devopsMergeRequestDO1.setState("closed")
        devopsMergeRequestDO1.setProjectId(1L)
        devopsMergeRequestDO1.setGitlabMergeRequestId(2L)
        devopsMergeRequestDO1.setAuthorId(1L)
        devopsMergeRequestDO1.setAssigneeId(1L)
        devopsMergeRequestMapper.insert(devopsMergeRequestDO1)
        DevopsMergeRequestDO devopsMergeRequestDO2 = new DevopsMergeRequestDO()
        devopsMergeRequestDO2.setId(3L)
        devopsMergeRequestDO2.setState("opened")
        devopsMergeRequestDO2.setProjectId(1L)
        devopsMergeRequestDO2.setGitlabMergeRequestId(3L)
        devopsMergeRequestDO2.setAuthorId(1L)
        devopsMergeRequestDO2.setAssigneeId(1L)
        devopsMergeRequestMapper.insert(devopsMergeRequestDO2)
        List<CommitDO> commitDOList = new ArrayList<>()
        CommitDO commitDO = new CommitDO()
        commitDOList.add(commitDO)
        ResponseEntity<List<CommitDO>> responseEntity = new ResponseEntity<>(commitDOList, HttpStatus.OK)
        DependencyInjectUtil.setAttribute(devopsGitRepository, "gitlabServiceClient", gitlabServiceClient)
        Mockito.when(gitlabServiceClient.listCommits(anyInt(),anyInt(),anyInt())).thenReturn(responseEntity).thenReturn(responseEntity).thenReturn(responseEntity)
        userAttrRepository.queryById(_ as Long) >> userAttrE

        when: '????????????????????????'
        def mergeRequest = restTemplate.getForObject("/v1/projects/1/apps/1/git/merge_request/list?page=0&size=10", Map.class)

        then: '???????????????'
        !mergeRequest.isEmpty()
    }

    def "CheckName"() {
        given: 'mock gitlab????????????'
        List<BranchDO> branchDOList = new ArrayList<>()
        BranchDO branchDO = new BranchDO()
        branchDO.setName("test")
        branchDOList.add(branchDO)
        ResponseEntity branchResponse = new ResponseEntity(branchDOList, HttpStatus.OK)
        Mockito.when(gitlabServiceClient.listBranches(anyInt(), anyInt())).thenReturn(branchResponse)

        when: '????????????????????????'
        def exception = restTemplate.getForEntity("/v1/projects/{project_id}/apps/{application_id}/git/check_name?branch_name=uniqueName", ExceptionResponse.class, 1L, 1L)

        then: '??????????????????????????????'
        exception.statusCode.is2xxSuccessful()
        notThrown(CommonException)

        // ??????app
        List<ApplicationDO> list = applicationMapper.selectAll()
        if (list != null && !list.isEmpty()) {
            for (ApplicationDO e : list) {
                applicationMapper.delete(e)
            }
        }
        // ??????branch
        List<DevopsBranchDO> list1 = devopsBranchMapper.selectAll()
        if (list1 != null && !list1.isEmpty()) {
            for (DevopsBranchDO e : list1) {
                devopsBranchMapper.delete(e)
            }
        }
        // ??????mergeRequest
        List<DevopsMergeRequestDO> list2 = devopsMergeRequestMapper.selectAll()
        if (list2 != null && !list2.isEmpty()) {
            for (DevopsMergeRequestDO e : list2) {
                devopsMergeRequestMapper.delete(e)
            }
        }
    }
}
