/*
 * Created on 18.05.2005 for PIROL
 *
 * SVN header information:
 *  $Author: LBST-PF-3\orahn $
 *  $Rev: 2509 $
 *  $Date: 2006-10-06 12:01:50 +0200 (Fr, 06 Okt 2006) $
 *  $Id: CorrelationCoefficients.java 2509 2006-10-06 10:01:50Z LBST-PF-3\orahn $
 */
package org.openjump.core.attributeoperations.statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.openjump.core.apitools.FeatureCollectionTools;
import org.openjump.core.apitools.comparisonandsorting.ObjectComparator;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;


/**
 * Class that calculates various correlation coefficients.
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev: 2509 $
 * modified: [sstein]: 16.Feb.2009 changed logger-entries to comments
 */
public class CorrelationCoefficients {
    protected Object[] dataArray = null;
    
    // TODO: use array or list instead of two, three, etc. variables
    protected String attrName1 = null, attrName2 = null;
    
    protected double[] means = null;
    protected Feature[] rawFeatures = null;
    
    //protected PersonalLogger logger = new PersonalLogger(DebugUserIds.OLE);
    
    protected class SpearmanRankNumberPair {
    	protected double a, b;

		public SpearmanRankNumberPair(double a, double b) {
			super();
			this.a = a;
			this.b = b;
		}
    	
    	public double getDifference(){
    		return this.a - this.b;
    	}

		public double getA() {
			return a;
		}

		public double getB() {
			return b;
		}

    }
    
    public class CorrelationInformation {
    	protected double coefficient = 0;
    	
		public CorrelationInformation(double coefficient) {
			super();
			this.coefficient = coefficient;
		}

		public double getCoefficient() {
			return coefficient;
		}
    	
    }
    
    public class RankCorrelationInformation extends CorrelationInformation {
    	
    	double secondCoefficient = Double.NaN;
    	int numberOfConcordantPairs, numberOfDiscordantPairs;

		public RankCorrelationInformation(double coefficient, double secondCoefficient, int numberOfConcordantPairs, int numberOfDiscordantPairs) {
			super(coefficient);
			this.secondCoefficient = secondCoefficient;
			this.numberOfConcordantPairs = numberOfConcordantPairs;
			this.numberOfDiscordantPairs = numberOfDiscordantPairs;
		}
		
		public RankCorrelationInformation(double coefficient, int numberOfConcordantPairs, int numberOfDiscordantPairs) {
			super(coefficient);
			this.numberOfConcordantPairs = numberOfConcordantPairs;
			this.numberOfDiscordantPairs = numberOfDiscordantPairs;
		}

		public int getNumberOfConcordantPairs() {
			return numberOfConcordantPairs;
		}

		public int getNumberOfDiscordantPairs() {
			return numberOfDiscordantPairs;
		}

		public double getSecondCoefficient() {
			return secondCoefficient;
		}

    }
    

    public CorrelationCoefficients(Feature[] features, String attr1, String attr2) {
        super();
        
        this.means = new double[2];
        this.attrName1 = attr1;
        this.attrName2 = attr2;
        
        this.rawFeatures = features;
        this.dataArray = this.initializeDataStorage(features);
    }
    
    protected CorrelationDataPair[] initializeDataStorage(Feature[] features){
        List<CorrelationDataPair> points = new ArrayList<CorrelationDataPair>();
        
        Feature feature = features[0];
        
        FeatureSchema fs = feature.getSchema();
        double x, y;
        
        int attrIndX = fs.getAttributeIndex(this.attrName1);
        int attrIndY = fs.getAttributeIndex(this.attrName2);
        
        this.means[0] = this.aritmeticMiddle(features, attrIndX);
        this.means[1] = this.aritmeticMiddle(features, attrIndY);
        
        for (int i=0; i<features.length; i++){
            feature = features[i];
            
            x = ObjectComparator.getDoubleValue(feature.getAttribute(attrIndX));
            y = ObjectComparator.getDoubleValue(feature.getAttribute(attrIndY));
            
            points.add(new CorrelationDataPair(new double[]{x,y}, i));
        }
        
        return points.toArray(new CorrelationDataPair[0]);
    }
    
