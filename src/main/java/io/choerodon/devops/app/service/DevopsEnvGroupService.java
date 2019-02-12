package io.choerodon.devops.app.service;

import java.util.List;

import io.choerodon.devops.api.dto.DevopsEnvGroupDTO;

public interface DevopsEnvGroupService {

    DevopsEnvGroupDTO create(String name, Long projectId);

    DevopsEnvGroupDTO update(DevopsEnvGroupDTO devopsEnvGroupDTO, Long projectId);

    List<DevopsEnvGroupDTO> listByProject(Long projectId);

    Boolean checkUniqueInProject(String name, Long projectId);

    void delete(Long id);
}
