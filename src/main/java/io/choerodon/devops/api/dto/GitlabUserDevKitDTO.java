package io.choerodon.devops.api.dto;

/**
 * Created by zzy on 2018/3/23.
 */
public class GitlabUserDevKitDTO extends GitlabUserDTO{

    private Boolean ladp;

    public Boolean getLadp() {
        return ladp;
    }

    public void setLadp(Boolean ladp) {
        this.ladp = ladp;
    }
}
