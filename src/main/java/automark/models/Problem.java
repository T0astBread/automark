package automark.models;

import java.io.*;

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
        EXCEPTION, NOT_SUBMITTED;
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
}
