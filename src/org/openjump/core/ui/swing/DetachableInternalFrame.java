package org.openjump.core.ui.swing;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.plaf.InternalFrameUI;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
import javax.swing.plaf.basic.BasicInternalFrameUI;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.workbench.ui.GUIUtil;

/**
 * A detacheable JInternalFrame. DetachableInternalFrame extends JInternalFrame
 * and adds a popup menu to the JInternalFrame's tiltle pane (the northpane).
 * With this popupmenu you can detach this JInternalFrame to a separate JFrame.
 * If the JFrame gets closing, the JInternalFrame comes back into the application.
 * <br/>
 * It is possible to detach and attach without the popup menu. There are public
 * detach() and attach() methods. The detached state is queryable with the
 * isDetached() method.
 *
 * @author Matthias Scholz <ms@jammerhund.de>
 */
public class DetachableInternalFrame extends JInternalFrame {

	private boolean detached = false;
	private JPopupMenu detachPopupMenu = null;
	private JMenuItem detachMenuItem = null;
	private JFrame detachedFrame = null;
	private JRootPane activeRootPane = null;
    private Point internalFrameLocation = null;

	/**
	 * Constructs a detachable JInternalFrame.<br/>
	 * Please read {@link JInternalFrame#JInternalFrame(java.lang.String, boolean, boolean, boolean, boolean) JInternalFrame(java.lang.String, boolean, boolean, boolean, boolean)}
	 */
	public DetachableInternalFrame(String title, boolean resizable, boolean closable, boolean maximizable, boolean iconifiable) {
		super(title, resizable, closable, maximizable, iconifiable);
		init();
	}

	/**
	 * Constructs a detachable JInternalFrame.<br/>
	 * Please read {@link JInternalFrame#JInternalFrame(java.lang.String, boolean, boolean, boolean) JInternalFrame(java.lang.String, boolean, boolean, boolean)}
	 */
	public DetachableInternalFrame(String title, boolean resizable, boolean closable, boolean maximizable) {
		super(title, resizable, closable, maximizable);
		init();
	}

	/**
	 * Constructs a detachable JInternalFrame.<br/>
	 * Please read {@link JInternalFrame#JInternalFrame(java.lang.String, boolean, boolean) JInternalFrame(java.lang.String, boolean, boolean)}
	 */
	public DetachableInternalFrame(String title, boolean resizable, boolean closable) {
		super(title, resizable, closable);
		init();
	}

	/**
	 * Constructs a detachable JInternalFrame.<br/>
	 * Please read {@link JInternalFrame#JInternalFrame(java.lang.String, boolean) JInternalFrame(java.lang.String, boolean)}
	 */
	public DetachableInternalFrame(String title, boolean resizable) {
		super(title, resizable);
		init();
	}

	/**
	 * Constructs a detachable JInternalFrame.<br/>
	 * Please read {@link JInternalFrame#JInternalFrame(java.lang.String) JInternalFrame(java.lang.String)}
	 */
	public DetachableInternalFrame(String title) {
		super(title);
		init();
	}

	/**
	 * Constructs a detachable JInternalFrame.<br/>
	 * Please read {@link JInternalFrame#JInternalFrame() JInternalFrame()}
	 */
	public DetachableInternalFrame() {
		init();
	}

	/**
	 * Initializes and binds the detach popup menu to the NorthPane of the
	 * JInternalFrame.
	 */
	private void init() {
		// build popupmenu
		detachPopupMenu = new JPopupMenu();
		detachMenuItem = new JMenuItem(I18N.get("org.openjump.core.ui.swing.DetachableInternalFrame.detach-window"));
		detachMenuItem.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				detach();
			}
		});
		detachPopupMenu.add(detachMenuItem);

		// add the right click MouseListener to the JInternalFrame's northpane for showing the popup
		InternalFrameUI ifui = this.getUI();
		if (ifui instanceof BasicInternalFrameUI) {
			BasicInternalFrameUI bifui = (BasicInternalFrameUI) ifui;
			JComponent northPane = bifui.getNorthPane();
			if (northPane instanceof BasicInternalFrameTitlePane) {
				northPane.addMouseListener(new MouseAdapter() {

					@Override
					public void mouseClicked(MouseEvent e) {
						super.mouseClicked(e);
						if (SwingUtilities.isRightMouseButton(e)) {
							detachPopupMenu.show((Component) e.getSource(),e.getX(), e.getY());
						}
					}

				});
				
			}
		}
	}

	/**
	 * Detach a this frame. The detached JFrame gets the same size and position
	 * as the JInternalFrame.
	 *
	 * @return true if could be detached and false if the frame was not attached.
	 */
	public boolean detach() {
		if (detached) return false;

		// gets position and size of the internal frame
		Rectangle bounds = this.getBounds();
		Point screenLocation = this.getLocationOnScreen();
		bounds.x = screenLocation.x;
		bounds.y = screenLocation.y;

		// save the rootpane, the content of the JInternalFrame
		activeRootPane = this.getRootPane();

		// switch the internalframe invisible
        /*
         * We do not set this JInternalFrame invisible! This results in a
         * ClassCastException in the AttributeTab class and generates problems
         * with the EnableCheck. Please see Bug Id 3573079
         * The problem is, that an invisible JInternalFrame can't be an active one.
         * The solution is simple. We move this JInternalFrame outside the visible
         * area. And on attach we move it back.
         */
        //this.setVisible(false);
        // save the actual location
        internalFrameLocation = this.getLocation();
        // move it outside
        this.setLocation(0 - bounds.width, 0 - bounds.height);

		// create a new JFrame instance with the content of the internalframe
		detachedFrame = getFrame();

		// keep icon of frame
		detachedFrame.setIconImage(GUIUtil.toImage(this.getFrameIcon()));
		
		// on closing the detached JFrame, we attach it back to the application
		// so we define it here
		detachedFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		detachedFrame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				attach();
			}

		});

		// set the title, position and size for the detached frame and make it visible
		detachedFrame.setTitle(this.getTitle());
		detachedFrame.setBounds(bounds);
		detachedFrame.setVisible(true);

		detached = true;
		return true;
	}

	/**
	 * Attaches this previously detached frame.
	 *
	 * @return true if could be attached and false if the frame was not detached.
	 */
	public boolean attach() {
		if (!detached) return false;

		// destroy the detached frame
		detachedFrame.dispose();
		// restore rootpane
		this.setRootPane(activeRootPane);
		// and make the internal frame visible again
        // PLEASE read the comment in the detach() method too!
        //this.setVisible(true);
        // move it back
        this.setLocation(internalFrameLocation);
		
		detached = false;
		return true;
	}

	/**
	 * @return the detached status
	 */
	public boolean isDetached() {
		return detached;
	}

	/**
	 * Sets the text of the detach popup menu. Default is "detach window".
	 *
	 * @param text
	 */
	public void setDetachMenuItemText(String text) {
		detachMenuItem.setText(text);
	}
	
	public JFrame getFrame() {
		return new JFrame() {

			@Override
			protected JRootPane createRootPane() {
				return DetachableInternalFrame.this.getRootPane();
			}

		};
	}
}
