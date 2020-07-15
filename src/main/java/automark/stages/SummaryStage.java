package automark.stages;

import automark.*;
import automark.io.*;
import automark.models.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

public class SummaryStage {
    public static List<Submission> run(File workingDir, Properties config, List<Submission> submissions) throws UserFriendlyException {
        File summaryFile = new File(workingDir, "summary.txt");
        if (summaryFile.exists())
            summaryFile.delete();

        try (PrintStream out = new PrintStream(summaryFile)) {
            for (Submission submission : submissions) {
                out.println(submission.getStudentName());

                if (submission.getProblems().size() > 0) {
                    submission.getProblems().stream()
                            .collect(Collectors.groupingBy(problem -> problem.type))
                            .forEach((type, problems) -> {
                                Summaries.printIndentation(out, 1);
                                out.println(type);
                                problems.forEach(problem -> {
                                    if (problem.type == Problem.Type.EXCEPTION) {
                                        Summaries.printIndentation(out, 2);
                                        out.println(problem.summary.lines().findFirst().orElse("<no details given>"));
                                    } else {
                                        Summaries.printProblemSummary(out, problem, 2);
                                    }
                                });
                            });
                } else {
                    Summaries.printIndentation(out, 1);
                    out.println("No problems");
                }

                out.println();
                out.println();
            }
        } catch (FileNotFoundException e) {
            throw new UserFriendlyException("Failed to write summary", e);
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(summaryFile))) {
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                System.out.println(line);
            }
        } catch (IOException e) {
            throw new UserFriendlyException("Failed to read out summary file after write");
        }

        return submissions;
    }
}
