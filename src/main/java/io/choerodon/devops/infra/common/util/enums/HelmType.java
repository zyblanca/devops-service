package io.choerodon.devops.infra.common.util.enums;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Created by Zenger on 2017/11/14.
 */
public enum HelmType {
    HELM_ERROR("helm_release_error"),
    HELM_RELEASE_PRE_INSTALL("helm_release_pre_install"),
    HELM_INSTALL_RELEASE("helm_install_release"),
    HELM_RELEASE_HOOK_LOGS("helm_release_hook_get_logs"),
    HELM_RELEASE_UPGRADE("helm_release_upgrade"),
    HELM_RELEASE_ROLLBACK("helm_release_rollback"),
    HELM_RELEASE_START("helm_release_start"),
    HELM_RELEASE_STOP("helm_release_stop"),
    HELM_RELEASE_DELETE("helm_release_delete"),
    HELM_RELEASE_PRE_UPGRADE("helm_release_pre_upgrade"),
    NETWORK_SERVICE("network_service"),
    NETWORK_INGRESS("network_ingress"),
    NETWORK_SERVICE_DELETE("network_service_delete"),
    NETWORK_INGRESS_DELETE("network_ingress_delete"),
    RESOURCE_UPDATE("resource_update"),
    HELM_RELEASES("helm_releases"),
    RESOURCE_DELETE("resource_delete"),
    HELM_RELEASE_INSTALL_FAILED("helm_release_install_failed"),
    HELM_RELEASE_UPGRADE_FAILED("helm_release_upgrade_failed"),
    HELM_RELEASE_ROLLBACK_FAILED("helm_release_rollback_failed"),
    HELM_RELEASE_START_FAILED("helm_release_start_failed"),
    HELM_RELEASE_STOP_FAILED("helm_release_stop_failed"),
    HELM_RELEASE_DELETE_FAILED("helm_release_delete_failed"),
    KUBERNETES_GET_LOGS("kubernetes_get_logs"),
    HELM_RELEASE_GET_CONTENT("helm_release_get_content"),
    HELM_RELEASE_GET_CONTENT_FAILED("helm_release_get_content_failed"),
    COMMAND_NOT_SEND("command_not_send"),
    NETWORK_SERVICE_UPDATE("network_service_update"),
    NETWORK_SERVICE_FAILED("network_service_failed"),
    NETWORK_SERVICE_DELETE_FAILED("network_service_delete_failed"),
    NETWORK_INGRESS_FAILED("network_ingress_failed"),
    NETWORK_INGRESS_DELETE_FAILED("network_ingress_delete_failed"),
    RESOURCE_SYNC("resource_sync"),
    JOB_EVENT("job_event"),
    RELEASE_POD_EVENT("release_pod_event"),
    GIT_OPS_SYNC_EVENT("git_ops_sync_event"),
    STATUS_SYNC_EVENT("status_sync_event"),
    STATUS_SYNC("status_sync"),
    CERT_ISSUED("cert_issued"),
    NAMESPACE_UPDATE("namespace_update"),
    UPGRADE_CLUSTER("upgrade"),
    CERT_FAILED("cert_failed"),
    EXECUTE_TEST_SUCCEED("execute_test_succeed"),
    EXECUTE_TEST_FAILED("execute_test_failed"),
    TEST_POD_EVENT("test_pod_event"),
    TEST_POD_UPDATE("test_pod_update"),
    TEST_JOB_LOG("test_job_log"),
    GET_TEST_APP_STATUS("get_test_app_status"),
    TEST_STATUS_RESPONSE("test_status_response"),
    TEST_STATUS("test_status"),
    CERT_MANAGER_INFO("cert_manager_info"),
    EXECUTE_TEST("execute_test");

    private static HashMap<String, HelmType> valuesMap = new HashMap<>(6);

    static {
        HelmType[] var0 = values();

        for (HelmType accessLevel : var0) {
            valuesMap.put(accessLevel.value, accessLevel);
        }

    }

    public final String value;

    HelmType(String value) {
        this.value = value;
    }

    @JsonCreator
    public static HelmType forValue(String value) {
        return valuesMap.get(value);
    }

