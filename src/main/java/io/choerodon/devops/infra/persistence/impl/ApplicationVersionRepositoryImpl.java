package io.choerodon.devops.infra.persistence.impl;

import java.util.*;
import java.util.stream.Collectors;

import io.kubernetes.client.JSON;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.choerodon.core.convertor.ConvertHelper;
import io.choerodon.core.convertor.ConvertPageHelper;
import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.domain.application.entity.ApplicationVersionE;
import io.choerodon.devops.domain.application.entity.ProjectE;
import io.choerodon.devops.domain.application.repository.ApplicationVersionRepository;
import io.choerodon.devops.domain.application.repository.IamRepository;
import io.choerodon.devops.infra.common.util.TypeUtil;
import io.choerodon.devops.infra.dataobject.ApplicationLatestVersionDO;
import io.choerodon.devops.infra.dataobject.ApplicationVersionDO;
import io.choerodon.devops.infra.dataobject.ApplicationVersionReadmeDO;
import io.choerodon.devops.infra.mapper.ApplicationVersionMapper;
import io.choerodon.devops.infra.mapper.ApplicationVersionReadmeMapper;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;

/**
 * Created by Zenger on 2018/4/3.
 */
@Service
public class ApplicationVersionRepositoryImpl implements ApplicationVersionRepository {

    private static final String APP_CODE = "appCode";
    private static final String APP_NAME = "appName";
    private static JSON json = new JSON();

    @Autowired
    private ApplicationVersionMapper applicationVersionMapper;
    @Autowired
    private ApplicationVersionReadmeMapper applicationVersionReadmeMapper;
    @Autowired
    private IamRepository iamRepository;

    @Override
    public List<ApplicationLatestVersionDO> listAppLatestVersion(Long projectId) {
        ProjectE projectE = iamRepository.queryIamProject(projectId);
        Long organizationId = projectE.getOrganization().getId();
        List<ProjectE> projectEList = iamRepository.listIamProjectByOrgId(organizationId, null, null);
        List<Long> projectIds = projectEList.stream().map(ProjectE::getId)
                .collect(Collectors.toCollection(ArrayList::new));

        return applicationVersionMapper.listAppLatestVersion(projectId, projectIds);
    }

    @Override
    public ApplicationVersionE create(ApplicationVersionE applicationVersionE) {
        ApplicationVersionDO applicationVersionDO =
                ConvertHelper.convert(applicationVersionE, ApplicationVersionDO.class);
        applicationVersionDO.setReadmeValueId(setReadme(applicationVersionE.getApplicationVersionReadmeV().getReadme()));
        if (applicationVersionMapper.insert(applicationVersionDO) != 1) {
            throw new CommonException("error.version.insert");
        }
        return ConvertHelper.convert(applicationVersionDO, ApplicationVersionE.class);
    }

    @Override
    public List<ApplicationVersionE> listByAppId(Long appId, Boolean isPublish) {
        List<ApplicationVersionDO> applicationVersionDOS = applicationVersionMapper.selectByAppId(appId, isPublish);
        if (applicationVersionDOS.isEmpty()) {
            return Collections.emptyList();
        }
        return ConvertHelper.convertList(applicationVersionDOS, ApplicationVersionE.class);
    }

    @Override
    public List<ApplicationVersionE> listDeployedByAppId(Long projectId, Long appId) {
        List<ApplicationVersionDO> applicationVersionDOS =
                applicationVersionMapper.selectDeployedByAppId(projectId, appId);
        if (applicationVersionDOS.isEmpty()) {
            return Collections.emptyList();
        }
        return ConvertHelper.convertList(applicationVersionDOS, ApplicationVersionE.class);
    }

    @Override
    public ApplicationVersionE query(Long appVersionId) {
        return ConvertHelper.convert(
                applicationVersionMapper.selectByPrimaryKey(appVersionId), ApplicationVersionE.class);
    }

    @Override
    public List<ApplicationVersionE> listByAppIdAndEnvId(Long projectId, Long appId, Long envId) {
        return ConvertHelper.convertList(
                applicationVersionMapper.listByAppIdAndEnvId(projectId, appId, envId), ApplicationVersionE.class);
    }

    @Override
    public String queryValue(Long versionId) {
        return applicationVersionMapper.queryValue(versionId);
    }

    @Override
    public ApplicationVersionE queryByAppAndVersion(Long appId, String version) {
        ApplicationVersionDO applicationVersionDO = new ApplicationVersionDO();
        applicationVersionDO.setAppId(appId);
        applicationVersionDO.setVersion(version);
        List<ApplicationVersionDO> applicationVersionDOS = applicationVersionMapper.select(applicationVersionDO);
        if (applicationVersionDOS.isEmpty()) {
            return null;
        }
        return ConvertHelper.convert(applicationVersionDOS.get(0), ApplicationVersionE.class);
    }

    @Override
    public void updatePublishLevelByIds(List<Long> appVersionIds, Long level) {
        ApplicationVersionDO applicationVersionDO = new ApplicationVersionDO();
        applicationVersionDO.setIsPublish(level);
        for (Long id : appVersionIds) {
            applicationVersionDO.setId(id);
            applicationVersionMapper.updateByPrimaryKeySelective(applicationVersionDO);
        }
    }

