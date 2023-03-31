/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.util.reflection;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReflectionUtil {

    private static final String ARRAY_INDEXER_REGEX = ".*\\[([0-9])+\\]";
    private static final String OPENING_BRACKET = "[";
    private static final String CLOSING_BRACKET = "]";

    /**
     * Utility function to get value of a field from an object. For field names currently the dot notation and array
     * indexers are supported:
     * <pre>
     *     someObject.someValue
     *     someObject[2].someValue //someObject must impement the List interface
     * </pre>
     *
     * @param object       The object
     * @param propertyName The name of the field
     * @return The field's value.
     * @throws ReflectionException if the field does not exist or is not accessible
     */
    public static <T> T getFieldValue(String propertyName, Object object) {
        Objects.requireNonNull(propertyName, "propertyName");
        Objects.requireNonNull(object, "object");

        if (propertyName.contains(".")) {
            var dotIx = propertyName.indexOf(".");
            var field = propertyName.substring(0, dotIx);
            var rest = propertyName.substring(dotIx + 1);
            object = getFieldValue(field, object);
            if (object == null) {
                return null;
            }
            return getFieldValue(rest, object);
        } else if (propertyName.matches(ARRAY_INDEXER_REGEX)) { //array indexer
            var openingBracketIx = propertyName.indexOf(OPENING_BRACKET);
            var closingBracketIx = propertyName.indexOf(CLOSING_BRACKET);
            var propName = propertyName.substring(0, openingBracketIx);
            var arrayIndex = Integer.parseInt(propertyName.substring(openingBracketIx + 1, closingBracketIx));
            var iterableObject = (List) getFieldValue(propName, object);
            return (T) iterableObject.get(arrayIndex);
        } else {
            try {
                if (object instanceof Map) {
                    var map = (Map) object;
                    return (T) map.get(propertyName);
                } else {
                    var field = getFieldRecursive(object.getClass(), propertyName);
                    if (field == null) {
                        throw new ReflectionException(propertyName);
                    }
                    field.setAccessible(true);
                    return (T) field.get(object);
                }
            } catch (IllegalAccessException e) {
                throw new ReflectionException(e);
            }
        }
    }


    /**
     * Utility function to get value of a field from an object. Essentially the same as
     * {@link ReflectionUtil#getFieldValue(String, Object)} but it does not throw an exception
     *
     * @param object       The object
     * @param propertyName The name of the field
     * @return The field's value. Returns null if the field does not exist or is inaccessible.
     */
    public static <T> T getFieldValueSilent(String propertyName, Object object) {
        try {
            return getFieldValue(propertyName, object);
        } catch (ReflectionException | IndexOutOfBoundsException ignored) {
            return null;
        }
    }

    @NotNull
    public static <T> Comparator<T> propertyComparator(boolean isAscending, String property) {
        return (def1, def2) -> {
            var o1 = ReflectionUtil.getFieldValueSilent(property, def1);
            var o2 = ReflectionUtil.getFieldValueSilent(property, def2);

            if (o1 == null || o2 == null) {
                return 0;
            }

            if (!(o1 instanceof Comparable)) {
                throw new IllegalArgumentException("A property '" + property + "' is not comparable!");
            }
            var comp1 = (Comparable) o1;
            var comp2 = (Comparable) o2;
            return isAscending ? comp1.compareTo(comp2) : comp2.compareTo(comp1);
        };
    }


    /**
     * Gets a field with a given name from all declared fields of a class including supertypes. Will include protected
     * and private fields.
     *
     * @param clazz     The class of the object
     * @param fieldName The fieldname
     * @return A field with the given name, null if the field does not exist
     */
    public static Field getFieldRecursive(Class<?> clazz, String fieldName) {
        return getAllFieldsRecursive(clazz).stream().filter(f -> f.getName().equals(fieldName)).findFirst().orElse(null);
    }

    /**
     * Recursively gets all fields declared in the class and all its superclasses
     *
     * @param clazz The class of the object
     * @return A list of {@link Field}s
     */
    public static List<Field> getAllFieldsRecursive(Class<?> clazz) {
        if (clazz == null) {
            return Collections.emptyList();
        }

        List<Field> result = new ArrayList<>(getAllFieldsRecursive(clazz.getSuperclass()));
        List<Field> filteredFields = Arrays.stream(clazz.getDeclaredFields()).collect(Collectors.toList());
        result.addAll(filteredFields);
        return result;
    }

    /**
     * Recursively gets all fields by navigating the path in dot notation starting by the provided class
     * This method also navigate through collections of Objects by inspecting the generic type of the collection.
     *
     * @param clazz The class of the object
     * @param path  The path in dot notation
     * @return A list of {@link Field}s
     */

    public static List<Field> getAllFieldsRecursiveWithPath(Class<?> clazz, String path) {
        var fields = new ArrayList<Field>();
        if (clazz == null || path.isEmpty()) {
            return fields;
        }

        var idx = path.indexOf(".");
        var fieldName = path;
        String rest = null;

        if (idx != -1) {
            fieldName = path.substring(0, idx);
            rest = path.substring(idx + 1);
        }


        var field = getFieldRecursive(clazz, fieldName);
        if (field != null) {
            fields.add(field);
            if (rest != null) {
                fields.addAll(getAllFieldsRecursiveWithPath(getCollectionFieldType(field), rest));
            }
        }

        return fields;
    }

    /**
     * Return the type of the field. If it's a collection it returns the generic type
     *
     * @param field The input field
     * @return The type of the field
     */
    @NotNull
    private static Class<?> getCollectionFieldType(Field field) {
        var target = field.getType();
        if (Collection.class.isAssignableFrom(target)) {
            Type t = field.getGenericType();
            if (t instanceof ParameterizedType) {
                var actualCollectionType = ((ParameterizedType) t).getActualTypeArguments()[0];
                if (actualCollectionType instanceof Class) {
                    target = (Class<?>) actualCollectionType;
                }
            }
        }
        return target;
    }


    /**
     * Get the first type argument for the given target from the given clazz.
     * It goes through the hierarchy starting from class and looking for target
     * And return the first type argument of the target
     *
     * @param clazz The class of the object
     * @return The type argument {@link Class} or null
     */
    public static Class<?> getSingleSuperTypeGenericArgument(Class<?> clazz, Class<?> target) {
        Type supertype = clazz.getGenericSuperclass();
        Class<?> superclass = clazz.getSuperclass();
        while (superclass != null && superclass != Object.class) {
            if (supertype instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) supertype;
                if (pt.getRawType() == target) {
                    return getSingleTypeArgument(supertype);
                }
            }

            supertype = superclass.getGenericSuperclass();
            superclass = superclass.getSuperclass();
        }
        return null;
    }

    /**
     * If the Type is a ParameterizedType return the actual type of the first type parameter
     *
     * @param genericType The genericType
     * @return The class of the type argument
     */
    private static Class<?> getSingleTypeArgument(Type genericType) {
        if (genericType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) genericType;
            Type[] actualTypeArguments = pt.getActualTypeArguments();
            if (actualTypeArguments.length == 1) {
                Type actualTypeArgument = actualTypeArguments[0];

                if (actualTypeArgument instanceof Class) {
                    return (Class<?>) actualTypeArgument;
                }
                if (actualTypeArgument instanceof ParameterizedType) {
                    ParameterizedType actualParametrizedType = (ParameterizedType) actualTypeArgument;
                    Type rawType = actualParametrizedType.getRawType();
                    if (rawType instanceof Class) {
                        return (Class<?>) rawType;
                    }
                }
            }
        }
        return null;
    }

}
