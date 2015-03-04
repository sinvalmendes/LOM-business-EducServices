package com.nanuvem.lom.business;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;

import com.nanuvem.lom.api.PropertyType;
import com.nanuvem.lom.api.EntityType;
import com.nanuvem.lom.api.MetadataException;
import com.nanuvem.lom.api.dao.AttributeDao;
import com.nanuvem.lom.api.dao.DaoFactory;
import com.nanuvem.lom.api.util.JsonNodeUtil;
import com.nanuvem.lom.business.validator.ValidationError;
import com.nanuvem.lom.business.validator.configuration.AttributeValidator;
import com.nanuvem.lom.business.validator.definition.AttributeTypeDefinition;
import com.nanuvem.lom.business.validator.definition.AttributeTypeDefinitionManager;

public class AttributeServiceImpl {

    private final Integer MINIMUM_ATTRIBUTE_SEQUENCE = 1;

    private final String PREFIX_EXCEPTION_MESSAGE_CONFIGURATION = "Invalid configuration for attribute";

    private AttributeDao attributeDao;
    private EntityServiceImpl entityService;
    private AttributeTypeDefinitionManager definitionManager;

    AttributeServiceImpl(DaoFactory dao, EntityServiceImpl entityService,
            AttributeTypeDefinitionManager definitionManager) {
        this.entityService = entityService;
        this.definitionManager = definitionManager;
        this.attributeDao = new AttributeDaoDecorator(dao.createAttributeDao());

    }

    private void validateCreate(PropertyType propertyType) {
        validateDuplicatedAttribute(propertyType);
        defineAttributeSequenceNumber(propertyType);

        validateAttributeName(propertyType);

        if (propertyType.getType() == null) {
            throw new MetadataException("The type of an Attribute is mandatory");
        }
        validateAttributeConfiguration(propertyType);
    }

    private void defineAttributeSequenceNumber(PropertyType propertyType) {
        int currentNumberOfAttributes = propertyType.getEntity().getAttributes().size();
        if (propertyType.getSequence() != null) {
            boolean minValueForSequence = propertyType.getSequence() < MINIMUM_ATTRIBUTE_SEQUENCE;
            boolean maxValueForSequence = currentNumberOfAttributes + 1 < propertyType.getSequence();

            if (minValueForSequence || maxValueForSequence) {
                throw new MetadataException("Invalid value for Attribute sequence: " + propertyType.getSequence());
            }
        } else {
            propertyType.setSequence(currentNumberOfAttributes + 1);
        }
    }

    private void validateAttributeName(PropertyType propertyType) {
        if (propertyType.getName() == null || propertyType.getName().isEmpty()) {
            throw new MetadataException("The name of an Attribute is mandatory");
        }

        if (!Pattern.matches("[a-zA-Z1-9]{1,}", propertyType.getName())) {
            throw new MetadataException("Invalid value for Attribute name: " + propertyType.getName());
        }
    }

    private List<PropertyType> findAllAttributesForEntity(EntityType entityType) {
        if (entityType != null && !entityType.getFullName().isEmpty()) {
            EntityType foundEntity = entityService.findByFullName(entityType.getFullName());
            if (foundEntity != null && foundEntity.getAttributes() != null && foundEntity.getAttributes().size() > 0) {
                return foundEntity.getAttributes();
            }
        }
        return null;
    }

    private void validateAttributeConfiguration(PropertyType propertyType) {
        String configuration = propertyType.getConfiguration();
        if (configuration != null && !configuration.isEmpty()) {
            JsonNode jsonNode = JsonNodeUtil.validate(configuration, "Invalid value for Attribute configuration: "
                    + configuration);

            AttributeTypeDefinition definition = definitionManager.get(propertyType.getType().name());
            validateFieldNames(definition, propertyType, jsonNode);
            validateFieldValues(definition, propertyType, jsonNode);
        }
    }

