package com.yong831.study.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.yong831.study.common.dto.PageResponse;
import com.yong831.study.common.exception.BusinessException;
import com.yong831.study.common.exception.ErrorCode;
import com.yong831.study.product.domain.Product;
import com.yong831.study.product.dto.ProductCreateRequest;
import com.yong831.study.product.dto.ProductResponse;
import com.yong831.study.product.dto.ProductUpdateRequest;
import com.yong831.study.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository productRepository;

    @InjectMocks
    ProductService productService;

    private Product product(String name) {
        return Product.builder().name(name).description("desc").price(new BigDecimal("1000.00")).stock(10).build();
    }

    @Test
    void create_savesAndReturnsResponse() {
        ProductCreateRequest request = new ProductCreateRequest("Keyboard", "desc", new BigDecimal("1000.00"), 10);
        given(productRepository.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.create(request);

        assertThat(response.name()).isEqualTo("Keyboard");
        assertThat(response.price()).isEqualByComparingTo("1000.00");
        then(productRepository).should().save(any(Product.class));
    }

    @Test
    void get_returnsProduct() {
        given(productRepository.findById(1L)).willReturn(Optional.of(product("Mouse")));

        assertThat(productService.get(1L).name()).isEqualTo("Mouse");
    }

    @Test
    void get_throwsWhenMissing() {
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.get(99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void search_withName_usesNameFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        given(productRepository.findByNameContainingIgnoreCase("key", pageable))
                .willReturn(new PageImpl<>(List.of(product("Keyboard")), pageable, 1));

        PageResponse<ProductResponse> result = productService.search("key", pageable);

        assertThat(result.content()).extracting(ProductResponse::name).containsExactly("Keyboard");
        assertThat(result.totalElements()).isEqualTo(1);
        then(productRepository).should(never()).findAll(any(Pageable.class));
    }

    @Test
    void search_withoutName_returnsAll() {
        Pageable pageable = PageRequest.of(0, 10);
        given(productRepository.findAll(pageable))
                .willReturn(new PageImpl<>(List.of(product("A"), product("B")), pageable, 2));

        assertThat(productService.search(null, pageable).content()).hasSize(2);
    }

    @Test
    void update_changesFields() {
        Product existing = product("Old");
        given(productRepository.findById(1L)).willReturn(Optional.of(existing));

        ProductResponse response = productService.update(1L,
                new ProductUpdateRequest("New", "new desc", new BigDecimal("2000.00"), 5));

        assertThat(response.name()).isEqualTo("New");
        assertThat(existing.getStock()).isEqualTo(5);
    }

    @Test
    void delete_removesProduct() {
        Product existing = product("Gone");
        given(productRepository.findById(1L)).willReturn(Optional.of(existing));

        productService.delete(1L);

        then(productRepository).should().delete(existing);
    }

    @Test
    void delete_throwsWhenMissing() {
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(99L)).isInstanceOf(BusinessException.class);
        then(productRepository).should(never()).delete(any(Product.class));
    }
}
