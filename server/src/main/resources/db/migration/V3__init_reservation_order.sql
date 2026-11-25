-- P3 预约 / 停车记录 / 订单 / 支付
CREATE TABLE reservation
(
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    user_id    BIGINT   NOT NULL,
    lot_id     BIGINT   NOT NULL,
    space_id   BIGINT   NOT NULL,
    start_time DATETIME NOT NULL COMMENT '预约开始（整点）',
    end_time   DATETIME NOT NULL COMMENT '预约结束',
    status     TINYINT  NOT NULL DEFAULT 0 COMMENT '0待使用 1已完成 2已取消 3已过期',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_res_space_start (space_id, start_time),
    KEY idx_res_user (user_id),
    KEY idx_res_space_time (space_id, start_time, end_time),
    CONSTRAINT fk_res_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
    CONSTRAINT fk_res_lot FOREIGN KEY (lot_id) REFERENCES parking_lot (id) ON DELETE CASCADE,
    CONSTRAINT fk_res_space FOREIGN KEY (space_id) REFERENCES parking_space (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='车位预约';

CREATE TABLE parking_record
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    user_id        BIGINT      NOT NULL,
    lot_id         BIGINT      NOT NULL,
    space_id       BIGINT      NOT NULL,
    reservation_id BIGINT      NULL COMMENT '关联预约',
    plate_no       VARCHAR(10) NOT NULL COMMENT '车牌号',
    entry_time     DATETIME    NOT NULL,
    exit_time      DATETIME    NULL,
    status         TINYINT     NOT NULL DEFAULT 0 COMMENT '0停车中 1已离场',
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_record_user (user_id),
    KEY idx_record_space (space_id, status),
    CONSTRAINT fk_record_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
    CONSTRAINT fk_record_lot FOREIGN KEY (lot_id) REFERENCES parking_lot (id) ON DELETE CASCADE,
    CONSTRAINT fk_record_space FOREIGN KEY (space_id) REFERENCES parking_space (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='停车记录';

CREATE TABLE parking_order
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    order_no       VARCHAR(32) NOT NULL,
    user_id        BIGINT      NOT NULL,
    record_id      BIGINT      NOT NULL,
    lot_id         BIGINT      NOT NULL,
    amount_cents   INT         NOT NULL COMMENT '金额（分）',
    billed_minutes INT         NOT NULL COMMENT '计费时长（分钟）',
    free_applied   TINYINT     NOT NULL DEFAULT 0 COMMENT '是否命中免费时长',
    status         TINYINT     NOT NULL DEFAULT 0 COMMENT '0待支付 1已支付 2已取消',
    paid_at        DATETIME    NULL,
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_order_user (user_id),
    CONSTRAINT fk_order_record FOREIGN KEY (record_id) REFERENCES parking_record (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_lot FOREIGN KEY (lot_id) REFERENCES parking_lot (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='停车订单';

CREATE TABLE payment_record
(
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    order_id     BIGINT      NOT NULL,
    payment_no   VARCHAR(40) NOT NULL,
    channel      VARCHAR(20) NOT NULL COMMENT 'MOCK/WECHAT/ALIPAY',
    amount_cents INT         NOT NULL,
    status       TINYINT     NOT NULL DEFAULT 0 COMMENT '0成功 1失败',
    paid_at      DATETIME    NOT NULL,
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_no (payment_no),
    KEY idx_payment_order (order_id),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES parking_order (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='支付流水';
