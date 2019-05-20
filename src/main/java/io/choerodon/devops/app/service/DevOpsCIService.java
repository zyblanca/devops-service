package io.choerodon.devops.app.service;

import io.choerodon.devops.api.dto.ProjectWebHookDto;

public interface DevOpsCIService {

    void getRepositorySize(ProjectWebHookDto project);
}
