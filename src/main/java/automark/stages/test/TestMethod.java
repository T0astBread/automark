package automark.stages.test;

public class TestMethod {
    private final String methodName, description;

    public TestMethod(String methodName, String description) {
        this.methodName = methodName;
        this.description = description;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getDescription() {
        return description;
    }
}
