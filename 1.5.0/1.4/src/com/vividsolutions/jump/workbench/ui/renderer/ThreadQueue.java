/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions 
 * Copyright (C) 2007 Intevation GmbH
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */
package com.vividsolutions.jump.workbench.ui.renderer;

import java.util.LinkedList;
import java.util.ArrayList;

/**
 * This thread queue executes at maximum N Runnables in parallel
 * were N is a given number of worker threads that should be used.
 * If N threads are running and busy each further incoming 
 * Runnable is queued until one of the threads has finished its current job.
 * If a worker thread becomes idle (no more job in the queue)
 * it is hold alive for 5 seconds. If during this period of time 
 * no new Runnable is enqueued the worker thread dies.
 *
 * @author Sascha L. Teichmann (sascha.teichmann@intevation.de)
 */
public class ThreadQueue
{
	/** The time a worker thread stays alive if idle */
	public static final long WORKER_STAY_ALIVE_TIME = 5000L;

	/**
	 * Worker thread. Fetches Runnable from the surrounding 
	 * ThreadQueue instance.
	 */
	protected class Worker 
	extends         Thread
	{
		public void run() {
			try {
				for (;;) {
					Runnable runnable;

					synchronized (queuedRunnables) {
						if (queuedRunnables.isEmpty()) {

							++waitingThreads;
							try {
								queuedRunnables.wait(WORKER_STAY_ALIVE_TIME);
							}
							catch (InterruptedException ie) {
							}
							finally {
								--waitingThreads;
							}

							// if still empty -> die!
							if (queuedRunnables.isEmpty())
								break;
						}
						if (disposed)
							break;
						runnable = (Runnable)queuedRunnables.remove();
					} // synchronized queuedRunnables

					try {
						runnable.run();
					}
					catch (Exception e) {
						e.printStackTrace();
					}

					// check if we are the last of the mohicans ...
					boolean lastRunningThread;
					synchronized (runningThreads) {
						lastRunningThread = runningThreads[0] == 1;
					}
					if (lastRunningThread) {
						fireAllRunningThreadsFinished();
					}
				} // for (;;)
			}
			finally { // guarantee that counter goes down
				synchronized (runningThreads) {
					--runningThreads[0];
				}
			}
		}
	} // class Worker

	/**
	 * If the number of running threads goes down to zero
	 * implementations of this interface are able to be informed.
	 */
	public interface Listener {
		void allRunningThreadsFinished();
	} // interface Listener

	/** Number of running threads */
	protected int [] runningThreads = new int[1];

	/** max. Number of threads running parallel */
	protected int maxRunningThreads;

	/** Number of threads that are currently idle */
	protected int waitingThreads;

	/** The queue of Runnables jobs waiting to be run */
	protected LinkedList queuedRunnables;

	/** Singals that the ThreadQueue is going to quit */
	protected boolean disposed;

	/** List of Listeners */
	protected ArrayList listeners = new ArrayList();

	/**
	 * Creates a ThreadQueue with one worker thread.
	 */
	public ThreadQueue() {
		this(1);
	}

	/** Creates a ThreadQueue with a given number of worker threads.
	 * @param maxRunningThreads the max. number of threads to be run parallel.
	 */
	public ThreadQueue(int maxRunningThreads) {
		this.maxRunningThreads = Math.max(1, maxRunningThreads);
		queuedRunnables = new LinkedList();
	}

	/**
	 * Adds a Listener to this ThreadQueue.
	 * @param listener the listener to add.
	 */
	public synchronized void add(Listener listener) {
		if (listener != null)
			listeners.add(listener);
	}

	/**
	 * Removes a Listener from this ThreadQueue.
	 * @param listener the listener to be removed.
	 */
	public synchronized void remove(Listener listener) {
		if (listener != null)
			listeners.remove(listener);
	}

	/**
	 * Informs Listeners of the fact that the number of running threads
	 * went to zero.
	 */
	protected void fireAllRunningThreadsFinished() {
		ArrayList copy;
		synchronized (this) { copy = new ArrayList(listeners); }
		for (int i = copy.size()-1; i >= 0; --i)
			((Listener)copy.get(i)).allRunningThreadsFinished();
	}


	/**
	 * The number of currently running worker threads.
	 * @return number of currently running worker threads.
	 */
	public int runningThreads() {
		synchronized (runningThreads) {
			return runningThreads[0];
		}
	}

	/**
	 * The number of currently running worker threads.
	 * Alias for runningThreads()
	 * @return number of currently running worker threads.
	 */
	public int getRunningThreads() {
		return runningThreads();
	}

	/**
	 * The number of currently waiting Runnables.
	 * @return number of currently waiting Runnables.
	 */
	public int waitingRunnables() {
		synchronized (runningThreads) {
			return queuedRunnables.size();
		}
	}

	/**
	 * The number of currently idle worker threads.
	 * @return number of currently idle worker threads.
	 */
	public int waitingThreads() {
		synchronized (queuedRunnables) {
			return waitingThreads;
		}
	}

	/**
	 * Adds a Runnables to the queue. It will be run in one
	 * of the worker threads.
	 * @param runnable The Runnables to add
	 */
	public void add(Runnable runnable) {
		int waiting;
		synchronized (queuedRunnables) {
			if (disposed)
				return;
			waiting = waitingThreads;
			queuedRunnables.add(runnable);
			queuedRunnables.notify();
		}  // synchronized (queuedRunnables)

		synchronized (runningThreads) {

			// if waitingThreads == 1 then
			// the queuedRunnables.notify() should have waked it up.

			if (waitingThreads < 2 && runningThreads[0] < maxRunningThreads) {
				++runningThreads[0];
				Worker w = new Worker();
				w.setDaemon(true);
				w.start();
			}
		} // synchronized (runningThreads)
	}

	/**
	 * Empties the queue of waiting Runnables.
	 */
	public void clear() {
		synchronized (queuedRunnables) {
			queuedRunnables.clear();
		}
	}

	/**
	 * Shuts down the ThreadQueue.
	 */
	public void dispose() {
		synchronized (queuedRunnables) {
			disposed = true;
			queuedRunnables.clear();
			// wakeup idle threads
			queuedRunnables.notifyAll();
		}
		synchronized (this) {
			listeners.clear();
		}
	}
}
// end of file
