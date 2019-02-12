package io.choerodon.devops.app.service.impl;


import java.util.List;

import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.choerodon.core.convertor.ConvertHelper;
import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.app.service.GitlabGroupService;
import io.choerodon.devops.domain.application.entity.ProjectE;
import io.choerodon.devops.domain.application.entity.UserAttrE;
import io.choerodon.devops.domain.application.entity.gitlab.GitlabGroupE;
import io.choerodon.devops.domain.application.event.GitlabGroupPayload;
import io.choerodon.devops.domain.application.repository.DevopsProjectRepository;
import io.choerodon.devops.domain.application.repository.GitlabRepository;
import io.choerodon.devops.domain.application.repository.IamRepository;
import io.choerodon.devops.domain.application.repository.UserAttrRepository;
import io.choerodon.devops.domain.application.valueobject.Organization;
import io.choerodon.devops.infra.common.util.TypeUtil;
import io.choerodon.devops.infra.dataobject.DevopsProjectDO;
import io.choerodon.devops.infra.dataobject.gitlab.GroupDO;
import io.choerodon.devops.infra.feign.GitlabServiceClient;

/**
 * Created with IntelliJ IDEA.
 * User: Runge
 * Date: 2018/4/8
 * Time: 10:05
 * Description:
 */
@Component
public class GitlabGroupServiceImpl implements GitlabGroupService {

    private static final String GITLAB_GROUP_NAME_PATTERN_STRING = "[^\\u4E00-\\u9FA5a-zA-Z0-9_\\-.\\s]";
    private static final String GROUP_NAME_FORMAT = "%s-%s%s";

    @Autowired
    private GitlabServiceClient gitlabServiceClient;
    @Autowired
    private DevopsProjectRepository devopsProjectRepository;
    @Autowired
    private UserAttrRepository userAttrRepository;
    @Autowired
    private IamRepository iamRepository;
    @Autowired
    private GitlabRepository gitlabRepository;

    @Override
    public void createGroup(GitlabGroupPayload gitlabGroupPayload, String groupCodeSuffix) {

        String gitlabProjectName = getGitlabProjectName(gitlabGroupPayload);

        //创建gitlab group
        GroupDO group = new GroupDO();
        // name: orgName-projectName
        group.setName(String.format(GROUP_NAME_FORMAT,
                gitlabGroupPayload.getOrganizationName(),
                gitlabProjectName,
                groupCodeSuffix));
        // path: orgCode-projectCode
        group.setPath(String.format(GROUP_NAME_FORMAT,
                gitlabGroupPayload.getOrganizationCode(),
                gitlabGroupPayload.getProjectCode(),
                groupCodeSuffix));
        UserAttrE userAttrE = userAttrRepository.queryById(gitlabGroupPayload.getUserId());
        GitlabGroupE gitlabGroupE = gitlabRepository.queryGroupByName(group.getPath(), TypeUtil.objToInteger(userAttrE.getGitlabUserId()));
        try {
            if (gitlabGroupE == null) {
                gitlabGroupE =
                        ConvertHelper.convert(gitlabServiceClient.createGroup(group, TypeUtil.objToInteger(userAttrE.getGitlabUserId())).getBody(), GitlabGroupE.class);
            }
        } catch (FeignException e) {
            throw new CommonException(e);
        }
        DevopsProjectDO devopsProjectDO = new DevopsProjectDO(gitlabGroupPayload.getProjectId());
        if (groupCodeSuffix.isEmpty()) {
            devopsProjectDO.setDevopsAppGroupId(TypeUtil.objToLong(gitlabGroupE.getId()));
        } else if ("-gitops".equals(groupCodeSuffix)) {
            devopsProjectDO.setDevopsEnvGroupId(TypeUtil.objToLong(gitlabGroupE.getId()));
        }
        devopsProjectRepository.updateProjectAttr(devopsProjectDO);
    }

    private String getGitlabProjectName(GitlabGroupPayload gitlabGroupPayload) {
        ProjectE projectE = iamRepository.queryIamProject(gitlabGroupPayload.getProjectId());
        Organization organization = iamRepository.queryOrganizationById(projectE.getOrganization().getId());
        List<ProjectE> projectES = iamRepository.listIamProjectByOrgId(organization.getId(), gitlabGroupPayload.getProjectName(), null);
        String validProjectName = getValidGroupName(gitlabGroupPayload.getProjectName());
        return projectES.size() > 1 ? validProjectName + "-" + (projectES.size() - 1) : validProjectName;
    }

    @Override
    public void updateGroup(GitlabGroupPayload gitlabGroupPayload, String groupCodeSuffix) {
        String gitlabProjectName = getGitlabProjectName(gitlabGroupPayload);

        //创建gitlab group
        GroupDO group = new GroupDO();
        // name: orgName-projectName
        group.setName(String.format(GROUP_NAME_FORMAT,
                gitlabGroupPayload.getOrganizationName(),
                gitlabProjectName,
                groupCodeSuffix));
        // path: orgCode-projectCode
        group.setPath(String.format(GROUP_NAME_FORMAT,
                gitlabGroupPayload.getOrganizationCode(),
                gitlabGroupPayload.getProjectCode(),
                groupCodeSuffix));
        UserAttrE userAttrE = userAttrRepository.queryById(gitlabGroupPayload.getUserId());
        GitlabGroupE gitlabGroupE = devopsProjectRepository.queryDevopsProject(gitlabGroupPayload.getProjectId());
        Integer groupId;
        if (groupCodeSuffix.isEmpty()) {
            groupId = TypeUtil.objToInteger(gitlabGroupE.getDevopsAppGroupId());
        } else {
            groupId = TypeUtil.objToInteger(gitlabGroupE.getDevopsEnvGroupId());
        }
        try {
            gitlabServiceClient.updateGroup(groupId, TypeUtil.objToInteger(userAttrE.getGitlabUserId()), group);
        } catch (FeignException e) {
            throw new CommonException(e);
        }

    }

    /**
     * process the original name to get a valid name.
     * The invalid characters will be replaced by '_' (lower dash)
     *
     * @param groupName the original group name
     * @return a valid name after processed
     */
    private String getValidGroupName(String groupName) {
        return groupName == null ? null : groupName.replaceAll(GITLAB_GROUP_NAME_PATTERN_STRING, "_");
    }
}
