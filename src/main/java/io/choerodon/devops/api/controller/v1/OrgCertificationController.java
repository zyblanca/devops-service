package io.choerodon.devops.api.controller.v1;

import java.util.List;
import java.util.Optional;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.iam.InitRoleCode;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.devops.api.dto.OrgCertificationDTO;
import io.choerodon.devops.api.dto.ProjectDTO;
import io.choerodon.devops.app.service.DevopsOrgCertificationService;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.swagger.annotation.CustomPageRequest;
import io.choerodon.swagger.annotation.Permission;

@RestController
@RequestMapping(value = "/v1/organizations/{organization_id}/certs")
public class OrgCertificationController {

    @Autowired
    private DevopsOrgCertificationService devopsOrgCertificationService;

    /**
     * 组织下创建证书
     *
     * @param organizationId      组织Id
     * @param orgCertificationDTO 证书信息
     */
    @Permission(level = ResourceLevel.ORGANIZATION, roles = {InitRoleCode.ORGANIZATION_ADMINISTRATOR})
    @ApiOperation(value = "组织下创建证书")
    @PostMapping
    public ResponseEntity create(
            @ApiParam(value = "组织Id", required = true)
            @PathVariable(value = "organization_id") Long organizationId,
            @ApiParam(value = "证书信息", required = true)
            @RequestBody OrgCertificationDTO orgCertificationDTO) {
        devopsOrgCertificationService.insert(organizationId, orgCertificationDTO);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * 更新证书下的项目
     *
     * @param organizationId      组织Id
     * @param orgCertificationDTO 集群对象
     */
    @Permission(level = ResourceLevel.ORGANIZATION, roles = {InitRoleCode.ORGANIZATION_ADMINISTRATOR})
    @ApiOperation(value = "更新证书下的项目")
    @PutMapping("/{certId}")
    public ResponseEntity update(
            @ApiParam(value = "组织Id", required = true)
            @PathVariable(value = "organization_id") Long organizationId,
            @ApiParam(value = "集群Id")
            @PathVariable Long certId,
            @ApiParam(value = "集群对象")
            @RequestBody OrgCertificationDTO orgCertificationDTO) {
        devopsOrgCertificationService.update(certId, orgCertificationDTO);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * 查询单个证书信息
     *
     * @param organizationId 组织Id
     * @param certId         集群Id
     */
    @Permission(level = ResourceLevel.ORGANIZATION, roles = {InitRoleCode.ORGANIZATION_ADMINISTRATOR})
    @ApiOperation(value = "查询单个证书信息")
    @GetMapping("/{certId}")
    public ResponseEntity<OrgCertificationDTO> query(
            @ApiParam(value = "组织Id", required = true)
            @PathVariable(value = "organization_id") Long organizationId,
            @ApiParam(value = "集群Id")
            @PathVariable Long certId) {
        return Optional.ofNullable(devopsOrgCertificationService.getCert(certId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.cert.query"));
    }

    /**
     * 校验证书名唯一性
     *
     * @param organizationId 项目id
     * @param name           集群name
     */
    @Permission(level = ResourceLevel.ORGANIZATION, roles = {InitRoleCode.ORGANIZATION_ADMINISTRATOR})
    @ApiOperation(value = "校验证书名唯一性")
    @GetMapping(value = "/check_name")
    public void checkName(
            @ApiParam(value = "组织Id", required = true)
            @PathVariable(value = "organization_id") Long organizationId,
            @ApiParam(value = "集群name", required = true)
            @RequestParam String name) {
        devopsOrgCertificationService.checkName(organizationId, name);
    }

    /**
     * 分页查询项目列表
     *
     * @param organizationId 项目id
     * @return Page
     */
    @Permission(level = ResourceLevel.ORGANIZATION,
            roles = {InitRoleCode.ORGANIZATION_ADMINISTRATOR})
    @ApiOperation(value = "分页查询项目列表")
    @CustomPageRequest
    @PostMapping("/page_projects")
    public ResponseEntity<Page<ProjectDTO>> pageProjects(
            @ApiParam(value = "组织ID", required = true)
            @PathVariable(value = "organization_id") Long organizationId,
            @ApiParam(value = "分页参数")
            @ApiIgnore PageRequest pageRequest,
            @ApiParam(value = "集群Id")
            @RequestParam(required = false) Long certId,
            @ApiParam(value = "模糊搜索参数")
            @RequestBody String[] params) {
        return Optional.ofNullable(devopsOrgCertificationService.listProjects(organizationId, certId, pageRequest, params))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.project.query"));
    }

    /**
     * 查询已有权限的项目列表
     *
     * @param organizationId 项目id
     * @return List
     */
    @Permission(level = ResourceLevel.ORGANIZATION,
            roles = {InitRoleCode.ORGANIZATION_ADMINISTRATOR})
    @ApiOperation(value = "查询已有权限的项目列表")
    @GetMapping("/list_cert_projects/{certId}")
    public ResponseEntity<List<ProjectDTO>> listCertProjects(
            @ApiParam(value = "组织ID", required = true)
            @PathVariable(value = "organization_id") Long organizationId,
            @ApiParam(value = "集群Id")
            @PathVariable Long certId) {
        return Optional.ofNullable(devopsOrgCertificationService.listCertProjects(certId))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.project.query"));
    }


    /**
     * 组织证书列表查询
     *
     * @param organizationId 组织ID
     * @return Page
     */
    @Permission(level = ResourceLevel.ORGANIZATION,
            roles = {InitRoleCode.ORGANIZATION_ADMINISTRATOR})
    @ApiOperation(value = "组织证书列表查询")
    @CustomPageRequest
    @PostMapping("/page_cert")
    public ResponseEntity<Page<OrgCertificationDTO>> listOrgCert(
            @ApiParam(value = "组织ID", required = true)
            @PathVariable(value = "organization_id") Long organizationId,
            @ApiParam(value = "分页参数")
            @ApiIgnore PageRequest pageRequest,
            @ApiParam(value = "查询参数")
            @RequestBody String params) {
        return Optional.ofNullable(devopsOrgCertificationService.pageCerts(organizationId, pageRequest, params))
                .map(target -> new ResponseEntity<>(target, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.cert.query"));
    }

    /**
     * 删除证书
     *
     * @param organizationId 组织ID
     * @param certId         证书Id
     * @return String
     */
    @Permission(level = ResourceLevel.ORGANIZATION,
            roles = {InitRoleCode.ORGANIZATION_ADMINISTRATOR})
    @ApiOperation(value = "删除证书")
    @CustomPageRequest
    @DeleteMapping("/{certId}")
    public ResponseEntity<String> deleteOrgCert(
            @ApiParam(value = "组织ID", required = true)
            @PathVariable(value = "organization_id") Long organizationId,
            @ApiParam(value = "集群Id")
            @PathVariable Long certId) {
        devopsOrgCertificationService.deleteCert(certId);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
