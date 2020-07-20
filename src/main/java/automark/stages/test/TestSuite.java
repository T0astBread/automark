package automark.stages.test;

import java.io.*;
import java.util.*;

public class TestSuite {
    private final String name;
    private final List<TestMethod> methods;
    private final File sourceFile;

    public TestSuite(List<TestMethod> methods, File sourceFile) {
        this.methods = methods;
        this.sourceFile = sourceFile;
        this.name = sourceFile.getName().substring(0, sourceFile.getName().length() - ".java".length());
    }

    public String getName() {
        return name;
    }

    public List<TestMethod> getMethods() {
        return methods;
    }

    public File getSourceFile() {
        return sourceFile;
    }
}