    /**
     * Returns the deviation of the values of the given attribute. Uses a given
     * mean to avoid multiple calculation of the mean. To get the mean take a look 
     * at the FeatureCollectionTools class. This class is also used by aritmeticMiddle().
     *@param features array containing the features we want the deviation for
     *@param attr name of the attribute to calculate the deviation for
     *@param mean the mean for the given features
     *@return the deviation
     *@throws IllegalArgumentException if the attribute is not of a numerical type
     *@see FeatureCollectionTools
     */
    public static double getDeviation(Feature[] features, String attr, double mean){
        //PersonalLogger logger = new PersonalLogger(DebugUserIds.OLE);
        
        Feature feat = features[0];
        
        int attrIndex = feat.getSchema().getAttributeIndex(attr);
        
        if (!FeatureCollectionTools.isAttributeTypeNumeric(feat.getSchema().getAttributeType(attrIndex)))
            throw new IllegalArgumentException("attribute is not numeric!");
        
        double squareSum = 0;
        
        
        for (int i=0; i<features.length; i++){
            feat = features[i];
            if (feat.getAttribute(attrIndex) != null)
                squareSum += Math.pow( ( ObjectComparator.getDoubleValue(feat.getAttribute(attrIndex)) - mean) ,2);
            else{ 
                //logger.printWarning("skipping value (NULL), when calculating deviation");
            }
        }
        
        return Math.sqrt( 1d/(features.length-1)*squareSum );
    }
    
    protected double getVariance(String attr){
        // not needed, yet
        // TODO: implement variance, use deviation (or the other way round)
        return 0.0;
    }
    
    protected double aritmeticMiddle(Feature[] features, int attr){
        return FeatureCollectionTools.getAritmeticMiddleForAttribute(features, attr);
    }
    
    /**
     * Get the aritmetic middle for the nr-th attribut given
     *@param nr index number of attribut to calculate the mean for
     *@return the mean for the attribute or Double.NaN, if errors occured
     */
    public double getMean(int nr){
        return this.means[nr];
    }
    
    /**
     * get Pearson's correlation coefficient (good, dimension-less measure, if there is a linear relation between the attributes)
     * <br>see: <a href="http://www.netzwelt.de/lexikon/Korrelationskoeffizient.html">http://www.netzwelt.de/lexikon/Korrelationskoeffizient.html</a> 
     *@return Pearson's correlation coefficient
     */
    public CorrelationInformation getPearsonCoefficient(){
        double coefficient = 0;

        CorrelationDataPair pkt;
        
        double x, y, errorX, errorY;
        double sumErrorProducts = 0;
        double sumXErrorSquares = 0, sumYErrorSquares = 0;
        
        for (int i=0; i<this.dataArray.length; i++){
            pkt = (CorrelationDataPair)this.dataArray[i];
            
            try {
                x = pkt.getX();
                y = pkt.getY();
            } catch (Exception e) {
                //logger.printWarning(e.getMessage());
                continue;
            }
            
            errorX = x - this.means[0];
            errorY = y - this.means[1];
            
            // zähler
            sumErrorProducts += (errorX*errorY);
            
            // nenner
            sumXErrorSquares += (errorX*errorX);
            sumYErrorSquares += (errorY*errorY);
        }
        
        coefficient = sumErrorProducts / ( Math.sqrt(sumXErrorSquares) * Math.sqrt(sumYErrorSquares));
        
        return new CorrelationInformation(coefficient);
    }
    
