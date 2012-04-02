/*
 * Created on 20.06.2005
 *
 * CVS information:
 *  $Author$
 *  $Date$
 *  $ID$
 *  $Rev$
 *  $Id$
 *  $Log$
 *  Revision 1.4  2007/03/24 19:33:19  mentaer
 *  changed ui, so that pirol formula propeties file is not assumed (used formulas are not stored)
 *
 *  Revision 1.3  2007/03/24 18:13:11  mentaer
 *  changed to inherit AbstractPlugIn instead of StandardPirolPlugIn, subsequently changed also to normal logger
 *
 *  Revision 1.2  2007/02/03 14:19:47  mentaer
 *  modified debug output for pirol stuff
 *
 *  Revision 1.1  2006/11/23 18:53:15  mentaer
 *  added EditAttributeByFormula Plugin by Pirol including some parts of the baseclasses - note: plugin needs java 1.5
 *
 *  Revision 1.6  2006/11/05 14:28:05  mentaer
 *  translated pirol attribute calculator plugin
 *
 *  Revision 1.5  2006/11/05 13:47:35  mentaer
 *  refactoring of menu positions and set OJ version to 1.1 B
 *
 *  Revision 1.4  2006/11/04 19:22:42  mentaer
 *  changed menu position
 *
 *  Revision 1.3  2006/11/04 19:16:09  mentaer
 *  changed enable check
 *
 *  Revision 1.2  2006/11/04 19:11:58  mentaer
 *  *** empty log message ***
 *
 *  Revision 1.1  2006/11/04 19:09:34  mentaer
 *  added Pirol Plugin for Attribute Calculations for testing, which needs the baseclasses.jar
 *
 *  Revision 1.12  2006/05/09 14:31:39  orahn
 *  small GUI beautification
 *
 *  Revision 1.11  2006/02/01 17:35:56  orahn
 *  + support attribute names with spaces
 *  + small, general update for the PlugIn
 *
 *  Revision 1.10  2005/08/03 13:50:44  orahn
 *  +i18n
 *  -warnings
 *
 *  Revision 1.9  2005/07/13 10:12:55  orahn
 *  Einsatz: MetaInformationHandler
 *
 *  Revision 1.8  2005/07/12 16:33:56  orahn
 *  +Nutzung des PropertiesHandler
 *
 *  Revision 1.7  2005/06/30 10:42:12  orahn
 *  besseres Fehler-Feedback bei der Formeleingabe
 *
 *  Revision 1.6  2005/06/30 08:37:40  orahn
 *  misslungene Formel beschaedigt nicht mehr das Layer
 *
 *  Revision 1.5  2005/06/29 16:03:57  orahn
 *  aufgemotzt
 *
 *  Revision 1.4  2005/06/28 15:35:18  orahn
 *  almost as far: it still lacks a "taste" like backspace for all operators and operands
 *  and the Formula text box must be protected against direct user input
 *
 *  Revision 1.3  2005/06/23 13:57:20  orahn
 *  nutzt jetzt uniqueAttributeName
 *
 *  Revision 1.2  2005/06/23 13:41:17  orahn
 *  erste BETA des Formel-Parser-PlugIns
 *
 *  Revision 1.1  2005/06/20 18:17:34  orahn
 *  erster Ansatz
 *
 */
package de.fho.jump.pirol.plugins.EditAttributeByFormula;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.openjump.core.apitools.FeatureCollectionTools;
import org.openjump.io.PropertiesHandler;
import org.openjump.util.metaData.MetaInformationHandler;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.plugin.AbstractPlugIn;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.MenuNames;

import de.fho.jump.pirol.utilities.FormulaParsing.FormulaValue;
import de.fho.jump.pirol.utilities.attributes.AttributeInfo;
import de.fho.jump.pirol.utilities.plugIns.StandardPirolPlugIn;
import de.fho.jump.pirol.utilities.settings.PirolPlugInSettings;

/**
 * 
 * PlugIn that creates a new attribute and assigns values to it, that are 
 * calculated by processing a formula that was created by the user.
 *
 * @author Ole Rahn
 * <br>
 * <br>FH Osnabr&uuml;ck - University of Applied Sciences Osnabr&uuml;ck,
 * <br>Project: PIROL (2005),
 * <br>Subproject: Daten- und Wissensmanagement
 * 
 * @version $Rev$
 */
//--[sstein 24.March.2007] changed to normal plugin - thus we do not need 
//  to load and create the properties file
//public class EditAttributeByFormulaPlugIn extends StandardPirolPlugIn {
public class EditAttributeByFormulaPlugIn extends AbstractPlugIn {

    protected static PropertiesHandler storedFormulas = null;
    protected static final String storedFormulasFileName = "Formula.properties"; //$NON-NLS-1$
    //[sstein 24.March.2007] added this logger instead using Personal logger
    private static final Logger LOG = Logger.getLogger(EditAttributeByFormulaPlugIn.class);
    
    public void initialize(PlugInContext context) throws Exception {
	    context.getFeatureInstaller().addMainMenuItem(this,
		        new String[] {MenuNames.TOOLS, MenuNames.TOOLS_EDIT_ATTRIBUTES },
				this.getName(), 
				false, 
				null, 
				createEnableCheck(context.getWorkbenchContext()));
    }
    
    public String getName(){
    	return I18N.get("pirol.plugIns.EditAttributeByFormulaPlugIn.Attribute-Calculator");
    }
    
    public static MultiEnableCheck createEnableCheck(WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);

