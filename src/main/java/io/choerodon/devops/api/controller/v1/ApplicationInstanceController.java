package io.choerodon.devops.api.controller.v1;

import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.iam.InitRoleCode;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.devops.api.dto.AppInstanceCodeDTO;
import io.choerodon.devops.api.dto.AppInstanceCommandLogDTO;
import io.choerodon.devops.api.dto.ApplicationDeployDTO;
import io.choerodon.devops.api.dto.ApplicationInstanceDTO;
import io.choerodon.devops.api.dto.ApplicationInstancesDTO;
import io.choerodon.devops.api.dto.DeployDetailDTO;
import io.choerodon.devops.api.dto.DeployFrequencyDTO;
import io.choerodon.devops.api.dto.DeployTimeDTO;
import io.choerodon.devops.api.dto.DevopsEnvPreviewDTO;
import io.choerodon.devops.api.dto.DevopsEnvPreviewInstanceDTO;
import io.choerodon.devops.api.dto.DevopsEnvResourceDTO;
import io.choerodon.devops.api.dto.ErrorLineDTO;
import io.choerodon.devops.api.dto.InstanceControllerDetailDTO;
import io.choerodon.devops.api.dto.InstanceEventDTO;
import io.choerodon.devops.app.service.ApplicationInstanceService;
import io.choerodon.devops.app.service.DevopsEnvResourceService;
import io.choerodon.devops.domain.application.valueobject.ReplaceResult;
import io.choerodon.devops.infra.common.util.enums.ResourceType;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.swagger.annotation.CustomPageRequest;
import io.choerodon.swagger.annotation.Permission;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Created by Zenger on 2018/4/3.
 */
@RestController
@RequestMapping(value = "/v1/projects/{project_id}/app_instances")
public class ApplicationInstanceController {
    private static final String ERROR_APPINSTANCE_QUERY = "error.appInstance.query";

    @Autowired
    private ApplicationInstanceService applicationInstanceService;
    @Autowired
    private DevopsEnvResourceService devopsEnvResourceService;

