package com.yong831.study.product.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.yong831.study.common.config.JpaConfig;
import com.yong831.study.product.domain.Product;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import(JpaConfig.class)
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    ProductRepository productRepository;

    private Product product(String name) {
        return Product.builder().name(name).price(new BigDecimal("1500.50")).stock(3).build();
    }

    @Test
    void save_setsIdAndAuditFields() {
        Product saved = productRepository.saveAndFlush(product("Monitor"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getPrice()).isEqualByComparingTo("1500.50");
    }

    @Test
    void findByNameContainingIgnoreCase_matchesPartialName() {
        productRepository.save(product("Gaming Keyboard"));
        productRepository.save(product("Office keyboard"));
        productRepository.save(product("Mouse"));

        Page<Product> page = productRepository.findByNameContainingIgnoreCase("KEYBOARD", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Product::getName)
                .containsExactlyInAnyOrder("Gaming Keyboard", "Office keyboard");
    }
}
