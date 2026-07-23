package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.common.Enums.FulfillmentType;
import com.hsf302.carshowroom.common.Enums.ProductStatus;
import com.hsf302.carshowroom.entity.CartItem;
import com.hsf302.carshowroom.entity.Product;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.exception.InsufficientStockException;
import com.hsf302.carshowroom.exception.MixedFulfillmentException;
import com.hsf302.carshowroom.repository.CartItemRepository;
import com.hsf302.carshowroom.repository.ProductRepository;
import com.hsf302.carshowroom.service.impl.CartServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartStockValidationTests {
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @InjectMocks private CartServiceImpl cartService;

    @Test
    void addingMoreOfTheSameProductIncludesCurrentCartQuantityInStockValidation() {
        User user = new User();
        user.setId(1);

        Product product = new Product();
        product.setId(10);
        product.setStatus(ProductStatus.ACTIVE);
        product.setPhysicalStock(3);
        product.setReservedStock(0);

        CartItem shippingItem = new CartItem();
        shippingItem.setId(100);
        shippingItem.setUser(user);
        shippingItem.setProduct(product);
        shippingItem.setQuantity(2);
        shippingItem.setFulfillmentType(FulfillmentType.SHIPPING);

        when(productRepository.findById(10)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUserAndProductAndFulfillmentType(
                user, product, FulfillmentType.SHIPPING)).thenReturn(Optional.of(shippingItem));
        when(cartItemRepository.findByUser(user)).thenReturn(List.of(shippingItem));

        assertThrows(InsufficientStockException.class,
                () -> cartService.addToCart(user, 10, 2, FulfillmentType.SHIPPING.name()));

        verify(cartItemRepository, never()).save(org.mockito.ArgumentMatchers.any(CartItem.class));
    }

    @Test
    void cartCannotContainShippingAndWorkshopItemsTogether() {
        User user = new User();
        user.setId(1);
        Product product = new Product();
        product.setId(10);
        CartItem shippingItem = new CartItem();
        shippingItem.setId(100);
        shippingItem.setUser(user);
        shippingItem.setProduct(product);
        shippingItem.setQuantity(1);
        shippingItem.setFulfillmentType(FulfillmentType.SHIPPING);
        when(cartItemRepository.findByUser(user)).thenReturn(List.of(shippingItem));

        assertThrows(MixedFulfillmentException.class,
                () -> cartService.addToCart(user, 10, 1, FulfillmentType.AT_WORKSHOP.name()));
    }
}
