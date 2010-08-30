/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * JUMP is Copyright (C) 2003 Vivid Solutions
 *
 * This program implements extensions to JUMP and is
 * Copyright (C) Stefan Steiniger.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 * Stefan Steiniger
 * perriger@gmx.de
 */

/***********************************************
 * created on 		05.11.2007
 * last modified: 	09.11.2007 - improved equal#/quantiles method for chained similar values
 * 
 * author:			sstein
 * 
 * description: 
 *  provides some 1-D classification method for 
 *  arrays of double values.
 * 
 * 
 ***********************************************/
package org.openjump.core.attributeoperations;

import java.util.ArrayList;
import java.util.List;

import org.math.array.DoubleArray;
import org.math.array.StatisticSample;

import com.vividsolutions.jump.I18N;

public class Classifier1D {

    private static String pluginname = "classifyplot";
    
	//-- note: add these strings to the List in Classifier1D.getAvailableClassificationMethods()
	//	 		to make them available for the JUMP GUI
	public static String EQUAL_RANGE = "Equal Range";
	public static String EQUAL_NUMBER = "Equal Number/Quantiles";
	public static String MEAN_STDEV = "Mean Standard Deviation";
	public static String MAX_BREAKS = "Maximal Breaks";
	public static String JENKS_BREAKS = "Jenks Optimization";
	
	public static  String KMEANS_OPTIMIZE = "Optimization with k-means";
	
	public static List getAvailableClassificationMethods(){
	
		//-- assign i18N strings
		//   this shall work because this method should be called before
		//   the class field strings are used for comparison

			Classifier1D.EQUAL_RANGE = I18N.get("ui.renderer.style.ColorThemingStylePanel.Equal-Interval");
			Classifier1D.EQUAL_NUMBER = I18N.get("ui.renderer.style.ColorThemingStylePanel.Quantile-Equal-Number");
			Classifier1D.MEAN_STDEV = I18N.get( "ui.renderer.style.ColorThemingStylePanel.Mean-Standard-Deviation");
			Classifier1D.MAX_BREAKS = I18N.get("ui.renderer.style.ColorThemingStylePanel.Maximal-Breaks");	
			Classifier1D.JENKS_BREAKS = I18N.get("ui.renderer.style.ColorThemingStylePanel.Jenks-Optimal-Method");	

		//-- make a list for the GUI
		List classifierList = new ArrayList();
		classifierList.add(Classifier1D.EQUAL_RANGE);
		classifierList.add(Classifier1D.EQUAL_NUMBER);
		classifierList.add(Classifier1D.MEAN_STDEV);
		classifierList.add(Classifier1D.MAX_BREAKS);
		classifierList.add(Classifier1D.JENKS_BREAKS);
		return classifierList;
	}
	
    /**
     * calculates class limits with equal range
     * @param data
     * @param numberClasses
     * @return break values for classes. E.g. for 4 ranges 3 breaks are returned. Min and Max Values are not returned.   
     */
    public static double[] classifyEqualRange(double[] data, int numberClasses){
        double[] limits = new double[numberClasses-1];
        double min = DoubleArray.min(data);
        double max = DoubleArray.max(data);
        double delta = (max - min)/numberClasses;
        for (int i = 0; i < limits.length; i++) {
            limits[i]=min + (delta*(i+1));
        }            
        return limits;
    }

    /**
     * calculates class limits with equal number, which is euqal to the "quantiles" method.
     * Note that differences in the items per classes occure, if items have same values
     * and need to be grouped into the same class. 
     * @param data
     * @param numberClasses
     * @return break values for classes. E.g. for 4 ranges 3 breaks are returned. Min and Max Values are not returned.   
     */
    public static double[] classifyEqualNumber(double[] data, int numberClasses){
        double[] limits = new double[numberClasses-1];
        int itemsPerClass = (int)Math.floor(data.length/numberClasses);
        double[] orderedItems = DoubleArray.sort(data);
        for (int i = 0; i < limits.length; i++) {
            int pos = 0 + itemsPerClass*(i+1);
            int bias = 0; //index-count used for cases when items have similar values
            double border = 0;
            if (orderedItems[bias + pos-1] != orderedItems[bias + pos]){
                border = 0.5*(orderedItems[bias + pos-1] + orderedItems[bias + pos]);
            }
            else{ //both values are equal
                //move on, until values are different
                int index = bias + pos;
                int nrEqualVal = 0;
                while(orderedItems[bias + pos-1] == orderedItems[index]){
                    index=index+1;
                    nrEqualVal = nrEqualVal+1;
                }
                border = 0.5*(orderedItems[bias + pos-1] + orderedItems[index]);
                bias = bias+nrEqualVal;
            }                
            limits[i]= border;
        }            
        return limits;
    }

