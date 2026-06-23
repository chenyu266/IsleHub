CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `real_name` VARCHAR(50),
    `phone` VARCHAR(20),
    `role` VARCHAR(20) NOT NULL DEFAULT 'operator',
    `status` TINYINT NOT NULL DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX `uk_username` (`username`)
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
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `product_sku` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `product_id` BIGINT NOT NULL,
    `sku_code` VARCHAR(100) NOT NULL,
    `spec` VARCHAR(200),
    `price` DECIMAL(10,2) NOT NULL DEFAULT 0,
    `stock` INT NOT NULL DEFAULT 0,
    `status` TINYINT NOT NULL DEFAULT 1,
    UNIQUE INDEX `uk_sku_code` (`sku_code`)
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
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX `uk_order_no` (`order_no`)
);

CREATE TABLE IF NOT EXISTS `order_item` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `sku_id` BIGINT NOT NULL,
    `product_name` VARCHAR(200),
    `sku_spec` VARCHAR(200),
    `price` DECIMAL(10,2) NOT NULL,
    `quantity` INT NOT NULL
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
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP
);
