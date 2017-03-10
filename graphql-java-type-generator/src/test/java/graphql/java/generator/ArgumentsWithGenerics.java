package graphql.java.generator;

import java.util.List;

public class ArgumentsWithGenerics {
    public int getSum(List<Integer> ints) {
        if (ints == null) return 0;
        int sum = 0;
        for (Integer i : ints) {
            sum += i;
        }
        return sum;
    }
}
