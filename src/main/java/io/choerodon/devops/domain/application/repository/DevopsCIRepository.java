package io.choerodon.devops.domain.application.repository;

import io.choerodon.devops.infra.dataobject.devopsCI.CIApplicationDO;

public interface DevopsCIRepository {

    CIApplicationDO getApplicationByGitAddress(String gitAddress);
}
