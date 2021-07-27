package workbench;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Iterator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.math.plot.utils.Array;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.vividsolutions.jump.util.commandline.CommandLine;
import com.vividsolutions.jump.util.commandline.Param;
import com.vividsolutions.jump.util.commandline.ParamSpec;
import com.vividsolutions.jump.util.commandline.ParseException;

@TestMethodOrder(OrderAnnotation.class)
public class CommandLineTest {

  static CommandLine c = new CommandLine();

  static String[] paramsArray = new String[] { "-parameter0", "-parameter1", "arg1", "-parameter2", "arg1", "arg2",
      "-parameter3", "arg1", "arg2", "arg3" };

  @Test
  @Order(1)
  public void setup() throws ParseException {
    c.addParamSpec(new ParamSpec(new String[] { "p0", "parameter0" }, 0, "parameter w/o arguments"));
    c.addParamSpec(new ParamSpec("parameter1", 1, "param with one arg"));
    c.addParamSpec(new ParamSpec("parameter2", 2, "param with two args"));
    c.addParamSpec(new ParamSpec("parameter3", 3, "param with three args"));
    System.out.println(c.printDoc());
  }

  @Test
  @Order(2)
  public void parseMissingFile() {
    try {
      c.parse(new String[] { "missing.file" });
      fail("CommandLine did not detect missing file");
    } catch (Exception e) {
      assertThat(e, instanceOf(ParseException.class));
    }
  }

  @Test
  @Order(2)
  public void parseWrongParam() {
    try {
      c.parse(new String[] { "-unknownParam", "arg0" });
      fail("CommandLine did not detect unknown param");
    } catch (Exception e) {
      assertThat(e, instanceOf(ParseException.class));
    }
  }

  @Test
  @Order(2)
  public void parseParams() throws ParseException {
    c.parse(paramsArray);
  }

  @Test
  @Order(3)
  public void testParams() {
    Iterator<Param> params = c.getParams();
    Param p0 = params.next();
    assertTrue(p0.getSpec().matches("parameter0"));
    Param p1 = params.next();
    assertTrue(p1.getSpec().matches("parameter1"));
    assertArrayEquals(Arrays.copyOfRange(paramsArray,2,3),p1.getArgs());
    Param p2 = params.next();
    assertTrue(p2.getSpec().matches("parameter2"));
    assertArrayEquals(Arrays.copyOfRange(paramsArray,4,6),p2.getArgs());
    Param p3 = params.next();
    assertTrue(p3.getSpec().matches("parameter3"));
    assertArrayEquals(Arrays.copyOfRange(paramsArray,7,10),p3.getArgs());
  }

}
