package io.choerodon.devops.domain.application.convertor;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import io.choerodon.core.convertor.ConvertorI;
import io.choerodon.devops.api.dto.ProjectPipelineResultTotalDTO;
import io.choerodon.devops.domain.application.valueobject.ProjectPipelineResultTotalV;

/**
 * Created by Zenger on 2018/4/4.
 */
@Component
public class ProjectPipelineResultConvertor implements ConvertorI<ProjectPipelineResultTotalV, Object, ProjectPipelineResultTotalDTO> {

    @Override
    public ProjectPipelineResultTotalDTO entityToDto(ProjectPipelineResultTotalV projectPipelineResultTotalV) {
        ProjectPipelineResultTotalDTO projectPipelineResultTotalDTO = new ProjectPipelineResultTotalDTO();
        BeanUtils.copyProperties(projectPipelineResultTotalV, projectPipelineResultTotalDTO);
        return projectPipelineResultTotalDTO;
    }
}
