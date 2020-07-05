package automark;

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
        print(text);
        String answer = readLine();
        System.out.println("Ok");
        return answer;
    }

    public String promptSecret(String text) {
        print(text);
        String answer = readPassword();
        System.out.println("Ok");
        return answer;
    }

    public boolean askForConfirmation(String text, boolean defaultAnswer) {
        Boolean result = null;
        while(result == null) {
            print(text);
            print(" [");
            print(defaultAnswer ? "Y" : "y");
            print("/");
            print(defaultAnswer ? "n" : "N");
            print("] ");
            String answer = readLine().trim().toLowerCase();
            if(answer.isEmpty()) result = defaultAnswer;
            else if(answer.equals("y")) result = true;
            else if(answer.equals("n")) result = false;
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
