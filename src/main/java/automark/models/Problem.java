package automark.models;

import javax.tools.*;
import java.io.*;
import java.util.*;
import java.util.stream.*;

public class Problem {
    public final Stage stage;
    public final Type type;
    public final String summary;

    public Problem(Stage stage, Type type) {
        this(stage, type, null);
    }

    public Problem(Stage stage, Type type, String summary) {
        this.stage = stage;
        this.type = type;
        this.summary = summary;
    }

    public enum Type {
        EXCEPTION, NOT_SUBMITTED, INVALID_SUBMISSION_FILE, PLAGIARIZED, COMPILATION_ERROR, TEST_SUITE_FAILURE, TEST_FAILURE;
    }

    public static Problem createException(Stage stage, Exception underlyingException) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (PrintStream printStream = new PrintStream(byteArrayOutputStream)) {
            underlyingException.printStackTrace(printStream);
        }
        String stackTrace = byteArrayOutputStream.toString();
        return new Problem(stage, Type.EXCEPTION, stackTrace);
    }

    public static Problem createInvalidSubmissionFile(Stage stage, String summary) {
        return new Problem(stage, Type.INVALID_SUBMISSION_FILE, summary);
    }

    public static Problem createdPlagiarized() {
        return new Problem(null, Type.PLAGIARIZED);
    }

    public static Problem createCompilationError(List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        String summary = diagnostics.stream()
                .map(diagnostic -> diagnostic.getMessage(null) + " at " + diagnostic.getSource().getName() + ":" + diagnostic.getLineNumber() + ":" + diagnostic.getColumnNumber())
                .collect(Collectors.joining("\n"));
        return new Problem(Stage.COMPILE, Type.COMPILATION_ERROR, summary);
    }

    public static Problem createTestSuiteFail(String testSuiteName) {
        return new Problem(Stage.TEST, Type.TEST_SUITE_FAILURE, testSuiteName);
    }

    public static Problem createTestFail(String testSuiteName, String testName) {
        return new Problem(Stage.TEST, Type.TEST_FAILURE, testSuiteName + "::" + testName);
    }
}
