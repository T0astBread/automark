package automark.stages;

import automark.*;
import automark.io.*;
import automark.models.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

public class SummaryStage {
    public static List<Submission> run(File workingDir, Properties config, List<Submission> submissions) throws UserFriendlyException {
        File summaryDir = Metadata.mkStageDir(Stage.SUMMARY, workingDir);

        for (Submission submission : submissions) {
            File submissionSummaryFile = new File(summaryDir, submission.getSlug() + ".txt");

            try (PrintStream out = new PrintStream(submissionSummaryFile)) {
                out.print("Subject: Feedback for ");
                out.println(config.getProperty(Config.ASSIGNMENT_NAME));
                out.println();

                combinedPrintln(out, submission.getStudentName());

                if (submission.getProblems().size() > 0) {
                    submission.getProblems().stream()
                            .collect(Collectors.groupingBy(problem -> problem.type))
                            .forEach((type, problems) -> {
                                combinedPrintIndentation(out, 1);
                                combinedPrintln(out, type.toString());
                                problems.forEach(problem -> {
                                    if (problem.type == Problem.Type.EXCEPTION) {
                                        combinedPrintIndentation(out, 2);
                                        combinedPrintln(out, problem.summary.lines().findFirst().orElse("<no details given>"));
                                    } else {
                                        Summaries.printProblemSummary(System.out, problem, 2);
                                        Summaries.printProblemSummary(out, problem, 2);
                                    }
                                });
                            });
                } else {
                    combinedPrintIndentation(out, 1);
                    combinedPrintln(out, "No problems");
                }

                System.out.println();
                System.out.println();
            } catch (FileNotFoundException e) {
                throw new UserFriendlyException("Failed to write summary", e);
            }
        }

        return submissions;
    }

    private static void combinedPrintln(PrintStream out, String message) {
        System.out.println(message);
        out.println(message);
    }

    private static void combinedPrintIndentation(PrintStream out, int level) {
        Summaries.printIndentation(System.out, level);
        Summaries.printIndentation(out, level);
    }
}
