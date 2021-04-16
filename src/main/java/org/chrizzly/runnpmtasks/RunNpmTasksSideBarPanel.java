package org.chrizzly.runnpmtasks;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;

/**
 *
 * @author Chrizzly
 */
public class RunNpmTasksSideBarPanel extends JPanel {
    public RunNpmTasksSideBarPanel(JTextComponent target) {
        super(new BorderLayout());
        add(new DrawingPanel(target), BorderLayout.CENTER);
    }
}