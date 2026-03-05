package com.qrordering.common.exception;

import lombok.Getter;

/**
 * Thrown when idempotency record insert fails due to duplicate key (another request already created the order).
 * Controller should return 200 with the existing order.
 */
@Getter
public class IdempotencyConflictException extends RuntimeException {

    private final String tenantId;
    private final String idempotencyKey;

    public IdempotencyConflictException(String tenantId, String idempotencyKey) {
        super("Order already created for this idempotency key");
        this.tenantId = tenantId;
        this.idempotencyKey = idempotencyKey;
    }
}
