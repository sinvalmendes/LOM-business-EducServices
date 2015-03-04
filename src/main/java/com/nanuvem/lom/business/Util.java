package com.nanuvem.lom.business;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.SerializationUtils;

import com.nanuvem.lom.api.PropertyType;
import com.nanuvem.lom.api.Property;
import com.nanuvem.lom.api.EntityType;
import com.nanuvem.lom.api.Entity;
import com.nanuvem.lom.api.MetadataException;
import com.nanuvem.lom.business.validator.ValidationError;

public class Util {

    static void removeDefaultNamespace(EntityType entityType) {
        if (entityType == null) {
            return;
        }

        String namespace = entityType.getNamespace();

        if (namespace == null) {
            return;
        }

        if (namespace.equals(EntityServiceImpl.DEFAULT_NAMESPACE)) {
            entityType.setNamespace("");
        }
    }

    static void removeDefaultNamespace(List<EntityType> list) {
        for (EntityType entityType : list) {
            removeDefaultNamespace(entityType);
        }
    }

    static void setDefaultNamespace(EntityType entityType) {
        if (entityType == null) {
            return;
        }

        if (entityType.getNamespace() == null || entityType.getNamespace().equals("")) {
            entityType.setNamespace(EntityServiceImpl.DEFAULT_NAMESPACE);
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends Serializable> T clone(T t) {
        return (T) SerializationUtils.clone(t);
    }

    static <T extends Serializable> List<T> clone(List<T> ts) {
        List<T> clones = new ArrayList<T>();

        for (T t : ts) {
            clones.add(clone(t));
        }

        return clones;
    }

    static String setDefaultNamespace(String fullName) {
        if (fullName != null && !fullName.contains(".")) {
            fullName = EntityServiceImpl.DEFAULT_NAMESPACE + "." + fullName;
        }
        return fullName;
    }

    static void removeDefaultNamespace(PropertyType propertyType) {
        if (propertyType != null) {
            removeDefaultNamespace(propertyType.getEntity());
        }
    }

    public static void removeDefaultNamespace(Entity entity) {
        if (entity != null) {
            removeDefaultNamespace(entity.getEntity());
        }
    }

    public static void removeDefaultNamespace(Property value) {
        if (value != null) {
            removeDefaultNamespace(value.getInstance());
        }
    }

    static void throwValidationErrors(List<ValidationError> errors, String message) {
        if (!errors.isEmpty()) {
            String errorMessage = "";
            for (ValidationError error : errors) {
                if (errorMessage.isEmpty()) {
                    errorMessage += message + error.getMessage();
                } else {
                    errorMessage += ", " + error.getMessage();
                }
            }
            throw new MetadataException(errorMessage);
        }
    }

    public static void removeDefaultNamespaceForInstance(List<Entity> entities) {
        for (Entity entity : entities) {
            removeDefaultNamespace(entity);
        }
    }
}
