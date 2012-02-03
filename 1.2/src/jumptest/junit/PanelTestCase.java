package jumptest.junit;

import junit.framework.TestCase;

import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.LayerViewPanelContext;

public class PanelTestCase extends TestCase {

  public PanelTestCase(String Name_) {
    super(Name_);
  }

  public static void main(String[] args) {
    String[] testCaseName = {PanelTestCase.class.getName()};
    junit.textui.TestRunner.main(testCaseName);
  }

  private class TestPanel extends LayerViewPanel {
    public TestPanel() {
      super(new LayerManager(), new LayerViewPanelContext() {
        public void setStatusMessage(String message) {}
        public void handleThrowable(Throwable t) { assertTrue(false); }
        public void warnUser(String warning) {}
      });
    }
    protected String format(double d, double pixelWidthInModelUnits) {
      return super.format(d, pixelWidthInModelUnits);
    }
  }

  public void testFormat() {
    TestPanel panel = new TestPanel();
    assertEquals("123.5", panel.format(123.456, 2222));
    assertEquals("123.5", panel.format(123.456, 7));
    assertEquals("123.5", panel.format(123.456, 1));
    assertEquals("123.46", panel.format(123.456, 0.1));
    assertEquals("123.46", panel.format(123.456, 0.09));
    assertEquals("123.456", panel.format(123.456, 0.01));
    assertEquals("123.456", panel.format(123.456, 0.001));
  }
}
