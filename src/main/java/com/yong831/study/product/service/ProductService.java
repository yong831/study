package com.yong831.study.product.service;

import com.yong831.study.common.config.CacheConfig;
import com.yong831.study.common.dto.PageResponse;
import com.yong831.study.common.exception.BusinessException;
import com.yong831.study.common.exception.ErrorCode;
import com.yong831.study.product.domain.Product;
import com.yong831.study.product.dto.ProductCreateRequest;
import com.yong831.study.product.dto.ProductResponse;
import com.yong831.study.product.dto.ProductUpdateRequest;
import com.yong831.study.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        Product saved = productRepository.save(request.toEntity());
        log.info("Product created: id={}", saved.getId());
        return ProductResponse.from(saved);
    }

    @Cacheable(cacheNames = CacheConfig.PRODUCT_CACHE, key = "#id")
    public ProductResponse get(Long id) {
        return ProductResponse.from(findProduct(id));
    }

    public PageResponse<ProductResponse> search(String name, Pageable pageable) {
        Page<Product> page = StringUtils.hasText(name)
                ? productRepository.findByNameContainingIgnoreCase(name, pageable)
                : productRepository.findAll(pageable);
        return PageResponse.from(page.map(ProductResponse::from));
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PRODUCT_CACHE, key = "#id")
    public ProductResponse update(Long id, ProductUpdateRequest request) {
        Product product = findProduct(id);
        product.update(request.name(), request.description(), request.price(), request.stock());
        productRepository.flush(); // apply auditing (updatedAt) before building the response
        log.info("Product updated: id={}", id);
        return ProductResponse.from(product);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PRODUCT_CACHE, key = "#id")
    public void delete(Long id) {
        productRepository.delete(findProduct(id));
        log.info("Product deleted: id={}", id);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found: id=" + id));
    }
}
