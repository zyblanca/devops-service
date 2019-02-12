package io.choerodon.devops.domain.application.convertor;


import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import io.choerodon.core.convertor.ConvertorI;
import io.choerodon.devops.api.dto.ApplicationRepDTO;
import io.choerodon.devops.domain.application.entity.ApplicationE;

@Component
public class ApplicationRepConvertor implements ConvertorI<ApplicationE, Object, ApplicationRepDTO> {

    @Override
    public ApplicationRepDTO entityToDto(ApplicationE applicationE) {
        ApplicationRepDTO applicationRepDTO = new ApplicationRepDTO();
        BeanUtils.copyProperties(applicationE, applicationRepDTO);
        if (applicationE.getApplicationTemplateE() != null) {
            applicationRepDTO.setApplicationTemplateId(applicationE.getApplicationTemplateE().getId());
        }
        if (applicationE.getIsSkipCheckPermission()) {
            applicationRepDTO.setPermission(true);
        } else {
            applicationRepDTO.setPermission(false);
        }
        applicationRepDTO.setRepoUrl(applicationE.getGitlabProjectE().getRepoURL());
        applicationRepDTO.setFail(applicationE.getFailed());
        applicationRepDTO.setProjectId(applicationE.getProjectE().getId());
        return applicationRepDTO;
    }
}