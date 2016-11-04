package org.openjump.core.ui.plugin.tools;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.FeatureEventType;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.UndoableCommand;
import com.vividsolutions.jump.workbench.plugin.EnableCheck;
import com.vividsolutions.jump.workbench.plugin.EnableCheckFactory;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.ui.*;
import com.vividsolutions.jump.workbench.ui.plugin.FeatureInstaller;
import org.openjump.core.ui.plugin.AbstractUiPlugIn;

import javax.swing.*;
import java.awt.*;
import java.util.*;

/**
 * Plugin to fill an attribute with randomly generated numbers
 */
public class GenerateRandomStringPlugIn extends AbstractUiPlugIn {

    private static String LAYER         = I18N.get("ui.GenericNames.select-layer");
    private static String ATTRIBUTE     = I18N.get("ui.GenericNames.select-attribute");
    private static String ATTRIBUTE_TT  = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomStringPlugIn.attribute-tooltip");
    private static String RANDOM        = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomStringPlugIn.random-generators");
    private static String MIN_LENGTH    = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomStringPlugIn.min-length");
    private static String MIN_LENGTH_TT = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomStringPlugIn.min-length-tooltip");
    private static String MAX_LENGTH    = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomStringPlugIn.max-length");
    private static String MAX_LENGTH_TT = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomStringPlugIn.max-length-tooltip");

    private static String LETTER_BASED  = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomStringPlugIn.letter-based");
    private static String WORD_BASED    = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomStringPlugIn.word-based");
    private static String DIGITS        = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomStringPlugIn.digits");
    private static String HEXA          = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomStringPlugIn.hexa");
    private static String ASCII         = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomStringPlugIn.ascii");
    private static String CITIES        = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomStringPlugIn.cities");
    private static String NAMES         = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomStringPlugIn.names");

    private static String NO_CANDIDATE  = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomStringPlugIn.no-editable-layer-with-string-attribute");
    private static String NON_EMPTY_ATT = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomStringPlugIn.non-empty-attribute");
    private static String OVERWRITE_PROMPT = I18N.get("org.openjump.core.ui.plugin.tools.GenerateRandomStringPlugIn.overwrite-prompt");

    private Layer layer;
    private String attribute;
    private int min, max;
    private boolean digits, hexa, ascii, cities, names;

    public void initialize(PlugInContext context) throws Exception {

        FeatureInstaller featureInstaller = new FeatureInstaller(context.getWorkbenchContext());
        featureInstaller.addMainMenuPlugin(
                this,
                new String[] {MenuNames.TOOLS, MenuNames.TOOLS_EDIT_ATTRIBUTES, RANDOM},
                getName() + "...", false, null,
                createEnableCheck(context.getWorkbenchContext()));
    }

    public static MultiEnableCheck createEnableCheck(final WorkbenchContext workbenchContext) {
        EnableCheckFactory checkFactory = new EnableCheckFactory(workbenchContext);
        return new MultiEnableCheck()
                .add(checkFactory.createAtLeastNLayersMustExistCheck(1))
                .add(checkFactory.createAtLeastNLayersMustBeEditableCheck(1))
                .add(new EnableCheck() {
                    @Override
                    public String check(JComponent component) {
                        if (AttributeTypeFilter.STRING_FILTER.filter(
                                workbenchContext.getLayerManager().getEditableLayers()).size() == 0) {
                            return NO_CANDIDATE;
                        }
                        return null;
                    }
                });
    }

    public boolean execute(PlugInContext context) throws Exception{
        this.reportNothingToUndoYet(context);

        MultiInputDialog dialog = new MultiInputDialog(
                context.getWorkbenchFrame(), getName(), true);
        setDialogValues(dialog, context);
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (! dialog.wasOKPressed()) { return false; }
        getDialogValues(dialog);
        boolean empty = checkAttributeEmpty();
        if (!empty) {
            JLabel label = new JLabel();
            label.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
            label.setText("<html><body>" + NON_EMPTY_ATT + "<br/>" + OVERWRITE_PROMPT + "</body></html>");
            OKCancelDialog okCancelDialog = new OKCancelDialog(dialog, NON_EMPTY_ATT, true,
                    label,
                    new OKCancelDialog.Validator() {
                        @Override
                        public String validateInput(Component component) {
                            return null;
                        }
                    });
            okCancelDialog.setVisible(true);
            if (!okCancelDialog.wasOKPressed()) {
                return false;
            }
        }
        computeRandomValues(context);
        return true;
    }

