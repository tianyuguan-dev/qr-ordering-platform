package com.qrordering.menu.converter;

import com.qrordering.menu.enums.MenuItemStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MenuItemStatusConverter implements AttributeConverter<MenuItemStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(MenuItemStatus status) {
        return status == null ? null : status.getCode();
    }

    @Override
    public MenuItemStatus convertToEntityAttribute(Integer code) {
        return code == null ? null : MenuItemStatus.fromCode(code);
    }
}
