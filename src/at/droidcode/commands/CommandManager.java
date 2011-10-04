package at.droidcode.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Log;
import at.droidcode.threadpaint.TpApplication;

public class CommandManager {
	private int commandIndex;
	private Bitmap originalBitmap;

	private final Paint transparencyPaint;
	private final List<Command> commandStack;

	/**
	 * Enables undo and redo actions via a stack of commands that are applied to an original Bitmap
	 * if a command needs to be un- or redone.
	 * 
	 * @param originalBitmap Bitmap representing the original state of the image. Will be copied.
	 */
	public CommandManager() {
		commandIndex = 0;
		commandStack = Collections.synchronizedList(new ArrayList<Command>());

		transparencyPaint = new Paint();
		transparencyPaint.setColor(Color.TRANSPARENT);
		transparencyPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
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
	 * Draw the Command on a Canvas and push it on the command stack.
	 * 
	 * @param command Command to draw and save.
	 * @param canvas Canvas to draw the command onto, typically is associated with a Bitmap.
	 */
	public void commitCommand(Command command, Canvas canvas) {
		command.draw(canvas);
		if (commandIndex < commandStack.size()) {
			Log.w(TpApplication.TAG, "discard old commands stacksize:" + commandStack.size() + " cmdIdx:"
					+ commandIndex);
			// Some commands have been undone, remove them from the stack first.
			for (int i = commandStack.size(); i > commandIndex; i--) {
				commandStack.remove(commandStack.size() - 1);
				Log.w(TpApplication.TAG, "discard " + i);
			}
		}
		commandIndex++;
		commandStack.add(command);
		Log.w(TpApplication.TAG, "new command stacksize:" + commandStack.size() + " cmdIdx:" + commandIndex);
	}

	/**
	 * Undos the last action as indicated by the command index by applying all commands from 0 to
	 * the current command index to a Canvas.
	 * 
	 * @param canvas Canvas to apply commands to.
	 */
	public void undoLast(Canvas canvas) {
		if (commandIndex > 0) {
			// clear canvas and redraw original
			canvas.drawPaint(transparencyPaint);
			canvas.drawBitmap(originalBitmap, 0, 0, null);
			commandIndex--;
			for (int i = 0; i < commandIndex; i++) {
				commandStack.get(i).draw(canvas);
			}
			Log.w(TpApplication.TAG, "undo stacksize:" + commandStack.size() + " cmdIdx:" + commandIndex);
		} else {
			Log.w(TpApplication.TAG, "cannot undo anymore stacksize:" + commandStack.size() + " cmdIdx:" + commandIndex);
		}
	}

	public void redoLast(Canvas canvas) {
		if (commandIndex < commandStack.size()) {
			commandStack.get(commandIndex).draw(canvas);
			commandIndex++;
			Log.w(TpApplication.TAG, "redo stacksize:" + commandStack.size() + " cmdIdx:" + commandIndex);
		} else {
			Log.w(TpApplication.TAG, "cannot redo anymore stacksize:" + commandStack.size() + " cmdIdx:" + commandIndex);
		}
	}
}
