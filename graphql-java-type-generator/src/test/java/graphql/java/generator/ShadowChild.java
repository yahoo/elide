package graphql.java.generator;

public class ShadowChild extends ShadowParent {
    public boolean shadow = true;

    public boolean isShadow() {
        return shadow;
    }

    public void setShadow(boolean shadow) {
        this.shadow = shadow;
    }
}
