package automark;

import automark.config.*;
import automark.errors.*;
import automark.execution.*;
import automark.models.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

public class UI {
    private static UI instance;

    public static void setup() {
        instance = new UI();
    }

    public static UI get() {
        return instance;
    }

    private final Console console;
    private final Scanner scanner;

    private UI() {
        this.console = System.console();
        this.scanner = console == null ? new Scanner(System.in) : null;
    }

    public String prompt(String text) {
        println("\nINPUT REQUIRED");
        print(text);
        String answer = readLine();
        System.out.println("Ok");
        return answer;
    }

    public String promptSecret(String text) {
        println("\nINPUT REQUIRED");
        print(text);
        String answer = readPassword();
        System.out.println("Ok");
        return answer;
    }

    public String promptList(String text) {
        String answer;
        do {
            answer = prompt(text);
            println("\nReceived entries:");
            for (String entry : answer.split("\\s+")) {
                println(entry);
            }
        } while (!askForConfirmation("\nIs that correct?", true));
        return answer;
    }

    public boolean askForConfirmation(String text, boolean defaultAnswer) {
        Boolean result = null;
        while (result == null) {
            print(text);
            print(" [");
            print(defaultAnswer ? "Y" : "y");
            print("/");
            print(defaultAnswer ? "n" : "N");
            print("] ");
            String answer = readLine().trim().toLowerCase();
            if (answer.isEmpty()) result = defaultAnswer;
            else if (answer.equals("y")) result = true;
            else if (answer.equals("n")) result = false;
            else {
                println("Invalid input (Y/y/N/n or blank are allowed)");
            }
        }
        print("Got ");
        println(result ? "YES" : "NO");
        return result;
    }

    public void print(String text) {
        System.out.print(text);
    }

    public void println(String text) {
        System.out.println(text);
    }

    public List<String> editWhitelist(List<String> possibleEntries, List<String> enabledEntries, String message) {
        while (true) {
            whitelistPrint(possibleEntries, enabledEntries);
            String answer = prompt("\n" + message +
                    "\n\nAdd: a <indices to list>\n" +
                    "Remove: r <indices to unlist>\n" +
                    "Add all: A\n" +
                    "Remove all: R\n" +
                    "Exit: q\n" +
                    "a/r/A/R/q> ");

            if (answer.startsWith("a ")) {
                List<String> newEnabledEntries = new ArrayList<>();
                List<Integer> newEntries = whitelistParseAnswer(answer);
                for (int i = 0; i < possibleEntries.size(); i++) {
                    String entry = possibleEntries.get(i);
                    if (newEntries.contains(i) || enabledEntries.contains(entry))
                        newEnabledEntries.add(entry);
                }
                enabledEntries = newEnabledEntries;

            } else if (answer.startsWith("r ")) {
                List<String> newEnabledEntries = new ArrayList<>();
                List<Integer> removedEntries = whitelistParseAnswer(answer);
                for (int i = 0; i < possibleEntries.size(); i++) {
                    String entry = possibleEntries.get(i);
                    if (!removedEntries.contains(i) && enabledEntries.contains(entry))
                        newEnabledEntries.add(entry);
                }
                enabledEntries = newEnabledEntries;

            } else if (answer.equals("A")) {
                enabledEntries = List.copyOf(possibleEntries);

            } else if (answer.equals("R")) {
                enabledEntries = Collections.emptyList();

            } else if (answer.equals("q")) {
                whitelistPrint(possibleEntries, enabledEntries);
                if (askForConfirmation("Is that correct?", true)) {
                    return enabledEntries;
                }
            }
        }
    }

    private void whitelistPrint(List<String> possibleEntries, List<String> enabledEntries) {
        println("");
        for (int i = 0; i < possibleEntries.size(); i++) {
            String e = possibleEntries.get(i);
            print(enabledEntries.contains(e) ? "x  " : "   ");
            print(i + "  ");
            println(e);
        }
    }

    private List<Integer> whitelistParseAnswer(String answer) {
        String[] tokens = answer.substring(2).split("\\s+");
        List<Integer> indices = new ArrayList<>();
        for (String token : tokens) {
            try {
                int idx = Integer.parseInt(token);
                indices.add(idx);
            } catch (NumberFormatException e) {
                println("Invalid index: " + token);
            }
        }
        return indices;
    }

