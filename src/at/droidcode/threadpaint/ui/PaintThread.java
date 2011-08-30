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

import static at.droidcode.threadpaint.ThreadPaintApp.TAG;

import java.util.Observable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import at.droidcode.threadpaint.R;
import at.droidcode.threadpaint.ThreadPaintApp;
import at.droidcode.threadpaint.dialog.BrushPickerDialog;
import at.droidcode.threadpaint.dialog.ColorPickerDialog;

/**
 * Continually draws a Path onto a Canvas which in turn is being drawn onto a SurfaceView.
 */
public class PaintThread extends Thread implements ColorPickerDialog.OnPaintChangedListener,
		BrushPickerDialog.OnBrushChangedListener {
	private final Object lock = new Object();

	private volatile boolean keepRunning;

	private volatile Path pathToDraw;
	private Bitmap workingBitmap;
	private final Canvas workingCanvas;
	private final Rect rectSurface;
	private int backgroundColor;
	private final Paint pathPaint;

	private final SurfaceHolder surfaceHolder;
	private final Observable observable;

	public PaintThread(SurfaceView view, Observable o) {
		surfaceHolder = view.getHolder();
		observable = o;

		keepRunning = false;
		pathToDraw = new Path();
		workingBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
		workingCanvas = new Canvas();
		workingCanvas.setBitmap(workingBitmap);
		rectSurface = new Rect();

		final ThreadPaintApp appContext = (ThreadPaintApp) view.getContext().getApplicationContext();
		backgroundColor = appContext.getResources().getColor(R.color.canvas_background);

		pathPaint = new Paint();
		pathPaint.setStyle(Paint.Style.STROKE);
		pathPaint.setAntiAlias(true);
		pathPaint.setDither(true);
		final int color = appContext.getResources().getColor(R.color.stroke_standard);
		pathPaint.setColor(color);
		pathPaint.setStyle(Paint.Style.STROKE);
		pathPaint.setStrokeJoin(Paint.Join.ROUND);
		pathPaint.setStrokeCap(Paint.Cap.ROUND);
		pathPaint.setStrokeWidth(appContext.maxStrokeWidth() / 2);
	}

	@Override
	public void run() {
		fillBackground();
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
		}
		workingBitmap.recycle();
		workingBitmap = null;
	}

	/**
	 * Draws the path onto a canvas which is then drawn onto the canvas of the SurfaceView.
	 * 
	 * @param canvas The Canvas onto which the Bitmap is drawn.
	 */
	private void doDraw(Canvas canvas) {
		workingCanvas.drawPath(pathToDraw, pathPaint);
		canvas.drawBitmap(workingBitmap, rectSurface, rectSurface, null);
	}

	@Override
	public void colorChanged(int color) {
		synchronized (lock) {
			pathPaint.setColor(color);
		}
	}

	@Override
	public void strokeChanged(int width) {
		synchronized (lock) {
			pathPaint.setStrokeWidth(width);
		}
	}

	@Override
	public void capChanged(Cap cap) {
		synchronized (lock) {
			pathPaint.setStrokeCap(cap);
			observable.notifyObservers(cap);
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
			workingBitmap = Bitmap.createScaledBitmap(workingBitmap, width, height, false);
			workingCanvas.setBitmap(workingBitmap);
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
	 * Sets a new Bitmap and recycles the old one.
	 * 
	 * @param bitmap New Bitmap to draw
	 */
	void setBitmap(Bitmap bitmap) {
		synchronized (lock) {
			if (workingBitmap != null) {
				workingBitmap.recycle();
			}
			workingBitmap = bitmap;
			workingCanvas.setBitmap(workingBitmap);
		}
	}

	/**
	 * @return Bitmap the thread draws onto the surface.
	 */
	Bitmap getBitmap() {
		synchronized (lock) {
			return workingBitmap;
		}
	}

	/**
	 * @return Path the thread draws onto the Canvas (Bitmap).
	 */
	Path getPath() {
		synchronized (lock) {
			return pathToDraw;
		}
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
			pathToDraw.quadTo(cx, cy, x2, y2);
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
			workingCanvas.drawPoint(x, y, pathPaint);
		}
		Log.d(TAG, "draw point x: " + x + " y: " + y);
	}

	/**
	 * Draw the current background color over the whole Canvas (Bitmap).
	 */
	void fillBackground() {
		synchronized (lock) {
			pathToDraw.rewind();
			workingCanvas.drawColor(backgroundColor);
		}
	}

	/**
	 * Change the current background color and draw it over the whole Canvas (Bitmap).
	 * 
	 * @param color Color to set as the new background color
	 */
	void fillBackground(int color) {
		synchronized (lock) {
			backgroundColor = color;
			pathToDraw.rewind();
			workingCanvas.drawColor(backgroundColor);
		}
	}
}
