package org.openjump.core.ccordsys.utils;

import org.openjump.core.ccordsys.Unit;

import java.io.UnsupportedEncodingException;

import static org.openjump.core.ccordsys.utils.SRSInfo.Registry.EPSG;
import static org.openjump.core.ccordsys.utils.SRSInfo.Registry.ESRI;
import static org.openjump.core.ccordsys.utils.SRSInfo.Registry.SRID;

/**
 * Small container for SRS information.
 * This class does not contain all information to perform coordinate transformation,
 * but enough to return metadata about SRS code or map unit
 */
public class SRSInfo {

    public static final String UNDEFINED = "0";
    public static final String USERDEFINED = "USER-DEFINED";

    public enum Registry{SRID, EPSG, ESRI, IGNF, SRORG}

    private String source;            // The source of SRS information (ex. prj file path)
    private Registry registry = EPSG; // The registry in which this SRS is referenced
    private String code = UNDEFINED;  // The code of the SRS
    private String description = "";  // The name or description of the SRS
    private Unit unit = Unit.UNKNOWN; // The unit used by this SRS

    public SRSInfo() {}

    public String getSource() {
        return source;
    }

    public SRSInfo setSource(String source) {
        this.source = source;
        return this;
    }

    public Registry getRegistry() {
        if (code == null && description == null)
            throw new IllegalStateException("SRSInfo must have a code or a description");
        if (registry != null) return registry;
        else return guessRegistry(getCode());
    }

    public SRSInfo setRegistry(Registry registry) {
        this.registry = registry;
        return this;
    }

    public SRSInfo setRegistry(String registry) {
        this.registry = Registry.valueOf(registry);
        return this;
    }

    public String getCode() {
        if (code == null && description == null)
            throw new IllegalStateException("SRSInfo must have a code or a description");
        return code;
    }

    public SRSInfo setCode(String code) {
        this.code = code;
        return this;
    }

    public String getDescription() {
        if (code == null && description == null)
            throw new IllegalStateException("SRSInfo must have a code or a description");
        return description;
    }

    public SRSInfo setDescription(String description) {
        this.description = description;
        return this;
    }

    public Unit getUnit() {
        if (code == null && description == null)
            throw new IllegalStateException("SRSInfo must have a code or a description");
        return unit;
    }

    public SRSInfo setUnit(Unit unit) {
        this.unit = unit;
        return this;
    }

    public SRSInfo setUnit(String unit) {
        this.unit = Unit.valueOf(unit);
        return this;
    }

    public void complete() throws UnsupportedEncodingException {
        SRSInfo sridTableInfo = SridLookupTable.getSrsAndUnitFromCode(code);
        if (sridTableInfo.getCode().equals(UNDEFINED)) {
            sridTableInfo = SridLookupTable.getSrsAndUnitFromName(description);
        }
        if (!sridTableInfo.getCode().equals(UNDEFINED)) {
            code = sridTableInfo.getCode();
            description = sridTableInfo.getDescription();
            unit = sridTableInfo.getUnit();
        }
        registry = guessRegistry(code);
    }

    @Override
    public String toString() {
        return getRegistry().toString() + ':' + getCode() + ' ' + getDescription() + '[' + getUnit() + ']';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SRSInfo srsInfo = (SRSInfo) o;

        if (registry != null ? !registry.equals(srsInfo.registry) : srsInfo.registry != null) return false;
        if (code != null ? !code.equals(srsInfo.code) : srsInfo.code != null) return false;
        if (description != null ? !description.equals(srsInfo.description) : srsInfo.description != null) return false;
        return unit != null ? unit.equals(srsInfo.unit) : srsInfo.unit == null;

    }

    @Override
    public int hashCode() {
        int result = registry != null ? registry.hashCode() : 0;
        result = 31 * result + (code != null ? code.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (unit != null ? unit.hashCode() : 0);
        return result;
    }

    private static Registry guessRegistry(String code) {
        // 1) WKID <32768 or >5999999
        // will result in an AUTHORITY name of "EPSG".
        // 2) A WKID in range between 33000 and 199999
        // will result in an AUTHORITY name of "ESRI".
        // (http://help.arcgis.com/en/arcgisserver/10.0/apis/soap/whnjs.htm#SOAP_Geometry_FindSRByWKID.htm)

        Registry registry = SRID; // Used if code is negative or non-numeric
        if (code != null && code.matches("\\d+")) {
            int srid = Integer.parseInt(code);
            if (srid < 32768 || srid > 5999999) {
                registry = EPSG;
            } else if (srid > 32999 && srid < 200000) {
                registry = ESRI;
            }
        }
        return registry;
    }
}