    private boolean checkAttributeEmpty() {
        for (Feature f : layer.getFeatureCollectionWrapper().getFeatures()) {
            if (f.getAttribute(attribute) != null) return false;
        }
        return true;
    }

    private void computeRandomValues(PlugInContext context) {
        final Collection<Feature> oldFeatures = new ArrayList<>();
        final Collection<Feature> newFeatures = new ArrayList<>();
        reportNothingToUndoYet(context);
        for (Feature f : layer.getFeatureCollectionWrapper().getFeatures()) {
            oldFeatures.add(f.clone(true, true));
            f = f.clone(true, true);
            f.setAttribute(attribute, generate(min, max));
            newFeatures.add(f);
        }
        context.getLayerManager().getUndoableEditReceiver().startReceiving();
        try {
            UndoableCommand command =
                    new UndoableCommand(I18N.get(AutoAssignAttributePlugIn.class.getName())) {

                        public void execute() {
                            Iterator<Feature> newFeatIterator = newFeatures.iterator();
                            for (Feature f : layer.getFeatureCollectionWrapper().getFeatures()) {
                                f.setAttribute(attribute, newFeatIterator.next().getAttribute(attribute));
                            }
                            layer.getLayerManager().fireFeaturesAttChanged(newFeatures,
                                    FeatureEventType.ATTRIBUTES_MODIFIED, layer, oldFeatures);
                        }

                        public void unexecute() {
                            Iterator<Feature> oldFeatIterator = oldFeatures.iterator();
                            for (Feature f : layer.getFeatureCollectionWrapper().getFeatures()) {
                                f.setAttribute(attribute, oldFeatIterator.next().getAttribute(attribute));
                            }
                            layer.getLayerManager().fireFeaturesAttChanged(
                                    layer.getFeatureCollectionWrapper().getFeatures(),
                                    FeatureEventType.ATTRIBUTES_MODIFIED, layer, newFeatures);
                        }
                    };
            command.execute();
            layer.getLayerManager().getUndoableEditReceiver().receive(command.toUndoableEdit());
        } finally {
            layer.getLayerManager().getUndoableEditReceiver().stopReceiving();
        }
    }

    private Random rnd = new Random();
    private char[] digitsArray = "0123456789".toCharArray();
    private char[] hexaArray   = "0123456789ABCDEF".toCharArray();
    private char[] asciiArray  = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~".toCharArray();
    private String[] cityArray = new String[]{"Shanghai","Karachi","Beijing","Delhi","Lagos","Tianjin","Istanbul","Tokyo","Guangzhou","Mumbai","Moscow","São Paulo","Shenzhen","Jakarta","Lahore","Seoul","Kinshasa","Cairo","Mexico City","Lima","London","New York City","Bengaluru","Bangkok","Ho Chi Minh City","Dongguan","Chongqing","5,473[d]","Nanjing","Tehran","Shenyang","Bogotá","Ningbo","Hong Kong","Hanoi","Baghdad","Changsha","Dhaka","Wuhan","Hyderabad","Chennai","Rio de Janeiro","Faisalabad","Foshan","Zunyi","Santiago","Riyadh","Ahmedabad","Singapore","Shantou","Yangon","Saint Petersburg"};
    private String[] surnames  = new String[]{"SMITH","JOHNSON","WILLIAMS","BROWN","JONES","MILLER","DAVIS","GARCIA","RODRIGUEZ","WILSON","MARTINEZ","ANDERSON","TAYLOR","THOMAS","HERNANDEZ","MOORE","MARTIN","JACKSON","THOMPSON","WHITE","LOPEZ","LEE","GONZALEZ","HARRIS","CLARK","LEWIS","ROBINSON","WALKER","PEREZ","HALL","YOUNG","ALLEN","SANCHEZ","WRIGHT","KING","SCOTT","GREEN","BAKER","ADAMS","NELSON","HILL","RAMIREZ","CAMPBELL","MITCHELL","ROBERTS","CARTER","PHILLIPS","EVANS","TURNER","TORRES","PARKER","COLLINS","EDWARDS","STEWART","FLORES","MORRIS","NGUYEN","MURPHY","RIVERA","COOK","ROGERS","MORGAN","PETERSON","COOPER","REED","BAILEY","BELL","GOMEZ","KELLY","HOWARD","WARD","COX","DIAZ","RICHARDSON","WOOD","WATSON","BROOKS","BENNETT","GRAY","JAMES","REYES","CRUZ","HUGHES","PRICE","MYERS","LONG","FOSTER","SANDERS","ROSS","MORALES","POWELL","SULLIVAN","RUSSELL","ORTIZ","JENKINS","GUTIERREZ","PERRY","BUTLER","BARNES","FISHER"};
    private String[] firstNames = new String[]{"James","Mary","John","Patricia","Robert","Jennifer","Michael","Elizabeth","William","Linda","David","Barbara","Richard","Susan","Joseph","Jessica","Thomas","Margaret","Charles","Sarah","Christopher","Karen","Daniel","Nancy","Matthew","Betty","Anthony","Dorothy","Donald","Lisa","Mark","Sandra","Paul","Ashley","Steven","Kimberly","George","Donna","Kenneth","Carol","Andrew","Michelle","Joshua","Emily","Edward","Helen","Brian","Amanda","Kevin","Melissa","Ronald","Deborah","Timothy","Stephanie","Jason","Laura","Jeffrey","Rebecca","Ryan","Sharon","Gary","Cynthia","Jacob","Kathleen","Nicholas","Shirley","Eric","Amy","Stephen","Anna","Jonathan","Angela","Larry","Ruth","Scott","Brenda","Frank","Pamela","Justin","Virginia","Brandon","Katherine","Raymond","Nicole","Gregory","Catherine","Samuel","Christine","Benjamin","Samantha","Patrick","Debra","Jack","Janet","Alexander","Carolyn","Dennis","Rachel","Jerry","Heather"};

