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

import static at.droidcode.threadpaint.TpApplication.TAG;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Path;
import android.graphics.Point;
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
 * Continually draws a Path onto a Canvas which in turn is being drawn onto a SurfaceView.
 */
public class PaintThread extends Thread implements ColorPickerDialog.OnPaintChangedListener,
		BrushPickerDialog.OnBrushChangedListener {
	private final Object lock = new Object();
	private final CommandManager commandManager;

	private volatile boolean keepRunning;
	private volatile boolean isPaused;

	private volatile Path pathToDraw;
	private Bitmap drawingBitmap;
	private final Canvas bitmapCanvas;
	private final Rect rectSurface;
	private final Rect rectBitmap;
	private final Point scroll;
	float zoom;
	private final Paint bitmapPathPaint; // only to draw onto the Bitmap
	private final Paint canvasPathPaint; // only to drawi onto the Canvas of the PaintView
	private final Paint checkeredPattern;
	private final Paint transparencyPaint;
	private final Xfermode eraseXfermode;
	private final SurfaceHolder surfaceHolder;

	public PaintThread(PaintView paintView, Bitmap bitmap) {
		surfaceHolder = paintView.getHolder();
		drawingBitmap = bitmap;

		commandManager = new CommandManager();

		keepRunning = false;
		isPaused = false;
		pathToDraw = new Path();
		bitmapCanvas = new Canvas();
		bitmapCanvas.setBitmap(drawingBitmap);
		rectSurface = new Rect();
		rectBitmap = new Rect();
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

		eraseXfermode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR); // SRC_OUT

		transparencyPaint = new Paint();
		transparencyPaint.setColor(Color.TRANSPARENT);
		transparencyPaint.setXfermode(eraseXfermode);
	}

	@Override
	public void run() {
		while (keepRunning) {
			Canvas canvas = null;
			try {
				synchronized (lock) {
					canvas = surfaceHolder.lockCanvas();
					doDraw(canvas);
				}
			} finally {
				if (canvas != null) {
					surfaceHolder.unlockCanvasAndPost(canvas);
				}
			}
			synchronized (lock) {
				if (isPaused) {
					try {
						lock.wait();
					} catch (InterruptedException e) {
						Log.e(TAG, "ERROR ", e);
					}
				}
			}
		}
		drawingBitmap.recycle();
		drawingBitmap = null;
	}

	private boolean drawPathOnBitmap = false;

	/**
	 * Causes the Thread to draw the pathToDraw onto the Bitmap instead of the Canvas during the
	 * next pass.
	 */
	void drawPathOnBitmap() {
		drawPathOnBitmap = true;
	}

	/**
	 * After the checkered background pattern the finished Path is drawn onto the Bitmap if
	 * necessary. Then the Bitmap and finally a still open Path are drawn onto the given Canvas.
	 * 
	 * @param canvas External Canvas onto which the thread draws.
	 */
	private void doDraw(Canvas canvas) {
		canvas.scale(zoom, zoom);
		canvas.translate(scroll.x, scroll.y);
		canvas.drawPaint(checkeredPattern);
		if (drawPathOnBitmap) {
			Command command = new Command(bitmapPathPaint, pathToDraw);
			commandManager.commitCommand(command, bitmapCanvas);

			pathToDraw.rewind();
			drawPathOnBitmap = false;
		}
		canvas.drawBitmap(drawingBitmap, null, rectBitmap, null);
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
	 * Called by the SurfaceView on surfaceChanged(). Important to make the inital bitmap actually
	 * as big as the screen.
	 * 
	 * @param width Width of the SurfaceView.
	 * @param height Height of the SurfaceView.
	 * @param rect Rect of the SurfaceView.
	 */
	void setSurfaceSize(int width, int height) {
		synchronized (lock) {
			rectSurface.set(0, 0, width, height);
			if (rectBitmap.isEmpty()) {
				rectBitmap.set(0, 0, width, height);
			}
			// Initial Bitmap provided by PaintView.
			if (drawingBitmap.getWidth() == 1 && drawingBitmap.getHeight() == 1) {
				drawingBitmap = Bitmap.createScaledBitmap(drawingBitmap, width, height, false);
				bitmapCanvas.setBitmap(drawingBitmap);

				commandManager.reset(drawingBitmap);
			}
		}
	}

	/**
	 * @param b true to keep running, false to terminate
	 */
	void setRunning(boolean b) {
		keepRunning = b;
	}

	/**
	 * Cause the thread to wait or resume. Passing false if the thread is paused will call notify()
	 * on the waiting lock.
	 * 
	 * @param pause true to pause drawing, false to resume
	 */
	void setPaused(boolean pause) {
		synchronized (lock) {
			if (!pause && isPaused) {
				lock.notify();
			}
			isPaused = pause;
		}
	}

	/**
	 * Sets a new Bitmap and recycles the old one.
	 * 
	 * @param bitmap New Bitmap to draw
	 */
	void setBitmap(Bitmap bitmap) {
		synchronized (lock) {
			if (drawingBitmap != null) {
				drawingBitmap.recycle();
			}
			zoom = 1;
			scroll.set(0, 0);
			drawingBitmap = bitmap;
			bitmapCanvas.setBitmap(drawingBitmap);
			rectBitmap.set(0, 0, bitmap.getWidth(), bitmap.getHeight());

			commandManager.reset(bitmap);
		}
	}

	/**
	 * @return Actual Bitmap the thread uses, a copy might be too big.
	 */
	Bitmap getBitmap() {
		return drawingBitmap;
	}

	/**
	 * @return Path the thread draws onto the Canvas (Bitmap).
	 */
	Path getPath() {
		return pathToDraw;
	}

	/**
	 * @return Paint the thread uses to draw a path onto the Bitmap.
	 */
	Paint getPaint() {
		return bitmapPathPaint;
	}

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
		synchronized (lock) {
			pathToDraw.rewind();
			translate(x, y);
			pathToDraw.moveTo(translate.x, translate.y);
		}
	}

	/**
	 * Continue and interpolate a started path from the previous to the new coordinates on the
	 * Bitmap.
	 * 
	 * @param x1 Previous X-Coordinate on the Bitmap.
	 * @param y1 Previous Y-Coordinate on the Bitmap.
	 * @param x2 New X-Coordinate on the Bitmap.
	 * @param y2 New Y-Coordinate on the Bitmap.
	 */
	void updatePath(float x1, float y1, float x2, float y2) {
		synchronized (lock) {
			translate(x1, y1);
			float xx1 = translate.x;
			float yy1 = translate.y;
			translate(x2, y2);
			float cx = (xx1 + translate.x) / 2;
			float cy = (yy1 + translate.y) / 2;
			pathToDraw.quadTo(xx1, yy1, cx, cy);
		}
	}

	/**
	 * Draws a point at the specified coordinates on the Bitmap.
	 * 
	 * @param x X-Coordinate of the point
	 * @param y Y-Coordinate of the point
	 */
	void drawPoint(float x, float y) {
		synchronized (lock) {
			translate(x, y);
			Command command = new Command(bitmapPathPaint, translate);
			commandManager.commitCommand(command, bitmapCanvas);
		}
	}

	void undo() {
		synchronized (lock) {
			commandManager.undoLast(bitmapCanvas);
		}
	}

	void redo() {
		synchronized (lock) {
			commandManager.redoLast(bitmapCanvas);
		}
	}

	/**
	 * Translate the Canvas by a given offset.
	 * 
	 * @param dx Offset on the x-axis
	 * @param dy Offset on the y-axis
	 */
	void scroll(int dx, int dy) {
		synchronized (lock) {
			float surfaceZoomedWidth = rectSurface.right / zoom;
			float surfaceZoomedHeight = rectSurface.bottom / zoom;

			// Don't scroll if the (zoomed) bitmap is smaller than the surface.
			if ((surfaceZoomedWidth - rectBitmap.right) > 0 && (surfaceZoomedHeight - rectBitmap.bottom) > 0) {
				scroll.set(0, 0);
			} else {
				scroll.offset(Math.round(dx / zoom), Math.round(dy / zoom));
				float xMax = -1 * (rectBitmap.right - surfaceZoomedWidth);
				float yMax = -1 * (rectBitmap.bottom - surfaceZoomedHeight);
				if (scroll.x < xMax) {
					scroll.x = Math.round(xMax);
				}
				if (scroll.y < yMax) {
					scroll.y = Math.round(yMax);
				}
				// Make checks for upper left corner after checks for
				// lower right corner to prevent jumping.
				if (scroll.x > 0) {
					scroll.x = 0;
				}
				if (scroll.y > 0) {
					scroll.y = 0;
				}
			}
		}
	}

	/**
	 * Sets the zoom factor for the Canvas.
	 * 
	 * @param scale (1..*) Factor to zoom
	 */
	void zoom(float scale) {
		synchronized (lock) {
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
	private Point translate(float x, float y) {
		translate.x = Math.round((x - scroll.x * zoom) / zoom);
		translate.y = Math.round((y - scroll.y * zoom) / zoom);
		return translate;
	}

	/**
	 * Draw the currently used paint over the whole Canvas (Bitmap).
	 */
	void fillWithPaint() {
		// bitmapCanvas.drawPaint(bitmapPathPaint);
		Command command = new Command(bitmapPathPaint);
		commandManager.commitCommand(command, bitmapCanvas);
	}

	/**
	 * Reset the Canvas with an empty, transparent Bitmap of surface size. Also reset scroll and
	 * zoom values.
	 */
	void resetCanvas() {
		synchronized (lock) {
			zoom = 1;
			scroll.set(0, 0);
			bitmapCanvas.drawPaint(transparencyPaint);
			rectBitmap.set(0, 0, rectSurface.right, rectSurface.bottom);
			drawingBitmap = Bitmap.createScaledBitmap(drawingBitmap, rectSurface.right, rectSurface.bottom, false);
			bitmapCanvas.setBitmap(drawingBitmap);

			commandManager.reset(drawingBitmap);
		}
	}
}
