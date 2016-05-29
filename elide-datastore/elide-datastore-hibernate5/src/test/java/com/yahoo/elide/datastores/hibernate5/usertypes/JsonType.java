/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.hibernate5.usertypes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.HibernateException;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;
import org.hibernate.engine.spi.SessionImplementor;

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

    private Class objectClass;

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] sqlTypes() {
        return new int[]{Types.LONGVARCHAR};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class returnedClass() {
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
    public Object nullSafeGet(ResultSet resultSet, String[] names, SessionImplementor session,
            Object ownerSession) throws HibernateException, SQLException {

        if (resultSet.getString(names[0]) != null) {

            // Get the rawJson
            String rawJson = resultSet.getString(names[0]);

            try {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(rawJson, this.objectClass);
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
    public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index, SessionImplementor session)
            throws HibernateException, SQLException  {

        if (value == null) {
            preparedStatement.setNull(index, Types.NULL);
        } else {
            ObjectMapper mapper = new ObjectMapper();
            try {
                String json = mapper.writeValueAsString(value);
                preparedStatement.setString(index, json);
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
