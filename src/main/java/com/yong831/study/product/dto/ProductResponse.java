package com.yong831.study.product.dto;

import com.yong831.study.product.domain.Product;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Serializable because it is stored in the Redis cache. */
public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        int stock,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) implements Serializable {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(),
                product.getPrice(), product.getStock(), product.getCreatedAt(), product.getUpdatedAt());
    }
}
