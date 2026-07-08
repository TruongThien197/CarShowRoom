package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.common.Enums.FulfillmentType;
import com.hsf302.carshowroom.common.Enums.ProductStatus;
import com.hsf302.carshowroom.dto.CartDTO;
import com.hsf302.carshowroom.entity.CartItem;
import com.hsf302.carshowroom.entity.Product;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.repository.CartItemRepository;
import com.hsf302.carshowroom.repository.ProductRepository;
import com.hsf302.carshowroom.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @Override
    public CartDTO getCart(User user) {
        List<CartItem> items = getCartItems(user);
        CartDTO cart = new CartDTO();
        cart.setItems(items);
        cart.setSubtotal(calculateSubtotal(items));
        return cart;
    }

    @Override
    public CartDTO addItemToCart(User user, Long productId, int quantity, String fulfillmentType) {
        addToCart(user, productId.intValue(), quantity, fulfillmentType);
        return getCart(user);
    }

    @Override
    public CartDTO updateCartItem(User user, Long itemId, int quantity) {
        updateQuantity(user, itemId.intValue(), quantity);
        return getCart(user);
    }

    @Override
    public List<CartItem> getCartItems(User user) {
        return cartItemRepository.findByUser(user);
    }

    @Override
    @Transactional
    public void addToCart(User user, Integer productId, Integer quantity) {
        addToCart(user, productId, quantity, FulfillmentType.SHIPPING.name());
    }

    @Override
    @Transactional
    public void addToCart(User user, Integer productId, Integer quantity, String fulfillmentType) {
        int requestedQuantity = quantity == null || quantity < 1 ? 1 : quantity;
        FulfillmentType resolvedFulfillmentType = resolveFulfillmentType(fulfillmentType);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm."));
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new RuntimeException("Sản phẩm hiện không khả dụng.");
        }

        CartItem cartItem = cartItemRepository.findByUserAndProductAndFulfillmentType(user, product, resolvedFulfillmentType).orElseGet(() -> {
            CartItem item = new CartItem();
            item.setUser(user);
            item.setProduct(product);
            item.setFulfillmentType(resolvedFulfillmentType);
            item.setQuantity(0);
            return item;
        });

        int newQuantity = cartItem.getQuantity() + requestedQuantity;
        validateStock(product, newQuantity);
        cartItem.setQuantity(newQuantity);
        cartItemRepository.save(cartItem);
    }

    @Override
    @Transactional
    public void updateQuantity(User user, Integer cartItemId, Integer quantity) {
        CartItem cartItem = getOwnedCartItem(user, cartItemId);
        if (quantity == null || quantity <= 0) {
            cartItemRepository.delete(cartItem);
            return;
        }
        Product product = cartItem.getProduct();
        validateStock(product, quantity);
        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
    }

    @Override
    @Transactional
    public void clearCart(User user) {
        cartItemRepository.deleteByUser(user);
    }

    @Override
    @Transactional
    public CartDTO removeCartItem(User user, Long itemId) {
        cartItemRepository.delete(getOwnedCartItem(user, itemId.intValue()));
        return getCart(user);
    }

    private CartItem getOwnedCartItem(User user, Integer cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng."));

        if (!cartItem.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Sản phẩm này không thuộc giỏ hàng của bạn.");
        }
        return cartItem;
    }

    @Override
    @Transactional
    public void removeItem(User user, Integer cartItemId) {
        cartItemRepository.delete(getOwnedCartItem(user, cartItemId));
    }

    @Override
    public BigDecimal calculateSubtotal(List<CartItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(item -> item.getProduct().getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    private void validateStock(Product product, int quantity) {
        if (product.getAvailableStock() < quantity) {
            throw new RuntimeException(
                    "Số lượng sản phẩm " + product.getProductName() +
                            " trong kho không đủ. Vui lòng giảm số lượng xuống còn " +
                            product.getAvailableStock() + " hoặc ít hơn."
            );
        }
    }

    private FulfillmentType resolveFulfillmentType(String fulfillmentType) {
        try {
            return FulfillmentType.valueOf((fulfillmentType == null ? "" : fulfillmentType).trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Vui lòng chọn hình thức nhận hàng hợp lệ.");
        }
    }
}
