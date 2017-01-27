package jumptest.junit;

import java.util.Map;

import com.vividsolutions.jump.util.Range;

import junit.framework.TestCase;

public class RangeTestCase extends TestCase {

    public RangeTestCase(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(RangeTestCase.class);
    }
    
    public void test() {
        Range a = new Range(new Double(0), true, new Double(0.5), false); 
        Map map = new Range.RangeTreeMap();
        map.put(a, "X");
        assertNotNull(map.get(a));
    }

}
