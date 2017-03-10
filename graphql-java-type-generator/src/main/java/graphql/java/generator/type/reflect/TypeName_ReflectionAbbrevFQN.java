package graphql.java.generator.type.reflect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.java.generator.type.strategies.TypeNameStrategy;

/**
 * Take the fully qualified name of the given Class.class or object's class,
 * and abbreviates the package names, discarding any periods.
 * This strategy will lead to type-name collisions, but is easy to use.
 * @author dwinsor
 *
 */
public class TypeName_ReflectionAbbrevFQN  implements TypeNameStrategy {
    private static Logger logger = LoggerFactory.getLogger(TypeName_ReflectionAbbrevFQN.class);
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
        
        String outputName = "";
        int nextIndex = 0;
        int fromIndex = 0;
        while (nextIndex != -1) {
            nextIndex = canonicalClassName.indexOf('.', fromIndex);
            if (nextIndex == -1) {
                outputName += canonicalClassName.substring(fromIndex);
            }
            else {
                outputName += canonicalClassName.charAt(fromIndex);
            }
            fromIndex = nextIndex+1;
        }
        return outputName;
    }
}
