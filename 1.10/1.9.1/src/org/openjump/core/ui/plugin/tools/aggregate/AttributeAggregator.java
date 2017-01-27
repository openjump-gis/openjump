package org.openjump.core.ui.plugin.tools.aggregate;

/**
 * A wrapper class including :
 * <ul>
 *     <li>name of the input attribute</li>
 *     <li>aggregator to be used to aggregate values</li>
 *     <li>name of the output attribute</li>
 * </ul>
 */
public class AttributeAggregator {

    private String inputName;
    private Aggregator aggregator;
    private String outputName;

    public AttributeAggregator(String inputName, Aggregator aggregator, String outputName) {
        this.inputName = inputName;
        this.aggregator = aggregator;
        this.outputName = outputName;
    }

    public String getInputName() {
        return inputName;
    }

    public Aggregator getAggregator() {
        return aggregator;
    }

    public String getOutputName() {
        return outputName;
    }
}
