package jumptest.junit;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.util.StringUtil;

public class CollectionUtilTestCase extends TestCase {

  public CollectionUtilTestCase(String Name_) {
    super(Name_);
  }

  public static void main(String[] args) {
    String[] testCaseName = {CollectionUtilTestCase.class.getName()};
    junit.textui.TestRunner.main(testCaseName);
  }

  public void testCombinations5() {
    List combinations = CollectionUtil.combinations(Arrays.asList(new String[]{
          "A", "B", "C"}), 2, "B");
    assertEquals(3, combinations.size());
    assertEquals("B", StringUtil.toCommaDelimitedString((List)combinations.get(0)));
    assertEquals("A, B", StringUtil.toCommaDelimitedString((List)combinations.get(1)));
    assertEquals("B, C", StringUtil.toCommaDelimitedString((List)combinations.get(2)));
  }

  public void testCombinations() {
    List combinations = CollectionUtil.combinations(Arrays.asList(new String[]{
          "A", "B", "C"}));
    assertEquals(7, combinations.size());
    assertEquals("A", StringUtil.toCommaDelimitedString((List)combinations.get(0)));
    assertEquals("B", StringUtil.toCommaDelimitedString((List)combinations.get(1)));
    assertEquals("A, B", StringUtil.toCommaDelimitedString((List)combinations.get(2)));
    assertEquals("C", StringUtil.toCommaDelimitedString((List)combinations.get(3)));
    assertEquals("A, C", StringUtil.toCommaDelimitedString((List)combinations.get(4)));
    assertEquals("B, C", StringUtil.toCommaDelimitedString((List)combinations.get(5)));
    assertEquals("A, B, C", StringUtil.toCommaDelimitedString((List)combinations.get(6)));
  }


  public void testCombinations2() {
    List combinations = CollectionUtil.combinations(Arrays.asList(new String[]{
          "A", "B", "C"}), 2);
    assertEquals(6, combinations.size());
    assertEquals("A", StringUtil.toCommaDelimitedString((List)combinations.get(0)));
    assertEquals("B", StringUtil.toCommaDelimitedString((List)combinations.get(1)));
    assertEquals("A, B", StringUtil.toCommaDelimitedString((List)combinations.get(2)));
    assertEquals("C", StringUtil.toCommaDelimitedString((List)combinations.get(3)));
    assertEquals("A, C", StringUtil.toCommaDelimitedString((List)combinations.get(4)));
    assertEquals("B, C", StringUtil.toCommaDelimitedString((List)combinations.get(5)));
  }

  public void testCombinations3() {
    List combinations = CollectionUtil.combinations(Arrays.asList(new String[]{
          "A"}), 2);
    assertEquals(1, combinations.size());
    assertEquals("A", StringUtil.toCommaDelimitedString((List)combinations.get(0)));
  }

  public void testCombinations4() {
    List combinations = CollectionUtil.combinations(Arrays.asList(new String[]{
          "A"}), 0);
    assertEquals(0, combinations.size());
  }
  
  public void testResize() {
      List list = StringUtil.fromCommaDelimitedString("A, B, C, D, E");
      CollectionUtil.resize(list, 3);
      assertEquals("A, B, C", StringUtil.toCommaDelimitedString(list)); 
  }
  public void testResize2() {
      List list = StringUtil.fromCommaDelimitedString("A, B, C, D, E");
      CollectionUtil.resize(list, 7);
      assertEquals("A, B, C, D, E, , ", StringUtil.toCommaDelimitedString(list)); 
  }  
  
  public void testSetIfNull() {
      List list = StringUtil.fromCommaDelimitedString("A, B, C, D, E");
      CollectionUtil.setIfNull(3, list, "F");
      assertEquals("A, B, C, D, E", StringUtil.toCommaDelimitedString(list));       
  }
  public void testSetIfNull2() {
      List list = StringUtil.fromCommaDelimitedString("A, B, C, D, E");
      list.set(3, null);
      CollectionUtil.setIfNull(3, list, "F");
      assertEquals("A, B, C, F, E", StringUtil.toCommaDelimitedString(list));       
  }  
}
