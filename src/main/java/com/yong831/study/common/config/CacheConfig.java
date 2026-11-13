package com.yong831.study.common.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * The cache backend is chosen by {@code spring.cache.type}: Redis at runtime, in-memory in tests.
 * Cached values use JDK serialization, so cached DTOs implement {@link java.io.Serializable}.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PRODUCT_CACHE = "product";
}
