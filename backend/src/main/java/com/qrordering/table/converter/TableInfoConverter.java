package com.qrordering.table.converter;

import com.qrordering.table.dto.response.TableInfoResponse;
import com.qrordering.table.entity.TableInfo;
import org.springframework.stereotype.Component;

@Component
public class TableInfoConverter {

    public TableInfoResponse toResponse(TableInfo entity) {
        if (entity == null) return null;
        return TableInfoResponse.builder()
                .id(entity.getId())
                .tableNumber(entity.getTableNumber())
                .qrCodeUrl(entity.getQrCodeUrl())
                .seats(entity.getSeats())
                .status(entity.getStatus() != null ? entity.getStatus().getCode() : null)
                .build();
    }
}
