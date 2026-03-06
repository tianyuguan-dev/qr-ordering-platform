package com.qrordering.sse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload sent over SSE: type (e.g. ORDER_CREATED) + data object.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SseMessage {

    private String type;
    private Object data;
}
