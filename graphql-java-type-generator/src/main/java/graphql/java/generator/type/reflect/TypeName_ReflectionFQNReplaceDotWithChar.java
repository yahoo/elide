package graphql.java.generator.type.reflect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.java.generator.type.strategies.TypeNameStrategy;

/**
 * Take the fully qualified name of the given Class.class or object's class,
 * and replace all dots with some other character, since dots
 * in the graphql query are not valid.
 * Default character is underscore (_)
 * @author dwinsor
 *
 */
public class TypeName_ReflectionFQNReplaceDotWithChar  implements TypeNameStrategy {
    private static Logger logger = LoggerFactory.getLogger(
            TypeName_ReflectionFQNReplaceDotWithChar.class);
    protected char newChar;
    public TypeName_ReflectionFQNReplaceDotWithChar() {
        this('_');
    }
    public TypeName_ReflectionFQNReplaceDotWithChar(char newChar) {
        this.newChar = newChar;
    }
    
    @Override
    public String getTypeName(Object object) {
        Class<?> clazz = ReflectionUtils.extractClassFromSupportedObject(object);
        if (clazz == null) return null;
        String canonicalClassName = clazz.getCanonicalName();
        if (canonicalClassName == null) {
            logger.debug("Name was null for class [{}]. "
                    + "local or anonymous class or an array whose component"
                    + " type does not have a canonical name", clazz);
            return null;
        }
        
        canonicalClassName = canonicalClassName.replace('.', newChar);
        return canonicalClassName;
    }
}
