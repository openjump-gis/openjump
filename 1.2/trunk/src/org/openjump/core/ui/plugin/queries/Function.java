package org.openjump.core.ui.plugin.queries;

import com.vividsolutions.jump.I18N;


/**
 * Function
 * Definition of functions used in the QueryDialog 
 * @author Michaël MICHAUD
 * @version 0.2 (16 Oct 2005)
 */

public class Function {
    private String key;
    public char type;  // B=boolean, N=numeric, S=string, E=enumeration, G=geometric
    public int[] args; // arguments for the substring function
    public double arg; // argument for the buffer function
    
    // BOOLEAN FUNCTION
    public final static Function BNOF = new Function("bnof", 'B');
    
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
    
    public static Function[] BOOLEAN_FUNCTIONS = new Function[] {BNOF};
    
    public static Function[] NUMERIC_FUNCTIONS = new Function[] {NNOF};
    
    public static Function[] STRING_FUNCTIONS = new Function[] {
            SNOF, TRIM, SUBS, LENG
    };
            
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
