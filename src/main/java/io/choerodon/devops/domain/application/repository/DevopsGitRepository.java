package io.choerodon.devops.domain.application.repository;

import java.util.List;
import java.util.Map;

import io.choerodon.core.domain.Page;
import io.choerodon.devops.api.dto.TagDTO;
import io.choerodon.devops.domain.application.entity.DevopsBranchE;
import io.choerodon.devops.domain.application.entity.gitlab.CommitE;
import io.choerodon.devops.domain.application.entity.gitlab.CompareResultsE;
import io.choerodon.devops.infra.dataobject.gitlab.BranchDO;
import io.choerodon.devops.infra.dataobject.gitlab.CommitDO;
import io.choerodon.devops.infra.dataobject.gitlab.TagDO;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;

/**
 * Creator: Runge
 * Date: 2018/7/2
 * Time: 14:03
 * Description:
 */
public interface DevopsGitRepository {

    void createTag(Integer gitLabProjectId, String tag, String ref, String msg, String releaseNotes, Integer userId);

    TagDO updateTag(Integer gitLabProjectId, String tag, String releaseNotes, Integer userId);

    void deleteTag(Integer gitLabProjectId, String tag, Integer userId);

    Integer getGitLabId(Long applicationId);

    Integer getGitlabUserId();

    Long getUserIdByGitlabUserId(Long gitLabUserId);

    String getGitlabUrl(Long projectId, Long appId);

    void createDevopsBranch(DevopsBranchE devopsBranchE);

    BranchDO createBranch(Integer projectId, String branchName, String baseBranch, Integer userId);

    List<BranchDO> listGitLabBranches(Integer projectId, String path, Integer userId);

    Page<DevopsBranchE> listBranches(Long appId, PageRequest pageRequest, String params);

    void deleteDevopsBranch(Long appId, String branchName);

    Page<TagDTO> getTags(Long appId, String path, Integer page, String params, Integer size, Integer userId);

    List<TagDO> getTagList(Long appId, Integer userId);

    List<TagDO> getGitLabTags(Integer projectId, Integer userId);

    BranchDO getBranch(Integer gitlabProjectId, String branch);

    CompareResultsE getCompareResults(Integer gitlabProjectId, String from, String to);

    DevopsBranchE queryByAppAndBranchName(Long appId, String branchName);

    void updateBranchIssue(Long appId, DevopsBranchE devopsBranchE);

    void updateBranchLastCommit(DevopsBranchE devopsBranchE);

    List<DevopsBranchE> listDevopsBranchesByAppId(Long appId);

    List<DevopsBranchE> listDevopsBranchesByAppIdAndBranchName(Long appId, String branchName);

    Map<String, Object> getMergeRequestList(Long projectId,
                                            Integer gitLabProjectId,
                                            String state,
                                            PageRequest pageRequest);

    DevopsBranchE queryByBranchNameAndCommit(String branchName, String commit);

    CommitE getCommit(Integer gitLabProjectId, String commit, Integer userId);

    List<CommitDO> getCommits(Integer gitLabProjectId, String branchName, String date);

    List<BranchDO> listBranches(Integer gitlabProjectId, Integer userId);
}
