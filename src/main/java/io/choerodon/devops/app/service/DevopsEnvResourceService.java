package io.choerodon.devops.app.service;

import java.util.List;

import io.choerodon.devops.api.dto.DevopsEnvResourceDTO;
import io.choerodon.devops.api.dto.InstanceEventDTO;

/**
 * Created by younger on 2018/4/25.
 */
public interface DevopsEnvResourceService {
    DevopsEnvResourceDTO listResources(Long instanceId);

    /**
     * 相较于{@link DevopsEnvResourceService#listResources(Long)} 方法只展示该应用的chart包中定义的资源，
     * 不包含前端页面之后创建的资源
     *
     * @param instanceId 实例id
     * @return 返回实例的资源
     */
    DevopsEnvResourceDTO listResourcesInHelmRelease(Long instanceId);

    List<InstanceEventDTO> listInstancePodEvent(Long instanceId);
}
