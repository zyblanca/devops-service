package io.choerodon.devops.api.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.domain.application.repository.ApplicationInstanceRepository;
import io.choerodon.devops.domain.application.repository.DevopsIngressRepository;
import io.choerodon.devops.domain.application.repository.DevopsServiceRepository;
import io.choerodon.devops.infra.common.util.enums.InstanceStatus;

/**
 * Creator: Runge
 * Date: 2018/6/13
 * Time: 11:25
 * Description:
 */
@Component
public class DevopsEnvironmentValidator {
    @Autowired
    private ApplicationInstanceRepository applicationInstanceRepository;
    @Autowired
    private DevopsIngressRepository devopsIngressRepository;
    @Autowired
    private DevopsServiceRepository devopsServiceRepository;

    /**
     * 验证环境是否可以禁用
     *
     * @param envId 环境ID
     */
    public void checkEnvCanDisabled(Long envId) {
        if (applicationInstanceRepository.selectByEnvId(envId).stream()
                .anyMatch(applicationInstanceE ->
                        InstanceStatus.RUNNING.getStatus().equals(applicationInstanceE.getStatus()))) {
            throw new CommonException("error.env.stop.instanceExist");
        }
        if (devopsServiceRepository.checkEnvHasService(envId)) {
            throw new CommonException("error.env.stop.serviceExist");
        }
        if (devopsIngressRepository.checkEnvHasIngress(envId)) {
            throw new CommonException("error.env.stop.IngressExist");
        }
    }
}
