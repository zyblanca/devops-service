package io.choerodon.devops.domain.application.entity;

public class DevopsClusterE {

    private Long id;
    private Long organizationId;
    private String name;
    private String token;
    private String code;
    private Boolean connect;
    private Boolean upgrade;
    private String upgradeMessage;
    private String description;
    private Boolean skipCheckProjectPermission;
    private String choerodonId;
    private String namespaces;
    private Boolean  isInit;

    public DevopsClusterE() {
    }

    public DevopsClusterE(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Boolean getSkipCheckProjectPermission() {
        return skipCheckProjectPermission;
    }

    public void setSkipCheckProjectPermission(Boolean skipCheckProjectPermission) {
        this.skipCheckProjectPermission = skipCheckProjectPermission;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getConnect() {
        return connect;
    }

    public void initConnect(Boolean connect) {
        this.connect = connect;
    }

    public Boolean getUpgrade() {
        return upgrade;
    }

    public void initUpgrade(Boolean upgrade) {
        this.upgrade = upgrade;
    }

    public String getUpgradeMessage() {
        return upgradeMessage;
    }

    public void setUpgradeMessage(String upgradeMessage) {
        this.upgradeMessage = upgradeMessage;
    }


    public String getChoerodonId() {
        return choerodonId;
    }

    public void setChoerodonId(String choerodonId) {
        this.choerodonId = choerodonId;
    }

    public String getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(String namespaces) {
        this.namespaces = namespaces;
    }

    public Boolean getInit() {
        return isInit;
    }

    public void setInit(Boolean init) {
        isInit = init;
    }
}
