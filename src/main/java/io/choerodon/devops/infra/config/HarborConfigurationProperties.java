package io.choerodon.devops.infra.config;

import java.util.*;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services.harbor")
public class HarborConfigurationProperties {

    private Boolean enabled;

    private String baseUrl;

    private String username;

    private String password;

    private String params;

    private Boolean insecureSkipTlsVerify;

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public Boolean getInsecureSkipTlsVerify() {
        return insecureSkipTlsVerify;
    }

    public void setInsecureSkipTlsVerify(Boolean insecureSkipTlsVerify) {
        this.insecureSkipTlsVerify = insecureSkipTlsVerify;
    }
}
