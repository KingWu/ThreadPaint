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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

public class Command implements Runnable {
	private Canvas cmdCanvas;
	private final Paint cmdPaint;
	private final Path cmdPath;
	private final Point cmdPoint;

	/**
	 * A command representing a drawn path.
	 * 
	 * @param paint Paint which was used to draw. Will be copied.
	 * @param path Path which which was drawn. Will be copied.
	 */
	public Command(Paint paint, Path path) {
		cmdPaint = new Paint(paint);
		cmdPath = new Path(path);
		cmdPoint = null;
	}

	/**
	 * A command representing a drawn point.
	 * 
	 * @param paint Paint which was used to draw. Will be copied.
	 * @param point Cooridinates of point which was drawn. Will be copied.
	 */
	public Command(Paint paint, Point point) {
		cmdPaint = new Paint(paint);
		cmdPath = null;
		cmdPoint = new Point(point);
	}

	/**
	 * A command representing paint drawn over the whole canvas.
	 * 
	 * @param paint Paint which was used to draw. Will be copied.
	 */
	public Command(Paint paint) {
		cmdPaint = new Paint(paint);
		cmdPath = null;
		cmdPoint = null;
	}

	void setCanvas(Canvas canvas) {
		cmdCanvas = canvas;
	}

	@Override
	public void run() {
		if (cmdPath != null) {
			cmdCanvas.drawPath(cmdPath, cmdPaint);
		} else if (cmdPoint != null) {
			cmdCanvas.drawPoint(cmdPoint.x, cmdPoint.y, cmdPaint);
		} else {
			cmdCanvas.drawPaint(cmdPaint);
		}
	}
}
