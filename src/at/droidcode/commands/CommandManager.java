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
	private int commandIndex;
	private Bitmap originalBitmap;

	private final Paint transparencyPaint;
	private final LinkedList<Command> commandStack;

	/**
	 * Enables undo and redo actions via a stack of commands that are applied to an original Bitmap
	 * if a command needs to be un- or redone.
	 * 
	 * @param originalBitmap Bitmap representing the original state of the image. Will be copied.
	 */
	public CommandManager() {
		commandIndex = 0;
		commandStack = new LinkedList<Command>();

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
	 * Apply the Command to the supplied Canvas and push it on the command stack. Any undone
	 * commands one the command stack will be discarded.
	 * 
	 * @param command Command to draw and save.
	 * @param canvas Canvas to draw the command onto, typically is associated with a Bitmap.
	 */
	public void commitCommand(Command command, Canvas canvas) {
		command.draw(canvas);
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
	public void undoLast(Canvas canvas) {
		if (commandIndex > 0) {
			// clear canvas and redraw original
			canvas.drawPaint(transparencyPaint);
			canvas.drawBitmap(originalBitmap, 0, 0, null);
			commandIndex--;
			for (int i = 0; i < commandIndex; i++) {
				commandStack.get(i).draw(canvas);
			}
		}
	}

	/**
	 * Redos the last undone action from the command stack.
	 * 
	 * @param canvas Canvas to apply commands to, typically is associated with a Bitmap.
	 */
	public void redoLast(Canvas canvas) {
		if (commandIndex < commandStack.size()) {
			commandStack.get(commandIndex).draw(canvas);
			commandIndex++;
		}
	}
}
