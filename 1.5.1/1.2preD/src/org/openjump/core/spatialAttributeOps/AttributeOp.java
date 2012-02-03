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
 * created on 		22.06.2006
 * last modified: 	
 * 
 * author:			sstein
 * 
 * description:
 * 	provides some function to calculate mathematical
 *  indices like mean, max, median for a set of features.
 * 
 ***********************************************/
package org.openjump.core.spatialAttributeOps;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jmat.MatlabSyntax;
import org.jmat.data.AbstractMatrix;
import org.jmat.data.Matrix;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;

/**
 * 
 *  description:
 * 	provides some function to calculate mathematical
 *  indices like mean, max, median for a set of features.
 * 
 * @author sstein
 *
 */
public class AttributeOp {
    public final static int MAJORITY = 0;
    public final static int MINORITY = 1;
    public final static int MEAN = 2;
    public final static int MEDIAN = 3;
    public final static int MIN = 4;
    public final static int MAX = 5;
    public final static int STD = 6;    
    public final static int SUM = 7;
    public final static int COUNT = 8;
    
    public static String getName(int attributeOP){
        String retval = "";
        if(attributeOP == 0){
            retval ="major";
        }
        else if(attributeOP == 1){
            retval ="minor";
        }
        else if(attributeOP == 2){
            retval ="mean";
        }
        else if(attributeOP == 3){
            retval ="median";
        }        
        else if(attributeOP == 4){
            retval ="min";
        }        
        else if(attributeOP == 5){
            retval ="max";
        }
        else if(attributeOP == 6){
            retval ="std";
        }
        else if(attributeOP == 7){
            retval ="sum";
        }
        else if(attributeOP == 8){
            retval ="count";
        }                                        
        return retval; 
    }
    
    public static double evaluateAttributes(int attributeOp, List features, String attributeName){
        double result= Double.NaN;
        if (features.size() > 0){
            Feature firstF = (Feature)features.get(0);
            FeatureSchema fs = firstF.getSchema();
            if (fs.hasAttribute(attributeName)){
                boolean doEval = true; 
                AttributeType at = fs.getAttributeType(attributeName);
		        int n = features.size();
		        Matrix mat = MatlabSyntax.zeros(n,1);
		        int count=0;
		        for (Iterator iter = features.iterator(); iter.hasNext();) {
		            Feature f = (Feature) iter.next();
		            if (at == AttributeType.DOUBLE){
		                Double val = (Double)f.getAttribute(attributeName);
		                mat.set(count,0, val.doubleValue());
		            }
		            else if(at == AttributeType.INTEGER){
		                Integer val = (Integer)f.getAttribute(attributeName);
		                mat.set(count,0, val.doubleValue());		                
		            }
		            else if(at == AttributeType.GEOMETRY){
		                //-- simply set to one for count 
		                Geometry geom = (Geometry)f.getAttribute(attributeName);
		                if (geom != null){
		                    mat.set(count,0, 1);
		                }
		                else{
		                    mat.set(count,0, 0);
		                }
		            }		            
		            else{
		                System.out.println("AttributeOp: attribute type not supported");
		                doEval = false;
		            }
		            count++;
		        }
		        if(doEval){
		            if (attributeOp == AttributeOp.MAJORITY){
		            	result = majorityEval(mat);
		            }
		            else if(attributeOp == AttributeOp.MINORITY){
		            	result = minorityEval(mat); 
		            }		            
		            else if(attributeOp == AttributeOp.MAX){
		                AbstractMatrix max = mat.max();
		            	result = max.get(0,0); 
		            }
		            else if(attributeOp == AttributeOp.MIN){
		                AbstractMatrix min = mat.min();
		            	result = min.get(0,0); 
		            }
		            else if(attributeOp == AttributeOp.MEAN){
		                AbstractMatrix mean = mat.mean();
		            	result = mean.get(0,0); 
		            }
		            else if(attributeOp == AttributeOp.STD){
		                AbstractMatrix var = mat.variance();
		            	result = Math.sqrt(var.get(0,0)); 
		            }		            
		            else if(attributeOp == AttributeOp.MEDIAN){
		        	    Matrix sortmat = MatlabSyntax.sort_Y(mat);
		        	    int index = (int)Math.ceil(mat.getRowDimension()/2.0);	                
		            	result = mat.get(index-1,0); 
		            }
		            else if(attributeOp == AttributeOp.SUM){
		                AbstractMatrix sum = mat.sum();
		            	result = sum.get(0,0); 
		            }
		            else if(attributeOp == AttributeOp.COUNT){
		            	result = (double) mat.getRowDimension(); 
		            }		            		            
		            else{
		                System.out.println("AttributeOp: attribute operation not supported");
		            }
		        }
            }
            else{
                System.out.println("AttributeOp: attribute does not exist");
            }
        }
        else{
        	if(attributeOp == AttributeOp.COUNT){
        		result = 0;
        	}			
        }
        return result;
    }
    
