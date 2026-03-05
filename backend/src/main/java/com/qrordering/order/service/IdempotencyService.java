package com.qrordering.order.service;

import com.qrordering.order.entity.IdempotencyRecord;
import com.qrordering.order.repository.IdempotencyRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Idempotency for order creation: same Idempotency-Key returns the same order (200 OK)
 * instead of creating a duplicate. Record is stored in the same transaction as order creation.
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    /**
     * Returns existing order id for this tenant and key, if any.
     */
    @Transactional(readOnly = true)
    public Optional<Long> findExistingOrderId(String tenantId, String idempotencyKey) {
        if (tenantId == null || idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return idempotencyRecordRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey.trim())
                .map(IdempotencyRecord::getOrderId);
    }

    /**
     * Stores idempotency record. Must be called within the same transaction as order creation.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void storeRecord(String tenantId, String idempotencyKey, Long orderId) {
        IdempotencyRecord record = IdempotencyRecord.builder()
                .tenantId(tenantId)
                .idempotencyKey(idempotencyKey.trim())
                .orderId(orderId)
                .createdAt(Instant.now())
                .build();
        idempotencyRecordRepository.save(record);
    }
}
