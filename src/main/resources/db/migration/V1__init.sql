-- ============================================================
-- RBAC Admin Scaffold - schema migration V1
--
-- Executed automatically by Flyway on application startup.
-- Requires an existing, empty MySQL database, for example:
--   CREATE DATABASE rbac_admin_scaffold DEFAULT CHARACTER SET utf8mb4;
--
-- Seed data (base permissions, default organization, system roles and
-- the default admin account) is inserted idempotently by
-- com.example.rbac.bootstrap.DataInitializer after Flyway finishes.
-- ============================================================

SET NAMES utf8mb4;

CREATE TABLE t_organization (
    id          INT          NOT NULL AUTO_INCREMENT,
    org_code    VARCHAR(50)  NOT NULL,
    org_name    VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    status      INT          NOT NULL DEFAULT 1 COMMENT '1=enabled, 0=disabled',
    create_time DATETIME     NULL,
    update_time DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_organization_code (org_code),
    UNIQUE KEY uk_organization_name (org_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '组织';

CREATE TABLE t_user (
    id              INT          NOT NULL AUTO_INCREMENT,
    username        VARCHAR(50)  NOT NULL,
    password        VARCHAR(100) NOT NULL COMMENT 'BCrypt hash',
    real_name       VARCHAR(50)  NULL,
    org_id          INT          NOT NULL,
    status          INT          NOT NULL DEFAULT 1 COMMENT '1=enabled, 0=disabled',
    last_login_time DATETIME     NULL,
    create_time     DATETIME     NULL,
    update_time     DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    KEY idx_user_org (org_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户';

CREATE TABLE t_role (
    id          INT          NOT NULL AUTO_INCREMENT,
    role_code   VARCHAR(50)  NOT NULL,
    role_name   VARCHAR(50)  NOT NULL,
    description VARCHAR(500) NULL,
    org_id      INT          NULL COMMENT 'NULL = global role',
    status      INT          NOT NULL DEFAULT 1 COMMENT '1=enabled, 0=disabled',
    create_time DATETIME     NULL,
    update_time DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code_org (role_code, org_id),
    KEY idx_role_org (org_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色';

CREATE TABLE t_user_role (
    id      INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_user_role_role (role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户-角色关联';

CREATE TABLE t_permission (
    id              INT          NOT NULL AUTO_INCREMENT,
    permission_code VARCHAR(100) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    status          INT          NOT NULL DEFAULT 1 COMMENT '1=enabled, 0=disabled',
    create_time     DATETIME     NULL,
    update_time     DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_permission_code (permission_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '权限';

CREATE TABLE t_role_permission (
    id            INT NOT NULL AUTO_INCREMENT,
    role_id       INT NOT NULL,
    permission_id INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY idx_role_permission_permission (permission_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色-权限关联';