    private static double majorityEval(Matrix mat){
        double result=0;
        //-- built list of all values
        ArrayList vals = new ArrayList();
        for(int i=0; i < mat.getRowDimension(); i++){
            for(int j=0; j < mat.getColumnDimension(); j++){
                double val = mat.get(i,j);
                if( (i==0) && (j==0)){
                    //-- add first value
                    vals.add(new Double(val));
                }
                else{
	                boolean stop = false; int count =0;
	                boolean found = false;
	                while(stop == false){
	                    Double d = (Double)vals.get(count);
	                    if(val == d.doubleValue()){
	                        stop = true;
	                        found = true;
	                    }
	                    count++;
	                    if(count == vals.size()){
	                        //-- if last value reached stop and add
	                        stop = true;	                
	                    }
	                }
	                if(found == false){
	                    vals.add(new Double(val));
	                }	                
                }                
            }
        }
        //-- count number of values
        int[] countVals = new int[vals.size()];
        //-- set to zero
        for (int i = 0; i < countVals.length; i++) {
            countVals[i]=0;
        }
        for(int i=0; i < mat.getRowDimension(); i++){
            for(int j=0; j < mat.getColumnDimension(); j++){
                double val = mat.get(i,j);
                boolean stop = false; int count =0;
                while(stop == false){
                    Double d = (Double)vals.get(count);
                    if(val == d.doubleValue()){
                        //-- count 
                        int oldVal = countVals[count];
                        countVals[count] = oldVal +1;
                        //-- stop 
                        stop = true;
                    }
                    count++;
                    if(count == countVals.length){
                        stop = true;
                    }
                }
            }
        }
//        if (mat.getRowDimension() > 15){
//            String s= "Stop here for debugging"; 
//        }        
        //-- get maximum
        int maxcount = 0;
        int pos = 0;
       	for (int i = 0; i < countVals.length; i++) {
       	    if (countVals[i] > maxcount){
       	       maxcount = countVals[i];
       	       pos = i;
       	    }
        }
       	//-- assign value which appears most
       	result = ((Double)vals.get(pos)).doubleValue();
        return result;
    }

    private static double minorityEval(Matrix mat){
        double result=0;
        //-- built list of all values
        ArrayList vals = new ArrayList();
        for(int i=0; i < mat.getRowDimension(); i++){
            for(int j=0; j < mat.getColumnDimension(); j++){
                double val = mat.get(i,j);
                if( (i==0) && (j==0)){
                    //-- add first value
                    vals.add(new Double(val));
                }
                else{
	                boolean stop = false; int count =0;
	                boolean found = false;
	                while(stop == false){
	                    Double d = (Double)vals.get(count);
	                    if(val == d.doubleValue()){
	                        stop = true;
	                        found = true;
	                    }
	                    count++;
	                    if(count == vals.size()){
	                        //-- if last value reached stop and add
	                        stop = true;	                
	                    }
	                }
	                if(found == false){
	                    vals.add(new Double(val));
	                }
                }                
            }
        }
        //-- count number of values
        int[] countVals = new int[vals.size()];
        //-- set to zero
        for (int i = 0; i < countVals.length; i++) {
            countVals[i]=0;
        }
        for(int i=0; i < mat.getRowDimension(); i++){
            for(int j=0; j < mat.getColumnDimension(); j++){
                double val = mat.get(i,j);
                boolean stop = false; int count =0;
                while(stop == false){
                    Double d = (Double)vals.get(count);
                    if(val == d.doubleValue()){
                        //-- count 
                        int oldVal = countVals[count];
                        countVals[count] = oldVal +1;
                        //-- stop 
                        stop = true;
                    }
                    count++;
                    if(count == countVals.length){
                        stop = true;
                    }
                }
            }
        }
        //-- get minimum count
        int mincount = countVals[0];
        int pos = 0;
       	for (int i = 1; i < countVals.length; i++) {
       	    if (countVals[i] < mincount){
       	       mincount = countVals[i];
       	       pos = i;
       	    }
        }
       	//-- assign value which appears fewest
       	result = ((Double)vals.get(pos)).doubleValue();
        return result;
    }

}
