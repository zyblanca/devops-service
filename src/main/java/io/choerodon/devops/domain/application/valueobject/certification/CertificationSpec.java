package io.choerodon.devops.domain.application.valueobject.certification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by n!Ck
 * Date: 2018/8/20
 * Time: 16:57
 * Description:
 */

public class CertificationSpec {
    private String commonName;
    private List<String> dnsNames;
    private CertificationAcme acme;
    private CertificationExistCert existCert;
    private Map<String, String> issuerRef;

    /**
     * empty construct
     */
    public CertificationSpec() {
    }

    /**
     * construct test param
     */
    public CertificationSpec(Boolean testCert) {
        if (testCert) {
            issuerRef = new HashMap<>();
            issuerRef.put("name", "localhost");
            issuerRef.put("kind", "ClusterIssuer");
        }
    }

    public Map<String, String> getIssuerRef() {
        return issuerRef;
    }

    public void setIssuerRef(Map<String, String> issuerRef) {
        this.issuerRef = issuerRef;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public List<String> getDnsNames() {
        return dnsNames;
    }

    public void setDnsNames(List<String> dnsNames) {
        this.dnsNames = dnsNames;
    }

    public CertificationAcme getAcme() {
        return acme;
    }

    public void setAcme(CertificationAcme acme) {
        this.acme = acme;
    }

    public CertificationExistCert getExistCert() {
        return existCert;
    }

    public void setExistCert(CertificationExistCert existCert) {
        this.existCert = existCert;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CertificationSpec)) {
            return false;
        }
        CertificationSpec that = (CertificationSpec) o;
        return Objects.equals(getCommonName(), that.getCommonName())
                && Objects.equals(getDnsNames(), that.getDnsNames())
                && Objects.equals(getAcme(), that.getAcme())
                && Objects.equals(getExistCert(), that.getExistCert())
                && Objects.equals(getIssuerRef(), that.getIssuerRef());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCommonName(), getDnsNames(), getAcme(), getExistCert(), getIssuerRef());
    }
}