    /**
     * 根据string类型返回枚举类型
     *
     * @param value String
     */
    public static HelmType forString(String value) {
        switch (value) {
            case "helm_release_error":
                return HelmType.HELM_ERROR;
            case "helm_release_pre_install":
                return HelmType.HELM_RELEASE_PRE_INSTALL;
            case "helm_install_release":
                return HelmType.HELM_INSTALL_RELEASE;
            case "helm_release_hook_get_logs":
                return HelmType.HELM_RELEASE_HOOK_LOGS;
            case "helm_release_upgrade":
                return HelmType.HELM_RELEASE_UPGRADE;
            case "helm_release_rollback":
                return HelmType.HELM_RELEASE_ROLLBACK;
            case "helm_release_start":
                return HelmType.HELM_RELEASE_START;
            case "helm_release_stop":
                return HelmType.HELM_RELEASE_STOP;
            case "helm_release_delete":
                return HelmType.HELM_RELEASE_DELETE;
            case "helm_release_pre_upgrade":
                return HelmType.HELM_RELEASE_PRE_UPGRADE;
            case "network_service":
                return HelmType.NETWORK_SERVICE;
            case "network_ingress":
                return HelmType.NETWORK_INGRESS;
            case "network_service_delete":
                return HelmType.NETWORK_SERVICE_DELETE;
            case "network_ingress_delete":
                return HelmType.NETWORK_INGRESS_DELETE;
            case "resource_update":
                return HelmType.RESOURCE_UPDATE;
            case "resource_delete":
                return HelmType.RESOURCE_DELETE;
            case "kubernetes_get_logs":
                return HelmType.KUBERNETES_GET_LOGS;
            case "helm_releases":
                return HelmType.HELM_RELEASES;
            case "helm_release_install_failed":
                return HelmType.HELM_RELEASE_INSTALL_FAILED;
            case "helm_release_upgrade_failed":
                return HelmType.HELM_RELEASE_UPGRADE_FAILED;
            case "helm_release_rollback_failed":
                return HelmType.HELM_RELEASE_ROLLBACK_FAILED;
            case "helm_release_start_failed":
                return HelmType.HELM_RELEASE_START_FAILED;
            case "helm_release_stop_failed":
                return HelmType.HELM_RELEASE_STOP_FAILED;
            case "helm_release_delete_failed":
                return HelmType.HELM_RELEASE_DELETE_FAILED;
            case "network_service_update":
                return HelmType.NETWORK_SERVICE_UPDATE;
            case "helm_release_get_content":
                return HelmType.HELM_RELEASE_GET_CONTENT;
            case "helm_release_get_content_failed":
                return HelmType.HELM_RELEASE_GET_CONTENT_FAILED;
            case "command_not_send":
                return HelmType.COMMAND_NOT_SEND;
            case "network_service_failed":
                return HelmType.NETWORK_SERVICE_FAILED;
            case "network_service_delete_failed":
                return HelmType.NETWORK_SERVICE_DELETE_FAILED;
            case "network_ingress_failed":
                return HelmType.NETWORK_INGRESS_FAILED;
            case "network_ingress_delete_failed":
                return HelmType.NETWORK_INGRESS_DELETE_FAILED;
            case "resource_sync":
                return HelmType.RESOURCE_SYNC;
            case "job_event":
                return HelmType.JOB_EVENT;
            case "release_pod_event":
                return HelmType.RELEASE_POD_EVENT;
            case "git_ops_sync_event":
                return HelmType.GIT_OPS_SYNC_EVENT;
            case "cert_issued":
                return CERT_ISSUED;
            case "cert_failed":
                return CERT_FAILED;
            case "status_sync":
                return HelmType.STATUS_SYNC;
            case "status_sync_event":
                return HelmType.STATUS_SYNC_EVENT;
            case "upgrade":
                return HelmType.UPGRADE_CLUSTER;
            case "namespace_update":
                return HelmType.NAMESPACE_UPDATE;
            case "execute_test":
                return HelmType.EXECUTE_TEST;
            case "execute_test_succeed":
                return HelmType.EXECUTE_TEST_SUCCEED;
            case "execute_test_failed":
                return HelmType.EXECUTE_TEST_FAILED;
            case "test_pod_event":
                return HelmType.TEST_POD_EVENT;
            case "test_pod_update":
                return HelmType.TEST_POD_UPDATE;
            case "test_job_log":
                return HelmType.TEST_JOB_LOG;
            case "test_status":
                return HelmType.TEST_STATUS;
            case "test_status_response":
                return HelmType.TEST_STATUS_RESPONSE;
            case "cert_manager_info":
                return HelmType.CERT_MANAGER_INFO;
            default:
                break;
        }
        return null;
    }

    @JsonValue
    public String toValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
