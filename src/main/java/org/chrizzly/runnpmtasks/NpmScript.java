package org.chrizzly.runnpmtasks;

/**
 *
 * @author Chris
 */
public class NpmScript {
    private final String name;
    private final int lineNumber;

    public NpmScript(String name, int lineNumber) {
        this.name = name;
        this.lineNumber = lineNumber;
    }

    public String getName() {
        return name;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
