#Authorization

##Core Concepts
Elide authorization involves a few core concepts:
  * **Permissions** - a set of annotations that describe how (read, update, delete, create) a model or model field can be accessed.
  * **Checks** - custom code that verifies whether a user can perform an action.  Checks are assigned to permissions.
  * **User** - an opaque object which represents the user identity and is passed to checks and collection filters.
  * **Persistent Resource** - all JPA annotated models are wrapped in `PersistentResource` objects.  Each `PersistentResource` includes a lineage or directed path of prior resources that were accessed to reach this particular resource.  A lineage can only traverse the JPA annotated relationships between models.  The current resource and any resource in its lineage can be referenced by a check.

##Application  
Security is applied in three ways:
  1. **Granting or denying access.**  If a specific model or model field is accessed and the requesting user does not belong to a role that has the associated permission, the request will be rejected with a 403 error code.  Otherwise the request is granted.
  2. **Filtering Collections.** If a model has any associated read permission checks, these checks are evaluated against each model that is a member of the collection.  Only the models the user has access to are returned in the response.
  3. **Filtering a model.**  If a user has read access to a model but only for a subset of a model’s fields, the disallowed fields are simply excluded from the output (rather than denying the request).  This filtering does not apply for explicit requests for sparse fieldsets (which are currently not supported).

##Hierarchical Security
JSON API does not qualify whether a URL can traverse the model relationship graph.  All documented examples only demonstrate access to a single collection, single resource, or a field belonging to a resource.

Without a hierarchy, all models must be accessible at the URL root.  This poses a number of problems for the declaration and evaluation of security policies:
  1. All models must enumerate all security checks.  The declarations become highly redundant and error prone.
  2. Security checks implementations may require access to a number of related models.  If a resource has more than a single parent, and the parent is needed to evaluate a check, it is not possible to determine the correct context to grant or deny authorization.

Given these difficulties, elide clarifies JSON API by explicitly allowing nested resource URL composition.  For example, consider the following entity relationship diagram:

TODO - insert diagram here.

  * One-to-one relationships can be navigated by specifying the relationship name without a corresponding ID: 
     1. The email address of the author of a specific post : '/post/1/author/email'
     2. The author of a post comment: '/post/1/comment/4/author'
  * One-to-many and many-to-many relationships can be navigated by specifying the relationship name along with a corresponding ID:
     1. A specific comment of a specific post: '/post/1/comment/4'

Checks are applied in a sequence based on the order in which models and their fields are accessed.  Consider a PATCH on '/post/1/comment/4' which 
changes the comment _name_ field.  Checks would be evaluated in the following sequence:
   1. Read permission check on the post model.
   2. Read field permission check on post.comment.
   3. Read permission check on the comment model.
   4. Write permission check on the comment model.
   5. Write field permission check on post.comment.

##Check

A check is simply a class which implements the following interface:
```
public interface Check {
    boolean ok(PersistentResource model, Object user);
}
```

##Permission Annotations

Permissions include `ReadPermission`, `WritePermission`, `CreatePermission`, and `DeletePermission`.

All permissions are configured in 1 of 2 ways:
  1. *Any* - A list of `Check` classes.  If any of the checks evaluate to true, permission is granted.
  2. *All* - A list of `Check` classes.  If all of the checks evaluate to true, permission is granted.

More complex check expressions can be implemented by composing and evaluating checks inside another check class.
