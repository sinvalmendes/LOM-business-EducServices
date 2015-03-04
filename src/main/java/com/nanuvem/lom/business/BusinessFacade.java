package com.nanuvem.lom.business;

import java.util.List;

import com.nanuvem.lom.api.PropertyType;
import com.nanuvem.lom.api.EntityType;
import com.nanuvem.lom.api.Facade;
import com.nanuvem.lom.api.Entity;
import com.nanuvem.lom.api.Relation;
import com.nanuvem.lom.api.RelationType;
import com.nanuvem.lom.api.dao.DaoFactory;
import com.nanuvem.lom.business.validator.definition.AttributeTypeDefinitionManager;

public class BusinessFacade implements Facade {

    private EntityServiceImpl entityService;
    private AttributeServiceImpl attributeService;
    private InstanceServiceImpl instanceService;
    private RelationTypeServiceImpl relationTypeService;
    private RelationServiceImpl relationService;

    public BusinessFacade(DaoFactory daoFactory) {
        entityService = new EntityServiceImpl(daoFactory);
        AttributeTypeDefinitionManager deployers = new AttributeTypeDefinitionManager();
        attributeService = new AttributeServiceImpl(daoFactory, entityService, deployers);
        instanceService = new InstanceServiceImpl(daoFactory, entityService, attributeService, deployers);
        relationTypeService = new RelationTypeServiceImpl(daoFactory);
        relationService = new RelationServiceImpl(daoFactory);
    }

    public EntityServiceImpl getEntityService() {
        return this.entityService;
    }

    public EntityType create(EntityType entityType) {
        return entityService.create(entityType);
    }

    public EntityType findEntityById(Long id) {
        return entityService.findById(id);
    }

    public EntityType findEntityByFullName(String fullName) {
        return entityService.findByFullName(fullName);
    }

    public List<EntityType> listAllEntities() {
        return entityService.listAll();
    }

    public List<EntityType> listEntitiesByFullName(String fragment) {
        return entityService.listByFullName(fragment);
    }

    public EntityType update(EntityType entityType) {
        return entityService.update(entityType);
    }

    public void deleteEntity(Long id) {
        entityService.delete(id);
    }

    public PropertyType create(PropertyType propertyType) {
        return attributeService.create(propertyType);
    }

    public PropertyType findAttributeById(Long id) {
        return attributeService.findAttributeById(id);
    }

    public PropertyType findAttributeByNameAndEntityFullName(String name, String fullEntityName) {
        return attributeService.findAttributeByNameAndEntityFullName(name, fullEntityName);
    }

    public PropertyType update(PropertyType propertyType) {
        return attributeService.update(propertyType);
    }

    public Entity create(Entity entity) {
        return instanceService.create(entity);
    }

    public Entity findInstanceById(Long id) {
        return instanceService.findInstanceById(id);
    }

    public List<Entity> findInstancesByEntityId(Long entityId) {
        return instanceService.findInstancesByEntityId(entityId);
    }

    public RelationType create(RelationType relationType) {
        relationType =  relationTypeService.create(relationType);
        return RelationType.cloneObject(relationType);
    }

    public RelationType findRelationTypeById(Long id) {
        RelationType relationType = relationTypeService.findRelationTypeById(id);
        return RelationType.cloneObject(relationType);
    }

    public List<RelationType> listAllRelationTypes() {
        return relationTypeService.listAllRelationTypes();
    }

    public RelationType update(RelationType relationType) {
        return relationTypeService.update(relationType);
    }

    public void deleteRelationType(Long id) {
        relationTypeService.delete(id);
    }

    public Relation create(Relation relation) {
        return relationService.create(relation);
    }

    public Relation findRelationById(Long id) {
        return relationService.findRelationById(id);
    }

    public List<Relation> listAllRelations() {
        return relationService.listAllRelations();
    }

    public Relation update(Relation relation) {
        return relationService.update(relation);
    }

    public void deleteRelation(Long id) {
        relationService.delete(id);
    }

    public List<Relation> findRelationsBySourceInstance(Entity source, RelationType relationType) {
        return relationService.findRelationsBySourceInstance(source, relationType);
    }

    public List<Relation> findRelationsByRelationType(RelationType relationType) {
        return relationService.findRelationsByRelationType(relationType);
    }

}
