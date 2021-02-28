package com.vividsolutions.jump.workbench;

/**
 * Installs most of the menus and toolbar buttons. Called when the app starts up.
 */
public interface Setup {
    void setup(WorkbenchContext workbenchContext) throws Exception;
}
