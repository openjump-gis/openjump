package org.openjump.core.model;

import java.util.EventListener;

/**
 * An interface wich must be implemented from the tasklistener.
 *
 * @author Matthias Scholz
 *
 *
 */
public interface TaskListener extends EventListener {

	/**
	 * This method will be called if a new Task was added via the WorkbenchFrame.addTaskFrame(TaskFrame taskFrame) method.
	 *
	 * @param taskEvent - The TaskEvent.
	 */
	public void taskAdded(TaskEvent taskEvent);

	/**
	 * This method will be called after a Task (the project file) was loaded.
	 *
	 * @param taskEvent - The TaskEvent.
	 */
	public void taskLoaded(TaskEvent taskEvent);


}
