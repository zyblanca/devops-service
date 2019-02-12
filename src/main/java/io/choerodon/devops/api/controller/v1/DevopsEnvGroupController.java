package io.choerodon.devops.api.controller.v1;

import java.util.List;
import java.util.Optional;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.choerodon.core.exception.CommonException;
import io.choerodon.core.iam.InitRoleCode;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.devops.api.dto.DevopsEnvGroupDTO;
import io.choerodon.devops.app.service.DevopsEnvGroupService;
import io.choerodon.swagger.annotation.Permission;


/**
 * Creator: Runge
 * Date: 2018/9/4
 * Time: 14:18
 * Description:
 */
@RestController
@RequestMapping(value = "/v1/projects/{project_id}/env_groups")
public class DevopsEnvGroupController {

    @Autowired
    private DevopsEnvGroupService devopsEnvGroupService;

    /**
     * 项目下创建环境组
     *
     * @param projectId          项目id
     * @param devopsEnvGroupName 环境组名称
     * @return ApplicationTemplateDTO
     */
    @Permission(level = ResourceLevel.PROJECT, roles = {InitRoleCode.PROJECT_OWNER})
    @ApiOperation(value = "项目下创建环境组")
    @PostMapping
    public ResponseEntity<DevopsEnvGroupDTO> create(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "环境组信息", required = true)
            @RequestParam String devopsEnvGroupName) {
        return Optional.ofNullable(devopsEnvGroupService.create(devopsEnvGroupName, projectId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.env.group.create"));
    }


    /**
     * 项目下更新环境组
     *
     * @param projectId         项目id
     * @param devopsEnvGroupDTO 环境组信息
     * @return ApplicationTemplateDTO
     */
    @Permission(level = ResourceLevel.PROJECT, roles = {InitRoleCode.PROJECT_OWNER})
    @ApiOperation(value = "项目下更新环境组")
    @PutMapping
    public ResponseEntity<DevopsEnvGroupDTO> update(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "环境组信息", required = true)
            @RequestBody DevopsEnvGroupDTO devopsEnvGroupDTO) {
        return Optional.ofNullable(devopsEnvGroupService.update(devopsEnvGroupDTO, projectId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.env.group.update"));
    }


    /**
     * 项目下查询环境组
     *
     * @param projectId 项目id
     * @return DevopsEnvGroupDTO
     */
    @Permission(level = ResourceLevel.PROJECT, roles = {InitRoleCode.PROJECT_OWNER})
    @ApiOperation(value = "项目下查询环境组")
    @GetMapping
    public ResponseEntity<List<DevopsEnvGroupDTO>> listByProject(
            @ApiParam(value = "项目 ID", required = true)
            @PathVariable(value = "project_id") Long projectId) {
        return Optional.ofNullable(devopsEnvGroupService.listByProject(projectId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.env.group.get"));
    }

    /**
     * 校验环境组名唯一性
     *
     * @param projectId 项目id
     * @return boolean
     */
    @Permission(level = ResourceLevel.PROJECT, roles = {InitRoleCode.PROJECT_OWNER})
    @ApiOperation(value = "校验环境组名唯一性")
    @GetMapping(value = "/checkName")
    public ResponseEntity<Boolean> checkName(
            @ApiParam(value = "项目 ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "环境组名", required = true)
            @RequestParam String name) {
        return Optional.ofNullable(devopsEnvGroupService.checkUniqueInProject(name, projectId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.env.group.get"));
    }


    /**
     * 环境组删除
     *
     * @param projectId 项目id
     * @param groupId   实例id
     * @return responseEntity
     */
    @Permission(level = ResourceLevel.PROJECT, roles = {InitRoleCode.PROJECT_OWNER})
    @ApiOperation(value = "环境组删除")
    @DeleteMapping(value = "/{groupId}")
    public ResponseEntity delete(
            @ApiParam(value = "项目ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "环境组ID", required = true)
            @PathVariable Long groupId) {
        devopsEnvGroupService.delete(groupId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
