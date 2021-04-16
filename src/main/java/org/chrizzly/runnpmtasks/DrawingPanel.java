package org.chrizzly.runnpmtasks;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.text.NbDocument;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Chrizzly
 */
public class DrawingPanel extends JPanel {
    Document document;

    private JPanel sideBarPanelContainer = new JPanel();
    private Action newFileAction = FileUtil.getConfigObject("Actions/Project/org-netbeans-modules-project-ui-NewFile.instance", Action.class);

    public DrawingPanel(JTextComponent target) {
        super(new GridLayout());
        document = target.getDocument();
        this.sideBarPanelContainer.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        setPreferredSize(new Dimension(250, getPreferredSize().height));
        setLayout(new BorderLayout());

        try {
            final FileObject packageJson = NbEditorUtilities.getDataObject(document).getPrimaryFile();
            final JSONParser jsonParser = new JSONParser();
            final JSONObject jsonObject = (JSONObject) jsonParser.parse(packageJson.asText());
            final JSONObject scriptsObject = (JSONObject) jsonObject.get("scripts");
            final Set<Entry<String, String>> entrySet = scriptsObject.entrySet();

            for (Entry<String, String> entry : entrySet) {
                // https://regex101.com/r/Dz8k7M/5
                final String taskName = String.format("\"%s\"", entry.getKey()); // TODO: Take regex from url.
                final JButton button = new JButton();

//                button.setHorizontalAlignment(SwingConstants.LEFT);
                button.setBorderPainted(false);
                button.setOpaque(true);
                button.setText("run " + taskName);
                button.setSize(200, 50);
                button.setIcon(ImageUtilities.loadImageIcon((String) newFileAction.getValue("iconBase"), false));

                final Pattern p = Pattern.compile(taskName);
                final Matcher matcher = p.matcher(target.getText());

                if (matcher.find()) {
//                    createPanel((NbDocument.findLineNumber((StyledDocument) document, matcher.start()) + 1), button);

                    constraints.anchor = GridBagConstraints.FIRST_LINE_START;
                    constraints.gridx = 0;
                    constraints.gridy = (NbDocument.findLineNumber((StyledDocument) document, matcher.start()) + 1) * 16 + 5;

                    this.sideBarPanelContainer.add(button, constraints);
//                    this.sideBarPanelContainer.setBounds(0, (NbDocument.findLineNumber((StyledDocument) document, matcher.start()) + 1) * 16 + 5, 25, 5);
                }
            }

            add(sideBarPanelContainer, BorderLayout.NORTH);

        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ParseException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void createPanel(int lineNumber, JButton button) {
//        sideBarPanelContainer.setBounds(0, lineNumber * 16 + 5, 25, 5);
//        sideBarPanelContainer.setBounds(0, lineNumber, 25, 5);
//        sideBarPanelContainer.setLayout(new BorderLayout());
//        sideBarPanelContainer.add(button, BorderLayout.SOUTH);
//        sideBarPanelContainer.setBackground(Color.decode("#FF0096"));

//        add(button, BorderLayout.SOUTH);
    }
}




