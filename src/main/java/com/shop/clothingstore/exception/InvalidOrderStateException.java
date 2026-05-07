package com.shop.clothingstore.exception;

import com.shop.clothingstore.entity.OrderStatus;
import java.util.Set;

public class InvalidOrderStateException extends AppException {
    public InvalidOrderStateException(OrderStatus from, OrderStatus to, Set<OrderStatus> allowed) {
        super(String.format(
            "Không thể chuyển trạng thái đơn hàng từ %s sang %s. Trạng thái hợp lệ: %s",
            from, to, allowed));
    }
}
