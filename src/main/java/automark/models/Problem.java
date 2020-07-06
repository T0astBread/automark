package automark.models;

import javax.tools.*;
import java.io.*;
import java.util.*;
import java.util.stream.*;

public class Problem {
    public final String stage;
    public final Type type;
    public final String summary;


    private Problem(String stage, Type type) {
        this(stage, type, type + " (in stage " + stage + ")");
    }

    private Problem(String stage, Type type, String summary) {
        this.stage = stage;
        this.type = type;
        this.summary = summary;
    }

    public enum Type {
        EXCEPTION, NOT_SUBMITTED, INVALID_SUBMISSION_FILE, PLAGIARIZED, COMPILATION_ERROR, TEST_SUITE_FAILURE, TEST_FAILURE;
    }

    public static Problem createException(String stage, Exception underlyingException) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (PrintStream printStream = new PrintStream(byteArrayOutputStream)) {
            underlyingException.printStackTrace(printStream);
        }
        String stackTrace = byteArrayOutputStream.toString();
        return new Problem(stage, Type.EXCEPTION, stackTrace);
    }

    public static Problem createNotSubmitted(String stage) {
        return new Problem(stage, Type.NOT_SUBMITTED);
    }

    public static Problem createInvalidSubmissionFile(String stage, String summary) {
        return new Problem(stage, Type.INVALID_SUBMISSION_FILE, summary);
    }

    public static Problem createdPlagiarized(String stage) {
        return new Problem(stage, Type.PLAGIARIZED);
    }

    public static Problem createCompilationError(String stage, List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        String summary = diagnostics.stream()
                .map(diagnostic -> diagnostic.getMessage(null) + " at " + diagnostic.getSource().getName() + ":" + diagnostic.getLineNumber() + ":" + diagnostic.getColumnNumber())
                .collect(Collectors.joining("\n"));
        return new Problem(stage, Type.COMPILATION_ERROR, summary);
    }

    public static Problem createTestSuiteFail(String stage, String testSuiteName) {
        return new Problem(stage, Type.TEST_SUITE_FAILURE, testSuiteName);
    }

    public static Problem createTestFail(String stage, String testSuiteName, String testName) {
        return new Problem(stage, Type.TEST_FAILURE, testSuiteName + "::" + testName);
    }
}
