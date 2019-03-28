package io.choerodon.devops.api.dto;

/**
 * 对接DevKit
 */
public class GitlabUserDTODevKit extends GitlabUserDTO {

    private Boolean ldap;

    public Boolean getLdap() {
        return ldap;
    }

    public void setLdap(Boolean ldap) {
        this.ldap = ldap;
    }
}
