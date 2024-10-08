package com.vividsolutions.jump.workbench.ui.wizard;


/**
 * adds missing method definitions in older interface
 * @author ed
 */
public interface WizardPanelV2 extends WizardPanel {

  /**
   * Called in WizardDialog when the user presses Previous on this panel's 
   * next panel to (re)initialize this panel, see e.g. {@link org.openjump.core.ui.plugin.file.open.SelectFilesPanel}
   * @throws Exception if an Exception occurs
   */
  void enteredFromRight() throws Exception;
  
  /**
   * Called in WizardDialog when the user leaves this panel without taking action
   */
  void exitingToLeft();
}
