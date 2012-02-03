package jumptest.io;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.geotools.dbffile.DbfFile;

import junit.framework.TestCase;

public class DbfFileTestCase extends TestCase {

    public DbfFileTestCase(String arg0) {
        super(arg0);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(DbfFileTestCase.class);
    }
    
    private static class TestDbfFile extends DbfFile {
        public Date parseDate(String s) throws ParseException {
            return super.parseDate(s);
        }
    }
    
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
    
    private TestDbfFile dbfFile = new TestDbfFile();
    
    public void testParseDate() throws Exception {
        assertEquals(null, dbfFile.parseDate("        "));
        assertEquals(dateFormatter.parse("0001-01-01"), dbfFile.parseDate("00000000"));
        try {
            Date date = dbfFile.parseDate("99999999");
            assertTrue(date.toString(), false);
        }
        catch(ParseException e) {
        }
        assertEquals(dateFormatter.parse("0203-04-05"), dbfFile.parseDate("02030405"));        
    }

}
