package jumptest.junit;

import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.ui.AttributeTypeFilter;
import junit.framework.TestCase;

import java.util.ArrayList;

/**
 * Test AttributeTypeFilter
 */
public class AttributeTypeFilterTestCase extends TestCase {

    public void testgetInverse() {
        FeatureSchema schema = new FeatureSchema();
        schema.addAttribute("geometry", AttributeType.GEOMETRY);
        schema.addAttribute("boolean",  AttributeType.BOOLEAN);
        schema.addAttribute("integer",  AttributeType.INTEGER);
        schema.addAttribute("long",     AttributeType.LONG);
        schema.addAttribute("double",   AttributeType.DOUBLE);
        schema.addAttribute("date",     AttributeType.DATE);
        schema.addAttribute("string",   AttributeType.STRING);
        schema.addAttribute("object",   AttributeType.OBJECT);
        int size = schema.getAttributeCount();
        assertTrue(AttributeTypeFilter.GEOMETRY_FILTER.filter(schema).size() == 1);
        assertTrue(AttributeTypeFilter.GEOMETRY_FILTER.getInverseFilter().filter(schema).size() == size-1);
        assertTrue(AttributeTypeFilter.STRING_FILTER.filter(schema).size() == 1);
        assertTrue(AttributeTypeFilter.STRING_FILTER.getInverseFilter().filter(schema).size() == size-1);
        assertTrue(AttributeTypeFilter.NUMERIC_FILTER.filter(schema).size() == 3);
        assertTrue(AttributeTypeFilter.NUMERIC_FILTER.getInverseFilter().filter(schema).size() == size-3);
        assertTrue(AttributeTypeFilter.NUMSTRING_FILTER.filter(schema).size() == 4);
        assertTrue(AttributeTypeFilter.NUMSTRING_FILTER.getInverseFilter().filter(schema).size() == size-4);
    }
}
