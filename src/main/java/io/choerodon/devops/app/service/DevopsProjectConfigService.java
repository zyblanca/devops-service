package io.choerodon.devops.app.service;

import io.choerodon.core.domain.Page;
import io.choerodon.devops.api.dto.DevopsProjectConfigDTO;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;

import java.util.List;

/**
 * @author zongw.lee@gmail.com
 * @since 2019/03/11
 */
public interface DevopsProjectConfigService {
    DevopsProjectConfigDTO create(Long projectId, DevopsProjectConfigDTO devopsProjectConfigDTO);

    DevopsProjectConfigDTO updateByPrimaryKeySelective(Long projectId, DevopsProjectConfigDTO devopsProjectConfigDTO);

    DevopsProjectConfigDTO queryByPrimaryKey(Long id);

    Page<DevopsProjectConfigDTO> listByOptions(Long projectId, PageRequest pageRequest,String params);

    void delete(Long id);

    List<DevopsProjectConfigDTO> queryByIdAndType(Long projectId, String type);

    /**
     * 创建配置校验名称是否存在
     *
     * @param projectId 项目id
     * @param name      配置name
     */
    void checkName(Long projectId, String name);

    Boolean checkIsUsed(Long configId);
}
