/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
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
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */
package com.vividsolutions.jump.workbench.ui;

import com.vividsolutions.jts.util.Assert;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;


/**
 * Prevents the user from entering invalid data.
 */
public class ValidatingTextField extends JTextField {
    public static final Validator LONG_VALIDATOR = new ValidatingTextField.Validator() {
        public boolean isValid(String text) {
            try {
                Long.parseLong(text.trim() + "0");

                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    };

    /**
     * Prevents the user from entering invalid integer.
     */
    public static final Validator INTEGER_VALIDATOR = new ValidatingTextField.Validator() {
        public boolean isValid(String text) {
            try {
                //Add "0" so user can type "-" or make it blank [Jon Aquino]
                Integer.parseInt(text.trim() + "0");

                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    };

    /**
     * Prevents the user from entering invalid double.
     */
    public static final Validator DOUBLE_VALIDATOR = new ValidatingTextField.Validator() {
        public boolean isValid(String text) {
            try {
                //Add "0" so user can type "-" or make it blank [Jon Aquino]
                Double.parseDouble(text.trim() + "0");

                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    };

    /**
     * Cleaner that does nothing.
     */
    public static Cleaner DUMMY_CLEANER = new Cleaner() {
        public String clean(String text) {
            return text;
        }
    };

    /**
     * The validators allow the user to simply enter "+", "-", or ".". If the
     * user doesn't go any farther, this cleaner will set the text to 0, which
     * is reasonable.
     */
    public static Cleaner NUMBER_CLEANER = new Cleaner() {
        public String clean(String text) {
            try {
                Double.parseDouble(text.trim());

                return text;
            } catch (NumberFormatException e) {
                return "0";
            }
        }
    };

    /**
     * Validator that does nothing.
     */
    public static Validator DUMMY_VALIDATOR = new Validator() {
        public boolean isValid(String text) {
            return true;
        }
    };

    private Cleaner cleaner;

    /**
     * Validator that uses dummy cleaner.
     */
    public ValidatingTextField(String text, int columns,
            final Validator validator) {
        this(text, columns, LEFT, validator, DUMMY_CLEANER);
    }

    /**
     * Validator for text fields.
     */
    public ValidatingTextField(String text, int columns,
            int horizontalAlignment, final Validator validator,
            final Cleaner cleaner) {
        super(text, columns);
        this.cleaner = cleaner;
        setHorizontalAlignment(horizontalAlignment);
        installValidationBehavior(this, validator, cleaner);

        // Clean the text, mainly so that parties wishing to install a
        // BlankCleaner need only pass "" for the text. [Jon Aquino]
        setText(cleaner.clean(getText()));

        //Bonus: workaround for how GridBagLayout shrinks components to
        //minimum sizes if it can't accomodate their preferred sizes.
        //[Jon Aquino]
        setMinimumSize(getPreferredSize());
    }

    //Hopefully this will let us add validation behaviour to combo boxes.
    //[Jon Aquino]
    public static void installValidationBehavior(final JTextField textField,
            final Validator validator, final Cleaner cleaner) {
        final boolean[] validating = new boolean[] { true };
        textField.setDocument(new PlainDocument() {
            public void insertString(int offs, String str, AttributeSet a)
                    throws BadLocationException {
                if (!validating[0]) {
                    super.insertString(offs, str, a);
                    return;
                }
                String currentText = this.getText(0, getLength());
                String beforeOffset = currentText.substring(0, offs);
                String afterOffset = currentText.substring(offs, currentText
                        .length());
                String proposedResult = beforeOffset + str + afterOffset;
                if (validator.isValid(cleaner.clean(proposedResult))) {
                    super.insertString(offs, str, a);
                }
            }

            public void remove(int offs, int len) throws BadLocationException {
                if (!validating[0]) {
                    super.remove(offs, len);
                    return;
                }
                String currentText = this.getText(0, getLength());
                String beforeOffset = currentText.substring(0, offs);
                String afterOffset = currentText.substring(len + offs,
                        currentText.length());
                String proposedResult = beforeOffset + afterOffset;
                if (validator.isValid(cleaner.clean(proposedResult))) {
                    super.remove(offs, len);
                }
            }
        });
        textField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                // Added validating flag to fix bug: on losing focus, #setText
                // called #remove, which cleared the text, which then failed
                // integer validation (being an empty string), which cancelled
                // the remove, causing duplicate strings to appear.
                // [Jon Aquino 2005-03-07]
                validating[0] = false;
                try {
                    textField.setText(cleaner.clean(textField.getText()));
                } finally {
                    validating[0] = true;
                }
            }
        });
    }

    public String getText() {
        //Focus may not be lost yet (e.g. when syncing with scrollbar) [Jon
        // Aquino]
        return cleaner.clean(super.getText());
    }

    public double getDouble() {
        return Double.parseDouble(getText().trim());
    }

    public int getInteger() {
        return Integer.parseInt(getText().trim());
    }

    public static interface Validator {
        public boolean isValid(String text);
    }

    public static interface Cleaner {
        public String clean(String text);
    }

    /**
     * Implements validator with a greater than threshold.
     */

    public static class GreaterThanValidator implements Validator {
        private double threshold;

        public GreaterThanValidator(double threshold) {
            this.threshold = threshold;
        }

        public boolean isValid(String text) {
            try {
                return Double.parseDouble(text.trim()) > threshold;
            } catch (NumberFormatException e) {
                //Handle -, ., E [Jon Aquino 2005-03-17]
                return true;
            }
        }
    }

    /**
     * Implements validator with a less than threshold.
     */

    public static class LessThanValidator implements Validator {
        private double threshold;

        public LessThanValidator(double threshold) {
            this.threshold = threshold;
        }

        public boolean isValid(String text) {
            try {
                return Double.parseDouble(text.trim()) < threshold;
            } catch (NumberFormatException e) {
                return true;
            }
        }
    }

    /**
     * Implements validator with a greater than or equal to threshold.
     */

    public static class GreaterThanOrEqualValidator implements Validator {
        private double threshold;

        public GreaterThanOrEqualValidator(double threshold) {
            this.threshold = threshold;
        }

        public boolean isValid(String text) {
            try {
                return Double.parseDouble(text.trim()) >= threshold;
            } catch (NumberFormatException e) {
                return true;
            }
        }
    }

    /**
     * Implements validator with a less than or equal to threshold.
     */

    public static class LessThanOrEqualValidator implements Validator {
        private double threshold;

        public LessThanOrEqualValidator(double threshold) {
            this.threshold = threshold;
        }

        public boolean isValid(String text) {
            try {
                return Double.parseDouble(text.trim()) <= threshold;
            } catch (NumberFormatException e) {
                return true;
            }
        }
    }

    /**
     * Leave untouched the really good stuff and the really bad stuff, but
     * replace the sort-of good stuff, as it's probably just transient.
     */
    public static class NumberCleaner implements ValidatingTextField.Cleaner {
        private String replacement;

        public NumberCleaner(String replacement) {
            this.replacement = replacement;
        }

        public String clean(String text) {
            try {
                Double.parseDouble(text.trim());
                return text.trim();
            } catch (NumberFormatException e) {
                try {
                    //Handle -, ., E [Jon Aquino 2004-08-04]
                    Double.parseDouble(text.trim() + "0");
                    return replacement;
                } catch (NumberFormatException e2) {
                    return text.trim();
                }
            }
        }

        protected String getReplacement() {
            return replacement;
        }
    }

    public static class BlankCleaner implements ValidatingTextField.Cleaner {
        private String replacement;

        public BlankCleaner(String replacement) {
            this.replacement = replacement;
        }

        public String clean(String text) {
            return (text.trim().length() == 0) ? getReplacement() : text;
        }

        protected String getReplacement() {
            return replacement;
        }
    }

    public static class MinIntCleaner implements Cleaner {
        private int minimum;

        public MinIntCleaner(int minimum) {
            this.minimum = minimum;
        }

        public String clean(String text) {
            return "" + Math.max(minimum, Integer.parseInt(text));
        }
    }

    /**
     * Extends CompositeValidator to validat that integers is within a set of
     * boundary values.
     */
    public static class BoundedIntValidator extends CompositeValidator {
        public BoundedIntValidator(int min, int max) {
            super(new Validator[] { INTEGER_VALIDATOR,
                    new GreaterThanOrEqualValidator(min),
                    new LessThanOrEqualValidator(max) });
            Assert.isTrue(min < max);
        }
    }

    public static class BoundedDoubleValidator extends CompositeValidator {
        public BoundedDoubleValidator(double min, boolean includeMin,
                double max, boolean includeMax) {
            super(new Validator[] {
                    DOUBLE_VALIDATOR,
                    includeMin ? (Validator) new GreaterThanOrEqualValidator(
                            min) : new GreaterThanValidator(min),
                    includeMax ? (Validator) new LessThanOrEqualValidator(max)
                            : new LessThanValidator(max) });
            Assert.isTrue(min < max);
        }
    }

    public static class MaxIntCleaner implements Cleaner {
        private int maximum;

        public MaxIntCleaner(int maximum) {
            this.maximum = maximum;
        }

        public String clean(String text) {
            return "" + Math.min(maximum, Integer.parseInt(text));
        }
    }

    /**
     * Implements validator to check for more than one condition.
     */

    public static class CompositeValidator implements Validator {
        private Validator[] validators;

        public CompositeValidator(Validator[] validators) {
            this.validators = validators;
        }

        public boolean isValid(String text) {
            for (int i = 0; i < validators.length; i++) {
                if (!validators[i].isValid(text)) {
                    return false;
                }
            }

            return true;
        }
    }

    public static class CompositeCleaner implements Cleaner {
        private Cleaner[] cleaners;

        public CompositeCleaner(Cleaner[] cleaners) {
            this.cleaners = cleaners;
        }

        public String clean(String text) {
            String result = text;
            for (int i = 0; i < cleaners.length; i++) {
                result = cleaners[i].clean(result);
            }

            return result;
        }
    }
}