    public String askForConfigValue(String key) {
        String message = getConfigValueMessage(key);

        switch (key) {
            case ConfigConstants.MOODLE_PASSWORD:
                return UI.get().promptSecret(message);
            case ConfigConstants.MOODLE_TEACHERS:
                return UI.get().promptList(message);
            default:
                return UI.get().prompt(message);
        }
    }

    private String getConfigValueMessage(String key) {
        switch (key) {
            case ConfigConstants.ASSIGNMENT_ID:
                return "Assignment ID\n" +
                        "This is the numeric ID from the Moodle course's URL\n" +
                        "> ";
            case ConfigConstants.MOODLE_USERNAME:
                return "Moodle username> ";
            case ConfigConstants.MOODLE_PASSWORD:
                return "Moodle password> ";
            case ConfigConstants.MOODLE_BASE_URL:
                return "Moodle base URL\n" +
                        "This is your login URL minus `/login/index.php`\n\n" +
                        "(Hint:You can permanently set this property for your user using\n " +
                        "`automark set --global -o \"moodleBaseURL=value\"`)\n\n" +
                        "> ";
            case ConfigConstants.MOODLE_TEACHERS:
                return "Email addresses of teachers in the Moodle course (space-seperated)\n" +
                        "No emails will be sent. These are used to filter out teachers\n" +
                        "> ";
            default:
                return key + "> ";
        }
    }

    public void printStatus(List<Submission> submissions) {
        for (Submission submission : submissions) {
            List<Problem> problems = submission.getProblems();
            boolean hasProblems = problems.size() > 0;
            if (submission.isDisqualified() || hasProblems) {
                print(submission.getSlug());
                print(" (");
                print(submission.getStudentName());
                println(")");
            }

            if (submission.isDisqualified()) {
                printIndent(1);
                println("DISQUALIFIED from further processing");
            }

            String problemSummary = problems.stream()
                    .collect(Collectors.groupingBy(problem -> problem.type + " in stage " + problem.stage))
                    .entrySet()
                    .stream()
                    .sorted(Comparator.comparingInt(this::sortIndexOf))
                    .map(entry ->
                            "\t" + entry.getKey()+ (
                                    entry.getValue().size() > 1 ? " (" + entry.getValue().size() + " times)" : ""
                            ) + shortSummary(entry.getValue()))
                    .collect(Collectors.joining("\n"));
            println(problemSummary);

//            for (Problem problem : problems) {
//                printIndent(1);
//                print(problem.type.toString());
//                print(" in stage ");
//                println(problem.stage);
////                for (String line : problem.summary.split("\n")) {
////                    printIndent(2);
////                    println(line);
////                }
//            }

            print("\n");
        }
    }

    private int sortIndexOf(Map.Entry<String, List<Problem>> entry) {
        return Executor.indexOfStage(entry.getValue().get(0).stage);
    }

    private String shortSummary(List<Problem> problems) {
        return problems.stream()
                .map(this::shortSummary)
                .filter(Objects::nonNull)
                .map(s -> "\n\t\t" + s)
                .collect(Collectors.joining()) + "\n";
    }

    private String shortSummary(Problem problem) {
        switch (problem.type) {
            case TEST_FAILURE:
            case TEST_SUITE_FAILURE:
            case COMPILATION_ERROR:
            case EXCEPTION:
                return problem.summary.replace("\n", "\n\t\t");
            default:
                return null;
        }
    }

    private void printIndent(int indentation) {
        print("\t".repeat(indentation));
    }

    private String readLine() {
        try {
            return console == null ? scanner.nextLine() : console.readLine();
        } catch (Exception e) {
            handleNonInteractive(e);
        }
        return null;  // should be unreachable
    }

    private String readPassword() {
        try {
            return console == null ? scanner.nextLine() : new String(console.readPassword());
        } catch (Exception e) {
            handleNonInteractive(e);
        }
        return null;  // should be unreachable
    }

    private void handleNonInteractive(Exception e) {
        e.printStackTrace();
        System.err.println("Probably, automark was run from a non-interactive session");
        System.err.println("automark must be run in an interactive terminal context");
        System.exit(ErrorCodes.USER_ERROR);
    }
}
