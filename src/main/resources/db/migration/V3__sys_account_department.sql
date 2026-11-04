-- ==================== 账号表：新增所属部门 ====================
ALTER TABLE `sys_account`
    ADD COLUMN `department_id` BIGINT DEFAULT NULL COMMENT '所属部门ID' AFTER `role_level`,
    ADD INDEX `idx_account_department_id` (`department_id`),
    ADD CONSTRAINT `fk_account_department` FOREIGN KEY (`department_id`) REFERENCES `sys_department` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;
