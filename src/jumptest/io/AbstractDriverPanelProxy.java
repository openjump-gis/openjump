package jumptest.io;
import java.awt.event.ActionListener;

import com.vividsolutions.jump.workbench.ui.AbstractDriverPanel;

/**
 *  A trick for getting an abstract component (AbstractDriverPanel) to show up
 *  in JBuilder's Designer. See http://www.visi.com/~gyles19/fom-serve/cache/97.html.
 *  The following line needs to be added to .jbuilder4/user.properties in your
 *  home directory: designer;proxy.com.vividsolutions.jump.workbench.ui.AbstractDriverPanel=jcstest.AbstractDriverPanelProxy
 */
public class AbstractDriverPanelProxy extends AbstractDriverPanel {

  public AbstractDriverPanelProxy() { }

  public String getValidationError() {
    return null;
  }

  public boolean wasOKPressed() {
    return false;
  }

  public void addActionListener(ActionListener l) { }

  public void removeActionListener(ActionListener l) { }
}
