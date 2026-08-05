-- ==========================================================
-- 浙江脉通智造 - 园区访客系统 (Visitor System)
-- 本地/生产 MySQL 数据库初始化建库与建表脚本
-- 数据库名: visitor_system_db
-- 字符集: utf8mb4
-- ==========================================================

CREATE DATABASE IF NOT EXISTS `visitor_system_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `visitor_system_db`;

-- 1. 系统配置表 (管理员可动态修改 OCR 地址等)
CREATE TABLE IF NOT EXISTS `system_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `config_key` VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    `config_value` TEXT COMMENT '配置值',
    `remark` VARCHAR(255) COMMENT '说明',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统全局配置表';

-- 1.1 系统管理员账号表 (默认账号: admin / Accupath@0723)
CREATE TABLE IF NOT EXISTS `sys_admin_users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '管理员用户名',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希值',
    `real_name` VARCHAR(50) DEFAULT '系统管理员' COMMENT '真实姓名',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员用户表';

-- 插入默认管理员凭据 admin / Accupath@0723
INSERT INTO `sys_admin_users` (`username`, `password_hash`, `real_name`) VALUES
('admin', 'Accupath@0723', '系统管理员')
ON DUPLICATE KEY UPDATE `password_hash` = VALUES(`password_hash`);

-- 插入默认 OCR 与系统配置
INSERT INTO `system_config` (`config_key`, `config_value`, `remark`) VALUES
('ocr.service.url', 'http://10.11.100.238:8081/ocr', '内网PaddleOCR识别服务API地址'),
('ocr.proxy.enabled', 'false', '是否开启后端代理转发模式'),
('ocr.proxy.url', 'http://localhost:8096/api/ocr/proxy', '后端代理服务转发地址'),
('dingtalk.corp.id', 'ding_corp_demo', '钉钉企业CorpID')
ON DUPLICATE KEY UPDATE `config_value` = VALUES(`config_value`);

-- 2. 保密协议模板版本控制表
CREATE TABLE IF NOT EXISTS `sys_nda_templates` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `version` VARCHAR(30) NOT NULL UNIQUE COMMENT '协议版本号 (如 V1.0.0)',
    `title` VARCHAR(200) NOT NULL DEFAULT '浙江脉通智造科技有限公司外来人员保密协议书' COMMENT '协议标题',
    `content` LONGTEXT NOT NULL COMMENT '保密协议条款正文 (HTML/Text)',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否当前启用版本 (1:启用 0:停用)',
    `created_by` VARCHAR(50) DEFAULT 'admin' COMMENT '创建/发布人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保密协议模板版本表';

-- 初始插入默认保密协议 V1.0.0
INSERT INTO `sys_nda_templates` (`version`, `title`, `content`, `is_active`, `created_by`) VALUES
('V1.0.0', '浙江脉通智造科技有限公司外来人员保密协议书', 
'<h3>浙江脉通智造科技有限公司外来人员保密协议书</h3>
<p>为保护浙江脉通智造科技有限公司（以下简称“本公司”）的商业秘密、技术秘密及知识产权，遵守相关法律法规，来访人员（以下简称“乙方”）进入本公司园区前，特签署本协议：</p>
<p><b>第一条 保密内容与范围</b><br/>
1. 乙方在进入本公司厂区、车间、实验室、办公区期间所接触、知悉的任何涉及本公司的研发技术、工艺路线、产品设计、生产设备、管理文档、客户信息等均属于保密信息。<br/>
2. 乙方不得对本公司禁拍区域进行拍照、录像、录音，不得擅自翻阅、拷贝任何商业文件。</p>
<p><b>第二条 法律效力与违约责任</b><br/>
1. 乙方若违反本协议，本公司有权立即终止其访问资格，并保留依法追究相关法律责任及经济赔偿的权利。<br/>
2. 本协议为线上电子确认签署，系统将抓取身份证数据、IP地址、设备指纹及精确时间戳存证，具备完整法律效力。</p>', 
1, 'System Admin')
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);

-- 3. 本地同步的组织架构人员与部门表 (由 10.11.100.202 单向 ETL 同步)
DROP TABLE IF EXISTS `sys_dept_sync`;

CREATE TABLE IF NOT EXISTS `sys_dept_sync` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `dept_name` VARCHAR(100) NOT NULL UNIQUE COMMENT '部门名称',
    `is_shielded` TINYINT DEFAULT 0 COMMENT '是否屏蔽防骚扰 1:是 0:否',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `synced_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织架构部门同步表';


INSERT INTO `sys_dept_sync` (`dept_name`, `is_shielded`) VALUES
('研发部', 0),
('市场部', 0),
('生产部', 0),
('行政部', 0),
('财务部', 1), -- 默认屏蔽财务部
('总裁办', 1)  -- 默认屏蔽总裁办
ON DUPLICATE KEY UPDATE `dept_name` = VALUES(`dept_name`);

