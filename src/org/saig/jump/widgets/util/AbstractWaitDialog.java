/* 
 * Kosmo - Sistema Abierto de Información Geográfica
 * Kosmo - Open Geographical Information System
 *
 * http://www.saig.es
 * (C) 2008, SAIG S.L.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation;
 * version 2.1 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * For more information, contact:
 * 
 * Sistemas Abiertos de Información Geográfica, S.L.
 * Avnda. República Argentina, 28
 * Edificio Domocenter Planta 2ª Oficina 7
 * C.P.: 41930 - Bormujos (Sevilla)
 * España / Spain
 *
 * Teléfono / Phone Number
 * +34 954 788876
 * 
 * Correo electrónico / Email
 * info@saig.es
 *
 */
package org.saig.jump.widgets.util;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.saig.core.util.SwingWorker;

import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.images.IconLoader;

/**
 * Shows a modal dialog while an operating is in process
 * <p>
 * </p>
 * 
 * @author Eduardo Montero Ruiz - emontero@saig.es
 * @since 1.0
 */
public abstract class AbstractWaitDialog extends JDialog {

    /** long serialVersionUID field */
    private static final long serialVersionUID = 1L;

    /** Method execution error message, if any */
    protected String errorMessage = ""; //$NON-NLS-1$

    /**
     * @param parent
     * @param title
     */
    public AbstractWaitDialog( JFrame parent, String title ) {
        super(parent, true);
        getContentPane().setLayout(new BorderLayout());
        setTitle(title);
        JLabel label = new JLabel();
        label.setIcon(IconLoader.icon("saig/loading.gif")); //$NON-NLS-1$
        label.setHorizontalAlignment(SwingConstants.CENTER);
        getContentPane().add(label, BorderLayout.CENTER);
        setSize(new Dimension(200, 100));
        GUIUtil.centreOnWindow(this);
        final SwingWorker worker = new SwingWorker(){
            public Object construct() {
                try {
                    methodToPerform();
                    return null;
                } catch (Exception e) {
                    Logger.error(e);
                    errorMessage = e.getMessage();
                    dispose();
                }
                return null;
            }
            // Runs on the event-dispatching thread.
            public void finished() {
                closeWindow();
            }
        };
        worker.start();
    }

    /**
     * Close the window
     */
    void closeWindow() {
        dispose();
    }

    /**
     * Executes in the wait dialog
     */
    protected abstract void methodToPerform() throws Exception;

    /**
     * Gets the error message associate with the execution, if any
     * 
     * @return String - Empty string if there is no error message, the exception message otherwise
     */
    public String getErrorMessage() {
        return errorMessage;
    }

}