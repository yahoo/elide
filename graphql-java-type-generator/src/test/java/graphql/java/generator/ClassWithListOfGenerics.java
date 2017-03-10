package graphql.java.generator;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class ClassWithListOfGenerics {
    public List<ParameterizedClass<Integer>> listOfParamOfInts = new ArrayList<ParameterizedClass<Integer>>() {{
        add(new ParameterizedClass<Integer>() {{
            setT(0);
        }});
    }};
    public List<ParameterizedClass<Integer>> getListOfParamOfInts() {
        return listOfParamOfInts;
    }
    
    public List<ParameterizedClass<InterfaceImpl>> listOfParamOfII = new ArrayList<ParameterizedClass<InterfaceImpl>>() {{
        add(new ParameterizedClass<InterfaceImpl>() {{
            setT(new InterfaceImpl());
        }});
    }};
    public List<ParameterizedClass<InterfaceImpl>> getListOfParamOfII() {
        return listOfParamOfII;
    }
}
