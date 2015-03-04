package com.nanuvem.lom.business.validator.definition;

import java.util.ArrayList;
import java.util.List;

import com.nanuvem.lom.api.PropertyType;
import com.nanuvem.lom.business.validator.MaximumLengthAttributeConfigurationValidator;
import com.nanuvem.lom.business.validator.MinimumLengthAttributeConfigurationValidator;
import com.nanuvem.lom.business.validator.RegexAttributeConfigurationValidator;
import com.nanuvem.lom.business.validator.configuration.AttributeValidator;
import com.nanuvem.lom.business.validator.configuration.AttributeValidatorWithValue;
import com.nanuvem.lom.business.validator.configuration.ConfigurationFieldValidator;
import com.nanuvem.lom.business.validator.configuration.MandatoryValidator;
import com.nanuvem.lom.business.validator.configuration.MinAndMaxValidator;

public class TextAttributeType implements AttributeTypeDefinition {

    public List<AttributeValidator> getValidators() {
        List<AttributeValidator> validators = new ArrayList<AttributeValidator>();
        validators.add(new MandatoryValidator());

        validators.add(new AttributeValidatorWithValue<String>(PropertyType.REGEX_CONFIGURATION_NAME,
                PropertyType.DEFAULT_CONFIGURATION_NAME, new RegexAttributeConfigurationValidator(), String.class));

        validators.add(new ConfigurationFieldValidator(PropertyType.DEFAULT_CONFIGURATION_NAME, String.class));
        validators
                .add(new AttributeValidatorWithValue<Integer>(PropertyType.MINLENGTH_CONFIGURATION_NAME,
                        PropertyType.DEFAULT_CONFIGURATION_NAME, new MinimumLengthAttributeConfigurationValidator(),
                        Integer.class));
        validators
                .add(new AttributeValidatorWithValue<Integer>(PropertyType.MAXLENGTH_CONFIGURATION_NAME,
                        PropertyType.DEFAULT_CONFIGURATION_NAME, new MaximumLengthAttributeConfigurationValidator(),
                        Integer.class));
        validators.add(new MinAndMaxValidator(PropertyType.MAXLENGTH_CONFIGURATION_NAME,
                PropertyType.MINLENGTH_CONFIGURATION_NAME));

        validators.add(new ConfigurationFieldValidator(PropertyType.MANDATORY_CONFIGURATION_NAME, Boolean.class));

        return validators;
    }

    public boolean containsConfigurationField(String fieldName) {
        return PropertyType.MANDATORY_CONFIGURATION_NAME.equals(fieldName)
                || PropertyType.DEFAULT_CONFIGURATION_NAME.equals(fieldName)
                || PropertyType.MINLENGTH_CONFIGURATION_NAME.equals(fieldName)
                || PropertyType.MAXLENGTH_CONFIGURATION_NAME.equals(fieldName)
                || PropertyType.REGEX_CONFIGURATION_NAME.equals(fieldName);
    }

    public Class<?> getAttributeClass() {
        return String.class;
    }

}
