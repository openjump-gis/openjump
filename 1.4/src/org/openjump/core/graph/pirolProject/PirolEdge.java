/*
 * Created on 05.01.2005
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2509 $
 *  $Date: 2006-10-06 12:01:50 +0200 (Fr, 06 Okt 2006) $
 *  $Id: PirolEdge.java 2509 2006-10-06 10:01:50Z LBST-PF-3\orahn $s
 */
package org.openjump.core.graph.pirolProject;

import java.util.Vector;

import com.vividsolutions.jts.geom.Envelope;

/**
 * 
 * Class that describes a single line, specified by it's starting and
 * end point. If offers methods e.g. to find intersection points with other
 * Kante objects or to determine on which side of the Kante object a given
 * punkt object resides.
 * 
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 2509 $
 * 
 * @see de.fhOsnabrueck.jump.pirol.utilities.PirolPoint
 * @see de.fhOsnabrueck.jump.pirol.utilities.Data2LayerConnector
 * modified: [sstein]: 16.Feb.2009 changed logger-entries to comments
 */
public class PirolEdge {
    
    public static PirolEdge KANTE_X0Y0ToX0Y1 = new PirolEdge( PirolPoint.NULLPUNKT, new PirolPoint(new double[]{0,1}), true, true);
    public static PirolEdge KANTE_X0Y0ToX1Y0 = new PirolEdge( PirolPoint.NULLPUNKT, new PirolPoint(new double[]{1,0}), true, true);
    
    protected PirolPoint anfang = null, ende = null;
    protected int punktIndexA = -1, punktIndexB = -1;
    protected boolean anfangUnbegrenzt = true, endeUnbegrenzt = true;
    protected boolean gueltig = true;
    
    protected static double infinityFaktor = Math.pow(10,10);
    
    //protected static PersonalLogger logger = new PersonalLogger(DebugUserIds.OLE);
    
    public PirolEdge(PirolPoint anfang, PirolPoint ende, boolean anfangUnbegrenzt, boolean endeUnbegrenzt){
        this.anfang = anfang;
        this.ende = ende;
        
        this.anfangUnbegrenzt = anfangUnbegrenzt;
        this.endeUnbegrenzt = endeUnbegrenzt;
    }
    
    public PirolEdge(PirolPoint anfang, PirolPoint ende){
        this.anfang = anfang;
        this.ende = ende;
        
        this.anfangUnbegrenzt = false;
        this.endeUnbegrenzt = false;
    }
    
    public PirolEdge(PirolPoint anfang, double steigung, double laenge){
        this.anfang = anfang;
        
        double alpha = Math.atan(steigung);
        
        double dx = Math.cos(alpha) * laenge;
        double dy = Math.sin(alpha) * laenge;
        
        try {
            this.ende = new PirolPoint( new double[]{anfang.getX()+dx, anfang.getY()+dy});
        } catch (Exception e) {
            // this should not happen!
            this.setGueltig(false);
            e.printStackTrace();
        }
        
        this.anfangUnbegrenzt = false;
        this.endeUnbegrenzt = false;
    }
    
    /**
     * creates a new {@link PirolEdge} object, that has a length
     * equal to <code>lineToShift</code>'s length and is parallel to
     * <code>lineToShift</code>.
     *@param lineToShift the line to be shifted
     *@param shiftingVector x,y,z component for the shifting (will be added to <code>lineToShift</code>'s starting and end point).
     *@return a new, shifted {@link PirolEdge} object
     * @throws Exception 
     */
    public final static PirolEdge shiftLine(PirolEdge lineToShift, PirolPoint shiftingVector) throws Exception{
        PirolPoint spkt = null, epkt = null;
        PirolPoint currPkt = null;
        
        double[] coords = new double[shiftingVector.getDimension()];
        
        for (int i=0; i<2; i++){
            if (i==0)
                currPkt = lineToShift.getAnfang();
            else
                currPkt = lineToShift.getEnde();
            
            for (int dim=0; dim<coords.length; dim++){
                coords[dim] = currPkt.getCoordinate(dim) + shiftingVector.getCoordinate(dim);
            }
            
            if (i==0)
                spkt = new PirolPoint((double[])coords.clone());
            else
                epkt = new PirolPoint((double[])coords.clone());
        }
        
        return new PirolEdge(spkt, epkt, lineToShift.isAnfangUnbegrenzt(), lineToShift.isEndeUnbegrenzt());
    }
    
    /**
     * switch starting and end point of <code>this</code> {@link PirolEdge} instance.
     *
     */
    public void switchPoints(){
        PirolPoint tmp = this.getEnde();
        this.setEnde(this.getAnfang());
        this.setAnfang(tmp);
    }
    
