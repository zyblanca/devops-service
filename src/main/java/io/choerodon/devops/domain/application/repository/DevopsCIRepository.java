package io.choerodon.devops.domain.application.repository;

import io.choerodon.devops.domain.application.entity.CIApplicationE;

public interface DevopsCIRepository {

    CIApplicationE getApplicationByGitAddress(String gitAddress);
}
