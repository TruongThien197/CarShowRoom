package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.common.Enums.ProductStatus;
import com.hsf302.carshowroom.entity.Category;
import com.hsf302.carshowroom.entity.Product;
import com.hsf302.carshowroom.repository.ProductRepository;
import com.hsf302.carshowroom.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {
    @Mock private ProductRepository productRepository;
    @InjectMocks private ProductServiceImpl productService;

    @Test
    void createProductTrimsNameAndSku() {
        Product product = validProduct("  Brake pad  ", "  SKU-001  ");
        when(productRepository.existsBySkuIgnoreCase("SKU-001")).thenReturn(false);
        when(productRepository.save(product)).thenReturn(product);

        Product result = productService.createProduct(product);

        assertEquals("Brake pad", result.getName());
        assertEquals("SKU-001", result.getSku());
        assertEquals(ProductStatus.ACTIVE, result.getStatus());
        verify(productRepository).save(product);
    }

    @Test
    void createProductRejectsDuplicateSku() {
        Product product = validProduct("Brake pad", "SKU-001");
        when(productRepository.existsBySkuIgnoreCase("SKU-001")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> productService.createProduct(product));
        verify(productRepository, never()).save(product);
    }

    @Test
    void createProductRejectsNegativePrice() {
        Product product = validProduct("Brake pad", "SKU-001");
        product.setPrice(BigDecimal.valueOf(-1));
        when(productRepository.existsBySkuIgnoreCase("SKU-001")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> productService.createProduct(product));
        verify(productRepository, never()).save(product);
    }

    @Test
    void updateProductCannotLowerStockBelowReservedStock() {
        Product existing = validProduct("Brake pad", "SKU-001");
        existing.setId(10);
        existing.setReservedStock(5);
        Product updated = validProduct("Brake pad", "SKU-001");
        updated.setPhysicalStock(4);
        when(productRepository.findById(10)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySkuIgnoreCaseAndIdNot("SKU-001", 10)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> productService.updateProduct(10, updated));
        verify(productRepository, never()).save(existing);
    }

    @Test
    void deleteProductSetsInactiveInsteadOfDeleting() {
        Product product = validProduct("Brake pad", "SKU-001");
        product.setId(10);
        when(productRepository.findById(10)).thenReturn(Optional.of(product));

        productService.deleteProduct(10);

        assertEquals(ProductStatus.INACTIVE, product.getStatus());
        verify(productRepository).save(product);
        verify(productRepository, never()).deleteById(10);
    }

    private Product validProduct(String name, String sku) {
        Product product = new Product();
        product.setCategory(new Category());
        product.setName(name);
        product.setSku(sku);
        product.setPrice(BigDecimal.valueOf(100));
        product.setPhysicalStock(10);
        return product;
    }
}
