package io.choerodon.devops.domain.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.choerodon.core.convertor.ApplicationContextHelper;
import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.domain.application.entity.UserAttrE;
import io.choerodon.devops.domain.application.entity.gitlab.GitlabMemberE;
import io.choerodon.devops.domain.application.repository.ApplicationRepository;
import io.choerodon.devops.domain.application.repository.GitlabProjectRepository;
import io.choerodon.devops.domain.application.repository.IamRepository;
import io.choerodon.devops.domain.application.repository.UserAttrRepository;
import io.choerodon.devops.domain.service.UpdateUserPermissionService;
import io.choerodon.devops.infra.common.util.TypeUtil;

/**
 * Created by n!Ck
 * Date: 2018/11/21
 * Time: 16:08
 * Description:
 */
public class UpdateAppUserPermissionServiceImpl extends UpdateUserPermissionService {

    private ApplicationRepository applicationRepository;
    private IamRepository iamRepository;
    private UserAttrRepository userAttrRepository;
    private GitlabProjectRepository gitlabProjectRepository;

    public UpdateAppUserPermissionServiceImpl() {
        this.applicationRepository = ApplicationContextHelper.getSpringFactory().getBean(ApplicationRepository.class);
        this.iamRepository = ApplicationContextHelper.getSpringFactory().getBean(IamRepository.class);
        this.userAttrRepository = ApplicationContextHelper.getSpringFactory().getBean(UserAttrRepository.class);
        this.gitlabProjectRepository = ApplicationContextHelper.getSpringFactory()
                .getBean(GitlabProjectRepository.class);
    }

    @Override
    public Boolean updateUserPermission(Long projectId, Long appId, List<Long> userIds, Integer option) {

        List<Integer> allMemberGitlabIdsWithoutOwner;
        List<Integer> addGitlabUserIds;
        List<Integer> deleteGitlabUserIds;
        List<Integer> updateGitlabUserIds;

        Integer gitlabProjectId = applicationRepository.query(appId).getGitlabProjectE().getId();

        // ?????????????????????gitlab project???????????????????????????????????????
        if (gitlabProjectId == null) {
            throw new CommonException("error.gitlab.project.sync.failed");
        }

        switch (option) {
            // ?????????????????????????????????????????????????????????????????????????????????gitlab????????????
            case 1:
                updateGitlabUserIds = userAttrRepository.listByUserIds(userIds)
                        .stream().map(e -> TypeUtil.objToInteger(e.getGitlabUserId())).collect(Collectors.toList());
                // ????????????????????????????????????gitlabUserIds???????????????????????????
                allMemberGitlabIdsWithoutOwner = getAllGitlabMemberWithoutOwner(projectId);

                addGitlabUserIds = new ArrayList<>(updateGitlabUserIds);
                addGitlabUserIds.removeAll(allMemberGitlabIdsWithoutOwner);

                deleteGitlabUserIds = new ArrayList<>(allMemberGitlabIdsWithoutOwner);
                deleteGitlabUserIds.removeAll(updateGitlabUserIds);

                super.updateGitlabUserPermission("app", gitlabProjectId, addGitlabUserIds, deleteGitlabUserIds);
                return true;
            // ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????gitlab??????
            case 2:
                // ????????????????????????????????????gitlabUserIds???????????????????????????
                allMemberGitlabIdsWithoutOwner = getAllGitlabMemberWithoutOwner(projectId);

                addGitlabUserIds = allMemberGitlabIdsWithoutOwner.stream()
                        .filter(e -> !gitlabProjectRepository.getAllMemberByProjectId(gitlabProjectId).stream()
                                .map(GitlabMemberE::getId).collect(Collectors.toList()).contains(e))
                        .collect(Collectors.toList());

                super.updateGitlabUserPermission("app", gitlabProjectId, addGitlabUserIds, new ArrayList<>());
                return true;
            // ????????????????????????????????????????????????????????????
            case 3:
                updateGitlabUserIds = userAttrRepository.listByUserIds(userIds).stream()
                        .map(e -> TypeUtil.objToInteger(e.getGitlabUserId())).collect(Collectors.toList());
                List<Integer> currentGitlabUserIds = gitlabProjectRepository.getAllMemberByProjectId(gitlabProjectId)
                        .stream().map(GitlabMemberE::getId).collect(Collectors.toList());

                addGitlabUserIds = new ArrayList<>(updateGitlabUserIds);
                addGitlabUserIds.removeAll(currentGitlabUserIds);

                deleteGitlabUserIds = new ArrayList<>(currentGitlabUserIds);
                deleteGitlabUserIds.removeAll(updateGitlabUserIds);

                super.updateGitlabUserPermission("app", gitlabProjectId, addGitlabUserIds, deleteGitlabUserIds);
                return true;
            default:
                return true;
        }
    }

    // ??????iam?????????????????????????????????gitlabUserId???????????????????????????
    private List<Integer> getAllGitlabMemberWithoutOwner(Long projectId) {
        return userAttrRepository.listByUserIds(iamRepository.getAllMemberIdsWithoutOwner(projectId)).stream()
                .map(UserAttrE::getGitlabUserId).collect(Collectors.toList()).stream()
                .map(TypeUtil::objToInteger).collect(Collectors.toList());
    }
}
