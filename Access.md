#Access

Elide allows limiting which models are exposed through JSON API with two annotations: `Include` and `Exclude`.

## Include
`Include` allows access to a given model or package of models.   Class annotations override the settings of package annotations.
`Include` takes two parameters:
  1. `rootLevel` - Whether or not the model(s) can be accessed at the root URL path (/post for example).  
  2. `type` - Overrides the name of the model on the URL path.  By default, this is the class name.

##Exclude 
`Exclude` disallows access to a given entity or package of entities.   Class annotations override the settings of package annotations.
