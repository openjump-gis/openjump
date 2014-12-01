package jumptest.junit;

import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.LayerViewPanelContext;
import junit.framework.TestCase;

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
    assertEquals(String.format("%.1f",123.456), panel.format(123.456, 2222));
    assertEquals(String.format("%.1f",123.456), panel.format(123.456, 7));
    assertEquals(String.format("%.1f",123.456), panel.format(123.456, 1));
    assertEquals(String.format("%.2f",123.456), panel.format(123.456, 0.1));
    assertEquals(String.format("%.2f",123.456), panel.format(123.456, 0.09));
    assertEquals(String.format("%.3f",123.456), panel.format(123.456, 0.01));
    assertEquals(String.format("%.3f",123.456), panel.format(123.456, 0.001));
  }
}
