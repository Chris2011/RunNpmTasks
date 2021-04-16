package org.chrizzly.runnpmtasks;

import javax.swing.JComponent;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.spi.editor.SideBarFactory;

/**
 *
 * @author Chrl
 */
//@MimeRegistration(mimeType = "text/package+x-json", service = SideBarFactory.class)
public class RunNpmTasksSideBarFactory implements SideBarFactory {

    @Override
    public JComponent createSideBar(JTextComponent target) {
        return new RunNpmTasksSideBarPanel(target);
    }
}
