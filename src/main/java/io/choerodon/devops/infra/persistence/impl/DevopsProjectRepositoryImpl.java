package io.choerodon.devops.infra.persistence.impl;

import org.springframework.stereotype.Component;

import io.choerodon.core.convertor.ConvertHelper;
import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.domain.application.entity.gitlab.GitlabGroupE;
import io.choerodon.devops.domain.application.repository.DevopsProjectRepository;
import io.choerodon.devops.infra.common.util.TypeUtil;
import io.choerodon.devops.infra.dataobject.DevopsProjectDO;
import io.choerodon.devops.infra.mapper.DevopsProjectMapper;

/**
 * Created by younger on 2018/3/29.
 */
@Component
public class DevopsProjectRepositoryImpl implements DevopsProjectRepository {
    private DevopsProjectMapper devopsProjectMapper;

    public DevopsProjectRepositoryImpl(DevopsProjectMapper devopsProjectMapper) {
        this.devopsProjectMapper = devopsProjectMapper;
    }

    @Override
    public GitlabGroupE queryDevopsProject(Long projectId) {
        DevopsProjectDO devopsProjectDO = devopsProjectMapper.selectByPrimaryKey(projectId);
        if (devopsProjectDO == null) {
            throw new CommonException("error.group.not.sync");
        }
        if (devopsProjectDO.getDevopsAppGroupId() == null || devopsProjectDO.getDevopsEnvGroupId() == null) {
            throw new CommonException("error.gitlab.groupId.select");
        }
        return ConvertHelper.convert(devopsProjectDO, GitlabGroupE.class);
    }

    @Override
    public GitlabGroupE queryByGitlabGroupId(Integer gitlabGroupId) {
        return ConvertHelper.convert(devopsProjectMapper.queryByGitlabGroupId(gitlabGroupId), GitlabGroupE.class);
    }

    @Override
    public GitlabGroupE queryByEnvGroupId(Integer envGroupId) {
        DevopsProjectDO devopsProjectDO = new DevopsProjectDO();
        devopsProjectDO.setDevopsEnvGroupId(TypeUtil.objToLong(envGroupId));
        return ConvertHelper.convert(devopsProjectMapper.selectOne(devopsProjectDO), GitlabGroupE.class);
    }

    @Override
    public void createProject(DevopsProjectDO devopsProjectDO) {
        if (devopsProjectMapper.insert(devopsProjectDO) != 1) {
            throw new CommonException("insert project attr error");
        }
    }

    @Override
    public void updateProjectAttr(DevopsProjectDO devopsProjectDO) {
        DevopsProjectDO oldDevopsProjectDO = devopsProjectMapper.selectByPrimaryKey(devopsProjectDO.getIamProjectId());
        if (oldDevopsProjectDO == null) {
            devopsProjectMapper.insert(devopsProjectDO);
        } else {
            devopsProjectDO.setObjectVersionNumber(oldDevopsProjectDO.getObjectVersionNumber());
            devopsProjectMapper.updateByPrimaryKeySelective(devopsProjectDO);
        }
    }
}
