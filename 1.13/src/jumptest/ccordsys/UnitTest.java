package jumptest.ccordsys;

import junit.framework.TestCase;
import org.openjump.core.ccordsys.Unit;
import org.openjump.core.ccordsys.utils.SRSInfo;
import org.openjump.core.rasterimage.TiffTags;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * Created by UMichael on 11/09/2016.
 */
public class UnitTest extends TestCase {

    public void testFindFromLengthName() throws Exception {
        assertEquals("unit METRE from metre", Unit.METRE, Unit.find("metre"));
        assertEquals("unit METRE from metres", Unit.METRE, Unit.find("metres"));
        assertEquals("unit METRE from mètre", Unit.METRE, Unit.find("mètre"));
        assertEquals("unit METRE from METRE", Unit.METRE, Unit.find("METRE"));
        assertEquals("unit METRE from METER", Unit.METRE, Unit.find("METER"));
        assertEquals("unit METRE from m", Unit.METRE, Unit.find("m"));
        assertEquals("unit METRE from 9001", Unit.METRE, Unit.find("9001"));
    }

    public void testFindFromAngleName() throws Exception {
        assertEquals("unit DEGREE from degree", Unit.DEGREE, Unit.find("degree"));
        assertEquals("unit DEGREE from deg", Unit.DEGREE, Unit.find("deg"));
        assertEquals("unit DEGREE from name", Unit.DEGREE, Unit.find("9102"));
    }
}
