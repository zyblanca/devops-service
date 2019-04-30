package io.choerodon.devops.api.dto;

/**
 * Created by zzy on 2018/3/23.
 */
public class GitlabUserDevKitDTO extends GitlabUserDTO{

    private Boolean ldap;

    public Boolean getLdap() {
        return ldap;
    }

    public void setLdap(Boolean ldap) {
        this.ldap = ldap;
    }
}
