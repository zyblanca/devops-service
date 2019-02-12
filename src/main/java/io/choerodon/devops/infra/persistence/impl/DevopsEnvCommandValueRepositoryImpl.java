package io.choerodon.devops.infra.persistence.impl;

import org.springframework.stereotype.Service;

import io.choerodon.core.convertor.ConvertHelper;
import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.domain.application.entity.DevopsEnvCommandValueE;
import io.choerodon.devops.domain.application.repository.DevopsEnvCommandValueRepository;
import io.choerodon.devops.infra.dataobject.DevopsEnvCommandValueDO;
import io.choerodon.devops.infra.mapper.DevopsEnvCommandValueMapper;

@Service
public class DevopsEnvCommandValueRepositoryImpl implements DevopsEnvCommandValueRepository {

    private DevopsEnvCommandValueMapper devopsEnvCommandValueMapper;

    public DevopsEnvCommandValueRepositoryImpl(DevopsEnvCommandValueMapper devopsEnvCommandValueMapper) {
        this.devopsEnvCommandValueMapper = devopsEnvCommandValueMapper;
    }

    @Override
    public DevopsEnvCommandValueE create(DevopsEnvCommandValueE devopsEnvCommandValueE) {
        DevopsEnvCommandValueDO devopsEnvCommandValueDO = ConvertHelper
                .convert(devopsEnvCommandValueE, DevopsEnvCommandValueDO.class);
        if (devopsEnvCommandValueMapper.insert(devopsEnvCommandValueDO) != 1) {
            throw new CommonException("error.env.command.value.insert");
        }
        return ConvertHelper.convert(devopsEnvCommandValueDO, DevopsEnvCommandValueE.class);
    }
}
