package io.choerodon.devops.infra.persistence.impl;

import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import org.springframework.stereotype.Component;

import io.choerodon.core.convertor.ConvertHelper;
import io.choerodon.core.convertor.ConvertPageHelper;
import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.domain.application.entity.DevopsSecretE;
import io.choerodon.devops.domain.application.repository.DevopsSecretRepository;
import io.choerodon.devops.infra.common.util.TypeUtil;
import io.choerodon.devops.infra.dataobject.DevopsSecretDO;
import io.choerodon.devops.infra.mapper.DevopsSecretMapper;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;

/**
 * Created by n!Ck
 * Date: 18-12-4
 * Time: 上午10:06
 * Description:
 */

@Component
public class DevopsSecretRepositoryImpl implements DevopsSecretRepository {

    private static final Gson gson = new Gson();
    private DevopsSecretMapper devopsSecretMapper;

    public DevopsSecretRepositoryImpl(DevopsSecretMapper devopsSecretMapper) {
        this.devopsSecretMapper = devopsSecretMapper;
    }

    @Override
    public DevopsSecretE create(DevopsSecretE devopsSecretE) {
        DevopsSecretDO devopsSecretDO = ConvertHelper.convert(devopsSecretE, DevopsSecretDO.class);
        if (devopsSecretMapper.insert(devopsSecretDO) != 1) {
            throw new CommonException("error.secret.insert");
        }
        return ConvertHelper.convert(devopsSecretDO, DevopsSecretE.class);
    }

    @Override
    public void update(DevopsSecretE devopsSecretE) {
        DevopsSecretDO devopsSecretDO = ConvertHelper.convert(devopsSecretE, DevopsSecretDO.class);
        DevopsSecretDO oldSecretDO = devopsSecretMapper.selectByPrimaryKey(devopsSecretE.getId());
        if (oldSecretDO == null) {
            throw new CommonException("secret.not.exists");
        }
        devopsSecretDO.setObjectVersionNumber(oldSecretDO.getObjectVersionNumber());
        if (devopsSecretMapper.updateByPrimaryKey(devopsSecretDO) != 1) {
            throw new CommonException("secret.update.error");
        }
    }

    @Override
    public void deleteSecret(Long secretId) {
        devopsSecretMapper.deleteByPrimaryKey(secretId);
        devopsSecretMapper.delete(new DevopsSecretDO(secretId));
    }

    @Override
    public void checkName(String name, Long envId) {
        DevopsSecretDO devopsSecretDO = new DevopsSecretDO();
        devopsSecretDO.setName(name);
        devopsSecretDO.setEnvId(envId);
        if (devopsSecretMapper.selectOne(devopsSecretDO) != null) {
            throw new CommonException("error.secret.name.already.exists");
        }
    }

    @Override
    public DevopsSecretE queryBySecretId(Long secretId) {
        return ConvertHelper.convert(devopsSecretMapper.selectById(secretId), DevopsSecretE.class);
    }

    @Override
    public DevopsSecretE selectByEnvIdAndName(Long envId, String name) {
        DevopsSecretDO devopsSecretDO = new DevopsSecretDO();
        devopsSecretDO.setEnvId(envId);
        devopsSecretDO.setName(name);
        return ConvertHelper.convert(devopsSecretMapper.selectOne(devopsSecretDO), DevopsSecretE.class);
    }

    @Override
    public Page<DevopsSecretE> listByOption(Long envId, PageRequest pageRequest, String params) {
        Map maps = gson.fromJson(params, Map.class);
        Map<String, Object> searchParamMap = TypeUtil.cast(maps.get(TypeUtil.SEARCH_PARAM));
        String paramMap = TypeUtil.cast(maps.get(TypeUtil.PARAM));
        Page<DevopsSecretDO> devopsSecretDOPage = PageHelper
                .doPageAndSort(pageRequest, () -> devopsSecretMapper.listByOption(envId, searchParamMap, paramMap));
        return ConvertPageHelper.convertPage(devopsSecretDOPage, DevopsSecretE.class);
    }

    @Override
    public List<DevopsSecretE> listByEnv(Long envId) {
        DevopsSecretDO devopsSecretDO = new DevopsSecretDO();
        devopsSecretDO.setEnvId(envId);
        return ConvertHelper.convertList(devopsSecretMapper.select(devopsSecretDO), DevopsSecretE.class);
    }
}
