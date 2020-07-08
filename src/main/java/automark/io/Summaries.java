package automark.io;

import automark.models.*;

import java.io.*;
import java.util.*;

public class Summaries {
    public static void printShortSummary(PrintStream out, List<Submission> submissions) {
        submissions.forEach(submission -> {
            out.print(submission.getSlug());
            out.print(" (");
            out.print(submission.getStudentName());
            out.println(")");

            submission.getProblems().forEach(problem -> {
                printIndentation(out, 1);
                out.print(problem.type);
            });

            out.println();
        });
    }

    public static void printDetailedSummary(PrintStream out, List<Submission> submissions, Stage currentStage) {
        submissions.forEach(submission -> {
            out.print(submission.getSlug());
            out.print(" (");
            out.print(submission.getStudentName());
            out.println(")");

            submission.getProblems().forEach(problem -> {
                printIndentation(out, 1);
                out.print(problem.type);
                out.print(" in stage ");
                out.println(problem.stage.name());
                out.println(problem.summary.replace("^", "\t\t"));
            });

            out.println();
        });
    }

    public static void printPublicSummary(PrintStream out, List<Submission> submissions) {}

    private static void printIndentation(PrintStream out, int level) {
        out.print("\t".repeat(level));
    }
}
