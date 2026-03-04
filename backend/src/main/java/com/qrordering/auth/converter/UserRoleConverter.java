package com.qrordering.auth.converter;

import com.qrordering.auth.enums.UserRole;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter for UserRole enum
 *
 * Converts between UserRole enum and database Integer value.
 * Database stores enum code (e.g., 1=RESTAURANT_ADMIN, 2=WAITER, 3=KITCHEN)
 */
@Converter(autoApply = true)
public class UserRoleConverter implements AttributeConverter<UserRole, Integer> {

    @Override
    public Integer convertToDatabaseColumn(UserRole role) {
        if (role == null) {
            return null;
        }
        return role.getCode();
    }

    @Override
    public UserRole convertToEntityAttribute(Integer code) {
        if (code == null) {
            return null;
        }
        return UserRole.fromCode(code);
    }
}
