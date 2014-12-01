package org.openjump.core.ui.plugin.queries;

import com.vividsolutions.jump.I18N;

/**
 * Operator contains the definition of common operators.
 * @author Micha&euml;l MICHAUD
 * @version 0.2 (16 Oct 2005)
 */
public class Operator {
    private String key;
    public char type;  // B=boolean, N=numeric, S=string, E=enumeration, G=geometric
    public Object arg; // arguments for the within distance and the relate functions
    
    // OPERATOR FOR BOOLEAN VALUES AND DATES
    public final static Operator BEQ = new Operator("beq", 'B');
    public final static Operator BNE = new Operator("bne", 'B');
    
    // OPERATOR FOR NUMERIC VALUES AND DATES
    public final static Operator EQ = new Operator("eq", 'N');
    public final static Operator NE = new Operator("ne", 'N');
    public final static Operator LT = new Operator("lt", 'N');
    public final static Operator GT = new Operator("gt", 'N');
    public final static Operator LE = new Operator("le", 'N');
    public final static Operator GE = new Operator("ge", 'N');

    // OPERATOR FOR STRING VALUES
    public final static Operator EQUA= new Operator("equa", 'S');
    public final static Operator DIFF = new Operator("diff", 'S');
    public final static Operator STAR = new Operator("star", 'S');
    public final static Operator ENDS = new Operator("ends", 'S');
    public final static Operator MATC = new Operator("matc", 'S');
    public final static Operator FIND = new Operator("find", 'S');
    public final static Operator BEFO = new Operator("befo", 'S');
    public final static Operator AFTE = new Operator("afte", 'S');
        
    // OPERATOR FOR GEOMETRIC ATTRIBUTE
    public final static Operator INTER = new Operator("inter", 'G');
    public final static Operator CONTA = new Operator("conta", 'G');
    public final static Operator WITHI = new Operator("withi", 'G');
    public final static Operator WSTRI = new Operator("wstri", 'G');
    public final static Operator WDIST = new Operator("wdist", 'G', 1000.0);
    public final static Operator TOUCH = new Operator("touch", 'G');
    public final static Operator CROSS = new Operator("cross", 'G');
    public final static Operator OVERL = new Operator("overl", 'G');
    public final static Operator DISJO = new Operator("disjo", 'G');
    public final static Operator RELAT = new Operator("relat", 'G', "FF*F1****");
    
    public static Operator[] BOOLEAN_OP = new Operator[] {BEQ, BNE};
    
    public static Operator[] NUMERIC_OP = new Operator[] {
        EQ, NE, LT, GT, LE, GE
    };
        
    public static Operator[] STRING_OP = new Operator[] {
        EQUA, DIFF, STAR, ENDS, MATC, FIND, BEFO, AFTE
    };
        
    public static Operator[] GEOMETRIC_OP = new Operator[] {
        INTER, CONTA, WITHI, WSTRI, WDIST, TOUCH, CROSS, OVERL, DISJO, RELAT
    };  
    
    public Operator(String key, char type) {
        this.key = key;
        this.type = type;
    }
    
    public Operator(String key, char type, Object arg) {
        this.key = key;
        this.type = type;
        this.arg = arg;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer(I18N.get("org.openjump.core.ui.plugin.queries.Operator."+key));
        if(arg == null) {return sb.toString();}
        else {return sb.toString() + " (" + arg + ")";}
    }

}
