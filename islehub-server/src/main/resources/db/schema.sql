CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `email` varchar(64) NOT NULL,
    `username` VARCHAR(50) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `phone` VARCHAR(20),
    `avatar` varchar(255) DEFAULT NULL COMMENT '头像地址',
    `last_login_time` datetime DEFAULT NULL,
    `last_login_ip` varchar(45) DEFAULT NULL,
    `delete_flag` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
    `role` VARCHAR(20) NOT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_email` (`email`),
    UNIQUE INDEX `uk_username` (`username`)
);

CREATE TABLE IF NOT EXISTS `category` (
   `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
   `name` VARCHAR(100) NOT NULL,
   `parent_id` BIGINT NOT NULL DEFAULT 0,
   `level` TINYINT NOT NULL DEFAULT 0 COMMENT '1=一级 2=二级 3=三级',
   INDEX `idx_category_parent` (`parent_id`, `level`),
   INDEX `idx_category_level` (`level`)
);


CREATE TABLE IF NOT EXISTS `product` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(200) NOT NULL,
    `category_id` BIGINT NOT NULL DEFAULT 0,
    `description` TEXT,
    `main_image` VARCHAR(500),
    `images` TEXT,
    `status` TINYINT NOT NULL DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_product_list` (`status`, `category_id`, `created_at`),
    INDEX `idx_product_created` (`created_at`)
);

CREATE TABLE IF NOT EXISTS `product_sku` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `product_id` BIGINT NOT NULL,
    `sku_code` VARCHAR(100) NOT NULL,
    `spec` VARCHAR(200),
    `price` DECIMAL(10,2) NOT NULL DEFAULT 0,
    `stock` INT NOT NULL DEFAULT 0,
    `status` TINYINT NOT NULL DEFAULT 1,
    UNIQUE INDEX `uk_sku_code` (`sku_code`),
    INDEX `idx_sku_product` (`product_id`, `status`)
);

CREATE TABLE IF NOT EXISTS `order_table` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_no` VARCHAR(32) NOT NULL,
    `user_id` BIGINT NOT NULL,
    `total_amount` DECIMAL(10,2) NOT NULL DEFAULT 0,
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending',
    `receiver_name` VARCHAR(50),
    `receiver_phone` VARCHAR(20),
    `receiver_address` VARCHAR(500),
    `remark` VARCHAR(500),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX `uk_order_no` (`order_no`),
    INDEX `idx_order_user` (`user_id`, `status`, `created_at`),
    INDEX `idx_order_status` (`status`, `created_at`)
);

CREATE TABLE IF NOT EXISTS `order_item` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `sku_id` BIGINT NOT NULL,
    `product_name` VARCHAR(200),
    `sku_spec` VARCHAR(200),
    `price` DECIMAL(10,2) NOT NULL,
    `quantity` INT NOT NULL,
    INDEX `idx_item_order` (`order_id`)
);

CREATE TABLE IF NOT EXISTS `order_shipping` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL,
    `carrier` VARCHAR(50),
    `tracking_no` VARCHAR(100),
    `shipped_at` DATETIME,
    `delivered_at` DATETIME,
    UNIQUE INDEX `uk_shipping_order` (`order_id`)
);

CREATE TABLE IF NOT EXISTS `address` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `receiver_name` VARCHAR(50) NOT NULL,
    `receiver_phone` VARCHAR(20) NOT NULL,
    `province` VARCHAR(50),
    `city` VARCHAR(50),
    `district` VARCHAR(50),
    `detail` VARCHAR(200) NOT NULL,
    `is_default` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_address_user` (`user_id`, `is_default`)
);

CREATE TABLE IF NOT EXISTS `china_region` (
                                              `code` INT PRIMARY KEY,
                                              `name` VARCHAR(50) NOT NULL,
                                              `parent_code` INT NOT NULL DEFAULT 0,
                                              `level` TINYINT NOT NULL COMMENT '1=省 2=市 3=区县',
                                              INDEX `idx_region_parent` (`parent_code`, `level`),
                                              INDEX `idx_region_name` (`name`, `level`)
);

