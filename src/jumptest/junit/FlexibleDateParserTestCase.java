package jumptest.junit;

import com.vividsolutions.jump.util.FlexibleDateParser;
import junit.framework.TestCase;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class FlexibleDateParserTestCase extends TestCase {
    private FlexibleDateParser parser = new FlexibleDateParser();
    private SimpleDateFormat simpleFormat1 = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat simpleFormat2 = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss:SSS");
    private SimpleDateFormat simpleFormat3 = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss:SSS zzz");
    private int year = Calendar.getInstance().get(Calendar.YEAR);

    public FlexibleDateParserTestCase(String arg0) {
        super(arg0);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(FlexibleDateParserTestCase.class);
    }

    public void test() throws Exception {
        parser.setVerbose(true);

        //Some of these test are based on the formats in Excel [Jon Aquino]
        assertEquals(simpleFormat1.parse("2003-05-21"),
            parser.parse("2003-05-21", false));
        assertEquals(simpleFormat1.parse("2003-05-21"),
            parser.parse("2003.05.21", false));
        //assertEquals(simpleFormat1.parse(year + "-03-14"),
        //    parser.parse("3/14", false));
        //assertEquals(simpleFormat1.parse("1998-03-14"),
        //    parser.parse("3/14/98", false));
        //assertEquals(simpleFormat1.parse("1998-03-14"),
        //    parser.parse("03/14/98", false));
        //assertEquals(simpleFormat1.parse(year + "-03-14"),
        //    parser.parse("14-Mar", false));
        //assertEquals(simpleFormat1.parse("1998-03-14"),
        //    parser.parse("14-Mar-98", false));
        assertEquals(simpleFormat1.parse("1998-03-01"),
            parser.parse("Mar-1998", false));
        assertEquals(simpleFormat1.parse("1998-03-01"),
            parser.parse("March-1998", false));
        assertEquals(simpleFormat1.parse("1998-03-14"),
            parser.parse("March 14, 1998", false));
        //assertEquals(simpleFormat2.parse("1998-03-14 13:30:00:000"),
        //    parser.parse("3/14/98 1:30 PM", false));
        //assertEquals(simpleFormat2.parse("1998-03-14 13:30:00:000"),
        //    parser.parse("3/14/98 13:30", false));
        assertEquals(simpleFormat1.parse("1998-03-14"),
            parser.parse("3/14/1998", false));
        assertEquals(simpleFormat1.parse("1998-03-14"),
            parser.parse("14-Mar-1998", false));
        assertEquals(simpleFormat2.parse(year + "-01-01 13:30:00:000"),
            parser.parse("13:30", false));
        assertEquals(simpleFormat2.parse(year + "-01-01 13:30:00:000"),
            parser.parse("1:30 PM", false));
        assertEquals(simpleFormat2.parse(year + "-01-01 13:30:55:000"),
            parser.parse("13:30:55", false));
        assertEquals(simpleFormat2.parse(year + "-01-01 13:30:55:000"),
            parser.parse("1:30:55 PM", false));
        //assertEquals(simpleFormat3.parse("2003-01-06 17:01:02:000 PST"),
        //    parser.parse("Jan 06 17:01:02 PST 2003", false));
        assertEquals(simpleFormat1.parse("1970-06-01"),
            parser.parse("Jun 1970", false));
        assertEquals(simpleFormat1.parse(year + "-06-19"),
            parser.parse("Jun 19", false));
        assertEquals(simpleFormat1.parse("1970-06-01"),
            parser.parse("June 1970", false));
        assertEquals(simpleFormat1.parse(year + "-06-19"),
            parser.parse("June 19", false));
        assertEquals(simpleFormat1.parse("2003-09-19"),
            parser.parse("Sep 19, 2003", false));

        //try {
           //assertEquals(simpleFormat1.parse(year + "-09-19"),
           //     parser.parse("Sept 19", false));
           //assertTrue(false);
        //} catch (ParseException e) {
        //    assertTrue(true);
        //}

        //American style preferred to European style. [Jon Aquino]
        assertEquals(simpleFormat1.parse("2004-02-03"),
            parser.parse("02/03/2004", false));

        //FME GML format. [Jon Aquino]            
        assertEquals(simpleFormat1.parse("2004-02-03"),
            parser.parse("20040203", false));
        assertNull(parser.parse("", false));
        assertNull(parser.parse("", true));
        assertNull(parser.parse(" ", false));
        assertNull(parser.parse(" ", true));
        //assertEquals(simpleFormat1.parse("2004-02-03"),
        //    parser.parse("02/03/04", false));
        //assertEquals(simpleFormat1.parse("1999-03-04"),
        //    parser.parse("99/03/04", false));
    }
}
