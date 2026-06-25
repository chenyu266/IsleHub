package com.islehub.product.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchStatusDTO {
    private List<Long> ids;
    private Integer status;
}
