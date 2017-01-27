package com.vividsolutions.jump.workbench.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import com.vividsolutions.jump.workbench.Logger;



public class InternalFrameTest {


	public static InternalFrameListener listener = new InternalFrameListener() {
		public void internalFrameOpened(InternalFrameEvent e) {
			Logger.debug(new Date()+ "public void internalFrameOpened(InternalFrameEvent e) {");
		}

		public void internalFrameClosing(InternalFrameEvent e) {
			Logger.debug(new Date()+ "public void internalFrameClosing(InternalFrameEvent e) {");
		}

		public void internalFrameClosed(InternalFrameEvent e) {
			Logger.debug(new Date()+ " public void internalFrameClosed(InternalFrameEvent e) {");
		}

		public void internalFrameIconified(InternalFrameEvent e) {
			Logger.debug(new Date()+ "public void internalFrameIconified(InternalFrameEvent e) {");
		}

		public void internalFrameDeiconified(InternalFrameEvent e) {
			Logger.debug(new Date()+ "public void internalFrameDeiconified(InternalFrameEvent e) {");
		}

		public void internalFrameActivated(InternalFrameEvent e) {
			Logger.debug(new Date()+ "public void internalFrameActivated(InternalFrameEvent e) {");
		}

		public void internalFrameDeactivated(InternalFrameEvent e) {
			Logger.debug(new Date()+ "public void internalFrameDeactivated(InternalFrameEvent e) {");
		}
	};

	public static void main(String[] args) {
		JInternalFrame internalFrame = new JInternalFrame("Test", true, true,
				true, true);
		internalFrame.setSize(100, 100);

		JDesktopPane desktopPane = new JDesktopPane();
		desktopPane.add(internalFrame);

		JFrame frame = new JFrame();
		frame.setSize(400, 400);
		frame.getContentPane().add(desktopPane);
		frame.setVisible(true);
		internalFrame.setVisible(true);

		//internalFrame.addInternalFrameListener(listener);

		GUIUtil.addInternalFrameListener(desktopPane, GUIUtil
				.toInternalFrameListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						Logger.debug(new Date().toString());
					}
				}));
	}
}