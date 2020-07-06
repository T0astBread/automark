package automark;

import automark.config.*;
import automark.errors.*;

import java.io.*;
import java.util.*;

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
                return key + ": ";
        }
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
