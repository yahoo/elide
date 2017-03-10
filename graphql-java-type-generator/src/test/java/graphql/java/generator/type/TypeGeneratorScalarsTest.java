package graphql.java.generator.type;

import graphql.Scalars;
import graphql.java.generator.DefaultBuildContext;
import graphql.schema.GraphQLScalarType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;

@RunWith(Parameterized.class)
public class TypeGeneratorScalarsTest {
    private static Logger logger = LoggerFactory.getLogger(
            TypeGeneratorScalarsTest.class);
    
    ITypeGenerator generator = DefaultBuildContext.reflectionContext;

    private Class<?> clazz;
    private GraphQLScalarType expected;
    
    @Before
    public void before() {
        DefaultBuildContext.defaultTypeRepository.clear();
    }
    
    public TypeGeneratorScalarsTest(Class<?> clazz, GraphQLScalarType expected) {
        this.clazz = clazz;
        this.expected = expected;
    }
    
    @Parameters
    public static Collection<Object[]> data() {
        @SuppressWarnings("serial")
        ArrayList<Object[]> list = new ArrayList<Object[]>() {{
            add(new Object[] {String.class, Scalars.GraphQLString});
            add(new Object[] {Boolean.class, Scalars.GraphQLBoolean});
            add(new Object[] {boolean.class, Scalars.GraphQLBoolean});
            add(new Object[] {Double.class, Scalars.GraphQLFloat});
            add(new Object[] {double.class, Scalars.GraphQLFloat});
            add(new Object[] {Float.class, Scalars.GraphQLFloat});
            add(new Object[] {float.class, Scalars.GraphQLFloat});
            add(new Object[] {Integer.class, Scalars.GraphQLInt});
            add(new Object[] {int.class, Scalars.GraphQLInt});
            add(new Object[] {Long.class, Scalars.GraphQLLong});
            add(new Object[] {long.class, Scalars.GraphQLLong});
            add(new Object[] {BigInteger.class, graphql.java.generator.Scalars.GraphQLBigInteger});
            add(new Object[] {BigDecimal.class, graphql.java.generator.Scalars.GraphQLBigDecimal});
        }};
        return list;
    }
    
    @Test
    public void testScalarsOutput() {
        logger.debug("testScalarsOutput {} {}", clazz, expected.getName());
        Assert.assertThat(generator.getOutputType(clazz),
                instanceOf(GraphQLScalarType.class));
        Assert.assertThat((GraphQLScalarType)generator.getOutputType(clazz),
                is(expected));
    }
    
    @Test
    public void testScalarsInput() {
        logger.debug("testScalarsInput {} {}", clazz, expected.getName());
        Assert.assertThat(generator.getInputType(clazz),
                instanceOf(GraphQLScalarType.class));
        Assert.assertThat((GraphQLScalarType)generator.getInputType(clazz),
                is(expected));
    }
}
