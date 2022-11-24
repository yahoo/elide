/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jpa.usertypes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

import java.io.IOException;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import java.util.Properties;

/**
 * JsonType serializes an object to json string and vice versa.
 */

public class JsonType implements UserType, ParameterizedType {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Class<?> objectClass;

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSqlType() {
        return Types.LONGVARCHAR;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> returnedClass() {
        return String.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object firstObject, Object secondObject)
            throws HibernateException {

        return Objects.equals(firstObject, secondObject);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode(Object object)
            throws HibernateException {

        return Objects.hashCode(object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object nullSafeGet(
            ResultSet resultSet,
            int i,
            SharedSessionContractImplementor sharedSessionContractImplementor,
            Object o) throws SQLException {
        if (resultSet.getString(i) != null) {

            // Get the rawJson
            String rawJson = resultSet.getString(i);

            try {
                return MAPPER.readValue(rawJson, this.objectClass);
            } catch (IOException e) {
                throw new HibernateException("Could not retrieve an instance of the mapped class from a JDBC resultset.");
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void nullSafeSet(PreparedStatement preparedStatement, Object value, int i, SharedSessionContractImplementor sharedSessionContractImplementor) throws HibernateException, SQLException {
        if (value == null) {
            preparedStatement.setNull(i, Types.NULL);
        } else {
            try {
                String json = MAPPER.writeValueAsString(value);
                preparedStatement.setString(i, json);
            } catch (JsonProcessingException e) {
                throw new HibernateException("Could not write an instance of the mapped class to a prepared statement.");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object deepCopy(Object object)
            throws HibernateException {

        // Since mutable is false, return the object
        return object;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMutable() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Serializable disassemble(Object value)
            throws HibernateException {

        return value == null ? null : (Serializable) deepCopy(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object assemble(Serializable cached, Object owner)
            throws HibernateException {

        return cached == null ? null : deepCopy(cached);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object replace(Object original, Object target, Object owner)
            throws HibernateException {

        return deepCopy(original);
    }

    /**
     * Setter used to set the class to serialize/deserialize.
     * @param properties properties object
     */
    @Override
    public void setParameterValues(Properties properties) {
        try {
            this.objectClass = Class.forName(properties.getProperty("class"));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable set the `class` parameter for serialization/deserialization");
        }
    }
}