    private void validateFieldNames(AttributeTypeDefinition definition, PropertyType propertyType, JsonNode jsonNode) {

        Iterator<String> fieldNames = jsonNode.getFieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (!definition.containsConfigurationField(fieldName)) {
                throw new MetadataException("Invalid configuration for attribute " + propertyType.getName() + ": the "
                        + fieldName + " configuration attribute is unknown");
            }
        }
    }

    private void validateFieldValues(AttributeTypeDefinition definition, PropertyType propertyType, JsonNode jsonNode) {
        List<ValidationError> errors = new ArrayList<ValidationError>();

        for (AttributeValidator validator : definition.getValidators()) {
            validator.validateDefault(errors, jsonNode);
        }

        Util.throwValidationErrors(errors, PREFIX_EXCEPTION_MESSAGE_CONFIGURATION + " " + propertyType.getName() + ": ");
    }

    private EntityType validateExistingEntityForAttribute(PropertyType propertyType) {
        EntityType entityType = null;
        try {
            entityType = entityService.findById(propertyType.getEntity().getId());
        } catch (MetadataException e) {
            throw new MetadataException("Invalid Entity: " + propertyType.getEntity().getFullName());
        }
        return entityType;
    }

    private void validateDuplicatedAttribute(PropertyType propertyType) {
        List<PropertyType> foundAttributes = this.findAllAttributesForEntity(propertyType.getEntity());
        if (foundAttributes != null) {
            for (PropertyType at : foundAttributes) {
                if (at.getName().equalsIgnoreCase(propertyType.getName())) {
                    this.throwMetadataExceptionOnAttributeDuplication(propertyType);
                }
            }
        }
    }

    private void validateExistingAttributeNotInEntityOnUpdate(PropertyType propertyType) {
        if (propertyType.getId() != null) {
            PropertyType foundAttributes = this.findAttributeByNameAndEntityFullName(propertyType.getName(), propertyType
                    .getEntity().getFullName());
            if (foundAttributes != null) {
                if (!propertyType.getId().equals(foundAttributes.getId())) {
                    this.throwMetadataExceptionOnAttributeDuplication(propertyType);
                }
            }
        }
    }

    private void throwMetadataExceptionOnAttributeDuplication(PropertyType propertyType) {
        throw new MetadataException("Attribute duplication on " + propertyType.getEntity().getFullName()
                + " Entity. It already has an attribute " + StringUtils.lowerCase(propertyType.getName() + "."));
    }

    public PropertyType create(PropertyType propertyType) {
        EntityType entityType = validateExistingEntityForAttribute(propertyType);
        propertyType.setEntity(entityType);
        validateCreate(propertyType);
        PropertyType createdAttribute = this.attributeDao.create(propertyType);
        entityService.update(createdAttribute.getEntity());
        return createdAttribute;
    }

    public List<PropertyType> listAllAttributes(String entityFullName) {
        EntityType entityType = entityService.findByFullName(entityFullName);
        return entityType.getAttributes();
    }

    public PropertyType findAttributeById(Long id) {
        if (id != null) {
            PropertyType propertyType = this.attributeDao.findAttributeById(id);
            return propertyType;
        } else {
            return null;
        }
    }

    public PropertyType findAttributeByNameAndEntityFullName(String nameAttribute, String entityFullName) {

        if ((nameAttribute != null && !nameAttribute.isEmpty())
                && (entityFullName != null && !entityFullName.isEmpty())) {

            PropertyType propertyType = this.attributeDao.findAttributeByNameAndEntityFullName(nameAttribute, entityFullName);
            return propertyType;
        }
        return null;
    }

    public PropertyType update(PropertyType propertyType) {
        this.validateAttributeName(propertyType);
        this.validateUpdateSequence(propertyType);
        this.validateUpdateType(propertyType);
        this.validateExistingAttributeNotInEntityOnUpdate(propertyType);
        this.validateAttributeConfiguration(propertyType);

        PropertyType updatedAttribute = this.attributeDao.update(propertyType);
        entityService.update(updatedAttribute.getEntity());
        return updatedAttribute;
    }

    private void validateUpdateType(PropertyType propertyType) {
        PropertyType attributeFound = this.findAttributeById(propertyType.getId());

        if (!attributeFound.getType().equals(propertyType.getType())) {
            throw new MetadataException("Can not change the type of an attribute");
        }
    }

    private void validateUpdateSequence(PropertyType propertyType) {
        EntityType entityType = entityService.findById(propertyType.getEntity().getId());
        int currentNumberOfAttributes = entityType.getAttributes().get(entityType.getAttributes().size() - 1).getSequence();

        if (propertyType.getSequence() != null) {
            boolean minValueForSequence = propertyType.getSequence() < MINIMUM_ATTRIBUTE_SEQUENCE;
            boolean maxValueForSequence = currentNumberOfAttributes < propertyType.getSequence();

            if (!(minValueForSequence || maxValueForSequence)) {
                return;
            }
        }
        throw new MetadataException("Invalid value for Attribute sequence: " + propertyType.getSequence());
    }
}

class AttributeDaoDecorator implements AttributeDao {

    private AttributeDao attributeDao;

    public AttributeDaoDecorator(AttributeDao attributeDao) {
        this.attributeDao = attributeDao;
    }

    public PropertyType create(PropertyType propertyType) {
        PropertyType createdAttribute = Util.clone(attributeDao.create(Util.clone(propertyType)));
        Util.removeDefaultNamespace(createdAttribute);
        return createdAttribute;
    }

    public PropertyType findAttributeById(Long id) {
        PropertyType propertyType = Util.clone(attributeDao.findAttributeById(id));
        Util.removeDefaultNamespace(propertyType);
        return Util.clone(propertyType);
    }

    public PropertyType findAttributeByNameAndEntityFullName(String nameAttribute, String entityFullName) {
        entityFullName = Util.setDefaultNamespace(entityFullName);
        PropertyType propertyType = Util.clone(attributeDao.findAttributeByNameAndEntityFullName(nameAttribute,
                entityFullName));
        Util.removeDefaultNamespace(propertyType);
        return Util.clone(propertyType);
    }

    public PropertyType update(PropertyType propertyType) {
        PropertyType updatedAttribute = Util.clone(attributeDao.update(Util.clone(propertyType)));
        Util.removeDefaultNamespace(updatedAttribute);
        return Util.clone(updatedAttribute);
    }

}