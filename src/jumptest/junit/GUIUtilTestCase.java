package jumptest.junit;
import java.io.File;

import junit.framework.TestCase;

import com.vividsolutions.jump.workbench.ui.GUIUtil;

public class GUIUtilTestCase extends TestCase {

    public GUIUtilTestCase(String Name_) {
        super(Name_);
    }

    public static void main(String[] args) {
        String[] testCaseName = { GUIUtilTestCase.class.getName()};
        junit.textui.TestRunner.main(testCaseName);
    }

    public void testGetExtension() {
        assertEquals("xml", GUIUtil.getExtension(new File("C:\\junk\\a.xml")));
        assertEquals("", GUIUtil.getExtension(new File("C:\\junk\\a.")));
        assertEquals("", GUIUtil.getExtension(new File("C:\\junk\\a")));
    }

    public void testCreateFileFilter() {
        assertEquals(
            "MS-Access (*.mdb; *.ldb)",
            GUIUtil
                .createFileFilter("MS-Access", new String[] { "mdb", "ldb" })
                .getDescription());
    }

} 


