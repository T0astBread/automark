package automark.gui;

import automark.io.*;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame {
    public static void show(CommandLineArgs args) {
        new MainWindow().setVisible(true);
    }

    public MainWindow() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(new Dimension(1400, 800));
        setLocationRelativeTo(null);  // centers the window
        setTitle("Automark");
        setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));

        setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.add(new JButton("ok"));
        add(toolBar, BorderLayout.NORTH);

        JSplitPane outerSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        outerSplitPane.setContinuousLayout(true);
        outerSplitPane.setResizeWeight(.7);
        add(outerSplitPane, BorderLayout.CENTER);

        JSplitPane innerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        innerSplitPane.setContinuousLayout(true);
        innerSplitPane.setResizeWeight(.075);
        outerSplitPane.setTopComponent(innerSplitPane);

        JList<String> stageList = new JList<>();
        stageList.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
        DefaultListModel<String> stageListModel = new DefaultListModel<>();
        stageListModel.addElement("Download");
        stageListModel.addElement("Unzip");
        stageListModel.addElement("Extract");
        stageListModel.addElement("JPlag");
        stageListModel.addElement("Prepare Compile");
        stageListModel.addElement("Compile");
        stageListModel.addElement("Test");
        stageListModel.addElement("Summary");
        stageList.setModel(stageListModel);
        innerSplitPane.setLeftComponent(stageList);

        JTree submissionsTree = new JTree();
        submissionsTree.setRootVisible(false);
        submissionsTree.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
        innerSplitPane.setRightComponent(submissionsTree);

        JTextArea outputField = new JTextArea();
        outputField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        outputField.setEditable(false);
        outputField.setBackground(Color.DARK_GRAY);
        outputField.setForeground(Color.WHITE);
        outputField.setText("Automark v0.0.0\nOutput will appear here\n\n");
        outputField.setMinimumSize(new Dimension(0, 0));
        outputField.setLineWrap(true);

        JScrollPane scrollPane = new JScrollPane(outputField);
        scrollPane.setAutoscrolls(true);
        outputField.setAutoscrolls(true);
        scrollPane.setMinimumSize(new Dimension(0, 0));
        outerSplitPane.setBottomComponent(scrollPane);

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep((int) (Math.random() * 100));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int len = (int) (Math.random() * 20);
                for (int j = 0; j < len; j++) {
                    outputField.append("lorem ");
                }
                outputField.append("\n");
                outputField.setCaretPosition(outputField.getDocument().getLength());
            }
        }).start();
    }
}