    public static PirolEdge kreiereKanteDurchPunktInnerhalbBegrenzung( PirolPoint p, double steigung, Envelope begrenzung ) throws Exception {
        PirolPoint p1 = null, p2 = null;
        if (steigung==0){
            p1 = new PirolPoint(new double[]{begrenzung.getMinX(), p.getY()} );
            p2 = new PirolPoint(new double[]{begrenzung.getMaxX(), p.getY()} );
        } else if (steigung==Double.POSITIVE_INFINITY) {
            p1 = new PirolPoint(new double[]{p.getX(), begrenzung.getMinY()} );
            p2 = new PirolPoint(new double[]{p.getX(), begrenzung.getMaxY()} );
        } else if (steigung==Double.NEGATIVE_INFINITY) {
            p2 = new PirolPoint(new double[]{p.getX(), begrenzung.getMinY()} );
            p1 = new PirolPoint(new double[]{p.getX(), begrenzung.getMaxY()} );
        } else {
            double minX = begrenzung.getMinX();
            double maxX = begrenzung.getMaxX();
            double minY = begrenzung.getMinY();
            double maxY = begrenzung.getMaxY();
            
            PirolPoint upperLeft = new PirolPoint(new double[]{minX, maxY});
            PirolPoint lowerLeft = new PirolPoint(new double[]{minX, minY});
            PirolPoint upperRight = new PirolPoint(new double[]{maxX, maxY});
            PirolPoint lowerRight = new PirolPoint(new double[]{maxX, minY});
            
            PirolEdge top = new PirolEdge(upperLeft,upperRight);
            PirolEdge right = new PirolEdge(upperRight,lowerRight);
            PirolEdge bottom = new PirolEdge(lowerRight,lowerLeft);
            PirolEdge left = new PirolEdge(lowerLeft,upperLeft);
            
            PirolEdge toBeCut = new PirolEdge(p, steigung, 5.0);
            toBeCut.setAnfangUnbegrenzt(true);
            toBeCut.setEndeUnbegrenzt(true);
            
            PirolEdge[] kanten = new PirolEdge[]{top, right, bottom, left};
            Vector schnittPunkte = new Vector();
            PirolPoint sp;
            
            for (int i=0; i<kanten.length; i++){
                sp = toBeCut.getSchnittpunkt(kanten[i]);
                
                if (sp != null){
                    schnittPunkte.add(sp);
                }
            }
            
            if (schnittPunkte.size() == 2){
                // this should happen!!
                PirolPoint tmp1 = (PirolPoint)schnittPunkte.get(0);
                PirolPoint tmp2 = (PirolPoint)schnittPunkte.get(1);
                
                if (steigung > 0){
                    if (tmp1.getY()<tmp2.getY()){
                        p1 = tmp1;
                        p2 = tmp2;
                    } else {
                        p1 = tmp2;
                        p2 = tmp1;
                    }
                } else {
                    if (tmp1.getY()>tmp2.getY()){
                        p1 = tmp1;
                        p2 = tmp2;
                    } else {
                        p1 = tmp2;
                        p2 = tmp1;
                    }
                }
            }
        }
        
        return new PirolEdge(p1, p2);
    }
    
    public boolean isParallelZu(PirolEdge k) throws Exception{
        return this.isParallelZu(k, infinityFaktor);
    }
    
    public boolean isParallelZu(PirolEdge k, double infinityFactor) throws Exception{
        PirolPoint sp = this.getSchnittpunkt(k);
        
        if (sp==null){
            return true;
        } 
        double thisLaenge = this.getAnfang().distanceTo(this.getEnde());
        double andereLaenge = k.getAnfang().distanceTo(k.getEnde());
        double laenge = (thisLaenge+andereLaenge) / 2.0;
        
        double infiniteDistance = laenge * infinityFactor;
        
        if (this.getAnfang().distanceTo(sp) > infiniteDistance && this.getEnde().distanceTo(sp) > infiniteDistance){
            // as good as parallel
            return true;
        }
        
        //logger.printDebug("not parallel: " + this.getAnfang().distanceTo(sp) + ", " + this.getEnde().distanceTo(sp) + ", infinity: " + infiniteDistance);

        return false;
    }
    
    public double getSteigung() throws Exception {
        double x1 = this.getAnfang().getX();
        double x2 = this.getEnde().getX();
        double y1 = this.getAnfang().getY();
        double y2 = this.getEnde().getY();
        
        double deltaX = x2 - x1;
        double deltaY = y2 - y1;
        
        if (deltaX!=0){
            return deltaY/deltaX;
        } 
        if (deltaY>0)
            return Double.POSITIVE_INFINITY;
        else if (deltaY<0)
            return Double.NEGATIVE_INFINITY;
        else
            throw new Exception("wasn't able to calculate the increase of line (starting point == end point)");
        
    }
    
