package com.nanuvem.lom.business.validator.configuration;

import java.util.List;

import org.codehaus.jackson.JsonNode;

import com.nanuvem.lom.api.PropertyType;
import com.nanuvem.lom.api.Property;
import com.nanuvem.lom.business.validator.ValidationError;

public class MandatoryValidator implements AttributeValidator {

    public void validateDefault(List<ValidationError> errors, JsonNode configuration) {
        // Do not need to validate default value
    }

    public void validateValue(List<ValidationError> errors, JsonNode configuration, Property value) {
        if (configuration != null && configuration.has(PropertyType.MANDATORY_CONFIGURATION_NAME)) {

            boolean isMandatory = configuration.get(PropertyType.MANDATORY_CONFIGURATION_NAME).getBooleanValue();

            if (isMandatory && (value.getValue() == null || value.getValue().isEmpty())) {
                ValidationError.addError(errors, "The value for the '" + value.getAttribute().getName()
                        + "' attribute is mandatory");
            }
        }
    }

}
