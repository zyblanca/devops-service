package io.choerodon.devops.api.dto;



/**
 * tankang
 */
public class GitlabGroupMemberDevKitDTO extends GitlabGroupMemberDTO {


    /**
     * GitlabGroupId
     */
    private Long devopsAppGroupId;

    public Long getDevopsAppGroupId() {
        return devopsAppGroupId;
    }

    public void setDevopsAppGroupId(Long devopsAppGroupId) {
        this.devopsAppGroupId = devopsAppGroupId;
    }
}
