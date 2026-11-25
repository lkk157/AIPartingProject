-- P2 停车场 / 车位 / 收费规则
CREATE TABLE parking_lot
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL COMMENT '停车场名称',
    address     VARCHAR(255) NOT NULL COMMENT '地址',
    area        VARCHAR(50)  NOT NULL COMMENT '区域',
    capacity    INT          NOT NULL DEFAULT 0 COMMENT '车位容量',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1营业 0停用',
    description VARCHAR(255) NULL COMMENT '简介',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_lot_area (area),
    KEY idx_lot_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='停车场';

CREATE TABLE parking_fee_rule
(
    id                     BIGINT      NOT NULL AUTO_INCREMENT,
    lot_id                 BIGINT      NOT NULL,
    rule_name              VARCHAR(50) NOT NULL COMMENT '规则名称',
    free_minutes           INT         NOT NULL DEFAULT 15 COMMENT '免费时长（分钟）',
    first_hour_cents       INT         NOT NULL COMMENT '首小时费用（分）',
    extra_per_hour_cents   INT         NOT NULL COMMENT '超出后每小时费用（分）',
    daily_cap_cents        INT         NOT NULL COMMENT '24小时封顶（分）',
    enabled                TINYINT     NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    created_at             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_fee_lot (lot_id),
    CONSTRAINT fk_fee_lot FOREIGN KEY (lot_id) REFERENCES parking_lot (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='停车收费规则';

CREATE TABLE parking_space
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    lot_id     BIGINT      NOT NULL,
    space_no   VARCHAR(20) NOT NULL COMMENT '车位编号',
    zone       VARCHAR(20) NULL COMMENT '分区',
    type       VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/CHARGING/DISABLED',
    status     TINYINT     NOT NULL DEFAULT 0 COMMENT '0空闲 1占用 2预约锁定 3维护',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_space_lot_no (lot_id, space_no),
    KEY idx_space_lot_status (lot_id, status),
    CONSTRAINT fk_space_lot FOREIGN KEY (lot_id) REFERENCES parking_lot (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='车位';

CREATE TABLE parking_space_status_log
(
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    space_id    BIGINT      NOT NULL,
    from_status TINYINT     NOT NULL,
    to_status   TINYINT     NOT NULL,
    reason      VARCHAR(100) NULL,
    operator_id BIGINT      NULL,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_log_space (space_id),
    CONSTRAINT fk_log_space FOREIGN KEY (space_id) REFERENCES parking_space (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='车位状态变更流水';

CREATE TABLE parking_lot_admin
(
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    lot_id     BIGINT   NOT NULL,
    user_id    BIGINT   NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_lot_admin (lot_id, user_id),
    KEY idx_lot_admin_user (user_id),
    CONSTRAINT fk_lot_admin_lot FOREIGN KEY (lot_id) REFERENCES parking_lot (id) ON DELETE CASCADE,
    CONSTRAINT fk_lot_admin_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='停车场管理员关联';
