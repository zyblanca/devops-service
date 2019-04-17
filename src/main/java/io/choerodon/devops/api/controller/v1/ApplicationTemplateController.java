package io.choerodon.devops.api.controller.v1;

import java.util.List;
import java.util.Optional;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.devops.api.dto.ApplicationTemplateDTO;
import io.choerodon.devops.api.dto.ApplicationTemplateRepDTO;
import io.choerodon.devops.api.dto.ApplicationTemplateUpdateDTO;
import io.choerodon.devops.app.service.ApplicationTemplateService;
import io.choerodon.mybatis.pagehelper.annotation.SortDefault;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.mybatis.pagehelper.domain.Sort;
import io.choerodon.swagger.annotation.CustomPageRequest;
import io.choerodon.swagger.annotation.Permission;

/**
 * Created by younger on 2018/3/27.
 */
@RestController
@RequestMapping(value = "/v1/organizations/{organization_id}/app_templates")
public class ApplicationTemplateController {
    private static final String ERROR_GET = "error.appTemplate.get";

    private ApplicationTemplateService applicationTemplateService;

    public ApplicationTemplateController(ApplicationTemplateService applicationTemplateService) {
        this.applicationTemplateService = applicationTemplateService;
    }

    /**
     * 组织下创建应用模板
     *
     * @param organizationId         组织id
     * @param applicationTemplateDTO 模板信息
     * @return ApplicationTemplateDTO
     */
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "组织下创建应用模板")
    @PostMapping
    public ResponseEntity<ApplicationTemplateRepDTO> create(
            @ApiParam(value = "组织ID", required = true)
            @PathVariable(value = "organization_id") Long organizationId,
            @ApiParam(value = "环境名", required = true)
            @RequestBody ApplicationTemplateDTO applicationTemplateDTO) {
        return Optional.ofNullable(applicationTemplateService.create(applicationTemplateDTO, organizationId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.appTemplate.create"));
    }

    /**
     * 组织下更新应用模板
     *
     * @param organizationId               组织id
     * @param applicationTemplateUpdateDTO 模板信息
     * @return ApplicationTemplateDTO
     */
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "组织下更新应用模板")
    @PutMapping
    public ResponseEntity<ApplicationTemplateRepDTO> update(
            @ApiParam(value = "组织ID", required = true)
            @PathVariable(value = "organization_id") Long organizationId,
            @ApiParam(value = "模板信息", required = true)
            @RequestBody ApplicationTemplateUpdateDTO applicationTemplateUpdateDTO) {
        return Optional.ofNullable(applicationTemplateService.update(applicationTemplateUpdateDTO, organizationId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.appTemplate.update"));
    }

    /**
     * 组织下删除应用模板
     *
     * @param organizationId 组织id
     * @param appTemplateId  模板id
     * @return ApplicationTemplateDTO
     */
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "组织下删除应用模板")
    @DeleteMapping(value = "/{appTemplateId}")
    public ResponseEntity delete(
            @ApiParam(value = "组织ID", required = true)
            @PathVariable(value = "organization_id") Long organizationId,
            @ApiParam(value = "环境名", required = true)
            @PathVariable Long appTemplateId) {
        applicationTemplateService.delete(appTemplateId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * 组织下查询单个应用模板
     *
     * @param organizationId 组织id
     * @param appTemplateId  模板id
     * @return ApplicationTemplateDTO
     */
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "组织下查询单个应用模板")
    @GetMapping(value = "/{appTemplateId}")
    public ResponseEntity<ApplicationTemplateRepDTO> queryByAppTemplateId(
            @ApiParam(value = "组织ID", required = true)
            @PathVariable(value = "organization_id") Long organizationId,
            @ApiParam(value = "环境名", required = true)
            @PathVariable Long appTemplateId) {
        return Optional.ofNullable(applicationTemplateService.query(appTemplateId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException(ERROR_GET));
    }

    /**
     * 组织下分页查询应用模板
     *
     * @param organizationId 组织id
     * @param pageRequest    分页参数
     * @param params         查询参数
     * @return Page
     */
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "组织下分页查询应用模板")
    @CustomPageRequest
    @PostMapping("/list_by_options")
    public ResponseEntity<Page<ApplicationTemplateRepDTO>> listByOptions(
            @ApiParam(value = "组织ID", required = true)
            @PathVariable(value = "organization_id") Long organizationId,
            @ApiParam(value = "分页参数")
            @ApiIgnore
            @SortDefault(value = "id", direction = Sort.Direction.ASC) PageRequest pageRequest,
            @ApiParam(value = "查询参数")
            @RequestBody(required = false) String params) {
        return Optional.ofNullable(applicationTemplateService.listByOptions(pageRequest, organizationId, params))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException(ERROR_GET));
    }

    /**
     * 组织下分页查询应用模板
     *
     * @param organizationId 组织id
     * @return Page
     */
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "组织下查询所有应用模板")
    @GetMapping
    public ResponseEntity<List<ApplicationTemplateRepDTO>> listByOrgId(
            @ApiParam(value = "组织ID", required = true)
            @PathVariable(value = "organization_id") Long organizationId) {
        return Optional.ofNullable(applicationTemplateService.list(organizationId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException(ERROR_GET));
    }

    /**
     * 创建模板校验名称是否存在
     *
     * @param organizationId 组织id
     * @param name           模板name
     */
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "创建模板校验名称是否存在")
    @GetMapping(value = "/check_name")
    public void checkName(
            @ApiParam(value = "组织ID", required = true)
            @PathVariable(value = "organization_id") Long organizationId,
            @ApiParam(value = "环境名", required = true)
            @RequestParam String name) {
        applicationTemplateService.checkName(organizationId, name);
    }

    /**
     * 创建模板校验编码是否存在
     *
     * @param organizationId 组织id
     * @param code           模板code
     */
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "创建模板校验编码是否存在")
    @GetMapping(value = "/check_code")
    public void checkCode(
            @ApiParam(value = "组织ID", required = true)
            @PathVariable(value = "organization_id") Long organizationId,
            @ApiParam(value = "环境名", required = true)
            @RequestParam String code) {
        applicationTemplateService.checkCode(organizationId, code);
    }

    /**
     * 根据模板code查询模板
     *
     * @param organizationId 组织id
     * @param code           模板code
     */
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "根据模板code查询模板")
    @GetMapping(value = "/query_by_code")
    public ResponseEntity<ApplicationTemplateRepDTO> queryByCode(
            @ApiParam(value = "组织ID", required = true)
            @PathVariable(value = "organization_id") Long organizationId,
            @ApiParam(value = "环境名", required = true)
            @RequestParam String code) {
        return Optional.ofNullable(applicationTemplateService.queryByCode(organizationId, code))
                .map(t->new ResponseEntity<>(t,HttpStatus.OK))
                .orElseThrow(()->new CommonException(ERROR_GET));
    }

}
