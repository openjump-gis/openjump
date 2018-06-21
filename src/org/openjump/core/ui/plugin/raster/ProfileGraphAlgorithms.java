package org.openjump.core.ui.plugin.raster;

public class ProfileGraphAlgorithms {

    private int m_iCount;
    private double m_dZMin, m_dZMax, m_dPlanetLenght, m_dTerrainLength;
    private final double totTime;
    private double m_dSlopeAll;
    private double[] doubleZ;
    private double[] doublePlanet;
    private double[] doubleTerrain;

    private double[] doubleRelativeSlope;
    private double[] doubleAbsoluteSlope;

    public ProfileGraphAlgorithms() {
        m_iCount = 0;
        doubleZ = new double[] { 0 };
        doublePlanet = new double[] { 0 };
        m_dZMin = Double.MAX_VALUE;
        totTime = 0.0D;
        m_dZMax = Double.NEGATIVE_INFINITY;
        ;

        m_dSlopeAll = 0.0D;
        m_dPlanetLenght = 0.0D;
        m_dTerrainLength = 0.0D;
        doubleTerrain = new double[] { 0 };
        doubleRelativeSlope = new double[] { 0 };
        doubleAbsoluteSlope = new double[] { 0 };

    }

    public void calculateValues(double[][] dataTableDouble)
    // , double velFlat,
    // double velUp, double velDown)
    {
        doubleZ = new double[dataTableDouble.length];
        doublePlanet = new double[dataTableDouble.length];
        doubleTerrain = new double[dataTableDouble.length];
        doubleRelativeSlope = new double[dataTableDouble.length];
        doubleAbsoluteSlope = new double[dataTableDouble.length];
        for (int i = 0; i < dataTableDouble.length; i++) {
            m_iCount++;
            doubleZ[i] = ((Double) dataTableDouble[i][1]).doubleValue();
            doublePlanet[i] = ((Double) dataTableDouble[i][0]).doubleValue();
            m_dZMax = Math.max(m_dZMax, doubleZ[i]);// doubleZ[0];
            m_dPlanetLenght = doublePlanet[doublePlanet.length - 1];
            m_dZMin = Math.min(m_dZMin, doubleZ[i]);
            if (i == 0) {
                doubleTerrain[i] = 0;
                doubleRelativeSlope[i] = 0;
                doubleAbsoluteSlope[i] = 0;
            } else {
                final double dDist1 = ((Double) dataTableDouble[i][0])
                        .doubleValue();
                final double dDist0 = ((Double) dataTableDouble[i - 1][0])
                        .doubleValue();
                final double Z1 = ((Double) dataTableDouble[i][1])
                        .doubleValue();
                final double Z0 = ((Double) dataTableDouble[i - 1][1])
                        .doubleValue();
                doubleTerrain[i] = Math.sqrt(Math.pow(Z1 - Z0, 2)
                        + Math.pow(dDist1 - dDist0, 2))
                        + doubleTerrain[i - 1];
                m_dTerrainLength = doubleTerrain[doubleTerrain.length - 1];
                final double slope = Math.atan(((Z1 - Z0) / (dDist1 - dDist0)));
                doubleRelativeSlope[i] = Math.toDegrees(slope);
                m_dSlopeAll += doubleRelativeSlope[i];
                if (slope > 0 || slope == 0) {
                    doubleAbsoluteSlope[i] = doubleRelativeSlope[i];
                } else if (slope < 0) {
                    doubleAbsoluteSlope[i] = doubleRelativeSlope[i] * -1;
                }
            }

            /*
             * if (doubleRelativeSlope[i] < 1.7184 && doubleRelativeSlope[i] >
             * -1.7184 && doubleTerrain[i] != 0) { // Flat totTime +=
             * ((doubleTerrain[i] - doubleTerrain[i - 1]) / 1000) (1 / velFlat);
             * } else if (doubleRelativeSlope[i] > 1.7184 && doubleTerrain[i] !=
             * 0) { // Up totTime += ((doubleTerrain[i] - doubleTerrain[i - 1])
             * / 1000) (1 / velUp); } else if (doubleRelativeSlope[i] < 1.7184
             * && doubleTerrain[i] != 0) { // Down totTime += ((doubleTerrain[i]
             * - doubleTerrain[i - 1]) / 1000) (1 / velDown);
             * 
             * }
             */

        }

    }

    public double getTime() {
        return totTime;
    }

    public double getSlope() {
        return m_dSlopeAll / (m_iCount - 1);
    }

    public double[] getRelativeSlopeData() {
        return doubleRelativeSlope;
    }

    public double[] getAbsoluteSlopeData() {
        return doubleAbsoluteSlope;
    }

    public double[] getZData() {
        return doubleZ;
    }

    public double[] getPlanetData() {
        return doublePlanet;
    }

    public double[] getTerrainData() {
        return doubleTerrain;
    }

    public int getCount() {

        return m_iCount;

    }

    public double getZMin() {

        return m_dZMin;

    }

    public double getTerrainLength() {

        return m_dTerrainLength;

    }

    public double getPlanetLength() {

        return m_dPlanetLenght;

    }

    public double getZMax() {

        return m_dZMax;

    }

}
