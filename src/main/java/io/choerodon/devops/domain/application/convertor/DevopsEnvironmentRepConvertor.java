package io.choerodon.devops.domain.application.convertor;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import io.choerodon.core.convertor.ConvertorI;
import io.choerodon.devops.api.dto.DevopsEnviromentRepDTO;
import io.choerodon.devops.domain.application.entity.DevopsEnvironmentE;

@Component
public class DevopsEnvironmentRepConvertor implements ConvertorI<DevopsEnvironmentE, Object, DevopsEnviromentRepDTO> {

    @Override
    public DevopsEnviromentRepDTO entityToDto(DevopsEnvironmentE devopsEnvironmentE) {
        DevopsEnviromentRepDTO devopsEnviromentRepDTO = new DevopsEnviromentRepDTO();
        BeanUtils.copyProperties(devopsEnvironmentE, devopsEnviromentRepDTO);
        if (devopsEnvironmentE.getClusterE() != null) {
            devopsEnviromentRepDTO.setClusterId(devopsEnvironmentE.getClusterE().getId());
        }
        return devopsEnviromentRepDTO;
    }

}