package io.choerodon.devops.domain.application.repository;

import io.choerodon.devops.api.dto.gitlab.MemberDTO;
import io.choerodon.devops.api.dto.gitlab.VariableDTO;
import io.choerodon.devops.domain.application.entity.gitlab.GitlabGroupE;
import io.choerodon.devops.domain.application.valueobject.DeployKey;
import io.choerodon.devops.domain.application.valueobject.ProjectHook;
import io.choerodon.devops.domain.application.valueobject.Variable;
import io.choerodon.devops.infra.dataobject.gitlab.GitlabProjectDO;

import java.util.List;

/**
 * Created by younger on 2018/3/29.
 */
public interface GitlabRepository {

    void addVariable(Integer gitlabProjectId, String key, String value, Boolean protecteds, Integer userId);

    void batchAddVariable(Integer gitlabProjectId, Integer userId,  List<VariableDTO> variableDTOS);

    List<String> listTokenByUserId(Integer gitlabProjectId, String name, Integer userId);

    String createToken(Integer gitlabProjectId, String name, Integer userId);

    GitlabGroupE queryGroupByName(String groupName, Integer userId);

    GitlabGroupE createGroup(GitlabGroupE gitlabGroupE, Integer userId);

    void createFile(Integer projectId, String path, String content, String commitMessage, Integer userId);

    void updateFile(Integer projectId, String path, String content, String commitMessage, Integer userId);

    void deleteFile(Integer projectId, String path, String commitMessage, Integer userId);

    void deleteDevOpsApp(String groupName, String projectName, Integer userId);

    void createProtectBranch(Integer projectId, String name, String mergeAccessLevel, String pushAccessLevel,
                             Integer userId);

    void deleteProject(Integer projectId, Integer userId);

    String updateProject(Integer projectId, Integer userId);

    ProjectHook createWebHook(Integer projectId, Integer userId, ProjectHook projectHook);

    GitlabProjectDO createProject(Integer groupId, String projectName, Integer userId, boolean visibility);

    void createDeployKey(Integer projectId, String title, String key, boolean canPush, Integer userId);

    Boolean getFile(Integer projectId, String branch, String filePath);

    ProjectHook updateWebHook(Integer projectId, Integer hookId, Integer userId);

    GitlabProjectDO getProjectById(Integer projectId);

    GitlabProjectDO getProjectByName(String groupName, String projectName, Integer userId);

    List<ProjectHook> getHooks(Integer projectId, Integer userId);

    List<Variable> getVariable(Integer projectId, Integer userId);

    List<DeployKey> getDeployKeys(Integer projectId, Integer userId);

    /**
     * ??????????????????gitlab?????????????????????????????????????????????????????????????????????????????????
     */
    void addMemberIntoProject(Integer projectId, MemberDTO memberDTO);

    void updateMemberIntoProject(Integer projectId, List<MemberDTO> list);

    void removeMemberFromProject(Integer projectId, Integer userId);

    List<GitlabProjectDO> getProjectsByUserId(Integer userId);
}
