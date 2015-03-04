package com.nanuvem.lom.business;

import java.util.List;
import java.util.regex.Pattern;

import com.nanuvem.lom.api.EntityType;
import com.nanuvem.lom.api.MetadataException;
import com.nanuvem.lom.api.dao.DaoFactory;
import com.nanuvem.lom.api.dao.EntityDao;

public class EntityServiceImpl {

    private EntityDao dao;

    public static final String DEFAULT_NAMESPACE = "default";

    EntityServiceImpl(DaoFactory factory) {
        this.dao = new EntityDaoDecorator(factory.createEntityDao());
    }

    public EntityType create(EntityType entityType) {
        validateEntity(entityType);
        EntityType createdEntity = dao.create(entityType);
        return createdEntity;
    }

    private void validateEntity(EntityType entityType) {
        if (entityType.getName() == null || entityType.getName().equals("")) {
            throw new MetadataException("The name of an Entity is mandatory");
        }

        if (entityType.getNamespace() == null) {
            entityType.setNamespace("");
        }

        lowerCase(entityType);

        validadeNameAndNamespacePattern(entityType);
        validadeEntityDuplication(entityType);
    }

    private void lowerCase(EntityType entityType) {
        entityType.setName(entityType.getName().toLowerCase());
        entityType.setNamespace(entityType.getNamespace().toLowerCase());
    }

    private void validadeNameAndNamespacePattern(EntityType entityType) {
        String namespace = entityType.getNamespace();

        if (!namespace.equals("") && !Pattern.matches("[a-zA-Z1-9.]{1,}", namespace)) {
            throw new MetadataException("Invalid value for Entity namespace: " + namespace);
        }

        if (!Pattern.matches("[a-zA-Z1-9]{1,}", entityType.getName())) {
            throw new MetadataException("Invalid value for Entity name: " + entityType.getName());
        }
    }

    private void validadeEntityDuplication(EntityType entityType) {
        EntityType found = null;
        try {
            found = findByFullName(entityType.getFullName());
        } catch (MetadataException me) {
            found = null;
        }

        if (found != null && !found.getId().equals(entityType.getId())) {
            StringBuilder message = new StringBuilder();
            message.append("The ");
            message.append(found.getFullName());
            message.append(" Entity already exists");
            throw new MetadataException(message.toString());
        }
    }

    // There is no test case for classFullName = null. How should the message
    // being thrown exception in this case?
    public EntityType findByFullName(String classFullName) {
        String namespace = null;
        String name = null;

        if (classFullName.contains(".")) {
            namespace = classFullName.substring(0, classFullName.lastIndexOf("."));
            name = classFullName.substring(classFullName.lastIndexOf(".") + 1, classFullName.length());
        } else {
            namespace = "";
            name = classFullName;
        }

        if (!Pattern.matches("[a-zA-Z1-9.]{1,}", namespace) && !namespace.isEmpty()) {
            this.formatStringAndThrowsExceptionInvalidKeyForEntity(classFullName);
        }

        if (!Pattern.matches("[a-zA-Z1-9]{1,}", name) && !name.isEmpty()) {
            this.formatStringAndThrowsExceptionInvalidKeyForEntity(classFullName);
        }

        if (namespace.isEmpty()) {
            namespace = "";
        }

        EntityType classByNamespaceAndName = dao.findByFullName(classFullName);

        if (classByNamespaceAndName == null) {
            if (classFullName.startsWith(".")) {
                classFullName = classFullName.substring(1);
            }
            if (classFullName.endsWith(".")) {
                classFullName = classFullName.substring(0, classFullName.length() - 1);
            }
            throw new MetadataException("Entity not found: " + classFullName);
        }

        return classByNamespaceAndName;
    }

    public EntityType findById(Long id) {
        EntityType entityType = this.dao.findById(id);
        return entityType;
    }

    public List<EntityType> listAll() {
        List<EntityType> list = dao.listAll();
        return list;
    }

    public List<EntityType> listByFullName(String fragment) {
        if (fragment == null) {
            fragment = "";
        }

        if (!Pattern.matches("[a-zA-Z1-9.]{1,}", fragment) && !fragment.isEmpty()) {
            throw new MetadataException("Invalid value for Entity full name: " + fragment);
        }

        List<EntityType> list = this.dao.listByFullName(fragment);
        return list;
    }

    private void formatStringAndThrowsExceptionInvalidKeyForEntity(String value) {
        if (value.startsWith(".")) {
            value = value.substring(1);
        }
        if (value.endsWith(".")) {
            value = value.substring(0, value.length() - 1);
        }
        throw new MetadataException("Invalid key for Entity: " + value);

    }

    public EntityType update(EntityType entityType) {
        this.validateEntityOnUpdate(entityType);
        this.validateEntity(entityType);
        EntityType updatedEntity = this.dao.update(entityType);
        return updatedEntity;
    }

    private void validateEntityOnUpdate(EntityType updateEntity) {
        if (updateEntity.getId() == null && updateEntity.getVersion() == null) {
            throw new MetadataException("The version and id of an Entity are mandatory on update");
        } else if (updateEntity.getId() == null) {
            throw new MetadataException("The id of an Entity is mandatory on update");
        } else if (updateEntity.getVersion() == null) {
            throw new MetadataException("The version of an Entity is mandatory on update");
        }
    }

    public void delete(long id) {
        this.dao.delete(id);
    }

}

class EntityDaoDecorator implements EntityDao {

    private EntityDao entityDao;

    public EntityDaoDecorator(EntityDao entityDao) {
        this.entityDao = entityDao;
    }

    public EntityType create(EntityType entityType) {
        EntityType entityClone = Util.clone(entityType);
        Util.setDefaultNamespace(entityClone);

        EntityType createdEntity = entityDao.create(entityClone);

        EntityType createdEntityClone = Util.clone(createdEntity);
        Util.removeDefaultNamespace(createdEntityClone);
        return createdEntityClone;
    }

    public List<EntityType> listAll() {
        List<EntityType> list = Util.clone(entityDao.listAll());
        Util.removeDefaultNamespace(list);
        return list;
    }

    public EntityType findById(Long id) {
        EntityType entityType = Util.clone(entityDao.findById(id));
        Util.removeDefaultNamespace(entityType);
        return entityType;
    }

    public List<EntityType> listByFullName(String fragment) {
        List<EntityType> list = Util.clone(entityDao.listByFullName(fragment));
        Util.removeDefaultNamespace(list);
        return list;
    }

    public EntityType findByFullName(String fullName) {
        fullName = Util.setDefaultNamespace(fullName);

        EntityType entityType = Util.clone(entityDao.findByFullName(fullName));
        Util.removeDefaultNamespace(entityType);
        return entityType;
    }

    public EntityType update(EntityType entityType) {
        EntityType entityClone = Util.clone(entityType);
        Util.setDefaultNamespace(entityClone);

        EntityType updatedEntity = entityDao.update(entityClone);

        EntityType updatedEntityClone = Util.clone(updatedEntity);
        Util.removeDefaultNamespace(updatedEntityClone);
        return updatedEntityClone;
    }

    public void delete(Long id) {
        entityDao.delete(id);
    }

}