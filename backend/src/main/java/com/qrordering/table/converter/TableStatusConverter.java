package com.qrordering.table.converter;

import com.qrordering.table.enums.TableStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TableStatusConverter implements AttributeConverter<TableStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(TableStatus status) {
        return status == null ? null : status.getCode();
    }

    @Override
    public TableStatus convertToEntityAttribute(Integer code) {
        return code == null ? null : TableStatus.fromCode(code);
    }
}