    /**
     * calculates class limits using mean value and standard deviation, i.e. for 5 classes:
     * c1: values < m- 2std, c2: m - 2std < values < m - 1std, 
     * c3: m - 1std < values < m + 1std, c4: m + 1std < values < m + 2std 
     * c5: values > m- 2std
     * 
     * @param data
     * @param numberClasses
     * @return break values for classes. E.g. for 4 ranges 3 breaks are returned. Min and Max Values are not returned.   
     */
    public static double[] classifyMeanStandardDeviation(double[] data, int numberClasses){
        double[] limits = new double[numberClasses-1];
        //double[] orderedItems = DoubleArray.sort(data);
        double mean = StatisticSample.mean(data);
        double std = StatisticSample.stddeviation(data);
        boolean evenNumber = true;
        if ( (numberClasses/2.0) != Math.floor(numberClasses/2.0)){
            evenNumber = false;
        }         
        int startMultiplier = -1*(int)Math.floor(numberClasses/2.0);
        if (evenNumber){//adjust for an even number of classes
            startMultiplier = startMultiplier + 1;
        }
        for (int i = 0; i < limits.length; i++) {            
            double border = mean + (startMultiplier*std);
            limits[i]= border;
            startMultiplier = startMultiplier+1;
            //-- ensure that middle class is around mean, for an un-even number of classes 
            if ((startMultiplier == 0) && (evenNumber == false)) {
                startMultiplier = 1;
            }
        }            
        return limits;
    }
    
    /**
     * calculates class limits using Maximum Breaks method (see e.g. T. A. Slocum:
     * "Thematic Cartography and Visualization", 1999)
     * 
     * @param data
     * @param numberClasses
     * @return break values for classes. E.g. for 4 ranges 3 breaks are returned. Min and Max Values are not returned.   
     */
    public static double[] classifyMaxBreaks(double[] data, int numberClasses){
        double[] limits = new double[numberClasses-1];
        double[] sortData = DoubleArray.sort(data);
        //-- calc differences (distance between values)
        double[] deltaX = new double[data.length];
        for (int i = 0; i < (sortData.length-1); i++) {
            deltaX[i] = sortData[i+1] - sortData[i];
        }
        //-- find largest differences
        double[] unSortedLimits = new double[numberClasses-1];
        double minX = DoubleArray.min(deltaX);
        for (int i = 0; i < limits.length; i++) {
            //-- get max value
            double maxX = DoubleArray.max(deltaX);
            //-- find max positions and replace value by minValue
            //   we need to replace the value, because in the next round
            //   we still want to get the right index to calc the breakpos
            boolean found = false; int j = 0;
            while (found == false){                
                if (deltaX[j] == maxX){
                    found = true;
                    unSortedLimits[i] = 0.5*(sortData[j] + sortData[j+1]);
                    deltaX[j] = minX; 
                }
                else{
                    j++;
                }
            }             
        }
        //-- sort limits from min to max
        limits = DoubleArray.sort(unSortedLimits);
        return limits;
    }

