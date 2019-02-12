package io.choerodon.devops.domain.application.convertor;


import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import io.choerodon.core.convertor.ConvertorI;
import io.choerodon.devops.api.dto.DevopsGitlabCommitDTO;
import io.choerodon.devops.domain.application.entity.DevopsGitlabCommitE;
import io.choerodon.devops.infra.dataobject.DevopsGitlabCommitDO;

@Component
public class DevopsGitlabCommitConvertor implements ConvertorI<DevopsGitlabCommitE, DevopsGitlabCommitDO, DevopsGitlabCommitDTO> {

    @Override
    public DevopsGitlabCommitE doToEntity(DevopsGitlabCommitDO devopsGitlabCommitDO) {
        DevopsGitlabCommitE devopsGitlabCommitE = new DevopsGitlabCommitE();
        BeanUtils.copyProperties(devopsGitlabCommitDO, devopsGitlabCommitE);
        return devopsGitlabCommitE;
    }

    @Override
    public DevopsGitlabCommitDO entityToDo(DevopsGitlabCommitE devopsGitlabCommitE) {
        DevopsGitlabCommitDO devopsGitlabCommitDO = new DevopsGitlabCommitDO();
        BeanUtils.copyProperties(devopsGitlabCommitE, devopsGitlabCommitDO);
        return devopsGitlabCommitDO;
    }

    @Override
    public DevopsGitlabCommitDTO entityToDto(DevopsGitlabCommitE devopsGitlabCommitE) {
        DevopsGitlabCommitDTO devopsGitlabCommitDTO = new DevopsGitlabCommitDTO();
        BeanUtils.copyProperties(devopsGitlabCommitE, devopsGitlabCommitDTO);
        return devopsGitlabCommitDTO;
    }
}
