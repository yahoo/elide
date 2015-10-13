#Extension

It is sometimes required to customize CRUD operation with business logic.  For example, a user model might require a cryptographic hash applied 
to a password field.  There may be a configurable limit on the number of model entities that a user can create.

The simplest way to add custom business logic to Elide is to add it directly to the model objects in the field accessor 
functions (getField, setField, and addField where 'Field' is the name of the model field).

Sometimes, this business logic however requires access to other objects and resources outside of the model.  Since all model objects
in Elide are ultimately constructed by the JPA ORM, and because elide does not directly depend on any specific
dependency injection framework, elide provides an alternate way to initialize a bean.

Elide can be configured with an `Initializer` implementation for a particular model class.  An `Initializer` is any class which implements
the following interface:

```
public interface Initializer<T> {

    /**
     * Initialize an entity bean
     *
     * @param entity Entity bean to initialize
     */
    public void initialize(T entity);
}
```

TODO - add example code for how to configure elide with initializers.