    protected HashMap<Integer,Double> getRank2SpearmanRankMap(Object[] sortedValues, HashMap<Object, Integer> value2NumAppearances){
    	HashMap<Integer,Double> rank2SpearmanRank = new HashMap<Integer, Double>();
    	
    	int sumOfRanks = 0;
    	int numAppearances;
    	double meanSum;
    	
    	for (int i=0; i<sortedValues.length; i++){
    		numAppearances = value2NumAppearances.get(sortedValues[i]);
    		meanSum = 0;
    		
    		for (int sr=0; sr<numAppearances; sr++){
    			meanSum += (sr + sumOfRanks + 1);
    		}
    		
    		rank2SpearmanRank.put(i, (double)(meanSum/numAppearances) );
    		
//    		logger.printDebug("putting: " + i + " -> " + (double)(meanSum/numAppearances) + " (" + numAppearances + ")");
    		
    		sumOfRanks += numAppearances;
    	}
    	
    	return rank2SpearmanRank;
    }
    
    /**
     * get Pearson's correlation coefficient (good, dimension-less measure, if there is a linear relation between the attributes)
     * <br>see: <a href="http://www.netzwelt.de/lexikon/Korrelationskoeffizient.html">http://www.netzwelt.de/lexikon/Korrelationskoeffizient.html</a> 
     *@return Pearson's correlation coefficient
     */
    public RankCorrelationInformation getSpearmansRhoCoefficient(){
        double coefficient = 0;

        // -------------
        
        FeatureSchema fs = this.rawFeatures[0].getSchema();
        
        int attributeIndex1 = fs.getAttributeIndex(this.attrName1), attributeIndex2 = fs.getAttributeIndex(this.attrName2);
        
        List valuesA = Collections.list( Collections.enumeration( FeatureCollectionTools.getSetOfDifferentAttributeValues(this.rawFeatures, attributeIndex1 )));
        List valuesB = Collections.list( Collections.enumeration( FeatureCollectionTools.getSetOfDifferentAttributeValues(this.rawFeatures, attributeIndex2 )));
        Collections.sort( valuesA );
        Collections.sort( valuesB );
        
        Object[] valuesArrayA = valuesA.toArray(), valuesArrayB = valuesB.toArray(); 
        
        HashMap<Object, Integer> value2NumAppearances1 = null, value2NumAppearances2 = null;
        
        HashMap<Object, Integer>[] maps = FeatureCollectionTools.getValueAppearancesCount(this.rawFeatures, new int[]{attributeIndex1,attributeIndex2});
        value2NumAppearances1 = maps[0];
        value2NumAppearances2 = maps[1];
        
        HashMap<Integer,Double> rank2SpearmanRankA = new HashMap<Integer, Double>(), rank2SpearmanRankB = new HashMap<Integer, Double>();
        
        rank2SpearmanRankA = this.getRank2SpearmanRankMap(valuesArrayA, value2NumAppearances1);
        rank2SpearmanRankB = this.getRank2SpearmanRankMap(valuesArrayB, value2NumAppearances2);
        
        CorrelationCoefficients.SpearmanRankNumberPair[] spearmanPairs = new CorrelationCoefficients.SpearmanRankNumberPair[this.rawFeatures.length];
        
        int rankA, rankB;
        
        for (int i=0; i<this.rawFeatures.length; i++){
        	rankA = valuesA.indexOf(this.rawFeatures[i].getAttribute(attributeIndex1));
        	rankB = valuesB.indexOf(this.rawFeatures[i].getAttribute(attributeIndex2));
        	
        	spearmanPairs[i] = new SpearmanRankNumberPair(rank2SpearmanRankA.get(rankA), rank2SpearmanRankB.get(rankB));
        }
        
        double sumOfSpearmanDifferences = 0, difference;
        int numCon = 0, numDis = 0;
        int numPairs = spearmanPairs.length;
        
        for (int i=0; i<numPairs; i++){
        	difference = spearmanPairs[i].getDifference();
        	sumOfSpearmanDifferences += Math.pow(difference, 2);
        	
        	if (difference>0)
        		numCon++;
        	else if (difference<0)
        		numDis++;
        }
        
        return new RankCorrelationInformation( 
        		( 1 - ( (6*sumOfSpearmanDifferences)/(Math.pow(numPairs, 3)-numPairs) ) ),
        		numCon,
        		numDis);
    }
    
