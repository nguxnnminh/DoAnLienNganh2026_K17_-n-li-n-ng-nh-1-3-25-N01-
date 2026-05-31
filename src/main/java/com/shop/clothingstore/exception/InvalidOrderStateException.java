package com.shop.clothingstore.exception;

import com.shop.clothingstore.entity.OrderStatus;
import java.util.Set;

public class InvalidOrderStateException extends AppException {
    public InvalidOrderStateException(OrderStatus from, OrderStatus to, Set<OrderStatus> allowed) {
        super(String.format(
            "Cannot transition order from %s to %s. Valid transitions: %s",
            from, to, allowed));
    }
}
