package i18n;

import java.util.Locale;

import com.vividsolutions.jump.I18N;

import junit.framework.TestCase;

public class I18NTest extends TestCase {

  private static I18N i18n = I18N.getInstance("i18n");
  private static I18N i18n_default = I18N.getInstance();

  public void testOJI18N() throws Exception {
//    System.out.println(new Object(){}.getClass().getEnclosingMethod().getName());
    Locale loc = I18N.fromCode("en_US");
    I18N.setLocale(loc);
    assertEquals("OpenJUMP", i18n_default.get("JUMPWorkbench.jump"));
    loc = I18N.fromCode("de_DE");
    I18N.setLocale(loc);
    assertEquals("OpenJUMP", i18n_default.get("JUMPWorkbench.jump"));
  }

  public void testGetEnglish() throws Exception {
//    System.out.println(new Object(){}.getClass().getEnclosingMethod().getName());
    Locale loc = I18N.fromCode("en_US");
    I18N.setLocale(loc);
    assertEquals("Test Key", i18n.get("test.key"));
  }

  public void testGetGerman() throws Exception {
//    System.out.println(new Object(){}.getClass().getEnclosingMethod().getName());
    Locale loc = I18N.fromCode("de_DE");
    I18N.setLocale(loc);
    assertEquals("Test Schlüssel", i18n.get("test.key"));
  }

  private final static String[] strings = new String[] { "one", "two", "three", "four", "five" };

  public void testGetObjects() throws Exception {
//    System.out.println(new Object(){}.getClass().getEnclosingMethod().getName());
    Locale loc = I18N.fromCode("en_US");
    I18N.setLocale(loc);
    assertEquals("Test one, two, three, four", i18n.get("test.objects", strings));
    assertEquals("Test four, three, two, one", i18n.get("test.objects.reversed", strings));
  }

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

  public void testFallThrough() throws Exception {
    Locale loc = I18N.fromCode("de_DE");
    I18N.setLocale(loc);
    assertEquals("root", i18n.get("fallthrough_root"));
    assertEquals("de", i18n.get("fallthrough_de"));
    assertEquals("de", i18n.get("fallthrough_commented_de"));
    assertEquals("root", i18n.get("fallthrough_commented_root"));
  }

  public void testPlaceholder() throws Exception {
    Locale loc = I18N.fromCode("de_DE");
    I18N.setLocale(loc);
    assertEquals("Winkel", i18n.get("placeholder_deDE_angle"));
    loc = I18N.fromCode("en_US");
    I18N.setLocale(loc);
    assertEquals("angle", i18n.get("placeholder_root_angle"));
  }
}
