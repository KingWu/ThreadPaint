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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import at.droidcode.threadpaint.R;
import at.droidcode.threadpaint.ThreadPaintApp;
import at.droidcode.threadpaint.dialog.ColorPickerDialog;

public class PaintThread extends Thread implements ColorPickerDialog.OnColorChangedListener,
		ColorPickerDialog.OnStrokeChangedListener {
	static final String TAG = "THREADPAINT";

	private final Object lock = new Object();

	private boolean keepRunning = false;

	private Bitmap workingBitmap;
	private Canvas workingCanvas;
	private Path pathToDraw;
	private Paint pathPaint;
	private Rect rectCanvas;
	private final int backgroundColor;

	private final SurfaceHolder mSurfaceHolder;

	public PaintThread(SurfaceView view) {
		mSurfaceHolder = view.getHolder();
		workingBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
		workingCanvas = new Canvas();
		pathToDraw = new Path();
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
		clearCanvas();
		while (keepRunning) {
			Canvas canvas = null;
			try {
				synchronized (lock) {
					canvas = mSurfaceHolder.lockCanvas();
					doDraw(canvas);
				}
			} finally {
				if (canvas != null) {
					mSurfaceHolder.unlockCanvasAndPost(canvas);
				}
			}
		}
		workingBitmap.recycle();
		workingBitmap = null;
		workingCanvas = null;
		pathToDraw = null;
		pathPaint = null;
	}

	public void setSurfaceSize(int width, int height, Rect rect) {
		synchronized (lock) {
			workingBitmap = Bitmap.createScaledBitmap(workingBitmap, width, height, true);
			workingCanvas.setBitmap(workingBitmap);
			rectCanvas = rect;
		}
	}

	public void setRunning(boolean b) {
		keepRunning = b;
	}

	public Path getPath() {
		synchronized (lock) {
			return pathToDraw;
		}
	}

	public void drawPoint(float x, float y) {
		synchronized (lock) {
			pathToDraw.rewind();
			workingCanvas.drawPoint(x, y, pathPaint);
		}
	}

	public void clearCanvas() {
		synchronized (lock) {
			pathToDraw.rewind();
			workingCanvas.drawColor(backgroundColor);
		}
	}

	private void doDraw(Canvas canvas) {
		workingCanvas.drawPath(pathToDraw, pathPaint);
		canvas.drawBitmap(workingBitmap, rectCanvas, rectCanvas, null);
	}

	@Override
	public void colorChanged(int color) {
		synchronized (lock) {
			pathPaint.setColor(color);
		}
	}

	@Override
	public void strokeChanged(int stroke) {
		synchronized (lock) {
			pathPaint.setStrokeWidth(stroke);
		}
	}
}
