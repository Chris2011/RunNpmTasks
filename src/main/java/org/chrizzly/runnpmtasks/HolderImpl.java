package org.chrizzly.runnpmtasks;

import javax.swing.text.Document;
import org.openide.explorer.ExplorerManager;

/**
 *
 * @author Chrl
 */
public class HolderImpl {
    public static HolderImpl get(Document doc) {
        HolderImpl instance = (HolderImpl) doc.getProperty(HolderImpl.class);

        if (instance == null) {
            doc.putProperty(HolderImpl.class, instance = new HolderImpl());
        }

        return instance;
    }

    private final ExplorerManager manager = new ExplorerManager();

    public ExplorerManager getManager() {
        return manager;
    }
}
