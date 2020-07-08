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
            out.print(submission.getStudentName());
            out.print(" (");
            out.print(submission.getSlug());
            out.println(")");

            if (submission.getProblems().size() == 0) {
                printIndentation(out, 1);
                out.println("No problems");
            } else {
                submission.getProblems().forEach(problem -> {
                    printIndentation(out, 1);
                    out.print(problem.type);
                    out.print(" in stage ");
                    out.println(problem.stage.name());

                    if (problem.summary != null) {
                        for (String line : problem.summary.split("\n")) {
                            printIndentation(out, 2);
                            out.println(line);
                        }
                    }
                });
            }

            out.println();
        });
    }

    public static void printPublicSummary(PrintStream out, List<Submission> submissions) {
    }

    private static void printIndentation(PrintStream out, int level) {
        out.print("\t".repeat(level));
    }
}
