package io.choerodon.devops.api.controller.v1;

import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.iam.InitRoleCode;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.devops.api.dto.DevopsAutoDeployDTO;
import io.choerodon.devops.api.dto.DevopsAutoDeployRecordDTO;
import io.choerodon.devops.app.service.DevopsAutoDeployService;
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

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * Creator: ChangpingShi0213@gmail.com
 * Date:  16:30 2019/2/25
 * Description:
 */
@RestController
@RequestMapping(value = "/v1/{project_id}/auto_deploy")
public class DevopsAutoDeployController {
    @Autowired
    private DevopsAutoDeployService devopsAutoDeployService;

    /**
     * 项目下创建自动部署
     *
     * @param projectId           项目id
     * @param devopsAutoDeployDTO 自动部署DTO
     * @return ApplicationRepDTO
     */
    @Permission(level = ResourceLevel.PROJECT, roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "项目下创建或更新自动部署")
    @PostMapping
    public ResponseEntity<DevopsAutoDeployDTO> createOrUpdate(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "自动部署信息", required = true)
            @RequestBody  @Valid DevopsAutoDeployDTO devopsAutoDeployDTO) {
        return Optional.ofNullable(devopsAutoDeployService.createOrUpdate(projectId, devopsAutoDeployDTO))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.auto.deploy.create"));
    }

    /**
     * 项目下删除自动部署
     *
     * @param projectId    项目id
     * @param autoDeployId 自动部署id
     * @return Boolean
     */
    @Permission(level = ResourceLevel.PROJECT, roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "项目下删除自动部署")
    @DeleteMapping("/{auto_deploy_id}")
    public ResponseEntity deleteById(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "自动部署id", required = true)
            @PathVariable(value = "auto_deploy_id") Long autoDeployId) {
        devopsAutoDeployService.delete(projectId, autoDeployId);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }


    @Permission(level = ResourceLevel.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "项目下分页查询自动部署")
    @CustomPageRequest
    @PostMapping("/list_by_options")
    public ResponseEntity<Page<DevopsAutoDeployDTO>> pageByOptions(
            @ApiParam(value = "项目Id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "用户Id", required = true)
            @RequestParam(value = "user_id") Long userId,
            @ApiParam(value = "应用Id")
            @RequestParam(value = "app_id", required = false) Long appId,
            @ApiParam(value = "环境Id")
            @RequestParam(value = "env_id", required = false) Long envId,
            @ApiParam(value = "是否分页")
            @RequestParam(value = "doPage", required = false) Boolean doPage,
            @ApiParam(value = "分页参数")
            @ApiIgnore PageRequest pageRequest,
            @ApiParam(value = "查询参数")
            @RequestBody(required = false) String params) {
        return Optional.ofNullable(
                devopsAutoDeployService.listByOptions(projectId, userId, appId, envId, doPage, pageRequest, params))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.auto.deploy.get"));
    }

    @Permission(level = ResourceLevel.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "项目下查询所有自动部署")
    @GetMapping("/list")
    public ResponseEntity<List<DevopsAutoDeployDTO>> queryByProjectId(
            @ApiParam(value = "项目Id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "用户Id", required = true)
            @RequestParam(value = "user_id") Long userId) {
        return Optional.ofNullable(
                devopsAutoDeployService.queryByProjectId(projectId, userId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.auto.deploy.list"));
    }


    @Permission(level = ResourceLevel.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "项目下根据Id查询自动部署")
    @GetMapping("/{auto_deploy_id}/detail")
    public ResponseEntity<DevopsAutoDeployDTO> queryById(
            @ApiParam(value = "项目Id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "自动部署id", required = true)
            @PathVariable(value = "auto_deploy_id") Long autoDeployId) {
        return Optional.ofNullable(
                devopsAutoDeployService.queryById(projectId, autoDeployId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.auto.deploy.query"));
    }

    @Permission(level = ResourceLevel.PROJECT,
            roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "项目下分页查询自动部署记录")
    @CustomPageRequest
    @PostMapping("/list_record_options")
    public ResponseEntity<Page<DevopsAutoDeployRecordDTO>> queryRecord(
            @ApiParam(value = "项目Id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "用户Id", required = true)
            @RequestParam(value = "user_id") Long userId,
            @ApiParam(value = "应用Id")
            @RequestParam(value = "app_id", required = false) Long appId,
            @ApiParam(value = "环境Id")
            @RequestParam(value = "env_id", required = false) Long envId,
            @ApiParam(value = "任务名称")
            @RequestParam(value = "task_name", required = false) String taskName,
            @ApiParam(value = "是否分页")
            @RequestParam(value = "doPage", required = false) Boolean doPage,
            @ApiParam(value = "分页参数")
            @ApiIgnore PageRequest pageRequest,
            @ApiParam(value = "查询参数")
            @RequestBody(required = false) String params) {
        return Optional.ofNullable(
                devopsAutoDeployService.queryRecords(projectId, userId, appId, envId, taskName, doPage, pageRequest, params))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.auto.deploy.record.get"));
    }

    /**
     * 创建自动部署校验名称是否存在
     *
     * @param projectId 项目Id
     * @param name      名称
     */
    @Permission(level = ResourceLevel.PROJECT, roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "创建自动部署校验名称是否存在")
    @GetMapping(value = "/check_name")
    public void checkName(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "任务名称", required = true)
            @RequestParam String name) {
        devopsAutoDeployService.checkName(null, projectId, name);
    }

    /**
     * 根据自动部署ID跟新是否启动
     *
     * @param projectId    项目Id
     * @param autoDeployId 自动部署Id
     * @param isEnabled    是否启用
     * @return
     */
    @Permission(level = ResourceLevel.PROJECT, roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "根据自动部署ID跟新是否启动")
    @PutMapping(value = "/{auto_deploy_id}")
    public ResponseEntity<DevopsAutoDeployDTO> updateIsEnabled(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "自动部署id", required = true)
            @PathVariable(value = "auto_deploy_id") Long autoDeployId,
            @ApiParam(value = "是否启用", required = true)
            @RequestParam Integer isEnabled) {
        return Optional.ofNullable(devopsAutoDeployService.updateIsEnabled(autoDeployId, isEnabled))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.auto.deploy.update"));
    }

}
