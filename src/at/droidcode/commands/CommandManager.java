package at.droidcode.commands;

import java.util.LinkedList;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

public class CommandManager {
	private Bitmap originalBitmap;
	private int commandIndex;
	// private boolean running;

	public Thread thread;
	// private final Runnable internalRunnable;
	private final Paint transparencyPaint;
	private final LinkedList<Command> commandStack;

	// private final LinkedList<Runnable> commandQueue;

	// private class InternalRunnable implements Runnable {
	// @Override
	// public void run() {
	// internalRun();
	// }
	// }

	/**
	 * Enables undo and redo actions via a stack of commands that are applied to an original Bitmap
	 * if a command needs to be un- or redone.
	 * 
	 * @param originalBitmap Bitmap representing the original state of the image. Will be copied.
	 */
	public CommandManager() {
		commandIndex = 0;
		commandStack = new LinkedList<Command>();
		// commandQueue = new LinkedList<Runnable>();
		// internalRunnable = new InternalRunnable();

		transparencyPaint = new Paint();
		transparencyPaint.setColor(Color.TRANSPARENT);
		transparencyPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
	}

	// public void start() {
	// if (!running) {
	// thread = new Thread(internalRunnable);
	// thread.setDaemon(true);
	// running = true;
	// thread.start();
	// }
	// }

	/**
	 * Clear Bitmap and command stack.
	 */
	public void clear() {
		originalBitmap.recycle();
		originalBitmap = null;
		commandStack.clear();
		commandIndex = 0;
		// running = false;
	}

	/**
	 * Clear the command stack and set an original Bitmap.
	 * 
	 * @param originalBitmap Bitmap representing the original state of the image. Will be copied.
	 */
	public void reset(Bitmap originalBitmap) {
		if (this.originalBitmap != null) {
			this.originalBitmap.recycle();
			commandStack.clear();
			commandIndex = 0;
		}
		this.originalBitmap = originalBitmap.copy(Config.ARGB_8888, true);
	}

	/**
	 * Apply the Command to the supplied Canvas and push it on the command stack. Any undone
	 * commands one the command stack will be discarded.
	 * 
	 * @param command Command to draw and save.
	 * @param canvas Canvas to draw the command onto, typically is associated with a Bitmap.
	 */
	public synchronized void commitCommand(Command command, Canvas canvas) {
		command.setCanvas(canvas);
		command.run(); // do on ui thread, no queue
		// Push the command into the queue to be executed by the internal Thread.
		// synchronized (commandQueue) {
		// commandQueue.addFirst(command);
		// commandQueue.notify();
		// }

		if (commandIndex < commandStack.size()) {
			// Remove remaining undone commands from the stack first.
			for (int i = commandStack.size(); i > commandIndex; i--) {
				commandStack.removeLast();
			}
		}
		commandIndex++;
		commandStack.add(command);
	}

	/**
	 * Undos the last action as indicated by the command index by redrawing the original Bitmap and
	 * applying all commands from 0 to the current command index to the supplied Canvas.
	 * 
	 * @param canvas Canvas to apply commands to, typically is associated with a Bitmap.
	 */
	public synchronized void undoLast(Canvas canvas) {
		if (commandIndex > 0) {
			// clear canvas and redraw original
			canvas.drawPaint(transparencyPaint);
			canvas.drawBitmap(originalBitmap, 0, 0, null);
			commandIndex--;
			// synchronized (commandQueue) {
			for (int i = 0; i < commandIndex; i++) {
				Command command = commandStack.get(i);
				command.run(); // do on ui thread, no queue
				// commandQueue.addFirst(command);
				// commandQueue.notify();
			}
			// }
		}
	}

	/**
	 * Redos the last undone action from the command stack.
	 * 
	 * @param canvas Canvas to apply commands to, typically is associated with a Bitmap.
	 */
	public synchronized void redoLast(Canvas canvas) {
		if (commandIndex < commandStack.size()) {
			// synchronized (commandQueue) {
			Command command = commandStack.get(commandIndex);
			command.run(); // do on ui thread, no queue
			// commandQueue.addFirst(command);
			// commandQueue.notify();
			// }
			commandIndex++;
		}
	}

	// Used by the internal Thread to retrieve a command from the queue.
	// Lets the Thread sleep if the queue is empty.
	// private Runnable getNextCommand() {
	// synchronized (commandQueue) {
	// if (commandQueue.isEmpty()) {
	// try {
	// commandQueue.wait();
	// } catch (InterruptedException e) {
	// Log.e(TAG, "ERROR ", e);
	// stop();
	// }
	// }
	// return commandQueue.removeLast();
	// }
	// }

	// Continually execute the commands waiting in the queue.
	// private void internalRun() {
	// while (running) {
	// Runnable command = getNextCommand();
	// command.run();
	// }
	// }
}
