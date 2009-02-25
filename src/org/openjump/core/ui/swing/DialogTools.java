/*
 * Created on 17.03.2005 for PIROL
 *
 * SVN header information:
 *  $Author: mentaer $
 *  $Rev: 1245 $
 *  $Date: 2007-11-22 02:35:11 -0700 (Do, 22 Nov 2007) $
 *  $Id: DialogTools.java 1245 2007-11-22 09:35:11Z mentaer $
 */
package org.openjump.core.ui.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openjump.core.apitools.ToolToMakeYourLifeEasier;

import com.vividsolutions.jump.workbench.ui.GUIUtil;


/**
 * @author <strong>Ole Rahn, Stefan Ostermann, Carsten Schulze</strong>
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck
 * <br>Project PIROL 2005
 * <br>Daten- und Wissensmanagement
 * 
 * @since 1.2: changed by Stefan Ostermann at 2005-04-26: added method to return localized
 * string of a double value.
 * @since <br>1.3 changed by Carsten Schulze at 2005-05-22: added a method to 
 * center a given AWT-Window (or subclasses) on the screen.
 * @version $Rev: 1245 $
 */
public class DialogTools extends ToolToMakeYourLifeEasier{
    
    /**
     * This method centers the window (or subclasses of it) on the screen.
     * @param window the java.awt.Window (or a subclass of it) that should be 
     * displayed in the middle of the screen. 
     */
    public static void centerOnScreen(Window window){
//        Dimension screenDim = window.getToolkit().getScreenSize();
//        Dimension windowDim = window.getSize();
//        int x = (screenDim.width / 2) - (windowDim.width / 2);
//        int y = (screenDim.height / 2) - (windowDim.height / 2);
//        
//        window.setLocation(x,y);
        
        GUIUtil.centreOnScreen(window);
    }
    
    public static void centerOnWindow(Component component){
      GUIUtil.centreOnWindow(component);
    }
    
    public static void centerOnWindow(Component component2move, Component component2CenterOn){
        GUIUtil.centre(component2move, component2CenterOn);
      }
    
    /**
     * This method creates a JPanel with several JLabels on it. For another 
     * method to display multiline bold text, have a look at the 
     * {javax.swing.JTextArea} and the {@link java.awt.Font} object.
     * @param text the text to split up into some JLabels.
     * @param charsPerLine the maximum number of characters per line text.
     * @return the panel with the labels on it.
     */
    public static JPanel getPanelWithLabels(String text, int charsPerLine){
        List<String> labelTextParts = new ArrayList<String>();
		if ( text.length() > charsPerLine ){
			int estimatedStrings = (int)Math.ceil((float)text.length() / charsPerLine);
			
			String copyLabelText = text.toString();
			for ( int i=0; i<estimatedStrings; i++ ){
				if (copyLabelText.indexOf(" ", charsPerLine)>-1)
					labelTextParts.add( copyLabelText.substring(0, copyLabelText.indexOf(" ", charsPerLine)+1) );
				else
					labelTextParts.add( copyLabelText );
				copyLabelText = copyLabelText.substring( ((String)labelTextParts.get(i)).length() );
			}
			
		} else {
			labelTextParts.add(text);
		}
		
		JPanel texts = new JPanel();
        GridLayout gl = new GridLayout(labelTextParts.size(), 1);
        gl.setHgap(0);
		texts.setLayout(gl);
		//texts.setPreferredSize( new Dimension(400, 50));
		for ( int i=0; i<labelTextParts.size(); i++ ){
			texts.add(new JLabel("  "+labelTextParts.get(i)+"  "));
		}
		texts.doLayout();
		return texts;
    }
    
    /**
     * This method replaces the localized decimal seperator with a dot.
     * @param s the String containing the double value.
     * @return the now dotted double value.
     * @see #numberStringToLocalNumberString(String)
     * @see #numberToLocalNumberString(double)
     */
    public static double localNumberStringToDouble (String s) {
    	DecimalFormatSymbols ds = new DecimalFormatSymbols();
    	s=s.replace(ds.getDecimalSeparator(),'.');
    	return Double.parseDouble(s);
    }
    
    /**
     * This method replaces the dot with the localized decimal seperator.
     * @param s the String containing the double value.
     * @return the localized String containing the double value.
     * @see #localNumberStringToDouble(String)
     * @see #numberToLocalNumberString(double)
     */
    public static String numberStringToLocalNumberString (String s) {
    	DecimalFormatSymbols ds = new DecimalFormatSymbols();
    	s=s.replace('.',ds.getDecimalSeparator());
    	return s;
    }
    
    /**
     * This method replaces the dot with the localized decimal seperator.
     * @param number the double value.
     * @return the localized String containing the double value.
     * @see #numberStringToLocalNumberString(String)
     * @see #localNumberStringToDouble(String)
     */
    public static String numberToLocalNumberString (double number) {
    	String s = new Double(number).toString();
    	return DialogTools.numberStringToLocalNumberString(s);
    }
    
    /**
     * Sets the prefered width of an JComponent and keeps it prefered height.
     * @param component the component to alter
     * @param width the new prefered width
     */
    public static void setPreferedWidth(JComponent component, int width) {
    	int preferedHeight = component.getPreferredSize().height;
    	component.setPreferredSize(new Dimension(width, preferedHeight));
    }
    
    /**
     * Sets the prefered height of an JComponent and keeps it prefered height.
     * @param component the component to alter
     * @param height the new prefered width
     */
    public static void setPreferedHeight(JComponent component, int height) {
    	int preferedWidth = component.getPreferredSize().width;
    	component.setPreferredSize(new Dimension(preferedWidth, height));
    }
    
    public static void setMaximumWidth(JComponent component, int width) {
    	int preferedHeight = component.getPreferredSize().height;
    	component.setMaximumSize(new Dimension(width, preferedHeight));
    }
    
    public static void setMaximumHeight(JComponent component, int height) {
    	int preferedWidth = component.getPreferredSize().width;
    	component.setMaximumSize(new Dimension(preferedWidth, height));
    }
}