	/**
	 * calculates class limits using Jenks's Optimisation Method(Natural Break)
	 * 
	 * @param data
	 * @param numberClasses
	 * @return break values for classes. E.g. for 4 ranges 3 breaks are
	 *         returned. Min and Max Values are not returned.
	 */
	public static double[] classifyNaturalBreaks(double[] data, int numberClasses) {
		double[] limits = new double[numberClasses - 1];

		double[] orderedItems = DoubleArray.sort(data);

		int numData = data.length;

		double[][] mat1 = new double[numData + 1][numberClasses + 1];
		double[][] mat2 = new double[numData + 1][numberClasses + 1];

		for (int i = 1; i <= numberClasses; i++) {
			mat1[1][i] = 1;
			mat2[1][i] = 0;
			for (int j = 2; j <= numData; j++)
				mat2[j][i] = Double.MAX_VALUE;
		}
		double v = 0;
		
		for (int l = 2; l <= numData; l++) {
            double s1 = 0;
            double s2 = 0;
            double w = 0;
            for (int m = 1; m <= l; m++) {
                int i3 = l - m + 1;
                double val = orderedItems[i3-1];                
                
                s2 += val * val;
                s1 += val;               
         
                w++;
                v = s2 - (s1 * s1) / w;
                int i4 = i3 - 1;
                if (i4 != 0) {
                    for (int j = 2; j <= numberClasses; j++) {
                        if (mat2[l][j] >= (v + mat2[i4][j- 1])) {
                            mat1[l][j] = i3;
                            mat2[l][j] = v + mat2[i4][j -1];
                        };
                    };
                };
            };
            mat1[l][1] = 1;
            mat2[l][1] = v;
        };
        int k = numData;

        for (int j = numberClasses; j >= 2; j--) {
            int id =  (int) (mat1[k][j]) - 2;
            //-- [sstein] modified version from Hisaji,
            // 			  otherwise breaks will be "on" one item            
            // limits[j - 2] = orderedItems[id];
            //-- new
            double limit = 0.5*(orderedItems[id]+orderedItems[id+1]);
            limits[j - 2] = limit;
            
            k = (int) mat1[k][j] - 1;   
        };
        
		return limits;
	}
	
    /**
     * calculates class limits using optimal breaks method (see e.g. T. A. Slocum:
     * "Thematic Cartography and Visualization", 1999, p.73) or B.D. Dent: "Cartography: 
     *  Thematic Map Design", 1999, p.146). \n 
     * Note: limits should not be equal to values. Since values that are equal to bounds 
     * can be classified into 2 classes. 
     * @param data
     * @param numberClasses
     * @param initialLimitAlgorithm: 1: maxBreaks, 2: equalRange, 3: quantiles, 4: MeanStd-Dev 5: Jenks
     * @return break values for classes. E.g. for 4 ranges 3 breaks are returned. Min and Max Values are not returned.   
     */
    public static double[] classifyKMeansOnExistingBreaks(double[] data, int numberClasses, int initialLimitAlgorithm){
        int maxRuns = 50;
        double[] limits = new double[numberClasses-1];
        //-- sort Data (to make movement of limits easier)
        double[] sortedData = DoubleArray.sort(data);
        
        //========== first round ==============
        //-- calc intial SDAM (squared deviation, array mean)
        double SDAM = Classifier1D.calcSDAM(sortedData);
        //-- develop class boundaries
        //   we start with xxx breaks groups
        double[] tempLimits = new double[limits.length];
        if (initialLimitAlgorithm == 1){     
            tempLimits =  Classifier1D.classifyMaxBreaks(sortedData, numberClasses);
        }
        else if(initialLimitAlgorithm == 2){
            tempLimits =  Classifier1D.classifyEqualRange(sortedData, numberClasses);
        }
        else if(initialLimitAlgorithm == 3){
            tempLimits =  Classifier1D.classifyEqualNumber(sortedData, numberClasses);
        }
        else if(initialLimitAlgorithm == 4){
            tempLimits =  Classifier1D.classifyMeanStandardDeviation(sortedData, numberClasses);
		} 
        else if (initialLimitAlgorithm == 5) {
			tempLimits = Classifier1D.classifyNaturalBreaks(sortedData,
					numberClasses);
        }        
        else{
            //=== Default ===
            //TODO: change this to create arbitrary ones???
            tempLimits = Classifier1D.classifyMaxBreaks(sortedData, numberClasses);
        }
        limits = tempLimits;
        double GVF = Classifier1D.calcGVF(sortedData, tempLimits, SDAM);
        //========== optimize ==============
        ArrayList<Double> gdfVals = new ArrayList<Double>();
        gdfVals.add(new Double(GVF));
        boolean moveOn = true; int runs = 0;
        while(moveOn){
            runs++;
            //-- move/adjust class boundaries
            tempLimits = Classifier1D.adjustLimitsKMeans(sortedData, limits);
            //-- calc fit (i.e. GVF)
            double newGVF = Classifier1D.calcGVF(sortedData, tempLimits, SDAM);
            //-- GVF should move towards 1 (i.e. newGVF should be larger)
            double dGVF = newGVF- GVF;
            if ((dGVF > 0) && (maxRuns > runs)){ 
                GVF = newGVF;
                limits = tempLimits;
            }
            else{
                moveOn = false;
            }
        }                
        //==================================
        return limits;
    }
    
