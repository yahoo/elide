#Audit

Audit assigns semantic meaning to CRUD operations for the purposes of logging and audit.  For example, we may want to log when users 
change their password or when an account is locked.  Both actions are PATCH operations on a user entity that update different fields.  
Audit can assign these actions to parameterized, human readable logging statements that can be logged to a file, written to a database, or even 
displayed to users.

##Core Concepts

All models in Elide are accessed through JPA annotated relationships.  For example, if a request URL has the path '/company/53/user', the
_user_ model is accessed through its relationship with a specific company.  The sequence of prior models traversed to access a particular model is called that model's **lineage**.  A model and every prior model in its lineage are fully accessible to parameterize audit logging in Elide.

#Annotations
Elide audits operations on classes and class fields marked with the `Audit` annotation.

The `Audit` annotation takes several arguments:
   1. The CRUD action performed (CREATE, DELETE, or UPDATE).
   2. An operation code which uniquely identifies the semantic meaning of the action.
   3. The statement to be logged.  This is a template string that allows '{}' variable substitution.
   4. An ordered list of [Unified Expression Language](https://uel.java.net/) expressions that are used to subtitute ‘{}’ in the log statement.  Elide binds the model that is being audited and every model in its lineage to variables that are accessible to the UEL expressions.  The variable names map to model's type (typically the class name).

##Example

Let's say I have a simple _user_ entity with a _password_ field.  I want to audit whenever the password is changed.
The user is accessed via the URL path '/company/53/user'.  I could annotate this action as follows:

```
@Entity
@Include
public class User {
    @Audit(action = Audit.Action.UPDATE,
           operation = 572,
           logStatement = "User {0} from company {1} changed password.",
           logExpressions = {"${user.userid}", "${company.name}"})
    private String password;
    private String userid;
}
```

Elide binds the `User` object to the variable name _user_ and the `Company` object to the variable name _company_.
The `Company` object is bound because it belongs to the `User` object's lineage.

#Customizing Logging
Customizing audit functionality in elide requires two steps:
  1. Define audit annotations on JPA entity classes and fields.  
  2. Provide a Logger implementation to customize the handling of audit triggers.  The default logger simply logs to slf4j. 

## Logger Implementation
A customized logger extends the following abstract class:

```
public abstract class Logger {
    public void log(LogMessage message);
    public abstract void commit() throws IOException;
}
```
