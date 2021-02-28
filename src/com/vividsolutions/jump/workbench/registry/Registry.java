package com.vividsolutions.jump.workbench.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jump.util.CollectionMap;

/**
 * While a Registry is similar to a Blackboard in that they are both flexible
 * repositories of information, there are some subtle differences:
 * <ul>
 * <li>The Registry is a bit more structured (values are Lists as opposed to
 * general Objects).
 * <li>There is only one Registry, whereas there are Blackboards on several
 * different levels (the Workbench Blackboard, the Task Blackboard, the Layer
 * Blackboard, the LayerViewPanel Blackboard), thus representing varying degrees
 * of scope.
 * <li>Registry keys are in general "well known" to a greater degree than
 * Blackboard keys, which plugins tend to create as needed. Thus the Registry
 * can be thought of as being more static, and the Blackboard more fluid.
 * <li>Registry entries are intended to be much more static than Blackboard
 * entries. You might well think about persisting a Registry, but probably never
 * a Blackboard
 * <li>In the bigger world, Registries have all kinds of security,
 * classification and lifecyle features that probably would not appear on a
 * Blackboard.
 * </ul>
 * 
 * @author jaquino
 * @author dzwiers
 */
public class Registry {

  // CollectionMap containing the data
  final private CollectionMap<String,Object> classificationToEntriesMap = new CollectionMap<>();

  // a Map making it possible to define defferent data types for each entry of the registry
  final private Map<String,Class<?>> typeMap = new HashMap<>();

  /**
   * Creating an entry in a registry classification means adding the entry
   * to the Collection associated to this classification.
   * If the classification doesn't exist yet, it is created.
   * The entry must be an instance of the class defined by typeMap for this
   * classification.
   *
   * @param classification the string to be used as a key of the registry
   * @param entry a new entry
   * @return The current Registry
   * 
   * @throws ClassCastException
   *           When the entry does not match a registered Classification Type
   * @see Registry#createClassification(String, Class)
   */
  public Registry createEntry(String classification, Object entry)
      throws ClassCastException {
    Class<?> c = typeMap.get(classification);
    if (c != null) {
      // check class type
      if (!c.isInstance(entry)) {
        throw new ClassCastException("Cannot Cast '" + entry + "' into "
            + c.getName() + " for classification '" + classification + "'");
      }
    }

    classificationToEntriesMap.addItem(classification, entry);
    return this;
  }

  /**
   * @param classification the string to be used as a key of the registry
   * @param entries entry collection to associate to this classification
   * @return The current Registry
   * 
   * @throws ClassCastException
   *           When the entries do not match a registered Classification Type
   * @see Registry#createClassification(String, Class)
   */
  public Registry createEntries(String classification, Collection<?> entries)
      throws ClassCastException {
    for (Object entry : entries) {
      createEntry(classification, entry);
    }
    return this;
  }

  public List getEntries(String classification) {
    return new ArrayList<>(
        classificationToEntriesMap.getItems(classification));
  }

  /**
   * Sets up the registry to be type-safe for a particular classification.
   * Should the user not specify a type mappingthrough this method, no checks
   * will be performed.
   * 
   * @param classification the string to be used as a key of the registry
   * @param type type of entries in the collection ossociated to this classification
   * @return The current Registry
   * 
   * @throws ClassCastException
   *           When the existing entries do not match Classification Type being
   *           registered.
   */
  public Registry createClassification(String classification, Class<?> type)
      throws ClassCastException {
    if (classificationToEntriesMap.containsKey(classification)) {
      // need to check here
      Collection<?> collection = classificationToEntriesMap.getItems(classification);
      if (collection != null) {
        for (Object entry : collection) {
          if (!type.isInstance(entry)) {
            throw new ClassCastException("Cannot Cast '" + entry + "' into "
                + type.getName() + " for classification '" + classification
                + "'");
          }
        }
      }
    }
    typeMap.put(classification, type);
    return this;
  }

}