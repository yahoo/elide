#Data Consistency

##Transactions
Accessing a hierarchy of objects requires a consistent view of related models.  Reading from and writing to individual resources without some form 
of transactions can result in inconsistent client state where some models reflect one state and others reflect a different state.  This can lead to subtle
bugs.

Elide wraps every request (either virgin JSON API or the patch extension) in a transaction.  By default this transaction maps to the underlying
default ORM implementation (in our case Hibernate).

##Field Accessors
Most JPA annotated models include 'get' and 'set' methods for reading and manipulating the model fields.  When adding a resource as a relationship member to another resource, elide first attempts to locate a method prefixed with 'add' and ending with field name ('addField' for example).  If this method does not exist, it invokes the standard 'set' method instead.  The primary reason for preferring 'add' functions by convention is that 'add' methods traditionally update both
sides of a bi-directional relationship rather than a single side.  For example, if adding a child to a parent model, the 'add' method would both update the child's
parent as well as the parent's list of children.

##Cascading Deletes
Many JPA ORM providers include a mechanism to cascade deletes so that model entities or even sub-hierarchies of model entities are removed whenever 
they become orphaned by a delete operation.

Elide relies on the functionality of the ORM to perform cascading deletes.