    /**
     * ????????????????????????
     *
     * @param projectId   ??????id
     * @param pageRequest ????????????
     * @param envId       ??????id
     * @param versionId   ??????id
     * @param appId       ??????id
     * @param params      ????????????
     * @return page of applicationInstanceDTO
     */
    @Permission(level = ResourceLevel.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER,
                    InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "????????????????????????")
    @CustomPageRequest
    @PostMapping(value = "/list_by_options")
    public ResponseEntity<Page<DevopsEnvPreviewInstanceDTO>> pageByOptions(
            @ApiParam(value = "??????ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiIgnore
            @ApiParam(value = "????????????") PageRequest pageRequest,
            @ApiParam(value = "??????ID")
            @RequestParam(required = false) Long envId,
            @ApiParam(value = "??????ID")
            @RequestParam(required = false) Long versionId,
            @ApiParam(value = "??????ID")
            @RequestParam(required = false) Long appId,
            @ApiParam(value = "????????????")
            @RequestBody(required = false) String params) {
        return Optional.ofNullable(applicationInstanceService.listApplicationInstance(
                projectId, pageRequest, envId, versionId, appId, params))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.application.version.query"));
    }

    /**
     * ??????????????????
     *
     * @param projectId ??????id
     * @param appId     ??????id
     * @return page of ApplicationInstancesDTO
     */
    @Permission(level = ResourceLevel.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER,
                    InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "?????????????????????")
    @GetMapping(value = "/all")
    public ResponseEntity<List<ApplicationInstancesDTO>> listByAppId(
            @ApiParam(value = "??????ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "??????ID")
            @RequestParam(required = false) Long appId) {
        return Optional.ofNullable(applicationInstanceService.listApplicationInstances(projectId, appId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.application.version.query"));
    }

    /**
     * ???????????? Value
     *
     * @param projectId     ??????id
     * @param appInstanceId ??????id
     * @return string
     */
    @Permission(level = ResourceLevel.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "???????????? Value")
    @GetMapping(value = "/{appInstanceId}/value")
    public ResponseEntity<ReplaceResult> queryValue(
            @ApiParam(value = "??????ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "??????ID", required = true)
            @PathVariable Long appInstanceId) {
        return Optional.ofNullable(applicationInstanceService.queryValue(appInstanceId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.instance.value.get"));
    }

    /**
     * ????????????id???deployment name????????????????????????(Json??????)
     *
     * @param projectId      ??????id
     * @param appInstanceId  ??????id
     * @param deploymentName deployment name
     * @return ????????????
     */
    @Permission(level = ResourceLevel.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "????????????id????????????????????????(Json??????)")
    @GetMapping(value = "/{appInstanceId}/deployment_detail_json")
    public ResponseEntity<InstanceControllerDetailDTO> getDeploymentDetailsJsonByInstanceId(
            @ApiParam(value = "??????ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "????????????", required = true)
            @RequestParam(value = "deployment_name") String deploymentName,
            @ApiParam(value = "??????ID", required = true)
            @PathVariable Long appInstanceId) {
        return new ResponseEntity<>(applicationInstanceService.getInstanceResourceDetailJson(appInstanceId, deploymentName, ResourceType.DEPLOYMENT), HttpStatus.OK);
    }

    /**
     * ????????????id????????????daemonSet??????(Json??????)
     *
     * @param projectId     ??????id
     * @param appInstanceId ??????id
     * @param daemonSetName daemonSet name
     * @return daemonSet??????
     */
    @Permission(level = ResourceLevel.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "????????????id????????????daemonSet??????(Json??????)")
    @GetMapping(value = "/{appInstanceId}/daemon_set_detail_json")
    public ResponseEntity<InstanceControllerDetailDTO> getDaemonSetDetailsJsonByInstanceId(
            @ApiParam(value = "??????ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "????????????", required = true)
            @RequestParam(value = "daemon_set_name") String daemonSetName,
            @ApiParam(value = "??????ID", required = true)
            @PathVariable Long appInstanceId) {
        return new ResponseEntity<>(applicationInstanceService.getInstanceResourceDetailJson(appInstanceId, daemonSetName, ResourceType.DAEMONSET), HttpStatus.OK);
    }

    /**
     * ????????????id????????????statefulSet??????(Json??????)
     *
     * @param projectId       ??????id
     * @param appInstanceId   ??????id
     * @param statefulSetName statefulSet name
     * @return statefulSet??????
     */
    @Permission(level = ResourceLevel.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "????????????id????????????statefulSet??????(Json??????)")
    @GetMapping(value = "/{appInstanceId}/stateful_set_detail_json")
    public ResponseEntity<InstanceControllerDetailDTO> getStatefulSetDetailsJsonByInstanceId(
            @ApiParam(value = "??????ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "????????????", required = true)
            @RequestParam(value = "stateful_set_name") String statefulSetName,
            @ApiParam(value = "??????ID", required = true)
            @PathVariable Long appInstanceId) {
        return new ResponseEntity<>(applicationInstanceService.getInstanceResourceDetailJson(appInstanceId, statefulSetName, ResourceType.STATEFULSET), HttpStatus.OK);
    }

    /**
     * ????????????id???deployment name????????????????????????(Yaml??????)
     *
     * @param projectId      ??????id
     * @param appInstanceId  ??????id
     * @param deploymentName deployment name
     * @return ????????????
     */
    @Permission(level = ResourceLevel.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "????????????id????????????????????????(Yaml??????)")
    @GetMapping(value = "/{appInstanceId}/deployment_detail_yaml")
    public ResponseEntity<InstanceControllerDetailDTO> getDeploymentDetailsYamlByInstanceId(
            @ApiParam(value = "??????ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "????????????", required = true)
            @RequestParam(value = "deployment_name") String deploymentName,
            @ApiParam(value = "??????ID", required = true)
            @PathVariable Long appInstanceId) {
        return new ResponseEntity<>(applicationInstanceService.getInstanceResourceDetailYaml(appInstanceId, deploymentName, ResourceType.DEPLOYMENT), HttpStatus.OK);
    }

    /**
     * ????????????id????????????daemonSet??????(Yaml??????)
     *
     * @param projectId     ??????id
     * @param appInstanceId ??????id
     * @param daemonSetName daemonSet name
     * @return daemonSet??????
     */
    @Permission(level = ResourceLevel.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "????????????id????????????daemonSet??????(Yaml??????)")
    @GetMapping(value = "/{appInstanceId}/daemon_set_detail_yaml")
    public ResponseEntity<InstanceControllerDetailDTO> getDaemonSetDetailsYamlByInstanceId(
            @ApiParam(value = "??????ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "????????????", required = true)
            @RequestParam(value = "daemon_set_name") String daemonSetName,
            @ApiParam(value = "??????ID", required = true)
            @PathVariable Long appInstanceId) {
        return new ResponseEntity<>(applicationInstanceService.getInstanceResourceDetailYaml(appInstanceId, daemonSetName, ResourceType.DAEMONSET), HttpStatus.OK);
    }

    /**
     * ????????????id????????????statefulSet??????(Yaml??????)
     *
     * @param projectId       ??????id
     * @param appInstanceId   ??????id
     * @param statefulSetName statefulSet name
     * @return statefulSet??????
     */
    @Permission(level = ResourceLevel.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "????????????id????????????statefulSet??????(Yaml??????)")
    @GetMapping(value = "/{appInstanceId}/stateful_set_detail_yaml")
    public ResponseEntity<InstanceControllerDetailDTO> getStatefulSetDetailsYamlByInstanceId(
            @ApiParam(value = "??????ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "????????????", required = true)
            @RequestParam(value = "stateful_set_name") String statefulSetName,
            @ApiParam(value = "??????ID", required = true)
            @PathVariable Long appInstanceId) {
        return new ResponseEntity<>(applicationInstanceService.getInstanceResourceDetailYaml(appInstanceId, statefulSetName, ResourceType.STATEFULSET), HttpStatus.OK);
    }

    /**
     * ???????????? Value
     *
     * @param projectId     ??????id
     * @param appInstanceId ??????id
     * @param appVersionId  ??????Id
     * @return ReplaceResult
     */
    @Permission(level = ResourceLevel.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER,
                    InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "???????????? Value")
    @GetMapping(value = "/{appInstanceId}/appVersion/{appVersionId}/value")
    public ResponseEntity<ReplaceResult> queryUpgradeValue(
            @ApiParam(value = "??????ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "??????ID", required = true)
            @PathVariable Long appInstanceId,
            @ApiParam(value = "??????Id", required = true)
            @PathVariable Long appVersionId) {
        return Optional.ofNullable(applicationInstanceService.queryUpgradeValue(appInstanceId, appVersionId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.instance.value.get"));
    }

    /**
     * ??????value??????
     *
     * @param projectId    ??????id
     * @param appId        ??????id
     * @param envId        ??????id
     * @param appVersionId ??????id
     * @return ReplaceResult
     */
    @Permission(level = ResourceLevel.PROJECT, roles = {InitRoleCode.PROJECT_OWNER,
            InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "??????value??????")
    @GetMapping("/value")
    public ResponseEntity<ReplaceResult> queryValues(
            @ApiParam(value = "??????ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "??????ID", required = true)
            @RequestParam Long appId,
            @ApiParam(value = "??????ID", required = true)
            @RequestParam Long envId,
            @ApiParam(value = "??????ID", required = true)
            @RequestParam Long appVersionId) {
        return Optional.ofNullable(applicationInstanceService.queryValues(appId, envId, appVersionId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.values.query"));
    }

    /**
     * @param projectId     ??????id
     * @param replaceResult ??????value
     * @return ReplaceResult
     */
    @Permission(level = ResourceLevel.PROJECT, roles = {InitRoleCode.PROJECT_OWNER,
            InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "????????????value")
    @PostMapping("/previewValue")
    public ResponseEntity<ReplaceResult> previewValues(
            @ApiParam(value = "??????ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "replaceResult", required = true)
            @RequestBody ReplaceResult replaceResult,
            @ApiParam(value = "??????ID", required = true)
            @RequestParam Long appVersionId) {
        return Optional.ofNullable(applicationInstanceService.previewValues(replaceResult, appVersionId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.values.query"));
    }

    /**
     * ??????values
     *
     * @param replaceResult values??????
     * @return List
     */
    @Permission(level = ResourceLevel.PROJECT, roles = {InitRoleCode.PROJECT_OWNER,
            InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "??????values")
    @PostMapping("/value_format")
    public ResponseEntity<List<ErrorLineDTO>> formatValue(
            @ApiParam(value = "??????ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "value", required = true)
            @RequestBody ReplaceResult replaceResult) {
        return new ResponseEntity<>(applicationInstanceService.formatValue(replaceResult), HttpStatus.OK);
    }

    /**
     * ????????????
     *
     * @param projectId            ??????id
     * @param applicationDeployDTO ????????????
     * @return ApplicationInstanceDTO
     */
    @ApiOperation(value = "????????????")
    @Permission(level = ResourceLevel.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER,
                    InitRoleCode.PROJECT_MEMBER})
    @PostMapping
    public ResponseEntity<ApplicationInstanceDTO> deploy(
            @ApiParam(value = "??????ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "????????????", required = true)
            @RequestBody ApplicationDeployDTO applicationDeployDTO) {
        return Optional.ofNullable(applicationInstanceService.createOrUpdate(applicationDeployDTO))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.application.deploy"));
    }

    /**
     * ????????????????????????
     *
     * @param projectId    ??????id
     * @param appId        ??????id
     * @param appVersionId ????????????id
     * @param envId        ??????id
     * @return list of AppInstanceCodeDTO
     */
    @Permission(level = ResourceLevel.PROJECT, roles = {InitRoleCode.PROJECT_OWNER,
            InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "????????????????????????")
    @GetMapping("/options")
    public ResponseEntity<List<AppInstanceCodeDTO>> listByAppVersionId(
            @ApiParam(value = "?????? ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "?????? ID")
            @RequestParam(required = false) Long envId,
            @ApiParam(value = "??????Id")
            @RequestParam(required = false) Long appId,
            @ApiParam(value = "???????????? ID")
            @RequestParam(required = false) Long appVersionId) {
        return Optional.ofNullable(applicationInstanceService.listByOptions(projectId, appId, appVersionId, envId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException(ERROR_APPINSTANCE_QUERY));
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @param projectId ??????id
     * @param appId     ??????id
     * @param envId     ??????id
     * @return list of AppInstanceCodeDTO
     */
    @Permission(level = ResourceLevel.PROJECT, roles = {InitRoleCode.PROJECT_OWNER,
            InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "?????????????????????????????????????????????")
    @GetMapping("/listByAppIdAndEnvId")
    public ResponseEntity<List<AppInstanceCodeDTO>> listByAppIdAndEnvId(
            @ApiParam(value = "?????? ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "?????? ID")
            @RequestParam Long envId,
            @ApiParam(value = "??????Id")
            @RequestParam Long appId) {
        return Optional.ofNullable(applicationInstanceService.listByAppIdAndEnvId(projectId, appId, envId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException(ERROR_APPINSTANCE_QUERY));
    }


    /**
     * ??????????????????????????????
     *
     * @param projectId     ??????id
     * @param appInstanceId ??????id
     * @return DevopsEnvResourceDTO
     */
    @Permission(level = ResourceLevel.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "??????????????????????????????")
    @GetMapping("/{appInstanceId}/resources")
    public ResponseEntity<DevopsEnvResourceDTO> listResources(
            @ApiParam(value = "??????ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "??????ID", required = true)
            @PathVariable Long appInstanceId) {
        return Optional.ofNullable(devopsEnvResourceService.listResources(appInstanceId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.resource.query"));
    }


    /**
     * ??????????????????Event??????
     *
     * @param projectId     ??????id
     * @param appInstanceId ??????id
     * @return List
     */
    @Permission(level = ResourceLevel.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "??????????????????Event??????")
    @GetMapping("/{app_instanceId}/events")
    public ResponseEntity<List<InstanceEventDTO>> listEvents(
            @ApiParam(value = "??????ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "??????ID", required = true)
            @PathVariable(value = "app_instanceId") Long appInstanceId) {
        return Optional.ofNullable(devopsEnvResourceService.listInstancePodEvent(appInstanceId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.event.query"));
    }

    /**
     * ????????????
     *
     * @param projectId  ??????id
     * @param instanceId ??????id
     * @return responseEntity
     */
    @Permission(level = ResourceLevel.PROJECT, roles = {InitRoleCode.PROJECT_OWNER,
            InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "????????????")
    @PutMapping(value = "/{instanceId}/stop")
    public ResponseEntity stop(
            @ApiParam(value = "?????? ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "??????ID", required = true)
            @PathVariable Long instanceId) {
        applicationInstanceService.instanceStop(instanceId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * ????????????
     *
     * @param projectId  ??????id
     * @param instanceId ??????id
     * @return responseEntity
     */
    @Permission(level = ResourceLevel.PROJECT, roles = {InitRoleCode.PROJECT_OWNER,
            InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "????????????")
    @PutMapping(value = "/{instanceId}/start")
    public ResponseEntity start(
            @ApiParam(value = "?????? ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "??????ID", required = true)
            @PathVariable Long instanceId) {
        applicationInstanceService.instanceStart(instanceId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * ??????????????????
     *
     * @param projectId  ??????id
     * @param instanceId ??????id
     * @return responseEntity
     */
    @Permission(level = ResourceLevel.PROJECT, roles = {InitRoleCode.PROJECT_OWNER,
            InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "??????????????????")
    @PutMapping(value = "/{instanceId}/restart")
    public ResponseEntity restart(
            @ApiParam(value = "?????? ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "??????ID", required = true)
            @PathVariable Long instanceId) {
        applicationInstanceService.instanceReStart(instanceId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * ????????????
     *
     * @param projectId  ??????id
     * @param instanceId ??????id
     * @return responseEntity
     */
    @Permission(level = ResourceLevel.PROJECT, roles = {InitRoleCode.PROJECT_OWNER,
            InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "????????????")
    @DeleteMapping(value = "/{instanceId}/delete")
    public ResponseEntity delete(
            @ApiParam(value = "?????? ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "??????ID", required = true)
            @PathVariable Long instanceId) {
        applicationInstanceService.instanceDelete(instanceId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * ????????????????????????
     *
     * @param projectId    ??????id
     * @param instanceName ?????????
     */
    @Permission(level = ResourceLevel.PROJECT, roles = {InitRoleCode.PROJECT_OWNER,
            InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "????????????????????????")
    @GetMapping(value = "/check_name")
    public void checkName(
            @ApiParam(value = "?????? ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "??????ID", required = true)
            @RequestParam(value = "instance_name") String instanceName) {
        applicationInstanceService.checkName(instanceName);
    }

    /**
     * ????????????????????????
     *
     * @param projectId ??????id
     * @param envId     ??????Id
     * @param params    ????????????
     * @return DevopsEnvPreviewDTO
     */
    @Permission(level = ResourceLevel.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER,
                    InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "????????????????????????")
    @PostMapping(value = "/{envId}/listByEnv")
    public ResponseEntity<DevopsEnvPreviewDTO> listByEnv(
            @ApiParam(value = "?????? ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "envId", required = true)
            @PathVariable(value = "envId") Long envId,
            @ApiParam(value = "????????????")
            @RequestBody(required = false) String params) {
        return Optional.ofNullable(applicationInstanceService.listByEnv(projectId, envId, params))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException(ERROR_APPINSTANCE_QUERY));
    }

    /**
     * ????????????????????????
     *
     * @param projectId ??????id
     * @param envId     ??????id
     * @param appIds    ??????id
     * @param startTime ????????????
     * @param endTime   ????????????
     * @return List
     */
    @Permission(level = ResourceLevel.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER,
                    InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "????????????????????????")
    @PostMapping(value = "/env_commands/time")
    public ResponseEntity<DeployTimeDTO> listDeployTime(
            @ApiParam(value = "?????? ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "envId")
            @RequestParam(required = false) Long envId,
            @ApiParam(value = "appIds")
            @RequestBody(required = false) Long[] appIds,
            @ApiParam(value = "startTime")
            @RequestParam(required = true) Date startTime,
            @ApiParam(value = "endTime")
            @RequestParam(required = true) Date endTime) {
        return Optional.ofNullable(applicationInstanceService.listDeployTime(projectId, envId, appIds, startTime, endTime))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.deploy.time.get"));
    }


    /**
     * ????????????????????????
     *
     * @param projectId ??????id
     * @param envIds    ??????id
     * @param appId     ??????id
     * @param startTime ????????????
     * @param endTime   ????????????
     * @return List
     */
    @Permission(level = ResourceLevel.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER,
                    InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "????????????????????????")
    @PostMapping(value = "/env_commands/frequency")
    public ResponseEntity<DeployFrequencyDTO> listDeployFrequency(
            @ApiParam(value = "?????? ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "appId")
            @RequestParam(required = false) Long appId,
            @ApiParam(value = "envIds")
            @RequestBody(required = false) Long[] envIds,
            @ApiParam(value = "startTime")
            @RequestParam(required = true) Date startTime,
            @ApiParam(value = "endTime")
            @RequestParam(required = true) Date endTime) {
        return Optional.ofNullable(applicationInstanceService.listDeployFrequency(projectId, envIds, appId, startTime, endTime))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.deploy.frequency.get"));
    }


    /**
     * ????????????????????????table
     *
     * @param projectId ??????id
     * @param envIds    ??????id
     * @param appId     ??????id
     * @param startTime ????????????
     * @param endTime   ????????????
     * @return List
     */
    @Permission(level = ResourceLevel.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER,
                    InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "????????????????????????table")
    @CustomPageRequest
    @PostMapping(value = "/env_commands/frequencyDetail")
    public ResponseEntity<Page<DeployDetailDTO>> pageDeployFrequencyDetail(
            @ApiParam(value = "?????? ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "????????????") PageRequest pageRequest,
            @ApiParam(value = "appId")
            @RequestParam(required = false) Long appId,
            @ApiParam(value = "envIds")
            @RequestBody(required = false) Long[] envIds,
            @ApiParam(value = "startTime")
            @RequestParam(required = true) Date startTime,
            @ApiParam(value = "endTime")
            @RequestParam(required = true) Date endTime) {
        return Optional.ofNullable(applicationInstanceService.pageDeployFrequencyDetail(projectId, pageRequest, envIds, appId, startTime, endTime))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.deploy.frequency.get"));
    }


    /**
     * ????????????????????????table
     *
     * @param projectId ??????id
     * @param envId     ??????id
     * @param appIds    ??????id
     * @param startTime ????????????
     * @param endTime   ????????????
     * @return List
     */
    @Permission(level = ResourceLevel.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER,
                    InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "????????????????????????table")
    @CustomPageRequest
    @PostMapping(value = "/env_commands/timeDetail")
    public ResponseEntity<Page<DeployDetailDTO>> pageDeployTimeDetail(
            @ApiParam(value = "?????? ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "????????????")
                    PageRequest pageRequest,
            @ApiParam(value = "envId")
            @RequestParam(required = false) Long envId,
            @ApiParam(value = "appIds")
            @RequestBody(required = false) Long[] appIds,
            @ApiParam(value = "startTime")
            @RequestParam(required = true) Date startTime,
            @ApiParam(value = "endTime")
            @RequestParam(required = true) Date endTime) {
        return Optional.ofNullable(applicationInstanceService.pageDeployTimeDetail(projectId, pageRequest, appIds, envId, startTime, endTime))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.deploy.time.get"));
    }

    /**
     * ???????????????????????????
     *
     * @param projectId            ??????id
     * @param applicationDeployDTO ????????????
     * @return ApplicationInstanceDTO
     */
    @ApiOperation(value = "???????????????????????????")
    @Permission(level = ResourceLevel.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER,
                    InitRoleCode.PROJECT_MEMBER})
    @PostMapping("/deploy_test_app")
    public void deployTestApp(
            @ApiParam(value = "??????ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "????????????", required = true)
            @RequestBody ApplicationDeployDTO applicationDeployDTO) {
        applicationInstanceService.deployTestApp(applicationDeployDTO);
    }

    /**
     * ??????pod?????????
     *
     * @param projectId      ??????id
     * @param envId          ??????id
     * @param deploymentName deploymentName
     * @param count          pod??????
     * @return ApplicationInstanceDTO
     */
    @ApiOperation(value = "??????pod?????????")
    @Permission(level = ResourceLevel.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER,
                    InitRoleCode.PROJECT_MEMBER})
    @PutMapping("/operate_pod_count")
    public void operatePodCount(
            @ApiParam(value = "??????ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "??????id", required = true)
            @RequestParam Long envId,
            @ApiParam(value = "deploymentName", required = true)
            @RequestParam String deploymentName,
            @ApiParam(value = "pod??????", required = true)
            @RequestParam Long count) {
        applicationInstanceService.operationPodCount(deploymentName, envId, count);
    }


    /**
     * ????????????????????????
     *
     * @param projectId     ??????id
     * @param appInstanceId ??????id
     * @param startTime     ????????????
     * @param endTime       ????????????
     * @return List
     */
    @Permission(level = ResourceLevel.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER,
                    InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "????????????????????????")
    @CustomPageRequest
    @PostMapping(value = "/command_log/{appInstanceId}")
    public ResponseEntity<Page<AppInstanceCommandLogDTO>> listCommandLogs(
            @ApiParam(value = "?????? ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "????????????") PageRequest pageRequest,
            @ApiParam(value = "?????? ID", required = true)
            @PathVariable Long appInstanceId,
            @ApiParam(value = "startTime")
            @RequestParam(required = false) Date startTime,
            @ApiParam(value = "endTime")
            @RequestParam(required = false) Date endTime) {
        return Optional.ofNullable(applicationInstanceService.listAppInstanceCommand(pageRequest, appInstanceId, startTime, endTime))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.deploy.log.get"));
    }

    /**
     * ?????????????????????????????? ?????????????????????????????????
     *
     * @param projectId ??????id
     * @param appId     ??????id
     * @param envId     ??????id
     * @return list of AppInstanceCodeDTO
     */
    @Permission(level = ResourceLevel.PROJECT, roles = {InitRoleCode.PROJECT_OWNER,
            InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "?????????????????????????????????????????????")
    @GetMapping("/getByAppIdAndEnvId")
    public ResponseEntity<List<AppInstanceCodeDTO>> getByAppIdAndEnvId(
            @ApiParam(value = "?????? ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "?????? ID", required = true)
            @RequestParam Long envId,
            @ApiParam(value = "??????Id", required = true)
            @RequestParam Long appId) {
        return Optional.ofNullable(applicationInstanceService.getByAppIdAndEnvId(projectId, appId, envId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException(ERROR_APPINSTANCE_QUERY));
    }

}
