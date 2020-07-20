package automark.io;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.*;
import java.util.*;

public class UI {
    private static boolean runningInGUIEnvironment;
    private static final Console console;
    private static Scanner scanner;

    static {
        console = System.console();
        if (console == null)
            scanner = new Scanner(System.in);
    }

    public static String prompt(String message, boolean isPassword) {
        if (runningInGUIEnvironment) {
            String legitimacyToken = Integer.toString((int) (Math.random() * 1000));
            String title = "Input required (" + legitimacyToken + ") - automark";

            System.out.println("INPUT REQUIRED");
            System.out.println("Enter the required information into the window titled:");
            Summaries.printIndentation(System.out, 1);
            System.out.println(title);
            System.out.println();

            JDialog dialog = new JDialog();
            dialog.setModal(true);
            dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setTitle(title);
            dialog.setResizable(false);
            dialog.setSize(500, 120);
            dialog.setLocationRelativeTo(null);
            dialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

            Font font = new Font(Font.MONOSPACED, Font.PLAIN, 24);

            JPanel panel = new JPanel();
            panel.setBorder(new EmptyBorder(5, 5, 5, 5));
            GridLayout layout = new GridLayout(2, 1);
            layout.setVgap(5);
            panel.setLayout(layout);

            JLabel label = new JLabel(message);
            label.setFont(font.deriveFont(Font.BOLD));

            JTextField inputField = isPassword ? new JPasswordField() : new JTextField();
            inputField.addActionListener(actionEvent -> dialog.setVisible(false));
            inputField.setFont(font);
            inputField.setBorder(BorderFactory.createCompoundBorder(
                    inputField.getBorder(),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ));
            inputField.grabFocus();

            panel.add(label);
            panel.add(inputField);

            dialog.setContentPane(panel);
            dialog.setVisible(true);
            dialog.dispose();

            return inputField.getText();
        } else {
            System.out.print(message);
            return isPassword ? readPassword() : readLine();
        }
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

    public static boolean isRunningInGUIEnvironment() {
        return runningInGUIEnvironment;
    }

    public static void setRunningInGUIEnvironment(boolean runningInGUIEnvironment) {
        UI.runningInGUIEnvironment = runningInGUIEnvironment;
    }
}
