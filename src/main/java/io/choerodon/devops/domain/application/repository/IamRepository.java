package io.choerodon.devops.domain.application.repository;

import java.util.List;

import io.choerodon.core.domain.Page;
import io.choerodon.devops.api.dto.RoleAssignmentSearchDTO;
import io.choerodon.devops.api.dto.iam.ProjectWithRoleDTO;
import io.choerodon.devops.api.dto.iam.UserDTO;
import io.choerodon.devops.api.dto.iam.UserWithRoleDTO;
import io.choerodon.devops.domain.application.entity.ProjectE;
import io.choerodon.devops.domain.application.entity.iam.UserE;
import io.choerodon.devops.domain.application.event.IamAppPayLoad;
import io.choerodon.devops.domain.application.valueobject.Organization;
import io.choerodon.devops.infra.dataobject.iam.ProjectDO;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;

/**
 * Created by younger on 2018/3/29.
 */
public interface IamRepository {

    ProjectE queryIamProject(Long projectId);

    Organization queryOrganization();

    Organization queryOrganizationById(Long organizationId);

    UserE queryByLoginName(String userName);

    List<ProjectE> listIamProjectByOrgId(Long organizationId, String name, String[] params);

    Page<ProjectDO> queryProjectByOrgId(Long organizationId, int page, int size, String name, String[] params);

    List<UserE> listUsersByIds(List<Long> ids);

    UserE queryUserByUserId(Long id);

    UserE queryByEmail(Long projectId, String email);

    Page<UserDTO> pagingQueryUsersByRoleIdOnProjectLevel(PageRequest pageRequest,
                                                         RoleAssignmentSearchDTO roleAssignmentSearchDTO, Long roleId,
                                                         Long projectId, Boolean doPage);

    Page<UserWithRoleDTO> queryUserPermissionByProjectId(Long projectId, PageRequest pageRequest, Boolean doPage);

    List<ProjectWithRoleDTO> listProjectWithRoleDTO(Long userId);

    Long queryRoleIdByCode(String roleCode);

    List<Long> getAllMemberIdsWithoutOwner(Long projectId);

    Boolean isProjectOwner(Long userId, ProjectE projectE);

    IamAppPayLoad createIamApp(Long organizationId, IamAppPayLoad iamAppPayLoad);

    IamAppPayLoad updateIamApp(Long organizationId, Long id, IamAppPayLoad iamAppPayLoad);

    IamAppPayLoad queryIamAppByCode(Long organizationId, String code);
}
