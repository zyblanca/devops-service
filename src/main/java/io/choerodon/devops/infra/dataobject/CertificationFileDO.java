package io.choerodon.devops.infra.dataobject;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import io.choerodon.mybatis.annotation.ModifyAudit;
import io.choerodon.mybatis.annotation.VersionAudit;
import io.choerodon.mybatis.domain.AuditDomain;

/**
 * Created by n!Ck
 * Date: 2018/8/20
 * Time: 19:51
 * Description:
 */
@ModifyAudit
@VersionAudit
@Table(name = "devops_certification_file")
public class CertificationFileDO extends AuditDomain {
    @Id
    @GeneratedValue
    private Long id;

    private String certFile;
    private String keyFile;

    /**
     * construct cert file
     *
     * @param certFile cert file
     * @param keyFile  private key file
     */
    public CertificationFileDO(String certFile, String keyFile) {
        this.certFile = certFile;
        this.keyFile = keyFile;
    }

    public CertificationFileDO() {

    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getCertFile() {
        return certFile;
    }

    public void setCertFile(String certFile) {
        this.certFile = certFile;
    }

    public String getKeyFile() {
        return keyFile;
    }

    public void setKeyFile(String keyFile) {
        this.keyFile = keyFile;
    }
}