CREATE TABLE IF NOT EXISTS `sys_user_sync` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `work_no` VARCHAR(50) NOT NULL UNIQUE COMMENT '员工工号',
    `ad_account` VARCHAR(50) COMMENT '域账号',
    `name` VARCHAR(50) NOT NULL COMMENT '姓名',
    `gender` VARCHAR(10) COMMENT '性别',
    `age` INT COMMENT '年龄',
    `birth_date` DATE COMMENT '出生日期',
    `phone` VARCHAR(20) COMMENT '手机号',
    `dept_id` BIGINT COMMENT '所属部门ID',
    `dept_name` VARCHAR(100) COMMENT '所属部门',
    `manager_id` BIGINT COMMENT '直属主管ID',
    `manager_name` VARCHAR(50) COMMENT '直属主管姓名',
    `status` VARCHAR(20) DEFAULT '在职' COMMENT '入离职状态(在职/离职)',
    `ding_userid` VARCHAR(100) COMMENT '钉钉Userid',
    `synced_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织架构人员同步表';

-- 插入初始化演示员工
INSERT INTO `sys_user_sync` (`work_no`, `ad_account`, `name`, `gender`, `age`, `phone`, `dept_id`, `dept_name`, `manager_name`, `status`, `ding_userid`) VALUES
('MT001', 'zhang.manager', '张经理', '男', 35, '13800001111', 1, '研发部', '李总监', '在职', 'ding_user_001'),
('MT002', 'li.director', '李总监', '男', 42, '13800002222', 1, '研发部', '总经理', '在职', 'ding_user_002'),
('MT003', 'wang.safety', '王安全', '男', 30, '13800003333', 4, '行政部', '张经理', '在职', 'ding_user_003'),
('MT004', 'liu.market', '刘市场', '女', 28, '13800004444', 2, '市场部', '李总监', '在职', 'ding_user_004'),
('MT005', 'zhao.cashier', '赵出纳', '女', 26, '13800005555', 5, '财务部', '财务总监', '在职', 'ding_user_005')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);


-- 3.1 来访事由字典选项表
CREATE TABLE IF NOT EXISTS `sys_visit_reasons` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `reason_name` VARCHAR(100) NOT NULL UNIQUE COMMENT '事由名称',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用(1:启用 0:禁用)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='来访事由选项表';

INSERT INTO `sys_visit_reasons` (`reason_name`, `sort_order`, `is_active`) VALUES
('商务洽谈', 1, 1),
('技术交流', 2, 1),
('面试沟通', 3, 1),
('设备维保', 4, 1),
('其他事由', 5, 1)
ON DUPLICATE KEY UPDATE `reason_name` = VALUES(`reason_name`);

-- 4. 访客申请与预约记录主表
CREATE TABLE IF NOT EXISTS `visitor_records` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `visit_no` VARCHAR(64) NOT NULL UNIQUE COMMENT '访客单号 (VYYYYMMDDXXXX)',
    `scenario` VARCHAR(20) NOT NULL DEFAULT 'B' COMMENT '场景类型: A-员工主动邀约, B-现场盲来扫码',
    `visitor_name` VARCHAR(50) NOT NULL COMMENT '访客姓名',
    `id_card_encrypted` VARCHAR(255) NOT NULL COMMENT '身份证号(AES-256密文)',
    `id_card_masked` VARCHAR(30) NOT NULL COMMENT '身份证号(脱敏: 3301021234****1234)',
    `phone` VARCHAR(20) NOT NULL COMMENT '访客手机号',
    `host_user_id` BIGINT NOT NULL COMMENT '受访员工ID',
    `host_name` VARCHAR(50) NOT NULL COMMENT '受访员工姓名',
    `host_dept` VARCHAR(100) COMMENT '受访部门',
    `visit_purpose` VARCHAR(255) DEFAULT '业务洽谈' COMMENT '来访事由',
    `visit_time` DATETIME NOT NULL COMMENT '计划/申请到访时间',
    `status` VARCHAR(30) NOT NULL DEFAULT 'PENDING_APPROVAL' COMMENT '状态: PENDING_APPROVAL(待审批), APPROVED(已通过/待签协议), NDA_SIGNED(协议已签/待放行), ENTERED(已核销入园), REJECTED(已驳回), EXPIRED(已失效)',
    `nda_signed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '保密协议签署状态 (0:未签 1:已签)',
    `nda_signed_at` DATETIME COMMENT '保密协议签署精确时间',
    `pass_token` VARCHAR(255) COMMENT '用于生成限时动态二维码的加密Token',
    `approved_by` VARCHAR(50) COMMENT '审批人姓名',
    `approved_at` DATETIME COMMENT '审批完成时间',
    `verified_by` VARCHAR(50) COMMENT '保安核销人姓名',
    `verified_at` DATETIME COMMENT '保安核销放行时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_visit_no` (`visit_no`),
    INDEX `idx_visitor_phone` (`phone`),
    INDEX `idx_host_user_id` (`host_user_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访客申请预约记录主表';

-- 5. 保密协议 (NDA) 签署证据链存证表
CREATE TABLE IF NOT EXISTS `visitor_nda_records` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `visitor_record_id` BIGINT NOT NULL COMMENT '关联访客单ID',
    `visit_no` VARCHAR(64) NOT NULL COMMENT '访客单号',
    `visitor_name` VARCHAR(50) NOT NULL COMMENT '访客真实姓名(OCR结果)',
    `id_card_encrypted` VARCHAR(255) NOT NULL COMMENT '身份证号AES密文',
    `signed_at` DATETIME(3) NOT NULL COMMENT '签署精确毫秒时间戳',
    `client_ip` VARCHAR(50) NOT NULL COMMENT '客户端公网/内网IP',
    `device_fingerprint` VARCHAR(255) COMMENT '设备指纹/User-Agent',
    `signature_base64` LONGTEXT COMMENT '访客电子手写签名Base64图像',
    `nda_version` VARCHAR(30) NOT NULL COMMENT '签署时生效的协议版本号 (如 V1.0.0)',
    `hash_chain` VARCHAR(255) NOT NULL COMMENT '数据哈希校验链 (SHA-256防篡改指纹)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_visitor_nda_records_record` FOREIGN KEY (`visitor_record_id`) REFERENCES `visitor_records`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保密协议签署证据链存证表';