        return new MultiEnableCheck()
                        .add(checkFactory.createAtLeastNLayersMustBeSelectedCheck(1))
                        .add(checkFactory.createSelectedLayersMustBeEditableCheck());
    }
    
    /* //-- [sstein 24.March 2007] disabled since we make it to a 
       //    normal plugin, not a StandardPirolPlugin
    public EditAttributeByFormulaPlugIn(){
        super(new PersonalLogger(DebugUserIds.ALL)); 
    }
    */
    
    /**
     * @inheritDoc
     */
    public String getIconString() {
        return null;
    }
    
    /**
     *@inheritDoc
     */
    public String getCategoryName() {
        return PirolPlugInSettings.getName_AttributeMenu();
    }

    /**
     * @inheritDoc
     */
    public boolean execute(PlugInContext context) throws Exception {
        Layer layer = StandardPirolPlugIn.getSelectedLayer(context);
        
        if (layer==null){
            StandardPirolPlugIn.warnUser(context,I18N.get("pirol.plugIns.EditAttributeByFormulaPlugIn.no-layer-selected")); //$NON-NLS-1$
            //--[sstein 24.March 2007]: disabled since I changed from StandardPirolPlugIn to AbstractPlugIn
            return false;
            //return this.finishExecution(context, false);
        } else if (!layer.isEditable()) {
            StandardPirolPlugIn.warnUser(context,I18N.get("pirol.plugIns.EditAttributeByFormulaPlugIn.layer-not-editable")); //$NON-NLS-1$
            //--[sstein 24.March 2007] disabled since I changed from StandardPirolPlugIn to AbstractPlugIn
            //return this.finishExecution(context, false);
            return false;
        }
        

        try {
        	//--[sstein 24.March 2007] currently the following line is useless
            EditAttributeByFormulaPlugIn.storedFormulas = new PropertiesHandler(EditAttributeByFormulaPlugIn.storedFormulasFileName);
            //--[sstein 24.March 2007] disabled - we dont assume an existing file
            //EditAttributeByFormulaPlugIn.storedFormulas.load();
        } 
        /* catch (FileNotFoundException e1) {
            //this.logger.printWarning(e1.getMessage());
        	this.LOG.warn(e1.getMessage());
        } 
        catch (IOException e1) {
            //this.logger.printWarning(e1.getMessage());
        	this.LOG.warn(e1.getMessage());
        } 
        **/
        catch (Exception e1) {
            //this.logger.printWarning(e1.getMessage());
        	this.LOG.warn(e1.getMessage());
        }
        
        /* [sstein 24.March 2007] replaced - since we dont have stored formulas
        EditAttributeByFormulaDialog dialog = new EditAttributeByFormulaDialog(context.getWorkbenchFrame(), 
        		I18N.get("pirol.plugIns.EditAttributeByFormulaPlugIn.specify-attribute-and-formula"), 
				true, 
				I18N.get("pirol.plugIns.EditAttributeByFormulaPlugIn.editByFormula-explaining-text"), 
				layer.getFeatureCollectionWrapper().getFeatureSchema(), 
				EditAttributeByFormulaPlugIn.storedFormulas); //$NON-NLS-1$ //$NON-NLS-2$
        */
        EditAttributeByFormulaDialog dialog = new EditAttributeByFormulaDialog(context.getWorkbenchFrame(), 
        		I18N.get("pirol.plugIns.EditAttributeByFormulaPlugIn.specify-attribute-and-formula"), 
				true, 
				I18N.get("pirol.plugIns.EditAttributeByFormulaPlugIn.editByFormula-explaining-text"), 
				layer.getFeatureCollectionWrapper().getFeatureSchema()); 
        
        dialog.setVisible(true);
        
        String formula = dialog.getFormula();
        
        if (!dialog.wasOkClicked() || formula==null || formula.length()==0 ){
        	//--[sstein 24.March 2007]: disabled since I changed from StandardPirolPlugIn to AbstractPlugIn
            //return this.finishExecution(context, false);
        	return false;
        }
        
        
        AttributeInfo attrInfo = dialog.getAttributeInfo();
        
        FeatureCollection oldFc = layer.getFeatureCollectionWrapper().getUltimateWrappee();
        
        attrInfo.setUniqueAttributeName(FeatureCollectionTools.getUniqueAttributeName(oldFc, attrInfo.getAttributeName()));
        
        try {
            FormulaValue parsedFormula = dialog.getParsedFormula();
            
            FeatureCollection newFc = FeatureCollectionTools.applyFormulaToFeatureCollection( oldFc, attrInfo, parsedFormula, true );
            layer.setFeatureCollection(newFc);
            
            MetaInformationHandler metaInfHandler = new MetaInformationHandler(layer);
            metaInfHandler.addMetaInformation(
            		I18N.get("pirol.plugIns.EditAttributeByFormulaPlugIn.formula-for") + 
					attrInfo.getUniqueAttributeName(), 
					formula); //$NON-NLS-1$

            if (storedFormulas != null){
                storedFormulas.setProperty(attrInfo.toString(), formula);
                storedFormulas.store(I18N.get("pirol.plugIns.EditAttributeByFormulaPlugIn.editByFormula-properties-comment")); //$NON-NLS-1$
            }
            
            
        } catch (Exception e){
        	//--[sstein 24.March 2007]: disabled since I changed from StandardPirolPlugIn to AbstractPlugIn
            //this.handleThrowable(e);
            LOG.debug(e.getMessage());
            e.printStackTrace();
            //return this.finishExecution(context, false);
            return false;
        }
        //--[sstein 24.March 2007]: disabled since I changed from StandardPirolPlugIn to AbstractPlugIn
        //return this.finishExecution(context, true);
        return false;
    }

}
