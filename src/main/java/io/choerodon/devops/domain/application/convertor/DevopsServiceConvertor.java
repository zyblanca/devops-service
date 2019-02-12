package io.choerodon.devops.domain.application.convertor;

import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import io.choerodon.core.convertor.ConvertorI;
import io.choerodon.devops.domain.application.entity.DevopsServiceE;
import io.choerodon.devops.domain.application.entity.PortMapE;
import io.choerodon.devops.infra.dataobject.DevopsServiceDO;

/**
 * Created by Zenger on 2018/4/18.
 */
@Component
public class DevopsServiceConvertor implements ConvertorI<DevopsServiceE, DevopsServiceDO, Object> {
    private Gson gson = new Gson();

    @Override
    public DevopsServiceDO entityToDo(DevopsServiceE entity) {
        DevopsServiceDO devopsServiceDO = new DevopsServiceDO();
        BeanUtils.copyProperties(entity, devopsServiceDO);
        devopsServiceDO.setPorts(gson.toJson(entity.getPorts()));
        return devopsServiceDO;
    }

    @Override
    public DevopsServiceE doToEntity(DevopsServiceDO dataObject) {
        DevopsServiceE devopsServiceE = new DevopsServiceE();
        BeanUtils.copyProperties(dataObject, devopsServiceE);
        devopsServiceE.setPorts(gson.fromJson(dataObject.getPorts(), new TypeToken<ArrayList<PortMapE>>() {
        }.getType()));
        return devopsServiceE;
    }
}
