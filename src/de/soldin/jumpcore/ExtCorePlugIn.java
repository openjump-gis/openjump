package de.soldin.jumpcore;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.plugin.PlugIn;

// adds i18n support
public abstract class ExtCorePlugIn implements PlugIn {

	protected String i18nPath = "language/plugin";
	protected ResourceBundle rb = null;

	private void initResourceBundle() {

		rb = ResourceBundle.getBundle(i18nPath, Locale.getDefault(), this.getClass().getClassLoader());

	}

	public String _e( final String string ) {
		return getMessageByMessage(string);
	}
	
	public String getMessageByMessage( final String string ) {

		Pattern pattern = Pattern.compile("(\\b)[\\d]+(\\b)");
		Matcher matcher = pattern.matcher( string );
		
	    // Find all the matches. 
		Collection numbers = new Vector();
		StringBuffer result = new StringBuffer();
		int i = 0;
	    while (matcher.find()) { 
	    	numbers.add( matcher.group() );
	    	matcher.appendReplacement( result, "{"+ (i++) +"}");
	    }
	    matcher.appendTail(result);
	    
	    // generate a properties-save key
	    final String key = result.toString().toLowerCase().replaceAll("[\\s:=-]+", "-");
	    String message = "";

		try {
			if ( rb == null ) 
				initResourceBundle();		
			final MessageFormat mformat = new MessageFormat(rb.getString(key));
			message = mformat.format( numbers.toArray() );
		} catch (NullPointerException e) {
			// sh** happens
			Logger.error("missing resource bundle", e);
			System.out.println( e.getLocalizedMessage() );
		} catch (java.util.MissingResourceException e) {
			// translation missing
			Logger.error("translation missing", e);
			System.out.println( e.getLocalizedMessage() );
		}
		
		// no message? use delivered
		return message.length()==0 ? string : message;
	}	
	
	// shortcut method1
	public String m(final String label) {
		return getMessage( label );
	}
	// shortcut method2	
	public String m(final String label, final Object[] objects) {
		return getMessage( label, objects );		
	}
	// shortcut method3	
	public String m(final String label, final Object object) {
		return getMessage( label, new Object[]{object} );		
	}	
	// shortcut method3	
	public String m(final String label, final Object object1, final Object object2) {
		return getMessage( label, new Object[]{object1,object2} );		
	}	
	
	public String getMessage(final String label) {
		return getMessage( label, new Object[]{} );
	}
	
	public String getMessage(final String label, final Object[] objects) {

		String message = null;
		try {
			if ( rb == null ) {
				initResourceBundle();
			}
			final MessageFormat mformat = new MessageFormat(rb.getString(label));
			message = mformat.format(objects);
		} catch (NullPointerException e) {
			// sh** happens
			Logger.error("missing resource bundle", e);
		} catch (java.util.MissingResourceException e) {
			// translation missing
			Logger.warn("translation missing", Logger.isDebugEnabled() ? e : null);
		}
		if (message == null) {
			final String[] labelpath = label.split("\\..+");
			// no default value, the resource key is used
			final MessageFormat mformat = new MessageFormat(
					labelpath[labelpath.length - 1]);
			message = mformat.format(objects);
		}

		return message;
	}
	
}
