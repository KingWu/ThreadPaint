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
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Xfermode;
import android.util.Log;
import android.view.SurfaceHolder;
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

	private volatile boolean keepRunning;
	private volatile boolean isPaused;

	private volatile Path pathToDraw;
	private Bitmap drawingBitmap;
	private final Canvas bitmapCanvas;
	private final Rect rectSurface;
	private final Paint bitmapPathPaint; // only to draw onto the Bitmap
	private final Paint canvasPathPaint; // only to drawi onto the Canvas of the PaintView
	private final Paint checkeredPattern;
	private final Paint transparencyPaint;
	private final Xfermode eraseXfermode;

	private final SurfaceHolder surfaceHolder;

	public PaintThread(PaintView paintView, Bitmap bitmap) {
		surfaceHolder = paintView.getHolder();
		drawingBitmap = bitmap;

		keepRunning = false;
		isPaused = false;
		pathToDraw = new Path();
		bitmapCanvas = new Canvas();
		bitmapCanvas.setBitmap(drawingBitmap);
		rectSurface = new Rect();

		eraseXfermode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR); // SRC_OUT

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

	private boolean bitmapNeedsUpdate = false;

	/**
	 * Causes the Thread to draw the pathToDraw onto the Bitmap during the next pass.
	 */
	void bitmapNeedsUpdate() {
		synchronized (lock) {
			bitmapNeedsUpdate = true;
		}
	}

	/**
	 * After the checkered background pattern the finished Path is drawn onto the Bitmap if necessary. Then the Bitmap
	 * and finally a still open Path are drawn onto the given Canvas.
	 * 
	 * @param canvas External Canvas onto which the thread draws.
	 */
	private void doDraw(Canvas canvas) {
		canvas.drawPaint(checkeredPattern);
		if (bitmapNeedsUpdate) {
			bitmapCanvas.drawPath(pathToDraw, bitmapPathPaint);
			pathToDraw.rewind();
			bitmapNeedsUpdate = false;
		}
		canvas.drawBitmap(drawingBitmap, rectSurface, rectSurface, null);
		canvas.drawPath(pathToDraw, canvasPathPaint);
	}

	@Override
	public void colorChanged(int color) {
		synchronized (lock) {
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
	}

	@Override
	public void capChanged(Cap cap) {
		synchronized (lock) {
			bitmapPathPaint.setStrokeCap(cap);
			canvasPathPaint.setStrokeCap(cap);
		}
	}

	@Override
	public void strokeChanged(int width) {
		synchronized (lock) {
			bitmapPathPaint.setStrokeWidth(width);
			canvasPathPaint.setStrokeWidth(width);
		}
	}

	/**
	 * Called by the SurfaceView on surfaceChanged().
	 * 
	 * @param width Width of the SurfaceView.
	 * @param height Height of the SurfaceView.
	 * @param rect Rect of the SurfaceView.
	 */
	void setSurfaceSize(int width, int height, Rect rect) {
		synchronized (lock) {
			drawingBitmap = Bitmap.createScaledBitmap(drawingBitmap, width, height, false);
			bitmapCanvas.setBitmap(drawingBitmap);
			rectSurface.set(rect);
		}
	}

	/**
	 * @param b true to keep running, false to terminate
	 */
	void setRunning(boolean b) {
		keepRunning = b;
	}

	/**
	 * Cause the thread to wait or resume. Passing false if the thread is paused will call notify() on the waiting lock.
	 * 
	 * @param pause true to pause drawing, false to resume
	 */
	public void setPaused(boolean pause) {
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
			drawingBitmap = bitmap;
			bitmapCanvas.setBitmap(drawingBitmap);
		}
	}

	/**
	 * @return Bitmap the thread draws onto the surface.
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

	/**
	 * Begin a new path at the specified coordinates on the Bitmap.
	 * 
	 * @param x X-Coordinate on the Bitmap.
	 * @param y Y-Coordinate on the Bitmap.
	 */
	void startPath(float x, float y) {
		synchronized (lock) {
			pathToDraw.rewind();
			pathToDraw.moveTo(x, y);
		}
		Log.d(TAG, "start path x: " + x + " y: " + y);
	}

	/**
	 * Continue and interpolate a started path from the previous to the new coordinates on the Bitmap.
	 * 
	 * @param x1 Previous X-Coordinate on the Bitmap.
	 * @param y1 Previous Y-Coordinate on the Bitmap.
	 * @param x2 New X-Coordinate on the Bitmap.
	 * @param y2 New Y-Coordinate on the Bitmap.
	 */
	void updatePath(float x1, float y1, float x2, float y2) {
		synchronized (lock) {
			final float cx = (x1 + x2) / 2;
			final float cy = (y1 + y2) / 2;
			// pathToDraw.quadTo(cx, cy, x2, y2);
			pathToDraw.quadTo(x1, y1, cx, cy);
		}
		Log.d(TAG, "update path x1: " + x1 + " y1: " + y1 + " x2: " + x2 + " y2: " + y2);
	}

	/**
	 * Draws a point at the specified coordinates on the Bitmap.
	 * 
	 * @param x X-Coordinate of the point
	 * @param y Y-Coordinate of the point
	 */
	void drawPoint(float x, float y) {
		synchronized (lock) {
			pathToDraw.rewind();
			bitmapCanvas.drawPoint(x, y, bitmapPathPaint);
		}
		Log.d(TAG, "draw point x: " + x + " y: " + y);
	}

	/**
	 * Draw the currently used paint over the whole Canvas (Bitmap).
	 */
	void fillWithPaint() {
		synchronized (lock) {
			pathToDraw.rewind();
			bitmapCanvas.drawPaint(bitmapPathPaint);
		}
	}

	void clearCanvas() {
		synchronized (lock) {
			pathToDraw.rewind();
			bitmapCanvas.drawPaint(transparencyPaint);
		}
	}
}
