package io.choerodon.devops.api.validator;

import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.api.dto.DevopsProjectConfigDTO;
import io.choerodon.devops.api.dto.ProjectConfigDTO;
import io.choerodon.devops.app.service.ApplicationService;
import io.choerodon.devops.infra.common.util.enums.ProjectConfigType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author zongw.lee@gmail.com
 * @since 2019/03/12
 */
@Component
@Scope("singleton")
public class DevopsProjectConfigValidator {

    @Autowired
    ApplicationService applicationService;

    public void checkConfigType(DevopsProjectConfigDTO devopsProjectConfigDTO) {
        ProjectConfigType type = ProjectConfigType.valueOf(devopsProjectConfigDTO.getType().toUpperCase());
        ProjectConfigDTO configDTO = devopsProjectConfigDTO.getConfig();
        switch (type) {
            case HARBOR:
                applicationService.checkHarborIsUsable(configDTO.getUrl(),configDTO.getUserName(),configDTO.getPassword(),configDTO.getProject(),configDTO.getEmail());
                break;
            case CHART:
                applicationService.checkChartIsUsable(configDTO.getUrl());
                break;
            default:
                throw new CommonException("error.project.config.type");
        }
    }
}
