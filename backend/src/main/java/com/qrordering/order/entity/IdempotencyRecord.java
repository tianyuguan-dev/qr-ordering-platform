package com.qrordering.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "idempotency_record", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "tenant_id", "idempotency_key" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