    public static PirolPoint getSchnittpunkt( PirolEdge k1, PirolEdge k2 ) throws Exception {
        PirolPoint a = k1.getAnfang();
        PirolPoint ab = new PirolPoint(new double[]{k1.getEnde().getX()-a.getX(), k1.getEnde().getY()-a.getY()});
        PirolPoint c = k2.getAnfang();
        PirolPoint cd = new PirolPoint(new double[]{k2.getEnde().getX()-c.getX(), k2.getEnde().getY()-c.getY()});
        
        if (a.distanceTo(k1.getEnde())==0){
            throw new Exception("zero length!");
        }
        
        double m = 0.0;
        
        if (ab.getX()==0 && cd.getX()!=0){
            m = (a.getX()-c.getX())/cd.getX();
        } else if (ab.getX()==0 && cd.getX()==0){
            if ( a.getX() != c.getX() )
                return null;
            else if ( (c.getY() >= a.getY() && c.getY() <= a.getY() + ab.getY()) || (c.getY() <= a.getY() && c.getY() >= a.getY() + ab.getY()) )
                return new PirolPoint( new double[]{a.getX(), c.getY()} );
            else if ( (a.getY() >= c.getY() && a.getY() <= c.getY() + cd.getY()) || (a.getY() <= c.getY() && a.getY() >= c.getY() + cd.getY()) )
                return new PirolPoint( new double[]{a.getX(), a.getY()} );
            else
                return null;
        } else if ( ab.getY()==0 && cd.getY()!=0 ){
            m = (a.getY()-c.getY())/cd.getY();
        }
        /*
         else if ( ab.getY() && cd.getY()==0 ){
         	// sollte nicht auftreten
         	m = 0;
         }
        */
        else if ( ab.getX()!=0 && (cd.getX()*ab.getY()/ab.getX())!=cd.getY() ){
            m = ( a.getY()-c.getY()+((c.getX()-a.getX())*ab.getY())/ab.getX() )/( cd.getY()-(cd.getX()*ab.getY()/ab.getX()) );
        } else {
            return null;
        }
        
        if ( !k2.isAnfangUnbegrenzt() && m<=0 ){
            return null;
        }
        if ( !k2.isEndeUnbegrenzt() && m>=1 ){
            return null;
        }
        
        double n = 0.0;
        
        if (ab.getX()==0 && ab.getY()!=0 && cd.getX()!=0){
            n = (a.getY()-c.getY()-((a.getX()-c.getX())*cd.getY()/cd.getX()))/(-ab.getY());
        } else if (ab.getX()!=0) {
            n = (c.getX()+m*cd.getX()-a.getX())/ab.getX();
        } else if (cd.getY()==0 && ab.getY()!=0){
            n = (c.getY()-a.getY())/ab.getY();
        } else {
            return null;
        }
        
        if ( !k1.isAnfangUnbegrenzt() && n<=0 )
            return null;
        if ( !k1.isEndeUnbegrenzt() && n>=1 )
            return null;
        
        PirolPoint schnittpunkt = new PirolPoint( new double[]{a.getX() + n*ab.getX(), a.getY() + n*ab.getY()} );
        
        return schnittpunkt;
    }
    
    public PirolPoint getSchnittpunkt( PirolEdge k2 ) throws Exception{
        return PirolEdge.getSchnittpunkt(this, k2);
    }
    
    public int vorzeichenDesNormalenFaktors( PirolPoint pkt ) throws Exception{
        double fact = this.getNormalenFaktorZu(pkt);
        
        if (fact > 0) return 1;
        if (fact < 0) return -1;
        return 0;
    }
    
    public double getABFaktorZumNormalenFaktor( PirolPoint pkt ) throws Exception{
        
        double normalenFaktor = this.getNormalenFaktorZu(pkt);
        
        PirolPoint a = this.getAnfang();
        PirolPoint b = this.getEnde();
        PirolPoint c = pkt;
        
        PirolPoint ab = new PirolPoint( new double[]{b.getX()-a.getX(), b.getY()-a.getY()} );
        // N ist normale zu ab
        PirolPoint N = new PirolPoint( new double[]{ab.getY(), -ab.getX()} );
        // berechne faktor y mit dem N multipliziert werden muss, um punkt3 zu erreichen
        
        if (ab.getX()!=0){
            double Xx = ( c.getX() - a.getX() - normalenFaktor*N.getX() ) / ab.getX();
            return Xx;
        }
        double Yx = ( c.getY() - a.getY() - normalenFaktor*N.getY() ) / ab.getY();
        return Yx;
    }
    
