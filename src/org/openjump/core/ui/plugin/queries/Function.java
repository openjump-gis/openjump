package org.openjump.core.ui.plugin.queries;

import com.vividsolutions.jump.I18N;


/**
 * Function
 * Definition of functions used in the QueryDialog 
 * @author Micha&euml;l MICHAUD
 * @version 0.2 (16 Oct 2005)
 */

public class Function {
    /** Function key is fully internationalized*/
    private String key;
    
    /** Returned type : B=boolean, N=numeric, S=string, E=enumeration, G=geometric*/
    public char type;
    
    /** Option function arguments (used for substring)*/
    public int[] args;
    
    /** Optional function double argument (used for buffer)*/ 
    public double arg; // argument for the buffer function
    
    // IS NULL FUNCTION (AVAILABLE FOR ANY TYPE BUT GEOMETRY)
    public final static Function ISNULL = new Function("isnull", 'B');
    
    // BOOLEAN FUNCTION
    public final static Function BNOF = new Function("bnof", 'B');
    
    // DATE FUNCTION
    public final static Function DDAY = new Function("dday", 'D');
    public final static Function DYEA = new Function("dyea", 'D');
    public final static Function DNOF = new Function("dnof", 'D');
    
    // NUMERIC FUNCTION
    public final static Function NNOF = new Function("nnof", 'N');
    
    // STRING FUNCTION
    public final static Function SNOF = new Function("snof", 'S');
    public final static Function TRIM = new Function("trim", 'S');
    public final static Function SUBS = new Function("subs", 'S', new int[]{0,2});
    
    public final static Function LENG = new Function("leng", 'N');
    
    // GEOMETRIC FUNCTION
    public final static Function GNOF = new Function("gnof", 'G');
    //public final static Function LENG = new Function("leng", 'N');
    public final static Function AREA = new Function("area", 'N');
    public final static Function NBPT = new Function("nbpt", 'N');
    public final static Function NBPA = new Function("nbpa", 'N');
    public final static Function BUFF = new Function("buff", 'G', 1000);
    public final static Function CENT = new Function("cent", 'G');
    public final static Function EMPT = new Function("empt", 'B');
    public final static Function SIMP = new Function("simp", 'B');
    public final static Function VALI = new Function("vali", 'B');
    
    /** Functions to be applied to Boolean attributes (currently not possible)*/
    public static Function[] BOOLEAN_FUNCTIONS = new Function[] {BNOF, ISNULL};
    
    /** Functions to be applied to Date attributes (currently not possible)*/
    public static Function[] DATE_FUNCTIONS = new Function[] {DNOF, DDAY, DYEA, ISNULL};
    
    /** Functions to be applied to numeric attributes*/
    public static Function[] NUMERIC_FUNCTIONS = new Function[] {NNOF, ISNULL};
    
    /** Functions to be applied to String attributes*/ 
    public static Function[] STRING_FUNCTIONS = new Function[] {
            SNOF, TRIM, SUBS, LENG, ISNULL
    };
    
    /** Functions to be applied to Geometry attributes*/
    public static Function[] GEOMETRIC_FUNCTIONS = new Function[] {
            GNOF, LENG, AREA, NBPT, NBPA, BUFF, CENT, EMPT, SIMP, VALI
    };    
    
    public Function(String key, char type) {
        this.key = key;
        this.type = type;
    }
    
    public Function(String key, char type, int[] args) {
        this.key = key;
        this.type = type;
        this.args = args;
    }
    
    public Function(String key, char type, double arg) {
        this.key = key;
        this.type = type;
        this.arg = arg;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer(I18N.get("org.openjump.core.ui.plugin.queries.Function."+key));
        if(this==BUFF) {return sb.toString() + " ("+arg+")";}
        else if (this==SUBS && args.length==1) {return sb.toString() + " ("+args[0] + ")";}
        else if (this==SUBS && args.length==2) {return sb.toString() + " ("+args[0]+","+args[1]+")";}
        else {return sb.toString();}
    }

}
