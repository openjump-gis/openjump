package com.vividsolutions.jump.workbench.ui;

import com.vividsolutions.jump.util.SuggestTree;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;

/**
 * SuggestTreeComboBox presents a selection of items starting with the
 * same letters as the one in the editor using a SuggestTree (Trie).
 * To be more flexible, suggestions are not limited to words starting
 * with the token typed in the editor but also includes items containing
 * such tokens.
 */
public class SuggestTreeComboBox extends JComboBox<String> {

    private SuggestTree trie;
    private Map<String,String> map;

    public SuggestTreeComboBox(final String[] array, int size) {
        super();
        map = new HashMap<>(array.length);
        this.trie = initTrie(array, size);
        this.setEditable(true);
        this.getEditor().getEditorComponent().addKeyListener(new MyKeyAdapter(this));
    }

    public void changeModel(String[] array) {
        map = new HashMap<>(array.length);
        this.trie = initTrie(array, trie.size());
    }

    private SuggestTree initTrie(String[] array, int size) {
        SuggestTree trie = new SuggestTree(size);
        for (String s : array) {
            String sm = s.toLowerCase();
            map.put(sm, s);
            trie.put(sm, 2);
            for (int split : split(sm)) {
                String permuted = sm.substring(split) + sm.substring(0, split);
                map.put(permuted, s);
                trie.put(permuted, 1);
            }
        }
        return trie;
    }

    private Integer[] split(String s) {
        List<Integer> list = new ArrayList<>();
        for (int i = 1 ; i < s.length() ; i++) {
            if (!Character.isLetter(s.charAt(i-1)) && Character.isLetter(s.charAt(i))) list.add(i);
            if (!Character.isDigit(s.charAt(i-1)) && Character.isDigit(s.charAt(i))) list.add(i);
        }
        return list.toArray(new Integer[list.size()]);
    }

    class MyKeyAdapter extends KeyAdapter {

        final SuggestTreeComboBox cb;

        MyKeyAdapter(SuggestTreeComboBox cb) {
            this.cb = cb;
        }
        @Override
        public void keyReleased(KeyEvent e) {
            super.keyReleased(e);
            if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_UP) {
                cb.getEditor().setItem(
                        cb.getSelectedItem()
                );
                return;
            }
            String newValue = ((String)cb.getEditor().getItem());
            if (newValue.length() == 0) return;
            SuggestTree.Node node = cb.trie.getAutocompleteSuggestions(newValue.toLowerCase());
            if (node == null || node.listLength() == 0) {
                return;
            } else if (node.listLength() == 1) {
                cb.setSelectedItem(map.get(node.getSuggestion(0).getTerm()));
                cb.hidePopup();
            } else {
                Set<String> set = new HashSet<>();
                for (int i = 0; i < node.listLength(); i++) {
                    set.add(map.get(node.getSuggestion(i).getTerm()));
                }
                cb.setModel(new DefaultComboBoxModel<>(set.toArray(new String[set.size()])));
                cb.showPopup();
                cb.getEditor().setItem(newValue);
                // Very strange : if I start OpenJUMP from my IDE with ant, I don't need to do that,
                // but in production, if I don't unselect editor's content, next key will replace
                // previous content, and I can't simply add a letter to the prvious one !
                ((JTextField)cb.editor.getEditorComponent()).select(newValue.length(),newValue.length());
            }
        }
    }

}

