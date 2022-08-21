package org.chrizzly.runnpmtasks;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.List;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.plaf.TextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.text.StyledDocument;
import javax.swing.text.View;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.BaseTextUI;
import org.netbeans.editor.EditorUI;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.text.NbDocument;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;

/**
 *
 * @author Chrizzly
 */
public class DrawingPanel extends JPanel {

    Document document;

    private JPanel sideBarPanelContainer = new JPanel();
    private Action newFileAction = FileUtil.getConfigObject("Actions/Project/org-netbeans-modules-project-ui-NewFile.instance", Action.class);

    private final JTextComponent txtComponent;
    private boolean enabled;
    private static final Logger LOGGER = Logger.getLogger(DrawingPanel.class.getName());

    public DrawingPanel(JTextComponent editor) {
        super(new BorderLayout());

        txtComponent = editor;
        document = (BaseDocument) editor.getDocument();

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
                final Matcher matcher = p.matcher(editor.getText());

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

    @Override
    protected void paintComponent(final Graphics g) {
        if (!enabled) {
            return;
        }
        super.paintComponent(g);
        Utilities.runViewHierarchyTransaction(txtComponent, true, () -> paintComponentUnderLock(g));
    }

    private void paintComponentUnderLock(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        Rectangle clip = g2d.getClipBounds();

        g2d.setColor(new Color(0, 0, 0));
        g2d.fillRect(clip.x, clip.y, clip.width, clip.height);

        JTextComponent component = txtComponent;
        TextUI textUI = component.getUI();
        if (textUI == null) {
            return;
        }
        EditorUI editorUI = Utilities.getEditorUI(component);
        if (editorUI == null) {
            return;
        }
        View rootView = Utilities.getDocumentView(component);
        if (rootView == null) {
            return;
        }
        try {
            drawColorRect(component, textUI, clip, rootView, g2d);
        } catch (BadLocationException ex) {
            LOGGER.log(Level.WARNING, "Incorrect offset : {0}", ex.offsetRequested()); // NOI18N
        }
    }

    private void drawColorRect(JTextComponent component, @NonNull TextUI textUI, Rectangle clip, View rootView, Graphics2D g2d) throws BadLocationException {
        int startPos = getPosFromY(component, textUI, clip.y);
        int startViewIndex = rootView.getViewIndex(startPos, Position.Bias.Forward);
        int rootViewCount = rootView.getViewCount();
        List<ColorCodesProvider> providers = getEnabledProviders();
        if (startViewIndex >= 0 && startViewIndex < rootViewCount) {
            Map<ColorCodesProvider, Map<String, List<ColorValue>>> variableColorValues = createVariableColorValuesMap(providers);
            int clipEndY = clip.y + clip.height;
            int start = getStartIndex(startViewIndex, providers);
            for (int i = start; i < rootViewCount; i++) {
                View view = rootView.getView(i);
                if (view == null) {
                    break;
                }

                // for zoom-in or zoom-out
                Rectangle rec1 = component.modelToView(view.getStartOffset());
                Rectangle rec2 = component.modelToView(view.getEndOffset() - 1);
                if (rec2 == null || rec1 == null) {
                    break;
                }

                int y = rec1.y;
                double lineHeight = (rec2.getY() + rec2.getHeight() - rec1.getY());
                if (document != null) {
                    String line = getLineText((BaseDocument) document, view);
                    int indexOfLF = line.indexOf("\n"); // NOI18N
                    if (indexOfLF != -1) {
                        line = line.substring(0, indexOfLF);
                    }

                    // get color values
                    List<ColorValue> colorValues = getAllColorValues(providers, line, i + 1, variableColorValues);

                    if (i < startViewIndex) {
                        continue;
                    }
                    drawColorRect(colorValues, g2d, lineHeight, y);
                }
                y += lineHeight;
                if (y >= clipEndY) {
                    break;
                }
            }
        }
    }

    private int getPosFromY(JTextComponent component, @NonNull TextUI textUI, int y) throws BadLocationException {
        if (textUI instanceof BaseTextUI) {
            return ((BaseTextUI) textUI).getPosFromY(y);
        } else {
            // fallback to ( less otimized than ((BaseTextUI) textUI).getPosFromY(y) )
            return textUI.modelToView(component, textUI.viewToModel(component, new Point(0, y))).y;
        }
    }

    private List<ColorCodesProvider> getEnabledProviders() {
        Collection<? extends ColorCodesProvider> allProviders = Lookup.getDefault().lookupAll(ColorCodesProvider.class);
        List<ColorCodesProvider> providers = new ArrayList<>();
        for (ColorCodesProvider provider : allProviders) {
            if (provider.isProviderEnabled(document)) {
                providers.add(provider);
            }
        }
        return providers;
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