    /**
     * "Spearman Rank Order Correlations (or "rho")  and Kendall's Tau-b (or "tau") Correlations are used when the variables are measured as ranks (from highest-to-lowest or lowest-to-highest)"
     * <br><a href="http://www.themeasurementgroup.com/datamining/definitions/correlation.htm">http://www.themeasurementgroup.com/datamining/definitions/correlation.htm</a>
     *@return RankCorrelationInformation
     */
    public RankCorrelationInformation getKendalsTauRankCoefficient(){
                
        CorrelationDataPair currentPair, referncePair;
        double currX, currY, refX, refY;
        
        int numConcordant = 0, 
        	numDiscordant = 0,
        	numTiesX = 0,
        	numTiesY = 0,
        	numVergleiche = 0;

        //logger.printDebug("starting calculation");
        
        for (int i=0; i<this.dataArray.length; i++){
            currentPair = (CorrelationDataPair)this.dataArray[i];
            try {
                currX = currentPair.getCoordinate(0);
                currY = currentPair.getCoordinate(1);
            } catch (Exception e1) {
                //logger.printMinorError(e1.getMessage());
                continue;
            }
            
            for (int j=i+1; j<this.dataArray.length; j++, numVergleiche++){
                referncePair = (CorrelationDataPair)this.dataArray[j];
                try {
                    refX = referncePair.getCoordinate(0);
                    refY = referncePair.getCoordinate(1);
                    
                } catch (Exception e2) {
                    //logger.printMinorError(e2.getMessage());
                    continue;
                }
                
                if ( (currX < refX && currY < refY) || (currX > refX && currY > refY) ) {
                    numConcordant ++;
                } else if ( (currX < refX && currY > refY) || (currX > refX && currY < refY) ) {
                    numDiscordant ++;
                }
                
                try {
	                if (currX == refX){
	                    numTiesX ++;
	                }
	                if (currY == refY){
	                    numTiesY ++;
	                }
                } catch (Exception e) {
                    //logger.printWarning(e.getMessage());
                }
            }
        }
        //logger.printDebug("finishing calculation");
        //logger.printDebug("Vergleiche: " + numVergleiche);
        //logger.printDebug("numConcordant: " + numConcordant);
        //logger.printDebug("numDiscordant: " + numDiscordant);
        
        if (numTiesX > 0){
            //logger.printWarning("numTiesX: " + numTiesX);
        }
        if (numTiesY > 0){
            //logger.printWarning("numTiesY: " + numTiesY);
        }
        // according to the formula:
        //double kendallsTau_alpha = (double)(numConcordant-numDiscordant) / (((double)this.dataArray.length*(double)(this.dataArray.length-1.0))/2.0);
        
        // less calculation, but equivalent (?) 
        double kendallsTau_alpha = (double)(numConcordant-numDiscordant) / (double)(numVergleiche);
        double kendallsTau_beta = (double)(numConcordant-numDiscordant) / Math.sqrt( (double)(numConcordant+numDiscordant+numTiesX) * (double)(numConcordant+numDiscordant+numTiesY) );
        // third kendalls tau coefficient should be equal to the first one, since we allways have the same number of x and y values
        //double kendallsTau_gamma = 2.0*this.dataArray.length * (numConcordant-numDiscordant) / ((this.dataArray.length-1)*(this.dataArray.length*this.dataArray.length));

        return new RankCorrelationInformation(kendallsTau_alpha, kendallsTau_beta, numConcordant, numDiscordant);
    }
    
 
}
