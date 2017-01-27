/*
 * (c) 2007 by lat/lon GmbH
 *
 * @author Ugo Taddei (taddei@latlon.de)
 *
 * This program is free software under the GPL (v2.0)
 * Read the file LICENSE.txt coming with the sources for details.
 */

package de.latlon.deejump.wfs.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.deegree.enterprise.WebUtils;
import org.deegree.ogcwebservices.wfs.operation.GetFeature;

import com.vividsolutions.jump.workbench.JUMPWorkbench;

import de.latlon.deejump.wfs.client.WFSClientHelper;
import de.latlon.deejump.wfs.client.WFSHttpClient;
import de.latlon.deejump.wfs.client.WFSPostMethod;
import de.latlon.deejump.wfs.deegree2mods.XMLFragment;
import de.latlon.deejump.wfs.i18n.I18N;

/**
 * Shows the GetFeature requests.
 * 
 * @author <a href="mailto:taddei@lat-lon.de">Ugo Taddei</a>
 * @author last edited by: $Author: stranger $
 * 
 * @version $Revision: 1516 $, $Date: 2008-07-17 11:32:16 +0200 (Do, 17 Jul 2008) $
 */
class RequestPanel extends JPanel {

    private static final long serialVersionUID = 8173462624638666293L;

    final WFSPanel wfsPanel;

    JTextArea requestTextArea;

    private JButton createReqButton, responseButton;

    private JButton validateReq;

  RequestPanel(WFSPanel wfsPanel) {
    this.wfsPanel = wfsPanel;

    createRequestButton();

    setLayout(new GridBagLayout());

    requestTextArea = new JTextArea();
    requestTextArea.setLineWrap(true);
    requestTextArea.setWrapStyleWord(true);
    requestTextArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    JScrollPane jsp = new JScrollPane(requestTextArea);

    add(jsp, new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0,
        GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0,
            0), 0, 0));
    
    JPanel buttonPanel = new JPanel(new GridBagLayout());
    buttonPanel.add(createReqButton, new GridBagConstraints(0, 0, 1, 1, 1.0,
        1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0,
            0, 0), 0, 0));
    buttonPanel.add(createValidationButton(), new GridBagConstraints(1, 0, 1,
        1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0));
    buttonPanel.add(createResponseButton(), new GridBagConstraints(2, 0, 1,
        1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0));
    
    add(buttonPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
        GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,
            0, 0, 0), 0, 0));
  }

    private JComponent createValidationButton() {
        validateReq = new JButton( I18N.get( "FeatureResearchDialog.validateRequest" ) );

        validateReq.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {

                String reqTxt = requestTextArea.getText();
                if ( reqTxt == null || reqTxt.length() == 0 ) {
                    return;
                }
                try {

                    // simple test for well-formedness
                    XMLFragment xf = new de.latlon.deejump.wfs.deegree2mods.XMLFragment();
                    xf.load( new StringReader( reqTxt ), "http://empty" );

                    if ( "1.1.0".equals( wfsPanel.wfService.getServiceVersion() ) ) {
                        // use deegree to validate request
                        GetFeature.create( null, xf.getRootElement() );
                    }
                } catch ( Exception ex ) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog( wfsPanel, ex.getMessage(), I18N.get( "error" ),
                                                   JOptionPane.ERROR_MESSAGE );
                }
            }
        } );
        return validateReq;
    }
    
    private JComponent createResponseButton() {
      responseButton = new JButton("response");
      responseButton.setEnabled(false);
  
      responseButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
  
          final String reqTxt = requestTextArea.getText();
          if (reqTxt == null || reqTxt.length() == 0) {
            return;
          }
  
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              // simply read resonse into the appropriate tab
              try {
                HttpClient client = new WFSHttpClient();
                String wfsUrl = wfsPanel.wfService.getGetFeatureURL();
                PostMethod post = new WFSPostMethod(wfsUrl);
                post.setRequestEntity(new StringRequestEntity(reqTxt, "text/xml",
                    "UTF-8"));
      
                WebUtils.enableProxyUsage(client, new URL(wfsUrl));
                int code = client.executeMethod(post);
                
                // detect xml encoding
                PushbackInputStream pbis = new PushbackInputStream(post.getResponseBodyAsStream(), 1024);
                String encoding = WFSClientHelper.readEncoding(pbis);
                InputStreamReader isrd = new InputStreamReader(pbis, encoding);
                
                BufferedReader in = new BufferedReader(isrd);
                StringBuffer body = new StringBuffer();
                String readLine;
                while ((readLine = in.readLine()) != null) {
                  body.append(readLine+"\n");
                }

                wfsPanel.setResponseText(Arrays.toString(post.getRequestHeaders())
                    + "\n\n"
                    + Arrays.toString(post.getResponseHeaders())
                    + "\n\n" + body);
              } catch (Exception ex) {
                JUMPWorkbench.getInstance().getFrame().handleThrowable(ex);
              }
            }
          });

        }
      });
      return responseButton;
    }

    private JComponent createRequestButton() {
        createReqButton = new JButton( I18N.get( "FeatureResearchDialog.createWFSRequest" ) );
        createReqButton.setBounds( 260, 20, 80, 20 );
        createReqButton.setAlignmentX( 0.5f );
        createReqButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                setRequestText( wfsPanel.createRequest() );
                requestTextArea.setCaretPosition( 0 );
                responseButton.setEnabled(true);
            }
        } );
        return createReqButton;
    }

    void setRequestText( String txt ) {
        this.requestTextArea.setText( txt.replaceAll( ">", ">\n" ) );
    }

    String getText() {
        return this.requestTextArea.getText();
    }

    @Override
    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        this.createReqButton.setEnabled( enabled );
        this.validateReq.setEnabled( enabled );
        this.requestTextArea.setEnabled( enabled );
    }

}
