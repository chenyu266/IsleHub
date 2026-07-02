package com.islehub.product.dto;

import lombok.Data;
import java.util.List;

/**
 * 批量状态更新请求 DTO，包含 ID 列表和目标状态。
 */
@Data
public class BatchStatusDTO {
    private List<Long> ids;
    private Integer status;
}
