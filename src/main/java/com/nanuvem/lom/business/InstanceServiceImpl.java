package com.nanuvem.lom.business;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonNode;

import com.nanuvem.lom.api.PropertyType;
import com.nanuvem.lom.api.Property;
import com.nanuvem.lom.api.EntityType;
import com.nanuvem.lom.api.Entity;
import com.nanuvem.lom.api.MetadataException;
import com.nanuvem.lom.api.dao.AttributeValueDao;
import com.nanuvem.lom.api.dao.DaoFactory;
import com.nanuvem.lom.api.dao.InstanceDao;
import com.nanuvem.lom.api.util.JsonNodeUtil;
import com.nanuvem.lom.business.validator.ValidationError;
import com.nanuvem.lom.business.validator.configuration.AttributeTypeValidator;
import com.nanuvem.lom.business.validator.configuration.AttributeValidator;
import com.nanuvem.lom.business.validator.definition.AttributeTypeDefinition;
import com.nanuvem.lom.business.validator.definition.AttributeTypeDefinitionManager;

public class InstanceServiceImpl {

    private final String PREFIX_EXCEPTION_MESSAGE_VALUE = "Invalid value for the Instance. ";

    private InstanceDao instanceDao;
    private AttributeValueDao attributeValueDao;
    private EntityServiceImpl entityService;
    private AttributeServiceImpl attributeService;
    private AttributeTypeDefinitionManager definitionManager;

    InstanceServiceImpl(DaoFactory daoFactory, EntityServiceImpl entityService, AttributeServiceImpl attributeService,
            AttributeTypeDefinitionManager definitionManager) {
        this.entityService = entityService;
        this.attributeService = attributeService;
        this.definitionManager = definitionManager;
        this.instanceDao = new InstanceDaoDecorator(daoFactory.createInstanceDao());
        this.attributeValueDao = new AttributeValueDaoDecorator(daoFactory.createAttributeValueDao());
    }

    public Entity create(Entity entity) {
        if (entity.getEntity() == null) {
            throw new MetadataException("Invalid value for Instance entity: The entity is mandatory");
        }
        EntityType entityType;
        try {
            entityType = this.entityService.findById(entity.getEntity().getId());
        } catch (MetadataException e) {
            throw new MetadataException("Unknown entity id: " + entity.getEntity().getId());
        }

        entity.setEntity(entityType);
        validateAndAssignDefaultValueInAttributesValues(entity, entityType);
        List<Property> values = entity.getValues();
        for (Property value : values) {
            PropertyType propertyType = attributeService.findAttributeById(value.getAttribute().getId());
            validateValue(propertyType.getConfiguration(), value);
        }

        List<Property> originalValues = new ArrayList<Property>(values);

        values.clear();
        Entity newInstance = this.instanceDao.create(entity);

        for (Property value : originalValues) {
            value.setInstance(newInstance);
            this.attributeValueDao.create(value);
        }

        return instanceDao.findInstanceById(newInstance.getId());
    }

    private void validateValue(String configuration, Property value) {
        List<ValidationError> errors = new ArrayList<ValidationError>();

        AttributeTypeDefinition definition = definitionManager.get(value.getAttribute().getType().name());
        AttributeTypeValidator typeValidator = new AttributeTypeValidator(definition.getAttributeClass());
        typeValidator.validateValue(errors, null, value);

        if (configuration != null && !configuration.isEmpty()) {
            JsonNode jsonNode = load(configuration);

            for (AttributeValidator validator : definition.getValidators()) {
                validator.validateValue(errors, jsonNode, value);
            }
        }

        Util.throwValidationErrors(errors, PREFIX_EXCEPTION_MESSAGE_VALUE);

    }

    private void validateAndAssignDefaultValueInAttributesValues(Entity entity, EntityType entityType) {

        for (Property property : entity.getValues()) {
            if (!(entityType.getAttributes().contains(property.getAttribute()))) {
                throw new MetadataException("Unknown attribute for " + entity.getEntity().getFullName() + ": "
                        + property.getAttribute().getName());
            }
            // this.validateTypeOfValue(attributeValue);

            String configuration = property.getAttribute().getConfiguration();
            if (configuration != null && !configuration.isEmpty()) {
                JsonNode jsonNode = load(configuration);
                this.applyDefaultValueWhenAvailable(property, jsonNode);
            }
        }
    }

    private JsonNode load(String configuration) {
        JsonNode jsonNode = JsonNodeUtil.validate(configuration, "Invalid value for Attribute configuration: "
                + configuration);
        return jsonNode;
    }

    private void applyDefaultValueWhenAvailable(Property property, JsonNode jsonNode) {

        String defaultConfiguration = PropertyType.DEFAULT_CONFIGURATION_NAME;
        if (jsonNode.has(defaultConfiguration)) {
            String defaultField = jsonNode.get(defaultConfiguration).asText();

            if (property.getValue() == null && defaultField != null) {
                property.setValue(defaultField);
            }
        }
    }

    public Entity findInstanceById(Long id) {
        return this.instanceDao.findInstanceById(id);
    }

    public List<Entity> findInstancesByEntityId(Long entityId) {
        return this.instanceDao.findInstancesByEntityId(entityId);
    }
}

class InstanceDaoDecorator implements InstanceDao {

    private InstanceDao instanceDao;

    public InstanceDaoDecorator(InstanceDao instanceDao) {
        this.instanceDao = instanceDao;
    }

    public Entity create(Entity entity) {
        Entity createdInstance = Util.clone(instanceDao.create(Util.clone(entity)));
        Util.removeDefaultNamespace(createdInstance);
        return createdInstance;
    }

    public Entity findInstanceById(Long id) {
        Entity entity = Util.clone(instanceDao.findInstanceById(id));
        Util.removeDefaultNamespace(entity);
        return entity;
    }

    public Entity update(Entity entity) {
        Entity updatedInstance = Util.clone(instanceDao.update(Util.clone(entity)));
        Util.removeDefaultNamespace(updatedInstance);
        return updatedInstance;
    }

    public void delete(Long id) {
        instanceDao.delete(id);
    }

    public List<Entity> findInstancesByEntityId(Long entityId) {
        List<Entity> entities = Util.clone(instanceDao.findInstancesByEntityId(entityId));
        Util.removeDefaultNamespaceForInstance(entities);
        return entities;
    }

}

class AttributeValueDaoDecorator implements AttributeValueDao {

    private AttributeValueDao attributeValueDao;

    public AttributeValueDaoDecorator(AttributeValueDao attributeValueDao) {
        this.attributeValueDao = attributeValueDao;
    }

    public Property create(Property value) {
        Property createdValue = Util.clone(attributeValueDao.create(Util.clone(value)));
        Util.removeDefaultNamespace(createdValue);
        return createdValue;
    }
}