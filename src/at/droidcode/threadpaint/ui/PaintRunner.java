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

package at.droidcode.threadpaint.ui;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Xfermode;
import android.util.Log;
import android.view.SurfaceHolder;
import at.droidcode.commands.Command;
import at.droidcode.commands.CommandManager;
import at.droidcode.threadpaint.R;
import at.droidcode.threadpaint.TpApplication;
import at.droidcode.threadpaint.dialog.BrushPickerDialog;
import at.droidcode.threadpaint.dialog.ColorPickerDialog;

/**
 * Draws Paint on the PaintView's surface using a Thread.
 */
public class PaintRunner extends TpRunner implements ColorPickerDialog.OnPaintChangedListener,
		BrushPickerDialog.OnBrushChangedListener {

	private Bitmap drawingBitmap;
	private final Path pathToDraw;
	private final Canvas bitmapCanvas;
	private final Rect rectSurface;
	private final Rect rectBitmap;
	private final PointF surfaceCenter;
	private final Point scroll;
	private float zoom;
	private final Paint bitmapPathPaint; // only to draw onto the Bitmap
	private final Paint canvasPathPaint; // only to draw onto the Canvas of the PaintView
	private final Paint checkeredPattern;
	private final Xfermode eraseXfermode;
	private final SurfaceHolder surfaceHolder;
	private final CommandManager commandManager;

	private class DrawLoop implements Runnable {
		@Override
		public void run() {
			Canvas canvas = null;
			try {
				canvas = surfaceHolder.lockCanvas();
				doDraw(canvas);
			} finally {
				if (canvas != null) {
					surfaceHolder.unlockCanvasAndPost(canvas);
				}
			}
		}
	}

	public PaintRunner(PaintView paintView) {
		surfaceHolder = paintView.getHolder();
		commandManager = new CommandManager();

		super.setRunnable(new DrawLoop());

		pathToDraw = new Path();
		pathToDraw.incReserve(42); // might be more efficient
		bitmapCanvas = new Canvas();
		rectSurface = new Rect();
		rectBitmap = new Rect();
		surfaceCenter = new PointF();
		scroll = new Point(0, 0);
		zoom = 1f;

		final TpApplication appContext = (TpApplication) paintView.getContext().getApplicationContext();

		final int color = appContext.getResources().getColor(R.color.stroke_standard);
		bitmapPathPaint = new Paint();
		bitmapPathPaint.setColor(color);
		bitmapPathPaint.setAntiAlias(true);
		bitmapPathPaint.setDither(true);
		bitmapPathPaint.setStyle(Paint.Style.STROKE);
		bitmapPathPaint.setStrokeJoin(Paint.Join.ROUND);
		bitmapPathPaint.setStrokeCap(Paint.Cap.ROUND);
		bitmapPathPaint.setStrokeWidth(appContext.maxStrokeWidth() / 2);
		canvasPathPaint = new Paint(bitmapPathPaint);

		Bitmap checkerboard = BitmapFactory.decodeResource(appContext.getResources(), R.drawable.transparent);
		BitmapShader shader = new BitmapShader(checkerboard, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
		checkeredPattern = new Paint();
		checkeredPattern.setShader(shader);

		eraseXfermode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
	}

	/**
	 * Stop the internal Thread, clear the Command Manager and Bitmap.
	 */
	@Override
	public synchronized void stop() {
		super.stop();
		commandManager.clear();
		drawingBitmap.recycle();
		drawingBitmap = null;
	}

	/**
	 * Called by the Thread to transform the canvas, draw the background, bitmap and the unfinished
	 * Path.
	 * 
	 * @param canvas SurfaceHolder's Canvas onto which the thread draws.
	 */
	private void doDraw(Canvas canvas) {
		// canvas.scale(zoom, zoom);
		canvas.scale(zoom, zoom, surfaceCenter.x, surfaceCenter.y);
		canvas.translate(scroll.x, scroll.y);
		canvas.drawPaint(checkeredPattern);
		canvas.drawBitmap(drawingBitmap, 0, 0, null);
		canvas.drawPath(pathToDraw, canvasPathPaint);
	}

	@Override
	public void colorChanged(int color) {
		bitmapPathPaint.setColor(color);
		canvasPathPaint.setColor(color);
		if (Color.alpha(color) == 0x00) {
			// draw with transparency onto the bitmap
			bitmapPathPaint.setXfermode(eraseXfermode);
			// draw the checkered background pattern onto the canvas
			canvasPathPaint.reset();
			canvasPathPaint.setStyle(bitmapPathPaint.getStyle());
			canvasPathPaint.setStrokeJoin(bitmapPathPaint.getStrokeJoin());
			canvasPathPaint.setStrokeCap(bitmapPathPaint.getStrokeCap());
			canvasPathPaint.setStrokeWidth(bitmapPathPaint.getStrokeWidth());
			canvasPathPaint.setShader(checkeredPattern.getShader());
		} else {
			// draw with normal paint again
			bitmapPathPaint.setXfermode(null);
			canvasPathPaint.set(bitmapPathPaint);
		}
	}

	@Override
	public void capChanged(Cap cap) {
		bitmapPathPaint.setStrokeCap(cap);
		canvasPathPaint.setStrokeCap(cap);
	}

	@Override
	public void strokeChanged(int width) {
		bitmapPathPaint.setStrokeWidth(width);
		canvasPathPaint.setStrokeWidth(width);
	}

	/**
	 * Called by the SurfaceView on surfaceChanged(). Creates a new surface sized bitmap if it is
	 * still null.
	 * 
	 * @param width Width of the SurfaceView.
	 * @param height Height of the SurfaceView.
	 * @param rect Rect of the SurfaceView.
	 */
	void setSurfaceSize(int width, int height) {
		synchronized (pThread) {
			resetPerspective();
			rectSurface.set(0, 0, width, height);
			surfaceCenter.x = rectSurface.exactCenterX();
			surfaceCenter.y = rectSurface.exactCenterY();
			if (drawingBitmap == null) {
				Log.w(TpApplication.TAG, "Creating new bitmap of surface size.");
				drawingBitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
				rectBitmap.set(0, 0, width, height);
				bitmapCanvas.setBitmap(drawingBitmap);
				commandManager.reset(drawingBitmap);
			}
		}
	}

	/**
	 * Sets a new Bitmap and recycles the old one. Alsor resets values for zoom and scroll.
	 * 
	 * @param bitmap New Bitmap to draw.
	 */
	void setBitmap(Bitmap bitmap) {
		synchronized (pThread) {
			if (drawingBitmap != null) {
				drawingBitmap.recycle();
			}
			resetPerspective();
			drawingBitmap = bitmap;
			commandManager.reset(bitmap);
			bitmapCanvas.setBitmap(drawingBitmap);
			rectBitmap.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
		}
	}

	/**
	 * @return Actual Bitmap the thread uses, a copy might be too big.
	 */
	Bitmap getBitmap() {
		return drawingBitmap;
	}

	/**
	 * @return Paint used to draw on the Bitmap.
	 */
	Paint getPaint() {
		return bitmapPathPaint;
	}

	/**
	 * @return Current zoom level [1.0..*].
	 */
	float getZoom() {
		return zoom;
	}

	/**
	 * Begin a new path at the specified coordinates on the Bitmap.
	 * 
	 * @param x X-Coordinate on the Bitmap.
	 * @param y Y-Coordinate on the Bitmap.
	 */
	void startPath(float x, float y) {
		pathToDraw.rewind();
		translate(x, y);
		pathToDraw.moveTo(translate.x, translate.y);
	}

	/**
	 * Continue an unfinished path from the previous to the new coordinates on the Bitmap.
	 * 
	 * @param x1 Previous X-Coordinate on the Screen.
	 * @param y1 Previous Y-Coordinate on the Screen.
	 * @param x2 New X-Coordinate on the Screen.
	 * @param y2 New Y-Coordinate on the Screen.
	 */
	void updatePath(float x1, float y1, float x2, float y2) {
		translate((x1 + x2) / 2f, (y1 + y2) / 2f);
		float cx = translate.x;
		float cy = translate.y;
		translate(x2, y2);
		pathToDraw.quadTo(cx, cy, translate.x, translate.y);
	}

	/**
	 * Draw the currently unfinished Path on the Bitmap and rewind it.
	 */
	void finishPath() {
		synchronized (pThread) {
			Command command = new Command(bitmapPathPaint, pathToDraw);
			commandManager.commitCommand(command, bitmapCanvas);
			pathToDraw.rewind();
		}
	}

	/**
	 * Draw a point at the specified screen coordinates translated to the Bitmap.
	 * 
	 * @param x X-Coordinate of the point on the Screen.
	 * @param y Y-Coordinate of the point on the Screen.
	 */
	void drawPoint(float x, float y) {
		synchronized (pThread) {
			translate(x, y);
			Command command = new Command(bitmapPathPaint, translate);
			commandManager.commitCommand(command, bitmapCanvas);
		}
	}

	/**
	 * Translate the Canvas by a given offset.
	 * 
	 * @param dx Offset on the x-axis.
	 * @param dy Offset on the y-axis.
	 */
	void scroll(int dx, int dy) {
		synchronized (pThread) {
			float surfaceZoomedWidth = rectSurface.right / zoom;
			float surfaceZoomedHeight = rectSurface.bottom / zoom;

			// Don't scroll if the (zoomed) bitmap is smaller than the surface.
			if ((surfaceZoomedWidth - rectBitmap.right) > 0 && (surfaceZoomedHeight - rectBitmap.bottom) > 0) {
				scroll.set(0, 0);
			} else {
				scroll.offset(Math.round(dx / zoom), Math.round(dy / zoom));
				float pivotX = surfaceCenter.x - (surfaceCenter.x / zoom);
				float pivotY = surfaceCenter.y - (surfaceCenter.y / zoom);
				float xMax = (surfaceZoomedWidth - rectBitmap.right) + pivotX;
				float yMax = (surfaceZoomedHeight - rectBitmap.bottom) + pivotY;
				if (scroll.x < xMax) {
					scroll.x = Math.round(xMax);
				}
				if (scroll.y < yMax) {
					scroll.y = Math.round(yMax);
				}
				// Make checks for upper left corner after checks for
				// lower right corner to prevent jumping.
				if (scroll.x - pivotX > 0) {
					scroll.x = Math.round(pivotX);
				}
				if (scroll.y - pivotY > 0) {
					scroll.y = Math.round(pivotY);
				}
			}
		}
	}

	/**
	 * Set the zoom factor for the Canvas.
	 * 
	 * @param scale [1.0..*] Factor to zoom.
	 */
	void zoom(float scale) {
		synchronized (pThread) {
			if (zoom >= 1) {
				zoom = scale;
			}
			if (zoom < 1) {
				zoom = 1;
			}
		}
	}

	private final Point translate = new Point();

	// Translate screen coordinates to bitmap coordinates.
	private void translate(float x, float y) {
		// Long form: (x - scroll.x * zoom - (center.x - center.x * zoom)) / zoom
		translate.x = (int) ((x - surfaceCenter.x) / zoom + surfaceCenter.x - scroll.x);
		translate.y = (int) ((y - surfaceCenter.y) / zoom + surfaceCenter.y - scroll.y);
	}

	// It's essential to set the canvas matrix null!
	private void resetPerspective() {
		zoom = 1f;
		scroll.set(0, 0);
		bitmapCanvas.setMatrix(null);
	}

	/**
	 * Draw the currently used Paint on the whole Bitmap.
	 */
	void fillWithPaint() {
		Command command = new Command(bitmapPathPaint);
		commandManager.commitCommand(command, bitmapCanvas);
	}

	/**
	 * Reset the Canvas by setting a new empty bitmap of surface size.
	 */
	void resetCanvas() {
		Bitmap bitmap = Bitmap.createBitmap(rectSurface.right, rectSurface.bottom, Config.ARGB_8888);
		setBitmap(bitmap);
	}

	/**
	 * Undo one step in the command manager.
	 */
	void undo() {
		synchronized (pThread) {
			commandManager.undoLast(bitmapCanvas);
		}
	}

	/**
	 * Redo one step in the command manager.
	 */
	void redo() {
		synchronized (pThread) {
			commandManager.redoLast(bitmapCanvas);
		}
	}
}
