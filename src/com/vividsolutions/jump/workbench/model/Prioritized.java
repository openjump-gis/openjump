package com.vividsolutions.jump.workbench.model;

import java.util.Comparator;

/**
 * an Interface to implement priority
 */
public interface Prioritized {
  public static int NOPRIORITY = Integer.MAX_VALUE;
  // a comparator respecting prioritized objects
  // non prioritized implementers will be treated as unprioritized
  public static final Comparator COMPARATOR = new Comparator<Object>() {
    public int compare(Object o1, Object o2) {
      int prioint1 = o1 instanceof Prioritized ? ((Prioritized) o1)
          .getPriority() : NOPRIORITY;
      int prioint2 = o2 instanceof Prioritized ? ((Prioritized) o2)
          .getPriority() : NOPRIORITY;

      if (prioint1 < prioint2)
        return -1;
      else if (prioint1 > prioint2)
        return 1;

      return 0;
    }
  };

  public int getPriority();
}
