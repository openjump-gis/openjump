package com.vividsolutions.jump.datastore.oracle;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.datastore.FilterQuery;
import com.vividsolutions.jump.datastore.SpatialReferenceSystemID;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import java.util.Map;

/**
 * Creates SQL query strings for a PostGIS database
 */
public class OracleSQLBuilder {

    private SpatialReferenceSystemID defaultSRID = null;
    private String[] colNames = null;
    private Map<String, String> geoIndexes = null;

    public OracleSQLBuilder(SpatialReferenceSystemID defaultSRID, String[] colNames) {
        this.defaultSRID = defaultSRID;
        this.colNames = colNames;
    }

    public OracleSQLBuilder(SpatialReferenceSystemID defaultSRID, String[] colNames, Map<String, String> geoIndexes) {
        this(defaultSRID, colNames);
        this.geoIndexes = geoIndexes;
    }

    public String getSQL(FilterQuery query) {
        return buildQueryString(query);
    }

    private String buildQueryString(FilterQuery query) {
        StringBuilder qs = new StringBuilder();
        //HACK
        // surrond query by a rownum clause, used for limit
        qs.append("SELECT ").append(getColumnListSpecifier(colNames, query.getGeometryAttributeName(), false));
        qs.append(" FROM ( ");
        qs.append("SELECT ");
        qs.append(getColumnListSpecifier(colNames, query.getGeometryAttributeName(), true));
        qs.append(" FROM ");
        // fixed by mmichaud on 2010-05-27 for mixed case dataset names
        qs.append("\"").append(query.getDatasetName().replaceAll("\\.", "\".\"")).append("\"");
        qs.append(" t WHERE ");

        qs.append(buildBoxFilter(query.getGeometryAttributeName(), query.getSRSName(), query.getFilterGeometry()));

        String whereCond = query.getCondition();
        if (whereCond != null) {
            qs.append(" AND ");
            qs.append(whereCond);
        }
        qs.append(")");
        
        int limit = query.getLimit();
        if (limit != 0 && limit != Integer.MAX_VALUE) {
            qs.append(" where ROWNUM <= ").append(limit);
        }
        
        JUMPWorkbench.getInstance().getFrame().log(
            "SQL query to get Spatial table features:\n\t" 
                + qs.toString(), this.getClass());

        return qs.toString();
    }

    /**
     * Buuilds a bbox filter only if geo column is indexed with a valid spatial index
     * @param geometryColName
     * @param SRID
     * @param geom
     * @return 
     */
    private String buildBoxFilter(String geometryColName, SpatialReferenceSystemID SRID, Geometry geom) {
        StringBuilder buf = new StringBuilder("1=1");
        
        if (this.geoIndexes != null && this.geoIndexes.containsKey(geometryColName)) {
            buf = new StringBuilder();
            Envelope env = geom.getEnvelopeInternal();

            // Example of Postgis SQL: where sdo_filter (geom, SDO_geometry(2003,2154,NULL,SDO_elem_info_array(1,1003,3),SDO_ordinate_array(100225,1002375,6549646,6810524))) = 'TRUE'
            String srid = getSRID(SRID) == null ? "null" : getSRID(SRID);

            // fixed by mmichaud on 2010-05-27 for mixed case geometryColName names
            buf.append("sdo_filter(\"").append(geometryColName).append("\" , SDO_geometry(");
            buf.append("2003,").append(srid).append(",NULL,SDO_elem_info_array(1,1003,3),SDO_ordinate_array(");
            buf.append(env.getMinX()).append(", ").append(env.getMinY()).append(", ").append(env.getMaxX()).append(", ").append(env.getMaxY());
            buf.append(srid).append(")))='TRUE'");

            JUMPWorkbench.getInstance().getFrame().log(
                "SQL query fragment to get spatial table BBOX filter:\n\t" 
                + buf.toString(), this.getClass());
        }

        return buf.toString();
    }

    private String getSRID(SpatialReferenceSystemID querySRID) {
        SpatialReferenceSystemID srid = defaultSRID;
        if (!querySRID.isNull()) {
            srid = querySRID;
        }

        if (srid.isNull() || srid.getString().trim().length() == 0) {
            return null;
        } else {
            return srid.getString();
        }
    }

    private String getColumnListSpecifier(String[] colNames, String geomColName, boolean addRownum) {
    // Added double quotes around each column name in order to read mixed case table names
        // correctly [mmichaud 2007-05-13]
        StringBuilder buf = new StringBuilder();
    // fixed by mmichaud using a patch from jaakko [2008-05-21]
        // query geomColName as geomColName instead of geomColName as geomColName + "_wkb"
        if (addRownum) {
            buf.append("ROWNUM, ");
        }
        buf.append(geomColName).append(" as ").append("\"").append(geomColName).append("\"");
        for (String colName : colNames) {
            if (!geomColName.equalsIgnoreCase(colName)) {
                buf.append(",\"").append(colName).append("\"");
            }
        }
        return buf.toString();
    }
}
