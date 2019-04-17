package io.choerodon.devops.domain.application.event;


/**
 * project　event
 *
 * @author tankang
 */
public class ProjectEventDevKit extends ProjectEvent {

    /**
     * 对接DevKit,GitlabGroupId
     */
    private Long devopsAppGroupId;

    public Long getDevopsAppGroupId() {
        return devopsAppGroupId;
    }

    public void setDevopsAppGroupId(Long devopsAppGroupId) {
        this.devopsAppGroupId = devopsAppGroupId;
    }


}
