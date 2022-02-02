package i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Locale;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.vividsolutions.jump.I18N;

@TestMethodOrder(OrderAnnotation.class)
public class I18NTest {

  private static I18N i18n = I18N.getInstance("i18n");
  private static I18N i18n_default = I18N.getInstance();
  private static I18N i18n_iso = I18N.getInstance(new File("i18n/language/iso88591"));
  private static I18N i18n_utf = I18N.getInstance(new File("i18n/language/utf8"));

  @Test
  @Order(1)
  public void testOJI18N() throws Exception {
//    System.out.println(new Object(){}.getClass().getEnclosingMethod().getName());
    Locale loc = I18N.fromCode("en_US");
    I18N.setLocale(loc);
    assertEquals("OpenJUMP", i18n_default.get("JUMPWorkbench.jump"));
    loc = I18N.fromCode("de_DE");
    I18N.setLocale(loc);
    assertEquals("OpenJUMP", i18n_default.get("JUMPWorkbench.jump"));
  }

  @Test
  @Order(2)
  public void testGetEnglish() throws Exception {
//    System.out.println(new Object(){}.getClass().getEnclosingMethod().getName());
    Locale loc = I18N.fromCode("en_US");
    I18N.setLocale(loc);
    assertEquals("Test Key", i18n.get("test.key"));
  }

  @Test
  @Order(3)
  public void testGetGerman() throws Exception {
//    System.out.println(new Object(){}.getClass().getEnclosingMethod().getName());
    Locale loc = I18N.fromCode("de_DE");
    I18N.setLocale(loc);
    assertEquals("Test Schlüssel", i18n.get("test.key"));
  }

  private final static String[] strings = new String[] { "one", "two", "three", "four", "five" };

  @Test
  @Order(4)
  public void testGetObjects() throws Exception {
//    System.out.println(new Object(){}.getClass().getEnclosingMethod().getName());
    Locale loc = I18N.fromCode("en_US");
    I18N.setLocale(loc);
    assertEquals("Test one, two, three, four", i18n.get("test.objects", strings));
    assertEquals("Test four, three, two, one", i18n.get("test.objects.reversed", strings));
  }

  @Test
  @Order(5)
  public void testProperBundleLoading() throws Exception {
    Locale loc;

    // all en variants do not exist, hence should use root
    loc = I18N.fromCode("en_US");
    I18N.setLocale(loc);
    assertEquals("root", i18n.get("locale"));
    loc = I18N.fromCode("en");
    I18N.setLocale(loc);
    assertEquals("root", i18n.get("locale"));

    loc = I18N.fromCode("de_DE");
    I18N.setLocale(loc);
    assertEquals("de_DE", i18n.get("locale"));
    loc = I18N.fromCode("de");
    I18N.setLocale(loc);
    assertEquals("de", i18n.get("locale"));
    // de_AT does not exist, should use de
    loc = I18N.fromCode("de_AT");
    I18N.setLocale(loc);
    assertEquals("de", i18n.get("locale"));
  }

  @Test
  @Order(6)
  public void testFallThrough() throws Exception {
    Locale loc = I18N.fromCode("de_DE");
    I18N.setLocale(loc);
    assertEquals("root", i18n.get("fallthrough_root"));
    assertEquals("de", i18n.get("fallthrough_de"));
    assertEquals("de", i18n.get("fallthrough_commented_de"));
    assertEquals("root", i18n.get("fallthrough_commented_root"));
  }

  @Test
  @Order(7)
  public void testPlaceholder() throws Exception {
    Locale loc = I18N.fromCode("de_DE");
    I18N.setLocale(loc);
    assertEquals("Winkel", i18n.get("placeholder_deDE_angle"));
    loc = I18N.fromCode("en_US");
    I18N.setLocale(loc);
    assertEquals("angle", i18n.get("placeholder_root_angle"));
  }

  @Test
  @Order(10)
  public void testIso() throws Exception {
    I18N.setEncoding("ISO-8859-1");
    assertEquals("äöü", i18n_iso.get("umlauts.plain"));
    assertEquals("äöü", i18n_iso.get("umlauts.escaped"));
  }

  @Test
  @Order(10)
  public void testUtf() throws Exception {
    I18N.setEncoding("UTF-8");
    assertEquals("äöü", i18n_utf.get("umlauts.plain"));
    assertEquals("äöü", i18n_utf.get("umlauts.escaped"));
  }

  @Test
  @Order(11)
  public void testPlaceholderFromOtherPackage() throws Exception {
    assertEquals("angle", i18n_utf.get("placeholder_utf_angle"));
  }
}
