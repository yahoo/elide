package graphql.java.generator;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class ClassWithListOfGenericsWithBounds {
    public List<ParameterizedClassWithBounds<InterfaceImpl>> listOfParamOfII = new ArrayList<ParameterizedClassWithBounds<InterfaceImpl>>() {{
        add(new ParameterizedClassWithBounds<InterfaceImpl>() {{
            setT(new InterfaceImpl());
        }});
    }};
    public List<ParameterizedClassWithBounds<InterfaceImpl>> getListOfParamOfII() {
        return listOfParamOfII;
    }
}