    private String generate(int min, int max) {
        int l = min + rnd.nextInt(1+Math.max(max-min, 0));
        StringBuilder sb = new StringBuilder(l);
        if (digits || hexa || ascii) {
            char[] array = digits ? digitsArray : (hexa ? hexaArray : asciiArray);
            for (int i = 0; i < l; i++) {
                sb.append(array[rnd.nextInt(array.length)]);
            }
        } else if (cities) {
            sb.append(cityArray[rnd.nextInt(cityArray.length)]);
        } else if (names) {
            sb.append(firstNames[rnd.nextInt(cityArray.length)]).append("").append(surnames[rnd.nextInt(surnames.length)]);
        }
        return sb.toString();
    }

    private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
        layer = context.getLayerableNamePanel().chooseEditableLayer();
        dialog.addLayerComboBox(LAYER, layer, null,
                AttributeTypeFilter.STRING_FILTER.filter(context.getLayerManager().getEditableLayers()));
        dialog.addAttributeComboBox(ATTRIBUTE, LAYER, AttributeTypeFilter.STRING_FILTER, ATTRIBUTE_TT);
        String GROUP = "group";
        dialog.addSubTitle(LETTER_BASED);
        dialog.addIntegerField(MIN_LENGTH, min, 12, MIN_LENGTH_TT);
        dialog.addIntegerField(MAX_LENGTH, max, 12, MAX_LENGTH_TT);
        dialog.addRadioButton(DIGITS, GROUP, digits, null);
        dialog.addRadioButton(HEXA,   GROUP, hexa, null);
        dialog.addRadioButton(ASCII,  GROUP, ascii, null);
        dialog.addSubTitle(WORD_BASED);
        dialog.addRadioButton(CITIES, GROUP, cities, null);
        dialog.addRadioButton(NAMES,  GROUP, names, null);
    }

    private void getDialogValues(MultiInputDialog dialog) {
        layer = dialog.getLayer(LAYER);
        attribute = dialog.getText(ATTRIBUTE);
        min = dialog.getInteger(MIN_LENGTH);
        max = dialog.getInteger(MAX_LENGTH);
        digits = dialog.getBoolean(DIGITS);
        hexa   = dialog.getBoolean(HEXA);
        ascii  = dialog.getBoolean(ASCII);
        cities = dialog.getBoolean(CITIES);
        names  = dialog.getBoolean(NAMES);
    }
}

