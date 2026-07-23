package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.entity.CartItem;
import com.hsf302.carshowroom.entity.Product;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.repository.CartItemRepository;
import com.hsf302.carshowroom.repository.ProductRepository;
import com.hsf302.carshowroom.service.CartInventoryValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartInventoryValidationServiceImpl implements CartInventoryValidationService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @Override
    public void validateCartItemStock(User user, Product product, Integer currentCartItemId, int additionalQuantity) {
        int otherQuantity = cartItemRepository.findByUserAndProduct(user, product).stream()
                .filter(item -> currentCartItemId == null || !item.getId().equals(currentCartItemId))
                .mapToInt(CartItem::getQuantity)
                .sum();
        
        int totalQuantity = otherQuantity + additionalQuantity;
        
        if (product.getAvailableStock() < totalQuantity) {
            throw new RuntimeException(
                    "Không đủ tồn kho cho " + product.getProductName() +
                            ". Vui lòng giảm số lượng xuống tối đa " +
                            (product.getAvailableStock() - otherQuantity) + "."
            );
        }
    }

    @Override
    public void validateCheckoutStock(List<CartItem> cartItems) {
        Map<Integer, Integer> productQuantities = cartItems.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getId(),
                        Collectors.summingInt(CartItem::getQuantity)
                ));

        for (Map.Entry<Integer, Integer> entry : productQuantities.entrySet()) {
            Product product = productRepository.findById(entry.getKey())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm."));
            if (product.getAvailableStock() < entry.getValue()) {
                throw new RuntimeException("Sản phẩm " + product.getProductName() + 
                        " không đủ tồn kho (yêu cầu " + entry.getValue() + 
                        ", có sẵn " + product.getAvailableStock() + "). Vui lòng điều chỉnh lại giỏ hàng.");
            }
        }
    }
}
