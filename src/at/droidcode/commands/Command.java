package at.droidcode.commands;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

public class Command {
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

	/**
	 * Draws the Path or Point on a Canvas.
	 * 
	 * @param canvas Canvas to draw on, typically is associated with a Bitmap.
	 */
	void draw(Canvas canvas) {
		if (cmdPath != null) {
			canvas.drawPath(cmdPath, cmdPaint);
		} else if (cmdPoint != null) {
			canvas.drawPoint(cmdPoint.x, cmdPoint.y, cmdPaint);
		} else {
			canvas.drawPaint(cmdPaint);
		}
	}
}
