/*
 * Copyright Maximilian Fellner <max.fellner@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
	private static final int MAXCOMMANDS = 256;
	private Bitmap originalBitmap;
	private int commandIndex; // [0..commandStack.size()]

	private final Canvas bitmapCanvas;
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

		bitmapCanvas = new Canvas();
		transparencyPaint = new Paint();
		transparencyPaint.setColor(Color.TRANSPARENT);
		transparencyPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
	}

	/**
	 * Clear Bitmap and command stack.
	 */
	public void clear() {
		if (originalBitmap != null) {
			originalBitmap.recycle();
			originalBitmap = null;
		}
		commandStack.clear();
		commandIndex = 0;
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
		bitmapCanvas.setBitmap(this.originalBitmap);
	}

	/**
	 * Apply the Command to the supplied Bitmap-Canvas and push it on the command stack. Any
	 * previously undone commands on the command stack will be discarded.
	 * 
	 * @param command Command to draw and save.
	 * @param canvas Bitmap-Canvas to apply the command to.
	 */
	public synchronized void commitCommand(Command command, Canvas canvas) {
		command.setCanvas(canvas);
		command.run();
		if (commandIndex < commandStack.size()) {
			// Remove remaining undone commands on top of the stack first.
			for (int i = commandStack.size(); i > commandIndex; i--) {
				commandStack.removeLast();
			}
		}
		if (commandIndex == MAXCOMMANDS) {
			// Apply first command to the Bitmap and remove it from the stack.
			Command removed = commandStack.removeFirst();
			removed.setCanvas(bitmapCanvas);
			removed.run();
		} else {
			commandIndex++;
		}
		commandStack.add(command);
	}

	/**
	 * Undos the last action by decrementing the command index and then applying all previous
	 * commands following up to it, after redrawing the original Bitmap first.
	 * 
	 * @param canvas Bitmap-Canvas to apply the commands to.
	 */
	public synchronized void undoLast(Canvas canvas) {
		if (commandIndex > 0) {
			// clear canvas and redraw original
			canvas.drawPaint(transparencyPaint);
			canvas.drawBitmap(originalBitmap, 0, 0, null);
			commandIndex--;
			for (int i = 0; i < commandIndex; i++) {
				Command command = commandStack.get(i);
				command.run(); // do on ui thread, no queue
			}
		}
	}

	/**
	 * Redos the last undone command from the command stack and increments the command index.
	 * 
	 * @param canvas Bitmap-Canvas to apply the command to.
	 */
	public synchronized void redoLast(Canvas canvas) {
		if (commandIndex < commandStack.size()) {
			Command command = commandStack.get(commandIndex);
			command.run(); // do on ui thread, no queue
			commandIndex++;
		}
	}
}
