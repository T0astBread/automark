package automark.gui;

import automark.models.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class SubmissionList extends JPanel {
    private static final Font DEFAULT_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 20);
    private static final Color COL_BG_SELECTED = new Color(184, 207, 229);
    private static final Color COL_BORDER_SELECTED = new Color(99, 130, 191);
    private static final Border SELECTED_BORDER = new LineBorder(COL_BORDER_SELECTED);
    private static final Border REGULAR_BORDER = new LineBorder(new Color(0, 0, 0, 0));

    private final SpringLayout layout;
    private List<Submission> data;
    private final Map<String, Boolean> expandedStates;

    public SubmissionList() {
        this.data = Collections.emptyList();
        this.expandedStates = new HashMap<>();

        this.layout = new SpringLayout();
        setLayout(this.layout);

        setBackground(Color.WHITE);
    }

    public List<Submission> getData() {
        return data;
    }

    public void setData(List<Submission> data) {
        this.data = data;
        removeAll();
        renderComponents();
    }

    private void renderComponents() {
        JPanel lastPanel = null;
        for (Submission submission : data) {
            boolean isExpanded = this.expandedStates.getOrDefault(submission.getSlug(), false);
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
            if(lastPanel == null) {
                this.layout.putConstraint(SpringLayout.NORTH, panel, 2, SpringLayout.NORTH, this);
            } else {
                this.layout.putConstraint(SpringLayout.NORTH, panel, 0, SpringLayout.SOUTH, lastPanel);
            }
            this.layout.putConstraint(SpringLayout.WEST, panel, 2, SpringLayout.WEST, this);
            this.layout.putConstraint(SpringLayout.EAST, panel, 2, SpringLayout.EAST, this);


            JLabel nameLabel = new JLabel(submission.getStudentName());
            nameLabel.setFont(DEFAULT_FONT);
            panel.add(nameLabel);

//            lbl.setOpaque(true);
            panel.setBackground(null);
            panel.setBorder(REGULAR_BORDER);
            panel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    panel.setBackground(COL_BG_SELECTED);
                    panel.setBorder(SELECTED_BORDER);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    panel.setBackground(null);
                    panel.setBorder(REGULAR_BORDER);
                }
            });

            add(panel);
            lastPanel = panel;
        }
    }
}
