package io.choerodon.devops.infra.persistence.impl;

import io.choerodon.core.convertor.ConvertHelper;
import io.choerodon.devops.domain.application.entity.CIApplicationE;
import io.choerodon.devops.domain.application.repository.DevopsCIRepository;
import io.choerodon.devops.infra.dataobject.devopsCI.CIApplicationDO;
import io.choerodon.devops.infra.feign.DevOpsCIClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class DevopsCIRepositoryImpl implements DevopsCIRepository {

    private final static Logger logger = LoggerFactory.getLogger(DevopsCIRepositoryImpl.class);

    public DevopsCIRepositoryImpl(DevOpsCIClient devOpsCIClient) {
        this.devOpsCIClient = devOpsCIClient;
    }

    DevOpsCIClient devOpsCIClient;

    @Override
    public CIApplicationE getApplicationByGitAddress(String gitAddress) {

        ResponseEntity<CIApplicationDO> result = devOpsCIClient.getApplicationByGitAddress(gitAddress);

        logger.info("ci return result: {}", result);

        return ConvertHelper.convert(result.getBody(), CIApplicationE.class);
    }
}