    @Override
    public Page<ApplicationVersionE> listApplicationVersionInApp(Long projectId, Long appId, PageRequest pageRequest,
                                                                 String searchParam, Boolean isProjectOwner,
                                                                 Long userId) {
        if (pageRequest.getSort() != null) {
            Map<String, String> map = new HashMap<>();
            map.put("version", "dav.version");
            map.put(APP_CODE, APP_CODE);
            map.put(APP_NAME, APP_NAME);
            map.put("creationDate", "dav.creation_date");
            pageRequest.resetOrder("dav", map);
        }

        Page<ApplicationVersionDO> applicationVersionQueryDOPage;
        if (!StringUtils.isEmpty(searchParam)) {
            Map<String, Object> searchParamMap = json.deserialize(searchParam, Map.class);
            applicationVersionQueryDOPage = PageHelper
                    .doPageAndSort(pageRequest, () -> applicationVersionMapper.listApplicationVersion(projectId, appId,
                            TypeUtil.cast(searchParamMap.get(TypeUtil.SEARCH_PARAM)),
                            TypeUtil.cast(searchParamMap.get(TypeUtil.PARAM)), isProjectOwner, userId));
        } else {
            applicationVersionQueryDOPage = PageHelper.doPageAndSort(
                    pageRequest, () -> applicationVersionMapper
                            .listApplicationVersion(projectId, appId, null, null, isProjectOwner, userId));
        }
        return ConvertPageHelper.convertPage(applicationVersionQueryDOPage, ApplicationVersionE.class);
    }

    @Override
    public List<ApplicationVersionE> listAllPublishedVersion(Long applicationId) {
        List<ApplicationVersionDO> applicationVersionDOList = applicationVersionMapper
                .getAllPublishedVersion(applicationId);
        return ConvertHelper.convertList(applicationVersionDOList, ApplicationVersionE.class);
    }

    @Override
    public Boolean checkAppAndVersion(Long appId, List<Long> appVersionIds) {
        if (appId == null || appVersionIds.isEmpty()) {
            throw new CommonException("error.app.version.check");
        }
        List<Long> versionList = applicationVersionMapper.selectVersionsByAppId(appId);
        if (appVersionIds.stream().anyMatch(t -> !versionList.contains(t))) {
            throw new CommonException("error.app.version.check");
        }
        return true;
    }

    @Override
    public Long setReadme(String readme) {
        ApplicationVersionReadmeDO applicationVersionReadmeDO = new ApplicationVersionReadmeDO(readme);
        applicationVersionReadmeMapper.insert(applicationVersionReadmeDO);
        return applicationVersionReadmeDO.getId();
    }

    @Override
    public String getReadme(Long readmeValueId) {
        String readme;
        try {
            readme = applicationVersionReadmeMapper.selectByPrimaryKey(readmeValueId).getReadme();
        } catch (Exception ignore) {
            readme = "# 暂无";
        }
        return readme;
    }

    @Override
    public void updateVersion(ApplicationVersionE applicationVersionE) {
        ApplicationVersionDO applicationVersionDO =
                ConvertHelper.convert(applicationVersionE, ApplicationVersionDO.class);
        if (applicationVersionMapper.updateByPrimaryKey(applicationVersionDO) != 1) {
            throw new CommonException("error.version.update");
        }
        updateReadme(applicationVersionMapper.selectByPrimaryKey(applicationVersionE.getId()).getReadmeValueId(), applicationVersionE.getApplicationVersionReadmeV().getReadme());
    }

    private void updateReadme(Long readmeValueId, String readme) {
        ApplicationVersionReadmeDO readmeDO;
        try {

            readmeDO = applicationVersionReadmeMapper.selectByPrimaryKey(readmeValueId);
            readmeDO.setReadme(readme);
            applicationVersionReadmeMapper.updateByPrimaryKey(readmeDO);
        } catch (Exception e) {
            readmeDO = new ApplicationVersionReadmeDO(readme);
            applicationVersionReadmeMapper.insert(readmeDO);
        }
    }

    @Override
    public List<ApplicationVersionE> selectUpgradeVersions(Long appVersionId) {
        return ConvertHelper.convertList(
                applicationVersionMapper.selectUpgradeVersions(appVersionId), ApplicationVersionE.class);
    }

    @Override
    public void checkProIdAndVerId(Long projectId, Long appVersionId) {
        Integer index = applicationVersionMapper.checkProIdAndVerId(projectId, appVersionId);
        if (index == 0) {
            throw new CommonException("error.project.AppVersion.notExist");
        }
    }

    @Override
    public ApplicationVersionE queryByCommitSha(String sha) {
        ApplicationVersionDO applicationVersionDO = new ApplicationVersionDO();
        applicationVersionDO.setCommit(sha);
        return ConvertHelper.convert(applicationVersionMapper.selectOne(applicationVersionDO), ApplicationVersionE.class);
    }

    @Override
    public ApplicationVersionE getLatestVersion(Long appId) {
        return ConvertHelper.convert(applicationVersionMapper.getLatestVersion(appId), ApplicationVersionE.class);
    }

    @Override
    public List<ApplicationVersionE> listByAppVersionIds(List<Long> appVersionIds) {
        return ConvertHelper.convertList(applicationVersionMapper.listByAppVersionIds(appVersionIds), ApplicationVersionE.class);
    }

    @Override
    public List<ApplicationVersionE> listByAppIdAndBranch(Long appId, String branch) {
        return ConvertHelper.convertList(applicationVersionMapper.listByAppIdAndBranch(appId, branch), ApplicationVersionE.class);
    }

    @Override
    public String queryByPipelineId(Long pipelineId, String branch) {
        return applicationVersionMapper.queryByPipelineId(pipelineId, branch);
    }
}
