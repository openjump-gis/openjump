package com.vividsolutions.jump.workbench.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.Iterator;


/**
 * Keeps registry of action listeners, triggers each of them on demand 
 * with a given ActionEvent.
 */


public class ActionEventFirer {
    private ArrayList actionListeners = new ArrayList();

    public void add(ActionListener listener) {
        actionListeners.add(listener);
    }

    public void remove(ActionListener listener) {
        actionListeners.remove(listener);
    }

    public void fire(Object source, int id, String command) {
        for (Iterator i = actionListeners.iterator(); i.hasNext();) {
            ActionListener listener = (ActionListener) i.next();
            listener.actionPerformed(new ActionEvent(source, id, command));
        }
    }
}
