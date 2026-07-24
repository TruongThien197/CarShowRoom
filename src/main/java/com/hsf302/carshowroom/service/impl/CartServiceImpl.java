package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.common.Enums.FulfillmentType;
import com.hsf302.carshowroom.common.Enums.ProductStatus;
import com.hsf302.carshowroom.dto.CartDTO;
import com.hsf302.carshowroom.entity.CartItem;
import com.hsf302.carshowroom.entity.Product;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.exception.InsufficientStockException;
import com.hsf302.carshowroom.exception.MixedFulfillmentException;
import com.hsf302.carshowroom.repository.CartItemRepository;
import com.hsf302.carshowroom.repository.ProductRepository;
import com.hsf302.carshowroom.service.CartInventoryValidationService;
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
    private final CartInventoryValidationService cartInventoryValidationService;

    /** Tạo dữ liệu giỏ hàng gồm danh sách sản phẩm và tổng tạm tính của khách. */
    @Override
    public CartDTO getCart(User user) {
        List<CartItem> items = getCartItems(user);
        CartDTO cart = new CartDTO();
        cart.setItems(items);
        cart.setSubtotal(calculateSubtotal(items));
        return cart;
    }

    /** Thêm sản phẩm vào giỏ qua API rồi trả về trạng thái giỏ mới nhất. */
    @Override
    public CartDTO addItemToCart(User user, Long productId, int quantity, String fulfillmentType) {
        addToCart(user, productId.intValue(), quantity, fulfillmentType);
        return getCart(user);
    }

    /** Cập nhật số lượng một dòng giỏ hàng qua API rồi trả về giỏ mới nhất. */
    @Override
    public CartDTO updateCartItem(User user, Long itemId, int quantity) {
        updateQuantity(user, itemId.intValue(), quantity);
        return getCart(user);
    }

    /** Lấy các dòng sản phẩm hiện có trong giỏ của khách hàng. */
    @Override
    public List<CartItem> getCartItems(User user) {
        return cartItemRepository.findByUser(user);
    }

    /** Thêm sản phẩm giao hàng vào giỏ bằng hình thức mặc định là giao tận nơi. */
    @Override
    @Transactional
    public void addToCart(User user, Integer productId, Integer quantity) {
        addToCart(user, productId, quantity, FulfillmentType.SHIPPING.name());
    }

    /** Thêm sản phẩm theo hình thức nhận hàng và kiểm tra tổng tồn kho của sản phẩm trong giỏ. */
    @Override
    @Transactional
    public void addToCart(User user, Integer productId, Integer quantity, String fulfillmentType) {
        int requestedQuantity = quantity == null || quantity < 1 ? 1 : quantity;
        FulfillmentType resolvedFulfillmentType = resolveFulfillmentType(fulfillmentType);
        validateSingleFulfillment(user, resolvedFulfillmentType);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm."));
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new RuntimeException("Sản phẩm này hiện không khả dụng.");
        }
        validateWorkshopEligibility(product, resolvedFulfillmentType);

        CartItem cartItem = cartItemRepository.findByUserAndProductAndFulfillmentType(user, product, resolvedFulfillmentType).orElseGet(() -> {
            CartItem item = new CartItem();
            item.setUser(user);
            item.setProduct(product);
            item.setFulfillmentType(resolvedFulfillmentType);
            item.setQuantity(0);
            return item;
        });

        int newQuantity = cartItem.getQuantity() + requestedQuantity;
        validateTotalStock(user, product, cartItem.getId(), newQuantity);
        cartItem.setQuantity(newQuantity);
        cartItemRepository.save(cartItem);
    }

    /** Sửa số lượng sản phẩm trong giỏ; xóa dòng giỏ khi số lượng bằng hoặc nhỏ hơn không. */
    @Override
    @Transactional
    public void updateFulfillmentType(User user, Integer cartItemId, String fulfillmentType) {
        CartItem cartItem = getOwnedCartItem(user, cartItemId);
        FulfillmentType targetType = resolveFulfillmentType(fulfillmentType);
        Product product = cartItem.getProduct();
        validateWorkshopEligibility(product, targetType);
        if (cartItem.getFulfillmentType() == targetType) {
            return;
        }

        CartItem existing = cartItemRepository.findByUserAndProductAndFulfillmentType(user, product, targetType)
                .orElse(null);
        if (existing != null && !existing.getId().equals(cartItem.getId())) {
            cartInventoryValidationService.validateCartItemStock(user, product, existing.getId(), existing.getQuantity() + cartItem.getQuantity());
            existing.setQuantity(existing.getQuantity() + cartItem.getQuantity());
            cartItemRepository.save(existing);
            cartItemRepository.delete(cartItem);
            return;
        }
        cartItem.setFulfillmentType(targetType);
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
        validateTotalStock(user, product, cartItem.getId(), quantity);
        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
    }

    /** Xóa toàn bộ sản phẩm trong giỏ của khách hàng. */
    @Override
    @Transactional
    public void clearCart(User user) {
        cartItemRepository.deleteByUser(user);
    }

    /** Xóa một dòng giỏ hàng qua API và trả về giỏ sau khi xóa. */
    @Override
    @Transactional
    public CartDTO removeCartItem(User user, Long itemId) {
        cartItemRepository.delete(getOwnedCartItem(user, itemId.intValue()));
        return getCart(user);
    }

    /** Lấy dòng giỏ hàng và kiểm tra dòng đó thuộc đúng khách hàng đang thao tác. */
    private CartItem getOwnedCartItem(User user, Integer cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng."));

        if (!cartItem.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Sản phẩm này không thuộc giỏ hàng của bạn.");
        }
        return cartItem;
    }

    /** Xóa một dòng giỏ hàng theo mã sau khi kiểm tra quyền sở hữu. */
    @Override
    @Transactional
    public void removeItem(User user, Integer cartItemId) {
        cartItemRepository.delete(getOwnedCartItem(user, cartItemId));
    }

    /** Tính tổng tiền phụ tùng của các dòng giỏ hàng. */
    @Override
    public BigDecimal calculateTotalAmount(List<CartItem> items, BigDecimal shippingFee) {
        BigDecimal subtotal = calculateSubtotal(items);

        if (shippingFee == null) {
            shippingFee = BigDecimal.ZERO;
        }

        return subtotal.add(shippingFee);
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


    /** Cộng dồn số lượng cùng sản phẩm ở mọi kiểu nhận hàng để ngăn vượt tồn kho. */
    private void validateTotalStock(User user, Product product, Integer cartItemId, int desiredQuantity) {
        boolean replacingExistingItem = cartItemId != null;
        int totalQuantity = cartItemRepository.findByUser(user).stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .mapToInt(item -> replacingExistingItem && item.getId().equals(cartItemId)
                        ? desiredQuantity
                        : item.getQuantity())
                .sum();

        if (!replacingExistingItem) {
            totalQuantity += desiredQuantity;
        }
        if (product.getAvailableStock() < totalQuantity) {
            throw new InsufficientStockException(product.getAvailableStock());
        }
    }

    /** Không cho phép trộn giao hàng và lắp tại xưởng trong cùng một giỏ hàng. */
    private void validateSingleFulfillment(User user, FulfillmentType requestedFulfillmentType) {
        boolean hasAnotherFulfillmentType = cartItemRepository.findByUser(user).stream()
                .map(CartItem::getFulfillmentType)
                .filter(java.util.Objects::nonNull)
                .anyMatch(type -> type != requestedFulfillmentType);
        if (hasAnotherFulfillmentType) {
            throw new MixedFulfillmentException();
        }
    }

    /** Chuyển chuỗi hình thức nhận hàng thành enum hợp lệ. */
    private FulfillmentType resolveFulfillmentType(String fulfillmentType) {
        try {
            return FulfillmentType.valueOf((fulfillmentType == null ? "" : fulfillmentType).trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Vui lòng chọn hình thức nhận hàng hợp lệ.");
        }
    }

    private void validateWorkshopEligibility(Product product, FulfillmentType fulfillmentType) {
        if (fulfillmentType == FulfillmentType.AT_WORKSHOP && !product.isInstallationSupported()) {
            throw new RuntimeException("Sản phẩm này không hỗ trợ lắp đặt tại xưởng.");
        }
    }
}
