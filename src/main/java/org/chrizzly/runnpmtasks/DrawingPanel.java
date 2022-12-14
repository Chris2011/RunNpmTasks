package org.chrizzly.runnpmtasks;

import java.awt.Color;
import java.awt.Container;
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

    public DrawingPanel(JTextComponent editor) {
        textComponent = editor;
        document = (BaseDocument) editor.getDocument();
        currentNpmScripts = new ArrayList<>(0);

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
        g.fillRect(clip.x, clip.y, clip.width, clip.height);

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

    private void drawColorRect(JTextComponent component, EditorUI editorUI, List<NpmScript> paintNpmScripts, View rootView, Graphics g) throws BadLocationException {
        paintNpmScripts.sort((elem1, elem2) -> {
            return elem1.getLineNumber() - elem2.getLineNumber();
        });

        int startViewIndex = paintNpmScripts.get(0).getLineNumber();
        int[] yCoords = new int[3];

        View view;

        for (int i = startViewIndex; i < (paintNpmScripts.size() + startViewIndex); i++) {
            view = rootView.getView(i);
            if (view == null) {
                break;
            }

            NpmScript ad = getNpmScript(paintNpmScripts);
            Rectangle2D rec1 = component.modelToView2D(view.getStartOffset() - (paintNpmScripts.size() + startViewIndex));

            if (rec1 == null) {
                break;
            }

            if (ad != null) {
                g.setColor(new Color(0, 170, 0));
                yCoords[0] = (int) rec1.getY() + editorUI.getLineAscent();
                yCoords[1] = (int) rec1.getY() + editorUI.getLineAscent() * 3 / 2;
                yCoords[2] = (int) rec1.getY() + editorUI.getLineAscent() * 2;

                g.fillPolygon(new int[]{3, 16, 3}, yCoords, 3);
            }
        }
    }
}
