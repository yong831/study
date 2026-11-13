package com.yong831.study.product.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yong831.study.common.dto.PageResponse;
import com.yong831.study.common.exception.BusinessException;
import com.yong831.study.common.exception.ErrorCode;
import com.yong831.study.common.web.RequestIdFilter;
import com.yong831.study.product.dto.ProductCreateRequest;
import com.yong831.study.product.dto.ProductResponse;
import com.yong831.study.product.dto.ProductUpdateRequest;
import com.yong831.study.product.service.ProductService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProductService productService;

    private static final String VALID_BODY = """
            {"name":"Keyboard","description":"Mechanical","price":1000.00,"stock":10}
            """;

    private ProductResponse response(long id, String name) {
        LocalDateTime now = LocalDateTime.now();
        return new ProductResponse(id, name, "Mechanical", new BigDecimal("1000.00"), 10, now, now);
    }

    @Test
    void create_returns201WithLocation() throws Exception {
        given(productService.create(any(ProductCreateRequest.class))).willReturn(response(1L, "Keyboard"));

        mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/products/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Keyboard"));
    }

    @Test
    void create_invalidBody_returns400WithFieldErrors() throws Exception {
        String invalid = """
                {"name":"","price":-1,"stock":-5}
                """;

        mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.price").exists())
                .andExpect(jsonPath("$.errors.stock").exists());
    }

    @Test
    void create_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void get_returnsProduct() throws Exception {
        given(productService.get(1L)).willReturn(response(1L, "Keyboard"));

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Keyboard"));
    }

    @Test
    void get_missing_returns404() throws Exception {
        given(productService.get(99L)).willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found: id=99"));

        mockMvc.perform(get("/api/v1/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void search_returnsPage() throws Exception {
        given(productService.search(eq("key"), any(Pageable.class)))
                .willReturn(new PageResponse<>(List.of(response(1L, "Keyboard")), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/products").param("name", "key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Keyboard"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void update_returnsUpdated() throws Exception {
        given(productService.update(eq(1L), any(ProductUpdateRequest.class))).willReturn(response(1L, "Keyboard"));

        mockMvc.perform(put("/api/v1/products/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/products/1"))
                .andExpect(status().isNoContent());

        then(productService).should().delete(1L);
    }

    @Test
    void delete_missing_returns404() throws Exception {
        willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found: id=99"))
                .given(productService).delete(99L);

        mockMvc.perform(delete("/api/v1/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void requestId_isPropagatedFromHeader() throws Exception {
        given(productService.get(1L)).willReturn(response(1L, "Keyboard"));

        mockMvc.perform(get("/api/v1/products/1").header(RequestIdFilter.HEADER, "trace-123"))
                .andExpect(header().string(RequestIdFilter.HEADER, "trace-123"));
    }

    @Test
    void requestId_isGeneratedWhenMissing() throws Exception {
        given(productService.get(1L)).willReturn(response(1L, "Keyboard"));

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(header().exists(RequestIdFilter.HEADER));
    }
}