    /**
     * Moves the limits, by assigning data points to the closest class mean value.
     * This approach is equal to the k-means procedure (see e.g. Duda, Hart and 
     * Stork 2000, p. 526).   
     * @param data (sortedData from min to max, e.g. use jmathtools DoubleArray.sort())
     * @param oldLimits
     * @return
     */
    public static double[] adjustLimitsKMeans(double[] data, double[] oldLimits){
        double[] newLimits = new double[oldLimits.length];        
        int numberClasses = oldLimits.length+1; 
        
        int[] oldClasses = Classifier1D.classifyData(data, oldLimits);
        //-- calc class means         
        double[] means = Classifier1D.calcClassMeans(data, oldClasses, numberClasses);
        //========== reclassify by assigning to closest mean ===========
        int[] newClasses = new int[data.length];
        double[] classChange = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            double smallestDist = 0;
            int assignedClass = -1;
            //-- init with first mean
            smallestDist = Math.abs(data[i]-means[0]);
            assignedClass = 0;
            for (int j = 1; j < means.length; j++) {
                double dist =  Math.abs(data[i]-means[j]);
                if (dist < smallestDist){
                    assignedClass = j;
                    smallestDist = dist;
                }
            }
            newClasses[i]=assignedClass;
            //-- record changes
            if (newClasses[i] == oldClasses[i]){
                classChange[i] = 0;
            }
            else{
                classChange[i] = 1;
            }
        }
        double modifications = DoubleArray.sum(classChange);
        if (modifications > 0){
            //System.out.println("Classifier1D.adjustLimitsKMeans(): points reassigned: " + (int)modifications);
            //========= calc limits by observing changes in newClasses =========
            //-- this works because data items are ordered
            int classPrev = newClasses[0];
            int classNext = -1;
            int limitIdx = 0;
            for (int i = 1; i < data.length; i++) {
                classNext = newClasses[i];
                if (classPrev != classNext){
                    //-- change occured => get limit
                    newLimits[limitIdx] = 0.5*(data[i-1] + data[i]);
                    limitIdx++;
                }
                classPrev = classNext;
            }        
        }
        else{
            newLimits = oldLimits;
            //System.out.println("Classifier1D.adjustLimitsKMeans(): no reassignment of points; limits not modified");
        }
        return newLimits;
    }
    
    /**
     * Classifies the given data according to the given limits. 
     * @param data
     * @param limits: The break/decision values between the classes. Highest and lowest values
     *          are not delivered. Example Limits are for instance delivered by the  
     *          Classifier1D.classifyEqualNumber() method.
     * @return array containg a class ID for every item.
     */
    public static int[] classifyData(double[] data, double[] limits){
        int[] classes = new int[data.length]; 
        int nClasses = limits.length+1; 
        //============  get Limits ===================
        //-- get min and max values
        double minAll = DoubleArray.min(data);
        double maxAll = DoubleArray.max(data);
        //-- add min and max limits
        double[] finalLimits = new double[limits.length+2];
        for (int i = 0; i < limits.length; i++) {
            finalLimits[i+1] = limits[i];  
        }
        finalLimits[0]= minAll;
        finalLimits[finalLimits.length-1] = maxAll;     
        //============ assign data to classes =============
        //   Note: lowest and highest needs to be equal to the limit/break value 
        boolean isInClass = false;
        for (int i = 0; i < data.length; i++) {
            //-- check with all classes
            //   maybe speed up with while loop (using "assigned")
            boolean assigned = false;
            for (int j = 0; j < nClasses; j++) {
                isInClass = Classifier1D.isInClass(data[i], finalLimits[j], finalLimits[j+1]);
                if (isInClass){
                    classes[i]=j;
                    assigned = true;
                }                
            } 
            if(assigned == false){
                classes[i]=-1;
                System.out.println("Classifier1D: could not classify point: " + i + " value:" + data[i] + " -- set class to -1");
            }
        }
        
        return classes;
    }
    
    /**
     * Checks if value is within limits.\n
     * Note: values equal to the bound values return "true".
     * (qery: lowerlimit <= val <= upperlimit)
     * @param val
     * @param lowerBound
     * @param upperBound
     * @return
     */
    public static boolean isInClass(double val, double lowerBound, double upperBound){
        boolean isInClass = false;
        if(val <= upperBound){
            if(val >= lowerBound){
                isInClass = true;
            }
        }
        return isInClass;
    }
    
    /**
     * SDAM (squared deviation [from] array mean): see B.D. Dent (1999, p. 148)
     * alternatively look for T.A. Slocum (1999, p. 73). \n
     * Used for Optimal Breaks Method.
     * @param data
     * @return
     */
    public static double calcSDAM(double[] data){
        double meanAll =  StatisticSample.mean(data);
        double SDAM = 0; double sum =0;
        for (int i = 0; i < data.length; i++) {
            sum = sum + ((data[i]-meanAll)*(data[i]-meanAll));
        }
        SDAM = sum;
        return SDAM;
    }

    /**
     * SDCM (squared deviations [from] class means): see B.D. Dent (1999, p. 148)
     * alternatively look for T.A. Slocum (1999, p. 73). \n
     * Used for Optimal Breaks Method.
     * @param data
     * @param classes: the classes for every item of the data array
     * @param classMeans
     * @param numClasses
     * @return
     */
    public static double calcSDCM(double[] data, int[] classes, double[] classMeans, int numClasses){
        
        double SDCM = 0; 
        double[] classSum = new double[numClasses];
        
        for (int i = 0; i < data.length; i++) {
            int z = classes[i];           
            classSum[z] = classSum[z] + ((data[i]-classMeans[z])*(data[i]-classMeans[z]));  
        }
        double sum = 0;
        for (int i = 0; i < classSum.length; i++) {
            sum = sum + classSum[i];
        }
        SDCM = sum;
        return SDCM;
    }
    
    /**
     * GVF (goodness of variance fit): see B.D. Dent (1999, p. 148)
     * alternatively look for T.A. Slocum (1999, p. 73). \n
     * Used for Optimal Breaks Method.
     * @param SDAM: squared deviation [from] array mean
     * @param SDCM: squared deviation [from] class mean
     * @return
     */
    public static double calcGVF(double SDAM, double SDCM){
        double gvf = (SDAM - SDCM) / SDAM;
        return gvf;
    }
    
    /**
     * GVF (goodness of variance fit): see B.D. Dent (1999, p. 148)
     * alternatively look for T.A. Slocum (1999, p. 73). \n
     * Used for Optimal Breaks Method.
     * @param data
     * @param limits: The break/decision values between the classes. Highest and lowest values
     *          are not delivered. Example Limits are for instance delivered by the  
     *          Classifier1D.classifyEqualNumber() method.
     * @param SDAM: squared deviation [from] array mean
     * @return
     */
    public static double calcGVF(double[] data, double[] limits, double SDAM){
        int numberClasses = limits.length+1;
        //-- assign to class with initial limits
        int[] classes = Classifier1D.classifyData(data, limits);
        //-- calc class mean values        
        double[] means = Classifier1D.calcClassMeans(data, classes, numberClasses);
        //-- calc SDCM (squared deviations, class means)
        double SDCM = Classifier1D.calcSDCM(data, classes, means, numberClasses);
        //-- calc Goodness of Variance fit (GVF)
        double GDF = Classifier1D.calcGVF(SDAM, SDCM);
        return GDF;
    }

    /**
     * 
     * @param data
     * @param classes: the vector containing the information on the class for an item
     * @param numClasses: the number of classes
     * @return
     */
    public static double[] calcClassMeans(double[] data, int[] classes, int numClasses){
        
        double means[] = new double[numClasses];
        double[] sumC = new double[numClasses];
        int[] countCMembers = new int[numClasses];
        for (int i = 0; i < data.length; i++) {
            if (classes[i] != -1){
                sumC[classes[i]] = sumC[classes[i]] + data[i];
                countCMembers[classes[i]] = countCMembers[classes[i]] +1;
            }
        }        
        for (int i = 0; i < means.length; i++) {
          means[i] = sumC[i]/countCMembers[i];          
        }         
        return means;
    }

}
