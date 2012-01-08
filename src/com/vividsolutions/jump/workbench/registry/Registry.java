package com.vividsolutions.jump.workbench.registry;

import java.util.*;

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
	
    private CollectionMap classificationToEntriesMap = new CollectionMap();

    /**
     * @param classification
     * @param entry
     * @return The current Registry
     * 
     * @throws ClassCastException When the entry does not match a registered Classification Type
     * @see Registry#createClassification(Object, Class)
     */
    public Registry createEntry(Object classification, Object entry) throws ClassCastException{
    	Class c = (Class) typeMap.get(classification);
    	if(c != null){
    		// check class type
    		if(!c.isInstance(entry)){
    			throw new ClassCastException("Cannot Cast '"+entry+"' into "+c.getName()+" for classification '"+classification+"'");
    		}
    	}
        classificationToEntriesMap.addItem(classification, entry);
        return this;
    }

    /**
     * @param classification
     * @param entries
     * @return The current Registry
     * 
     * @throws ClassCastException When the entries do not match a registered Classification Type
     * @see Registry#createClassification(Object, Class)
     */
    public Registry createEntries(Object classification, Collection entries) throws ClassCastException {
    	for (Iterator i = entries.iterator(); i.hasNext(); ) {
    		createEntry(classification, i.next());    		
    	}
        return this;
    }

    public List getEntries(Object classification) {
        return (List) new ArrayList(classificationToEntriesMap.getItems(classification));
    }
    
    // new api
    private Map typeMap = new HashMap(); // HashMap<Object,Class> --- use this for templating in jvm1.5
    
    /**
     * Sets up the registry to be type-safe for a particular classification. 
     * Should the user not specify a type mappingthrough this method, no 
     * checks will be performed.
     * 
     * @param classification
     * @param type
     * @return The current Registry
     * 
     * @throws ClassCastException When the existing entries do not match Classification Type being registered.
     */
    public Registry createClassification(Object classification, Class type) throws ClassCastException{
    	if(classificationToEntriesMap.containsKey(classification)){
    		// need to check here
    		Collection c = classificationToEntriesMap.getItems(classification);
    		if(c!=null){
    			for(Iterator i=c.iterator();i.hasNext();){
    				Object entry = i.next();
    	    		if(!type.isInstance(entry)){
    	    			throw new ClassCastException("Cannot Cast '"+entry+"' into "+type.getName()+" for classification '"+classification+"'");
    	    		}
    			}
    		}
    	}
    	typeMap.put(classification,type);
    	return this;
    }
}