package org.openjump.sextante.core;

/**
 * Refractor of Sextante class es.unex.sextante.core.ObjectAndDescription from
 * lib Sextante.jar. This class creates an object defined by the component
 * description and the component . Ex: ObjectAndDescription oa = new
 * ObjectAndDescription(name, component); where name=String and component could
 * be JPanel, JTextArea, JTextPane, JLabel, JTable, HTMLPanel , PlotPanel
 * 
 * 
 * @author Giuseppe Aruta [2017-12-12]
 *
 */
public class ObjectAndDescription implements Comparable<Object> {

    private String m_sDescription;
    private Object m_Object;

    public ObjectAndDescription(final String sDescription, final Object object) {

        // String timeStamp = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
        // .format(Calendar.getInstance().getTime());
        // m_sDescription = sDescription + "_" + timeStamp;
        m_sDescription = sDescription;
        m_Object = object;

    }

    /**
     * gets the component (JPanel, JTextArea, JTextPane, JLabel, JTable,
     * HTMLPanel , PlotPanel)
     * 
     * @return
     */
    public Object getObject() {

        return m_Object;

    }

    /**
     * sets the component (JPanel, JTextArea, JTextPane, JLabel, JTable,
     * HTMLPanel , PlotPanel)
     * 
     * @return
     */
    public void setObject(final Object object) {

        m_Object = object;

    }

    /**
     * gets the description
     * 
     * @return
     */
    public String getDescription() {

        return m_sDescription;

    }

    /**
     * sets the description
     * 
     * @return
     */
    public void setDescription(final String sDescription) {

        m_sDescription = sDescription;

    }

    /**
     * gets the description
     * 
     * @return
     */

    @Override
    public String toString() {

        return getDescription();

    }

    /**
     * Compare two ObjectAndDescription objects using their descriptions
     */
    @Override
    public int compareTo(final Object arg0) {

        return getDescription().compareTo(
                ((ObjectAndDescription) arg0).getDescription());

    }

    /**
     * Compare two ObjectAndDescription objects using their descriptions and
     * their components
     */

    @Override
    public boolean equals(final Object ob) {

        if (ob == null) {
            return false;
        }

        try {
            final ObjectAndDescription oad = (ObjectAndDescription) ob;
            boolean bObjsEqual;
            boolean bDescsEqual;
            if (oad.getObject() == null) {
                bObjsEqual = m_Object == null;
            } else {
                bObjsEqual = oad.getObject().equals(m_Object);
            }
            if (oad.getDescription() == null) {
                bDescsEqual = m_sDescription == null;
            } else {
                bDescsEqual = oad.getDescription().equals(m_sDescription);
            }
            return bObjsEqual && bDescsEqual;
        } catch (final Exception e) {
            return false;
        }

    }

}
