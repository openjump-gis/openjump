package com.vividsolutions.jump.workbench.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * A Macro is a list of actions which can be persisted in a xml file in order
 * to be replayed.
 */
public class Macro {

    List<Recordable> processes = new ArrayList<Recordable>();

    public Macro() {}

    public void addProcess(Recordable process) {
        processes.add(process);
    }

    public List<Recordable> getProcesses() {
        return processes;
    }
}
