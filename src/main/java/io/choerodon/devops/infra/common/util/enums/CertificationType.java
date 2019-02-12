package io.choerodon.devops.infra.common.util.enums;

/**
 * Created by n!Ck
 * Date: 2018/8/20
 * Time: 17:29
 * Description:
 */
public enum CertificationType {
    REQUEST("request"),
    UPLOAD("upload");

    private String type;

    CertificationType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
