package io.choerodon.devops.domain.application.convertor;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import io.choerodon.core.convertor.ConvertorI;
import io.choerodon.devops.domain.application.entity.gitlab.GitlabPipelineE;
import io.choerodon.devops.infra.dataobject.gitlab.PipelineDO;

/**
 * Created by Zenger on 2018/4/3.
 */
@Component
public class GitlabPipelineConvertor implements ConvertorI<GitlabPipelineE, PipelineDO, Object> {

    @Override
    public GitlabPipelineE doToEntity(PipelineDO pipelineDO) {
        GitlabPipelineE gitlabPipelineE = new GitlabPipelineE();
        BeanUtils.copyProperties(pipelineDO, gitlabPipelineE);
        if (pipelineDO.getUser() != null) {
            gitlabPipelineE.initUser(pipelineDO.getUser().getId(), pipelineDO.getUser().getUsername());
        }
        return gitlabPipelineE;
    }
}
