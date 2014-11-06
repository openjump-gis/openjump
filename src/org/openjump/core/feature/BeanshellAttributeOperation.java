/*
Copyright (c) 2012, Michaël Michaud
All rights reserved.
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of its authors nor the names of its contributors may
      be used to endorse or promote products derived from this software without
      specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS "AS IS" AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.openjump.core.feature;

import bsh.EvalError;
import bsh.Interpreter;

import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.util.FlexibleDateParser;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.model.Layer;

import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Creates an Operation using the Beanshell scripting language.
 * @author Micha&euml;l Michaud
 * @version 0.1 (2012-11-17)
 */
 // 0.1 (2012-11-17)
public class BeanshellAttributeOperation implements Operation {

    private static final FlexibleDateParser DATE_PARSER = new FlexibleDateParser();

    private PlugInContext context;
    private AttributeType type;
    private String bshExpression;
    private Interpreter interpreter;
    
    public BeanshellAttributeOperation(PlugInContext context,
                              AttributeType type,
                              String bshExpression) throws EvalError {
        this.context = context;
        this.type = type;
        this.bshExpression = bshExpression;
        this.interpreter = initInterpreter(context);
    }
    
    public Object invoke(Feature feature) throws Exception {
        return evaluate((BasicFeature)feature);
    }
    
    public Object evaluate(BasicFeature f) throws EvalError, 
                           NumberFormatException, IllegalArgumentException, ParseException {
        
        FeatureSchema schema = f.getSchema();
        
        // attributes evaluated dynamically are tagged in the feature userData 
        // to avoid cyclic reference 
        Set<Integer> evaluatedAttributes = (Set<Integer>)f.getUserData("evaluatedAttributes");
        if (evaluatedAttributes == null) {
            evaluatedAttributes = new HashSet<Integer>();
            f.setUserData("evaluatedAttributes", evaluatedAttributes);
        }
        //evaluatedAttributes.add(attributeIndex);
        
        for (int i = 0 ; i < schema.getAttributeCount() ; i++) {
            // to avoid cyclic references, dynamic attributes are evaluated only once
            //System.out.println("attribute " + schema.getAttributeName(i));
            if (schema.getOperation(i) == this) {
                evaluatedAttributes.add(i);
                continue;
            }
            if (schema.isOperation(i) && evaluatedAttributes.contains(i)) continue;
            try {
                interpreter.set(normalizeVarName(schema.getAttributeName(i)), 
                                f.getAttribute(i));
                //System.out.println("  set to " + f.getAttribute(i));
                evaluatedAttributes.add(i);
            }
            catch(EvalError e) {
                context.getWorkbenchFrame().warnUser(e.toString());
                throw e;
            }
        }
        f.removeUserData((Object)"evaluatedAttributes");
        try {
            interpreter.set("geometry", f.getGeometry());
            interpreter.set("Geometry", f.getGeometry());
            interpreter.set("GEOMETRY", f.getGeometry());
            interpreter.set("feature", f);
            interpreter.set("Feature", f);
            interpreter.set("FEATURE", f);
            Object obj = interpreter.eval(bshExpression);
            //AttributeType type = schema.getAttributeType(attributeIndex);
            if (obj == null) return null;
            else if (type == AttributeType.STRING) {
                if (obj instanceof String) return obj;
                else return obj.toString();
            }
            else if (type == AttributeType.DOUBLE) {
                if (obj instanceof Double) return obj;
                else return new Double(obj.toString());
            }
            else if (type == AttributeType.INTEGER) {
                if (obj instanceof Integer) return obj;
                else return new Integer(obj.toString());
            }
            else if (type == AttributeType.DATE) {
                if (obj instanceof Date) return obj;
                else return DATE_PARSER.parse(obj.toString(), true);
            }
            else return obj;
        }
        catch(EvalError e) {
            context.getWorkbenchFrame().warnUser(e.toString());
            throw e;
        } 
        catch(NumberFormatException e) {
            context.getWorkbenchFrame().warnUser(e.toString());
            throw e;
        } 
        catch(IllegalArgumentException e) {
            context.getWorkbenchFrame().warnUser(e.toString());
            throw e;
        }
    }
    
    public String toString() {
        return getClass().getName() + "\n" + bshExpression;
    }
    
    public Object clone() throws CloneNotSupportedException {
        try {
            return new BeanshellAttributeOperation(context, type, bshExpression);
        } catch(EvalError e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Creates a new Beanshell Interpreter initialized with an instance of
     * OpenJUMP's WorkbenchContext ("wc") and the definition of a few useful 
     * methods.
     */
    private Interpreter initInterpreter(PlugInContext context) throws EvalError {
        Interpreter interpreter = new Interpreter();
        interpreter.setClassLoader(context.getWorkbenchContext().getWorkbench()
                .getPlugInManager().getClassLoader());
        interpreter.set("wc", context.getWorkbenchContext());
        interpreter.eval("setAccessibility(true)");
        interpreter.eval("import com.vividsolutions.jts.geom.*");
        interpreter.eval("import com.vividsolutions.jump.feature.*");
        interpreter.eval("import com.vividsolutions.jts.operation.union.UnaryUnionOp");
        
        interpreter.eval("selection() {"+
            "return wc.layerViewPanel.selectionManager.featuresWithSelectedItems;}");
        interpreter.eval(
            "dataset(String layerName) {" +
            "return wc.layerManager.getLayer(layerName).getFeatureCollectionWrapper().features;}");
        interpreter.eval("intersects(Feature feature, Collection features) {" +
            "for (f : features) {if (feature.geometry.intersects(f.geometry) && feature.ID!=f.ID) return true;}" +
            "return false;}");
        interpreter.eval("distance(Feature feature, Collection features) {" +
            "min = Double.MAX_VALUE;" +
            "for (f : features) {min = Math.min(min,feature.geometry.distance(f.geometry));}" +
            "return min == Double.MAX_VALUE ? null : min;}");
        interpreter.eval(
            "round(double d, int i) {" +
            " p10 = Math.pow(10.0,(double)i);" +
            " return Math.rint(d*p10)/p10;" +
            "}");
        return interpreter;
    }
    
    public static String normalizeVarName(String s) {
        StringBuffer sb = new StringBuffer(s);
        for (int i = 0 ; i < s.length() ; i++) {
            if (!Character.isJavaIdentifierPart(sb.charAt(i))) {
                sb.setCharAt(i, '_');
            }
        }
        return sb.toString();
    }                                                   
    
}
                                               
