package io.choerodon.devops.domain.application.convertor;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import io.choerodon.core.convertor.ConvertorI;
import io.choerodon.devops.api.dto.SecretReqDTO;
import io.choerodon.devops.domain.application.entity.DevopsSecretE;
import io.choerodon.devops.infra.common.util.Base64Util;
import io.choerodon.devops.infra.dataobject.DevopsSecretDO;

/**
 * Created by n!Ck
 * Date: 18-12-4
 * Time: 上午10:16
 * Description:
 */
@Component
public class SecretConvertor implements ConvertorI<DevopsSecretE, DevopsSecretDO, SecretReqDTO> {

    private static final Gson gson = new Gson();

    @Override
    public DevopsSecretE dtoToEntity(SecretReqDTO secretReqDTO) {
        DevopsSecretE devopsSecretE = new DevopsSecretE();
        Map<String, String> encodedSecretMaps = new HashMap<>();
        BeanUtils.copyProperties(secretReqDTO, devopsSecretE);
        if (!secretReqDTO.getValue().isEmpty()) {
            for (Map.Entry<String, String> e : secretReqDTO.getValue().entrySet()) {
                encodedSecretMaps.put(e.getKey(), Base64Util.getBase64EncodedString(e.getValue()));
            }
            devopsSecretE.setValue(encodedSecretMaps);
        }
        return devopsSecretE;
    }

    @Override
    public DevopsSecretDO entityToDo(DevopsSecretE devopsSecretE) {
        DevopsSecretDO devopsSecretDO = new DevopsSecretDO();
        BeanUtils.copyProperties(devopsSecretE, devopsSecretDO);
        devopsSecretDO.setValue(gson.toJson(devopsSecretE.getValue()));
        return devopsSecretDO;
    }

    @Override
    public DevopsSecretE doToEntity(DevopsSecretDO devopsSecretDO) {
        DevopsSecretE devopsSecretE = new DevopsSecretE();
        BeanUtils.copyProperties(devopsSecretDO, devopsSecretE);
        Map<String, String> secretMaps = gson
                .fromJson(devopsSecretDO.getValue(), new TypeToken<Map<String, String>>() {
                }.getType());
        devopsSecretE.setValue(secretMaps);
        return devopsSecretE;
    }

    @Override
    public SecretReqDTO entityToDto(DevopsSecretE entity) {
        SecretReqDTO secretReqDTO = new SecretReqDTO();
        BeanUtils.copyProperties(entity, secretReqDTO);
        return secretReqDTO;
    }
}
