package graphql.java.generator;

public class RecursiveClass {
    public RecursiveClass() {
        
    }
    
    public RecursiveClass(int recursionLevel) {
        this.recursionLevel = String.valueOf(recursionLevel);
        if (recursionLevel > 0 && recursionLevel < 10) {
            recursive = new RecursiveClass(recursionLevel - 1);
        }
    }

    public String getRecursionLevel() {
        return recursionLevel;
    }
    public RecursiveClass getRecursive() {
        return recursive;
    }

    public String recursionLevel;
    public RecursiveClass recursive = null;
    
    //don't worry about hashcode
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof RecursiveClass)) {
            return false;
        }
        RecursiveClass recursive = (RecursiveClass) other;
        if (this.recursionLevel == null) {
            if (recursive.recursionLevel != null) {
                return false;
            }
        }
        else if (!(this.recursionLevel.equals(recursive.recursionLevel))) {
            return false;
        }
        if (this.recursive == null) {
            if (recursive.recursive != null) {
                return false;
            }
        }
        else if (!(this.recursive.equals(recursive.recursive))) {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString() {
        return "[recursionLevel=" + recursionLevel
                + ", recursive=" + recursive + "]";
    }
}
