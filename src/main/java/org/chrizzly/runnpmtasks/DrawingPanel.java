package org.chrizzly.runnpmtasks;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.TextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import javax.swing.text.View;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.EditorUI;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.openide.filesystems.FileObject;
import org.openide.text.NbDocument;
import org.openide.util.Exceptions;

/**
 *
 * @author Chrizzly
 */
public class DrawingPanel extends JPanel {

    private final JTextComponent textComponent;
    private Document document;
    private List<NpmScript> currentNpmScripts;
    public DrawingPanel sidebarPanel = this;

    public DrawingPanel(JTextComponent editor) {
        textComponent = editor;
        document = (BaseDocument) editor.getDocument();
        currentNpmScripts = new ArrayList<>(0);

        document.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateSidebar(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateSidebar(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateSidebar(e);
            }

            // Aktualisiert die Sidebar basierend auf Änderungen an der package.json
            private void updateSidebar(DocumentEvent e) {
                try {
                    // Hole das Document und die Änderungen
                    Document doc = e.getDocument();
                    String text = doc.getText(0, doc.getLength());

                    // Aktualisiere die Sidebar
                    updateSidebar(text);
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }

            // Aktualisiert die Sidebar basierend auf den gegebenen Text
            private void updateSidebar(String text) {
                // Entferne alle Komponenten aus der Sidebar
                removeAll();

                Utilities.runViewHierarchyTransaction(textComponent, true, () -> paintComponentUnderLock(sidebarPanel.getGraphics()));

                // Aktualisiere das SidebarPanel
                revalidate();
                repaint();
            }
        });

        this.setPreferredSize(new Dimension(20, HEIGHT));

        try {
            final FileObject packageJson = NbEditorUtilities.getDataObject(document).getPrimaryFile();
            final JSONParser jsonParser = new JSONParser();
            final JSONObject jsonObject = (JSONObject) jsonParser.parse(packageJson.asText());
            final JSONObject scriptsObject = (JSONObject) jsonObject.get("scripts");

            if (scriptsObject != null) {
                final Set<Entry<String, String>> entrySet = scriptsObject.entrySet();

                for (Entry<String, String> entry : entrySet) {
                    // https://regex101.com/r/Dz8k7M/5
                    final String taskName = String.format("%s", entry.getKey()); // TODO: Take regex from url.
                    final Pattern p = Pattern.compile(taskName);
                    final Matcher matcher = p.matcher(editor.getText());

                    if (matcher.find()) {
                        currentNpmScripts.add(new NpmScript(taskName, (NbDocument.findLineNumber((StyledDocument) document, matcher.start()) + 1)));
                    }
                }
            }
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ParseException | IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);

        Utilities.runViewHierarchyTransaction(textComponent, true, () -> paintComponentUnderLock(g));
    }

    private void paintComponentUnderLock(Graphics g) {
        Rectangle clip = g.getClipBounds();

        g.setColor(backgroundColor());

        if (clip != null) {
            g.fillRect(clip.x, clip.y, clip.width, clip.height);
        }

        JTextComponent component = textComponent;
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

        List<NpmScript> paintNpmScripts = currentNpmScripts;
        if (paintNpmScripts == null || paintNpmScripts.isEmpty()) {
            return;
        }

        try {
            drawColorRect(component, editorUI, paintNpmScripts, rootView, g);
        } catch (BadLocationException ex) {

        }
    }

    private Color backgroundColor() {
        Container c = getParent();
        if (c == null) {
            return defaultBackground();
        } else {
            return c.getBackground();
        }
    }

    private Color defaultBackground() {
        if (textComponent != null) {
            return textComponent.getBackground();
        }
        return Color.WHITE;
    }

    private NpmScript getNpmScript(List<NpmScript> npmScripts) {
        if (npmScripts == null) {
            return null;
        }

        for (int i = 0; i < npmScripts.size(); i++) {
            NpmScript npmScript = npmScripts.get(i);

            return npmScript;
        }

        return null;
    }

    private class TrianglesPanel extends JPanel {

        private final int recY;

        private final int editorUILineAscent;

        public TrianglesPanel(Rectangle2D rec, EditorUI editoUi) {
            this.recY = (int) rec.getY();
            this.editorUILineAscent = editoUi.getLineAscent();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            g.setColor(new Color(0, 170, 0));
            // Zeichne das Dreieck an der richtigen Stelle
            int[] xPoints = {5, 15, 5};
            int[] yPoints = {recY + editorUILineAscent + 5, recY + editorUILineAscent + 15, recY + editorUILineAscent + 25};
            g.fillPolygon(xPoints, yPoints, 3);
        }
    }

    private void drawColorRect(JTextComponent component, EditorUI editorUI, List<NpmScript> paintNpmScripts, View rootView, Graphics g) throws BadLocationException {
        paintNpmScripts.sort((elem1, elem2) -> {
            return elem1.getLineNumber() - elem2.getLineNumber();
        });

        int startViewIndex = paintNpmScripts.get(0).getLineNumber();

        View view;

        for (int i = startViewIndex; i < (paintNpmScripts.size() + startViewIndex); i++) {
            view = rootView.getView(i);
            if (view == null) {
                break;
            }

            Rectangle2D rec1 = component.modelToView2D(view.getStartOffset() - (paintNpmScripts.size() + startViewIndex));
            NpmScript ad = getNpmScript(paintNpmScripts);
            if (ad != null) {
                TrianglesPanel trianglesPanel = new TrianglesPanel(rec1, editorUI);
                trianglesPanel.paintComponent(g);
            }
        }
    }
}
