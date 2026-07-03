package com.hsf302.carshowroom.service.impl;

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
    public List<CartItem> getCartItems(User user) {
        return cartItemRepository.findByUser(user);
    }


    @Override
    @Transactional
    public void addToCart(User user, Integer productId, Integer quantity) {

        int requestedQuantity = quantity == null || quantity < 1 ? 1 : quantity;
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm."));
        if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
            throw new RuntimeException("Sản phẩm hiện không khả dụng.");
        }

        CartItem cartItem = cartItemRepository.findByUserAndProduct(user, product).orElseGet(() -> {
            CartItem item = new CartItem();
            item.setUser(user);
            item.setProduct(product);
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
        Product product = productRepository.findById(cartItem.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm liên kết."));

        validateStock(product, quantity);
        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
    }


    @Override
    @Transactional
    public void clearCart(User user) {
        cartItemRepository.deleteByUser(user);
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
        if (product.getStockQuantity() < quantity) {
            throw new RuntimeException(
                    "Số lượng sản phẩm " + product.getProductName() +
                            " trong kho không đủ. Vui lòng giảm số lượng xuống còn " +
                            product.getStockQuantity() + " hoặc ít hơn."
            );
        }
    }
}
