package automark.io;

import java.io.Console;
import java.util.Scanner;

public class UI {
    private static final Console console;
    private static Scanner scanner;

    static {
        console = System.console();
        if(console == null)
            scanner = new Scanner(System.in);
    }

    public static String prompt(String message, boolean isPassword) {
        System.out.print(message);
        return isPassword ? readPassword() : readLine();
    }

    public static String readLine() {
        return hasConsole() ? console.readLine() : scanner.nextLine();
    }

    public static String readPassword() {
        return hasConsole() ? new String(console.readPassword()) : scanner.nextLine();
    }

    private static boolean hasConsole() {
        return console != null;
    }
}
