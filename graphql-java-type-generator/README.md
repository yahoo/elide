# graphql-java-type-generator

##About
This library will autogenerate GraphQL types for usage in com.graphql-java:graphql-java

The generation of types can be controlled using different strategies.

A default strategy implementation is provided, one which reads java .classes through reflection.

The default strategy is augmentable to customize any aspect at all.

In fact, there is a customizable strategy for everything from the name of a type, to which DataFetcher a field uses, down to which input arguments are available.

##Usage
Since everything about type generation is configurable, types are generated based upon a BuildContext that specifies the various strategies, parameters, and type repositories.

A DefaultBuildContext is provided, or another one can be made.

The best entry point to this library is BuildContext's .getOutputType(obj), .getInputType(obj), or .getInterfaceType(obj).

Direct access is given if, for example, only fields are needed.

#FAQ
* Recursive object creation? Yes.
* Java Generics? Yes, as long as type erasure has not happened.
* Enums? Yup
* Unions? Not yet... java doesn't really have those natively.
* Lists? Yes, and lists of lists
* Scalars? We additionally handle BigInteger and BigDecimal, byte, short, and char

Let us know what else you need.
