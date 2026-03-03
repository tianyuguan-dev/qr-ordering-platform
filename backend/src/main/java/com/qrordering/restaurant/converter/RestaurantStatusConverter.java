package com.qrordering.restaurant.converter;

import com.qrordering.restaurant.enums.RestaurantStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter for RestaurantStatus enum
 *
 * Converts between enum and database integer value
 */
@Converter(autoApply = true)
public class RestaurantStatusConverter implements AttributeConverter<RestaurantStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(RestaurantStatus status) {
        if (status == null) {
            return null;
        }
        return status.getCode();
    }

    @Override
    public RestaurantStatus convertToEntityAttribute(Integer code) {
        if (code == null) {
            return null;
        }
        return RestaurantStatus.fromCode(code);
    }
}
