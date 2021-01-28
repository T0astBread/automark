package automark.io;

import automark.models.*;

import java.io.*;
import java.util.*;

public class Summaries {
    public static final String INDENTATION = "  ";

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

            if (submission.isDisqualified()) {
                printIndentation(out, 1);
                out.println("Disqualified from further processing");
            }

            if (submission.getProblems().size() == 0) {
                printIndentation(out, 1);
                out.println("No problems");
            } else {
                submission.getProblems().forEach(problem -> {
                    printIndentation(out, 1);
                    out.print(problem.type);
                    if (problem.stage != null) {
                        out.print(" in stage ");
                        out.print(problem.stage.name());
                    }
                    out.println();

                    printProblemSummary(out, problem, 2);
                });
            }

            out.println();
        });
    }

    public static void printIndentation(PrintStream out, int level) {
        out.print(INDENTATION.repeat(level));
    }

    public static void printProblemSummary(PrintStream out, Problem problem, int indentation) {
        if (problem.summary != null) {
            for (String line : problem.summary.split("\n")) {
                printIndentation(out, indentation);
                out.println(line);
            }
        }
    }
}
