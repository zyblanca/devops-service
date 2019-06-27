package io.choerodon.devops.infra.persistence.impl;

import io.choerodon.core.convertor.ConvertHelper;
import io.choerodon.devops.domain.application.entity.CIApplicationE;
import io.choerodon.devops.domain.application.repository.DevopsCIRepository;
import io.choerodon.devops.infra.dataobject.devopsCI.CIApplicationDO;
import io.choerodon.devops.infra.feign.DevOpsCIClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class DevopsCIRepositoryImpl implements DevopsCIRepository {

    public DevopsCIRepositoryImpl(DevOpsCIClient devOpsCIClient) {
        this.devOpsCIClient = devOpsCIClient;
    }

    DevOpsCIClient devOpsCIClient;

    @Override
    public CIApplicationE getApplicationByGitAddress(String gitAddress) {

        ResponseEntity<CIApplicationDO> result = devOpsCIClient.getApplicationByGitAddress(gitAddress);

        return ConvertHelper.convert(result.getBody(), CIApplicationE.class);
    }
}
