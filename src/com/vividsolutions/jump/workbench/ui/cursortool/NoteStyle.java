package com.vividsolutions.jump.workbench.ui.cursortool;

import java.awt.*;
import java.awt.geom.Point2D;

import javax.swing.*;
import javax.swing.JLabel;
import javax.swing.JTextArea;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.renderer.style.Style;

public class NoteStyle implements Style {
	
	public static final int WIDTH = 80;
	public static final int HEIGHT = 30;
	public static final String CREATED = I18N.get("ui.cursortool.NoteStyle.CREATED");
	public static final String MODIFIED = I18N.get("ui.cursortool.NoteStyle.MODIFIED");
	public static final String TEXT = I18N.get("ui.cursortool.NoteStyle.TEXT");
	public static final String GEOMETRY = "GEOMETRY";
   	
	private JTextArea myTextArea = createTextArea();
	
	private Layer layer;
	
	public NoteStyle() {
	}
	
	public static FeatureSchema createFeatureSchema() {
		return new FeatureSchema() {   				
			{
				addAttribute(CREATED, AttributeType.DATE);
				addAttribute(MODIFIED, AttributeType.DATE);
				addAttribute(TEXT, AttributeType.STRING);
				addAttribute(GEOMETRY, AttributeType.GEOMETRY);
			}
		};
	}
		
    public static JTextArea createTextArea() {
        return new JTextArea() {
            {
                setFont(new JLabel().getFont());
                setLineWrap(true);
                setWrapStyleWord(true);
                setBorder(BorderFactory.createLineBorder(Color.lightGray));
            }
        };
    }
    
	public void paint(Feature f, Graphics2D g, Viewport viewport) throws Exception {
		paint(f, viewport.toViewPoint(f.getGeometry().getCoordinate()), g);
	}
	
	private void paint(Feature f, Point2D location, Graphics2D g) {
		myTextArea.setText(f.getString(TEXT));
		int ht = myTextArea.getPreferredSize().height;
		int wt = myTextArea.getPreferredSize().width;
		if (ht < HEIGHT) ht = HEIGHT;
		if (wt < WIDTH) wt = WIDTH;
		myTextArea.setBounds(0, 0, wt, ht);
		//myTextArea.setBounds(0, 0, WIDTH, HEIGHT);
		Composite originalComposite = g.getComposite();
		g.translate(location.getX(), location.getY());
		try {
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
					layer.getBasicStyle().getAlpha() / 255f));
			myTextArea.paint(g);
		} finally {
			g.setComposite(originalComposite);
			g.translate(-location.getX(), -location.getY());
		}
	}
	
	public void initialize(Layer layer) {
		this.layer = layer;
		myTextArea.setBackground(layer.getBasicStyle().getFillColor());
	}
	
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			Assert.shouldNeverReachHere();
			return null;
		}
	}
	
	public void setEnabled(boolean enabled) {
	    // [mmichaud 2011-09-30] fix bug 3415409
	    // java2xml serialization needs this method
		//throw new UnsupportedOperationException();
    }
	
	public boolean isEnabled() {
		return true;
	}
	
}
