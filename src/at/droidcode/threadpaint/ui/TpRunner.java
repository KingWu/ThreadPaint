package at.droidcode.threadpaint.ui;

import static at.droidcode.threadpaint.TpApplication.TAG;

import java.lang.Thread.State;

import android.util.Log;
import at.droidcode.threadpaint.TpApplication;

public class TpRunner {
	protected final Thread pThread;
	private Runnable pRunnable;
	private boolean running;
	private boolean paused;

	/**
	 * Wrapper for a Thread executing an arbitrary Runnable.
	 */
	public TpRunner() {
		pThread = new Thread(new InternalRunnable());
		pThread.setDaemon(true);
	}

	private class InternalRunnable implements Runnable {
		@Override
		public void run() {
			internalRun();
		}
	}

	/**
	 * Continously execute the Runnable.
	 */
	private void internalRun() {
		while (running) {
			synchronized (pThread) {
				if (paused) {
					try {
						pThread.wait();
					} catch (InterruptedException e) {
						Log.e(TpApplication.TAG, "ERROR ", e);
					}
				} else {
					pRunnable.run();
				}
			}
		}
	}

	/**
	 * @param runnable Runnable to be executed by the internal Thread.
	 */
	public synchronized void setRunnable(Runnable runnable) {
		pRunnable = runnable;
	}

	/**
	 * Start the internal Thread or unpause it. Does nothing if the Runnable was not set or the
	 * internal Thread has already been stopped.
	 */
	public synchronized void start() {
		if (pRunnable == null || pThread.getState().equals(State.TERMINATED)) {
			return;
		}
		running = true;
		if (paused) {
			setPaused(false);
		} else {
			pThread.start();
		}
	}

	/**
	 * Stop the internal Thread.
	 */
	public synchronized void stop() {
		setPaused(false);
		running = false;
		if (pThread.isAlive()) {
			boolean retry = true;
			while (retry) {
				try {
					pThread.join();
					retry = false;
				} catch (InterruptedException e) {
					Log.e(TAG, "ERROR ", e);
				}
			}
		}
	}

	/**
	 * Cause the internal Thread to wait or resume. Passing false if the Thread is paused will call
	 * notify() on the monitor.
	 * 
	 * @param pause true to pause drawing, false to resume
	 */
	public synchronized void setPaused(boolean pause) {
		synchronized (pThread) {
			if (!pause && paused) {
				pThread.notify();
			}
			paused = pause;
		}
	}
}
