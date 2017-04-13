package org.openjump.core.ccordsys;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

import static org.openjump.core.ccordsys.Quantity.ANGLE;
import static org.openjump.core.ccordsys.Quantity.LENGTH;

/**
 * List of UOM is created from ESG database with the following code :
 * SELECT
 *     rpad(upper(regexp_replace(unit_of_meas_name,'[ ''\\(\\)-]+','_','g')), 16) || '(' ||
 *     rpad('"' || unit_of_meas_name || '"', 32, ' ') || ', ' ||
 *     uom_code::text || ', ' ||
 *     rpad(upper(unit_of_meas_type), 6) || ', ' ||
 *     rpad((coalesce(factor_b,0)/coalesce(factor_c,1))::text, 21, ' ') || ', ' ||
 *     '"m"' || '),'
 * FROM epsg_unitofmeasure
 * WHERE deprecated = 0 AND unit_of_meas_type IN ('length','angle') AND uom_code > 9000
 * ORDER BY uom_code
 *
 * Some handwriting reformatting/correction are necessary though
 * Some abbreviations have been found in http://w3.energistics.org/uom/poscUnits22.xml
 */
public enum Unit {
    UNKNOWN         ("Unknown", 0,                 Quantity.UNKNOWN, 0.0                  , ""),
    METRE           ("metre"                         , 9001, LENGTH, 1                    , "m"),
    FOOT            ("foot"                          , 9002, LENGTH, 0.3048               , "ft"),
    US_SURVEY_FOOT  ("US survey foot"                , 9003, LENGTH, 0.304800609601219    , "ftUS"),
    CLARKE_S_FOOT   ("Clarke's foot"                 , 9005, LENGTH, 0.3047972654         , "ftCla"),
    FATHOM          ("fathom"                        , 9014, LENGTH, 1.8288               , "fathom"),
    NAUTICAL_MILE   ("nautical mile"                 , 9030, LENGTH, 1852                 , "nmi"),
    GERMAN_METER    ("German legal metre"            , 9031, LENGTH, 1.0000135965         , "mGer"),
    US_SURVEY_CHAIN ("US survey chain"               , 9033, LENGTH, 20.1168402336805     , "chUS"),
    US_SURVEY_LINK  ("US survey link"                , 9034, LENGTH, 0.201168402336805    , "lkUS"),
    US_SURVEY_MILE  ("US survey mile"                , 9035, LENGTH, 1609.34721869444     , "miUS"),
    KILOMETRE       ("kilometre"                     , 9036, LENGTH, 1000.0               , "km"),
    CLARKE_S_YARD   ("Clarke's yard"                 , 9037, LENGTH, 0.9143917962         , "ydCla"),
    CLARKE_S_CHAIN  ("Clarke's chain"                , 9038, LENGTH, 20.1166195164        , "chCla"),
    CLARKE_S_LINK   ("Clarke's link"                 , 9039, LENGTH, 0.201166195164       , "lkCla"),
    SEARS_YARD      ("British yard (Sears 1922)"     , 9040, LENGTH, 0.914398414616029    , "ydSe"),
    SEARS_FOOT      ("British foot (Sears 1922)"     , 9041, LENGTH, 0.304799471538676    , "ftSe"),
    SEARS_CHAIN     ("British chain (Sears 1922)"    , 9042, LENGTH, 20.1167651215526     , "chSe"),
    SEARS_LINK      ("British link (Sears 1922)"     , 9043, LENGTH, 0.201167651215526    , "lkSe"),
    BENOIT_YARD_A   ("British yard (Benoit 1895 A)"  , 9050, LENGTH, 0.9143992            , "ydBnA"),
    BENOIT_FOOT_A   ("British foot (Benoit 1895 A)"  , 9051, LENGTH, 0.304799733333333    , "ftBnA"),
    BENOIT_CHAIN_A  ("British chain (Benoit 1895 A)" , 9052, LENGTH, 20.1167824           , "chBnA"),
    BENOIT_LINK_A   ("British link (Benoit 1895 A)"  , 9053, LENGTH, 0.201167824          , "lkBnA"),
    BENOIT_YARD_B   ("British yard (Benoit 1895 B)"  , 9060, LENGTH, 0.914399204289812    , "ydBnB"),
    BENOIT_FOOT_B   ("British foot (Benoit 1895 B)"  , 9061, LENGTH, 0.304799734763271    , "ftBnB"),
    BENOIT_CHAIN_B  ("British chain (Benoit 1895 B)" , 9062, LENGTH, 20.1167824943759     , "chBnB"),
    BENOIT_LINK_B   ("British link (Benoit 1895 B)"  , 9063, LENGTH, 0.201167824943759    , "lkBnB"),
    BRITISH_FOOT_65 ("British foot (1865)"           , 9070, LENGTH, 0.304800833333333    , "ftBr(65)"),
    INDIAN_FOOT     ("Indian foot"                   , 9080, LENGTH, 0.304799510248147    , "ftInd"),
    INDIAN_FOOT_1937("Indian foot (1937)"            , 9081, LENGTH, 0.30479841           , "ftInd(37)"),
    INDIAN_FOOT_1962("Indian foot (1962)"            , 9082, LENGTH, 0.3047996            , "ftInd(62)"),
    INDIAN_FOOT_1975("Indian foot (1975)"            , 9083, LENGTH, 0.3047995            , "ftInd(75)"),
    INDIAN_YARD     ("Indian yard"                   , 9084, LENGTH, 0.914398530744441    , "ydInd"),
    INDIAN_YARD_1937("Indian yard (1937)"            , 9085, LENGTH, 0.91439523           , "ydInd(1937)"),
    INDIAN_YARD_1962("Indian yard (1962)"            , 9086, LENGTH, 0.9143988            , "ydInd(1962)"),
    INDIAN_YARD_1975("Indian yard (1975)"            , 9087, LENGTH, 0.9143985            , "ydInd(1975)"),
    STATUTE_MILE    ("Statute mile"                  , 9093, LENGTH, 1609.344             , "miUS"),
    GOLD_COAST_FOOT ("Gold Coast foot"               , 9094, LENGTH, 0.304799710181509    , "ftGC"),
    BRITISH_FOOT_36 ("British foot (1936)"           , 9095, LENGTH, 0.3048007491         , "ftBr(36)"),
    YARD            ("yard"                          , 9096, LENGTH, 0.9144               , "yd"),
    CHAIN           ("chain"                         , 9097, LENGTH, 20.1168              , "ch"),
    LINK            ("link"                          , 9098, LENGTH, 0.201168             , "lk"),
    SEARS_YARD_TR   ("British yard (Sears 1922 truncated)", 9099, LENGTH, 0.914398        , "ydSeTr"),
    RADIAN          ("radian"                        , 9101, ANGLE , 1.0                  , "rad"),
    DEGREE          ("degree"                        , 9102, ANGLE , 0.0174532925199433   , "deg"),
    ARC_MINUTE      ("arc-minute"                    , 9103, ANGLE , 0.000290888208665721 , "min"),
    ARC_SECOND      ("arc-second"                    , 9104, ANGLE , 4.84813681109535e-006, "sec"),
    GRAD            ("grad"                          , 9105, ANGLE , 0.015707963267949    , "gr"),
    DMS             ("degree minute second"          , 9107, ANGLE , 0                    , ""),
    DMSH            ("degree minute second hemisphere", 9108, ANGLE , 0                    , ""),
    MICRORADIAN     ("microradian"                   , 9109, ANGLE , 1e-006               , "\u00B5m"),
    SEXAGESIMAL_DMS ("sexagesimal DMS"               , 9110, ANGLE , 0                    , ""),
    SEXAGESIMAL_DM  ("sexagesimal DM"                , 9111, ANGLE , 0                    , ""),
    CENTESIMAL_MIN  ("centesimal minute"             , 9112, ANGLE , 0.000157079632679489 , "cgr"),
    CENTESIMAL_SEC  ("centesimal second"             , 9113, ANGLE , 1.5707963267949e-006 , "ccgr"),
    MIL_6400        ("mil_6400"                      , 9114, ANGLE , 0.000981747704246809 , "mil"),
    DEGREE_MINUTE   ("degree minute"                 , 9115, ANGLE , 0                    , ""),
    DEGREE_HEM      ("degree hemisphere"             , 9116, ANGLE , 0                    , ""),
    HEM_DEGREE      ("hemisphere degree"             , 9117, ANGLE , 0                    , ""),
    DMH             ("degree minute hemisphere"      , 9118, ANGLE , 0                    , ""),
    HDM             ("hemisphere degree minute"      , 9119, ANGLE , 0                    , ""),
    HDMS            ("hemisphere degree minute sec"  , 9120, ANGLE , 0                    , ""),
    SEXA_DMS_S      ("sexagesimal DMS.s"             , 9121, ANGLE , 0                    , ""),
    DEGREE_SUPPLIER_("degree (supplier to define representation)", 9122, ANGLE , 0.0174532925199433, ""),
    SEARS_FOOT_TR   ("British foot (Sears 1922 truncated)", 9300, LENGTH, 0.304799333333333,"ftSe"),
    SEARS_CHAIN_TR  ("British chain (Sears 1922 truncated)", 9301, LENGTH, 20.116756      , "chSe"),
    SEARS_LINK_TR   ("British link (Sears 1922 truncated)", 9302, LENGTH, 0.20116756      , "lkSe");


