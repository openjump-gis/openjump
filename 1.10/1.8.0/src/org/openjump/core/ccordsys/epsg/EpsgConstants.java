package org.openjump.core.ccordsys.epsg;

import javax.xml.namespace.QName;

public class EpsgConstants {
  public static final String NAMESPACE_URI = "http://epsg.org/";

  public static final QName BC_ALBERS = new QName(NAMESPACE_URI, "3005");

  public static final QName UTM_ZONE_1 = new QName(NAMESPACE_URI, "26901");

  public static final QName UTM_ZONE_2 = new QName(NAMESPACE_URI, "26902");

  public static final QName UTM_ZONE_3 = new QName(NAMESPACE_URI, "26903");

  public static final QName UTM_ZONE_4 = new QName(NAMESPACE_URI, "26904");

  public static final QName UTM_ZONE_5 = new QName(NAMESPACE_URI, "26905");

  public static final QName UTM_ZONE_6 = new QName(NAMESPACE_URI, "26906");

  public static final QName UTM_ZONE_7 = new QName(NAMESPACE_URI, "26907");

  public static final QName UTM_ZONE_8 = new QName(NAMESPACE_URI, "26908");

  public static final QName UTM_ZONE_9 = new QName(NAMESPACE_URI, "26909");

  public static final QName UTM_ZONE_10 = new QName(NAMESPACE_URI, "26910");

  public static final QName UTM_ZONE_11 = new QName(NAMESPACE_URI, "26911");

  public static final QName UTM_ZONE_12 = new QName(NAMESPACE_URI, "26912");

  public static final QName UTM_ZONE_13 = new QName(NAMESPACE_URI, "26913");

  public static final QName UTM_ZONE_14 = new QName(NAMESPACE_URI, "26914");

  public static final QName UTM_ZONE_15 = new QName(NAMESPACE_URI, "26915");

  public static final QName UTM_ZONE_16 = new QName(NAMESPACE_URI, "26916");

  public static final QName UTM_ZONE_17 = new QName(NAMESPACE_URI, "26917");

  public static final QName UTM_ZONE_18 = new QName(NAMESPACE_URI, "26918");

  public static final QName UTM_ZONE_19 = new QName(NAMESPACE_URI, "26919");

  public static final QName UTM_ZONE_20 = new QName(NAMESPACE_URI, "26920");

  public static final QName UTM_ZONE_21 = new QName(NAMESPACE_URI, "26921");

  public static final QName UTM_ZONE_22 = new QName(NAMESPACE_URI, "26922");

  public static final QName UTM_ZONE_23 = new QName(NAMESPACE_URI, "26923");

  public static final QName UTM_ZONE_24 = new QName(NAMESPACE_URI, "26924");

  public static final QName UTM_ZONE_25 = new QName(NAMESPACE_URI, "26925");

  public static final QName UTM_ZONE_26 = new QName(NAMESPACE_URI, "26926");

  public static final QName UTM_ZONE_27 = new QName(NAMESPACE_URI, "26927");

  public static final QName UTM_ZONE_28 = new QName(NAMESPACE_URI, "26928");

  public static final QName UTM_ZONE_29 = new QName(NAMESPACE_URI, "26929");

  public static final QName UTM_ZONE_30 = new QName(NAMESPACE_URI, "26930");

  public static final QName UTM_ZONE_31 = new QName(NAMESPACE_URI, "26931");

  public static final QName UTM_ZONE_32 = new QName(NAMESPACE_URI, "26932");

  public static final QName UTM_ZONE_33 = new QName(NAMESPACE_URI, "26933");

  public static final QName UTM_ZONE_34 = new QName(NAMESPACE_URI, "26934");

  public static final QName UTM_ZONE_35 = new QName(NAMESPACE_URI, "26935");

  public static final QName UTM_ZONE_36 = new QName(NAMESPACE_URI, "26936");

  public static final QName UTM_ZONE_37 = new QName(NAMESPACE_URI, "26937");

  public static final QName UTM_ZONE_38 = new QName(NAMESPACE_URI, "26938");

  public static final QName UTM_ZONE_39 = new QName(NAMESPACE_URI, "26939");

  public static final QName UTM_ZONE_40 = new QName(NAMESPACE_URI, "26940");

  public static final QName UTM_ZONE_41 = new QName(NAMESPACE_URI, "26941");

  public static final QName UTM_ZONE_42 = new QName(NAMESPACE_URI, "26942");

  public static final QName UTM_ZONE_43 = new QName(NAMESPACE_URI, "26943");

  public static final QName UTM_ZONE_44 = new QName(NAMESPACE_URI, "26944");

  public static final QName UTM_ZONE_45 = new QName(NAMESPACE_URI, "26945");

  public static final QName UTM_ZONE_46 = new QName(NAMESPACE_URI, "26946");

  public static final QName UTM_ZONE_47 = new QName(NAMESPACE_URI, "26947");

  public static final QName UTM_ZONE_48 = new QName(NAMESPACE_URI, "26948");

  public static final QName UTM_ZONE_49 = new QName(NAMESPACE_URI, "26949");

  public static final QName UTM_ZONE_50 = new QName(NAMESPACE_URI, "26950");

  public static final QName UTM_ZONE_51 = new QName(NAMESPACE_URI, "26951");

  public static final QName UTM_ZONE_52 = new QName(NAMESPACE_URI, "26952");

  public static final QName UTM_ZONE_53 = new QName(NAMESPACE_URI, "26953");

  public static final QName UTM_ZONE_54 = new QName(NAMESPACE_URI, "26954");

  public static final QName UTM_ZONE_55 = new QName(NAMESPACE_URI, "26955");

  public static final QName UTM_ZONE_56 = new QName(NAMESPACE_URI, "26956");

  public static final QName UTM_ZONE_57 = new QName(NAMESPACE_URI, "26957");

  public static final QName UTM_ZONE_58 = new QName(NAMESPACE_URI, "26958");

  public static final QName UTM_ZONE_59 = new QName(NAMESPACE_URI, "26959");

  public static final QName UTM_ZONE_60 = new QName(NAMESPACE_URI, "26960");

  public static int getSrid(QName srid) {
    return Integer.parseInt(srid.getLocalPart());
  }

  public static QName getSrid(int srid) {
    return new QName(NAMESPACE_URI, String.valueOf(srid));
  }
}