    public double getNormalenFaktorZu( PirolPoint pkt ) throws Exception{
        PirolPoint punkt1 = this.getAnfang();
        PirolPoint punkt2 = this.getEnde();
        PirolPoint punkt3 = pkt;
        /*
        #           (C)
        #          / |  \
        #        /   |     \
        #      /     |y*N     \
        #   (A)______|_________(B)
        # ab ist vektor punkt1 -> punkt2 = a -> b
        */
        PirolPoint ab = PirolPoint.createVector(punkt1,punkt2);
        // N ist normale zu ab
        PirolPoint N = new PirolPoint( new double[]{ab.getY(), -ab.getX()} );
        // berechne faktor y mit dem N multipliziert werden muss, um punkt3 zu erreichen

        double ergebnis = 0;
        
        try {
            if (ab.getX() != 0 && ab.getY() != 0)
                ergebnis = ((punkt1.getY()- punkt3.getY())-( punkt1.getX() - punkt3.getX()) * ab.getY() / ab.getX() )/ (N.getX() * ab.getY() / ab.getX() - N.getY());
            else if (ab.getX() == 0 )
                ergebnis = (punkt3.getX() - punkt1.getX()) / N.getX();
            else
                ergebnis = (punkt3.getY() - punkt1.getY()) / N.getY();
        } catch (Exception e) {
            // faengt division durch null fehler ab, die hier aber nicht auftauchen koennen sollten
            ergebnis = 1;
            //logger.printError(e.getMessage());
        }
        
        /*
        if (Double.isNaN(ergebnis)){
            logger.printError("got Nan!");
            logger.printDebug(this.toString());
            logger.printDebug(pkt.toString());
            logger.printDebug(ab.toString());
            logger.printDebug("---");
        }
        */

        return ergebnis;
    }

    public PirolEdge getNormalenKante( double laenge ) throws Exception{
        PirolPoint punkt1 = this.getAnfang();
        PirolPoint punkt2 = this.getEnde();

        PirolPoint ab = new PirolPoint( new double[]{punkt2.getX()-punkt1.getX(), punkt2.getY()-punkt1.getY()} );
        PirolPoint N = new PirolPoint( new double[]{ab.getY(), -ab.getX()} );

        PirolPoint nullPunkt = PirolPoint.NULLPUNKT;
        double nLaenge = nullPunkt.distanceTo( N );
        
        double faktor;
        
        try{
            faktor = laenge / nLaenge * -1.0;
        } catch ( Exception e ) {
            faktor = -1.0;
        }
        
        N.setX( N.getX()*faktor );
        N.setY( N.getY()*faktor );
        
        PirolEdge nKante = new PirolEdge( new PirolPoint( new double[]{punkt2.getX(), punkt2.getY()} ), new PirolPoint( new double[]{punkt2.getX()+N.getX(), punkt2.getY()+N.getY()} ), false, false );
        
        return nKante;
    }
    
    public String toString() {
        return "Kante<"+this.punktIndexA+","+this.punktIndexB+">["+this.getAnfang().toString()+","+this.getEnde().toString()+"]";
    }
    
    public double getLaenge() throws Exception {
        if (this.anfang!=null && this.ende!=null){
            if (this.anfangUnbegrenzt || this.isEndeUnbegrenzt()) return Double.POSITIVE_INFINITY;
            return anfang.distanceTo(ende);
        }
        throw new Exception(PirolEdge.class.getName()+": Starting point or end point not specified!"); 
    }
    
    public PirolPoint getAnfang() {
        return anfang;
    }
    public void setAnfang(PirolPoint anfang) {
        this.anfang = anfang;
    }
    public boolean isAnfangUnbegrenzt() {
        return anfangUnbegrenzt;
    }
    public void setAnfangUnbegrenzt(boolean anfangUnbegrenzt) {
        this.anfangUnbegrenzt = anfangUnbegrenzt;
    }
    public PirolPoint getEnde() {
        return ende;
    }
    public void setEnde(PirolPoint ende) {
        this.ende = ende;
    }
    public boolean isEndeUnbegrenzt() {
        return endeUnbegrenzt;
    }
    public void setEndeUnbegrenzt(boolean endeUnbegrenzt) {
        this.endeUnbegrenzt = endeUnbegrenzt;
    }
    public boolean isGueltig() {
        return gueltig;
    }
    public void setGueltig(boolean gueltig) {
        this.gueltig = gueltig;
    }
    public int getPunktIndexA() {
        return punktIndexA;
    }
    public void setPunktIndexA(int punktIndexA) {
        this.punktIndexA = punktIndexA;
    }
    public int getPunktIndexB() {
        return punktIndexB;
    }
    public void setPunktIndexB(int punktIndexB) {
        this.punktIndexB = punktIndexB;
    }
}