    private String name;
    private int epsgCode;
    private Quantity quantity;
    private double siFactor;
    private String abbreviation;
    static Map<String,Unit> map = new HashMap<>();

    static {
        for (Unit u : Unit.values()) {
            map.put(u.name().toLowerCase(), u);
            map.put(u.getName().toLowerCase(), u);
            map.put(Integer.toString(u.getEpsgCode()), u);
            if (u.getAbbreviation().length()>0) map.put(u.getAbbreviation(), u);
        }
    }

    Unit(String name, int epsgCode, Quantity quantity, double siFactor, String abbreviation) {
        this.name = name;
        this.epsgCode = epsgCode;
        this.quantity = quantity;
        this.siFactor = siFactor;
        this.abbreviation = abbreviation;
    }

    String getName() {
        return name;
    }

    int getEpsgCode() {
        return epsgCode;
    }

    Quantity getQuantity() {
        return quantity;
    }

    double getSIFactor() {
        return siFactor;
    }

    String getAbbreviation() {
        return abbreviation;
    }

    public static Unit find(String nameOrCode) {
        if (nameOrCode.trim().length() == 0) return UNKNOWN;
        nameOrCode = Normalizer.normalize(nameOrCode, Normalizer.Form.NFD); // separe base character from accent
        nameOrCode = nameOrCode.replaceAll("\\p{M}", ""); // remove accents
        nameOrCode = nameOrCode.toLowerCase();
        nameOrCode = nameOrCode.replaceAll("feet","foot");
        nameOrCode = nameOrCode.replaceAll("meter","metre");
        nameOrCode = nameOrCode.replaceAll("grade","grad");
        nameOrCode = nameOrCode.replaceAll("(metre|yard|mile|degree|grad|radian)s\\b","$1");
        return map.get(nameOrCode);
    }

    public String toString() {
        return name;
    }

}
