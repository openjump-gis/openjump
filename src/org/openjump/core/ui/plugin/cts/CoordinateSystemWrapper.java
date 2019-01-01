package org.openjump.core.ui.plugin.cts;


import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.coordsys.CoordinateSystem;
import com.vividsolutions.jump.coordsys.Geographic;
import com.vividsolutions.jump.coordsys.Planar;
import com.vividsolutions.jump.coordsys.Projection;
import org.cts.IllegalCoordinateException;
import org.cts.crs.CoordinateReferenceSystem;
import org.cts.cs.Axis;
import org.cts.op.CoordinateOperationException;
import org.cts.op.CoordinateSwitch;
import org.cts.op.UnitConversion;
import org.cts.units.Unit;

/**
 * A wrapper to wrap a {@link org.cts.crs.CoordinateReferenceSystem} into a
 * {@link com.vividsolutions.jump.coordsys.CoordinateSystem}.
 */
public class CoordinateSystemWrapper extends CoordinateSystem {
    private CoordinateReferenceSystem crs;
    CoordinateSystemWrapper(final CoordinateReferenceSystem crs) {
        super(crs.getName(),
                (crs.getAuthorityKey().toUpperCase().equals("EPSG") ?
                        Integer.parseInt(crs.getAuthorityKey()) : 0),
                new Projection() {

                    @Override
                    public Planar asPlanar(Geographic q0, Planar p) {
                        double[] dd = new double[]{q0.lat, q0.lon, q0.hgt};
                        try {
                            // La latitude et la longitude sont échangées si besoin
                            if (crs.getCoordinateSystem().getAxis(0) == Axis.LONGITUDE) {
                                dd = CoordinateSwitch.SWITCH_LAT_LON.transform(dd);
                            }
                            // Les coordonnées géographiques sont passées en radian
                            dd = UnitConversion.createUnitConverter(Unit.DEGREE, Unit.RADIAN).transform(dd);
                            // puis projetées
                            dd = crs.getProjection().transform(dd);
                            return new Planar(dd[0], dd[1]);
                        } catch(CoordinateOperationException|IllegalCoordinateException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public Geographic asGeographic(Planar p, Geographic q) {
                        double[] dd = new double[]{p.x, p.y, p.z};
                        try {
                            // Les coordonnées sont passées en géographiques
                            dd = crs.getProjection().inverse().transform(dd);
                            // La latitude et la longitude sont échangées au besoin
                            if (crs.getCoordinateSystem().getAxis(0) == Axis.LONGITUDE) {
                                dd = CoordinateSwitch.SWITCH_LAT_LON.transform(dd);
                            }
                            // Les unités sont converties en degrés
                            dd = UnitConversion.createUnitConverter(Unit.RADIAN, Unit.DEGREE).transform(dd);
                            return new Geographic(dd[0], dd[1]);
                        } catch(CoordinateOperationException|IllegalCoordinateException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
        this.crs = crs;
    }
    public String toString() {
        return crs.getName();
    }
    public String getName() {
        return crs.getName();
    }
    public Projection getProjection() {
        return new Projection() {
            @Override
            public Planar asPlanar(Geographic q0, Planar p) {
                return null;
            }

            @Override
            public Geographic asGeographic(Planar p, Geographic q) {
                return null;
            }
        };
    }

    public int getEPSGCode() {
        return crs.getAuthorityName().equals("EPSG") ?
                Integer.parseInt(crs.getAuthorityKey()) : 0;
    }

    public int compareTo(Object o) {
        Assert.isTrue(o instanceof CoordinateSystem);
        if (this == o) { return 0; }
        if (this == UNSPECIFIED) { return -1; }
        if (o == UNSPECIFIED) { return 1; }
        return toString().compareTo(o.toString());
    }
}


