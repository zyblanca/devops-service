ALTER TABLE `devops_service`.`devops_env` ADD COLUMN `gitlab_env_project_path` VARCHAR(128) NULL AFTER `gitlab_env_project_id`;
update devops_service.devops_env set gitlab_env_project_path=code;