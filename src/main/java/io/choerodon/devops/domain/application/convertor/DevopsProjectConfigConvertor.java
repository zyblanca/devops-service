package io.choerodon.devops.domain.application.convertor;

import com.google.gson.Gson;
import io.choerodon.core.convertor.ConvertorI;
import io.choerodon.devops.api.dto.DevopsProjectConfigDTO;
import io.choerodon.devops.api.dto.ProjectConfigDTO;
import io.choerodon.devops.domain.application.entity.DevopsProjectConfigE;
import io.choerodon.devops.infra.dataobject.DevopsProjectConfigDO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * @author zongw.lee@gmail.com
 * @since 2019/03/11
 */
@Component
public class DevopsProjectConfigConvertor implements ConvertorI<DevopsProjectConfigE, DevopsProjectConfigDO, DevopsProjectConfigDTO> {

    private static final Gson gson = new Gson();

    @Override
    public DevopsProjectConfigE doToEntity(DevopsProjectConfigDO devopsProjectConfigDO) {
        DevopsProjectConfigE devopsProjectConfigE = new DevopsProjectConfigE();
        BeanUtils.copyProperties(devopsProjectConfigDO, devopsProjectConfigE);
        ProjectConfigDTO configDTO = gson.fromJson(devopsProjectConfigDO.getConfig(),ProjectConfigDTO.class);
        devopsProjectConfigE.setConfig(configDTO);
        return devopsProjectConfigE;
    }

    @Override
    public DevopsProjectConfigDO entityToDo(DevopsProjectConfigE devopsProjectConfigE) {
        DevopsProjectConfigDO devopsProjectConfigDO = new DevopsProjectConfigDO();
        BeanUtils.copyProperties(devopsProjectConfigE, devopsProjectConfigDO);
        String configJson = gson.toJson(devopsProjectConfigE.getConfig());
        devopsProjectConfigDO.setConfig(configJson);
        return devopsProjectConfigDO;
    }


    @Override
    public DevopsProjectConfigE dtoToEntity(DevopsProjectConfigDTO devopsProjectConfigDTO) {
        DevopsProjectConfigE devopsProjectConfigE = new DevopsProjectConfigE();
        BeanUtils.copyProperties(devopsProjectConfigDTO, devopsProjectConfigE);
        return devopsProjectConfigE;
    }

    @Override
    public DevopsProjectConfigDTO entityToDto(DevopsProjectConfigE devopsProjectConfigE) {
        DevopsProjectConfigDTO devopsProjectConfigDTO = new DevopsProjectConfigDTO();
        BeanUtils.copyProperties(devopsProjectConfigE, devopsProjectConfigDTO);
        return devopsProjectConfigDTO;
    }
}
