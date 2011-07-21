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

package at.droidcode.threadpaint;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

class PaintView extends SurfaceView implements SurfaceHolder.Callback, View.OnTouchListener {
	static final String TAG = "THREADPAINT";

	static final float UPDATE_PATH_TOLERANCE = 2f;

	static Bitmap mBitmap;

	private PaintThread thread;

	class PaintThread extends Thread {
		private boolean keepRunning = false;
		private boolean getPathCalled = false;

		private Rect rectCanvas;

		private Path pathToDraw;
		private Canvas mCanvas;

		private Paint pathPaint;

		private SurfaceHolder mSurfaceHolder;

		public PaintThread(SurfaceHolder surfaceHolder) {
			mSurfaceHolder = surfaceHolder;
			pathToDraw = new Path();

			pathPaint = new Paint();
			pathPaint.setStyle(Paint.Style.STROKE);
			pathPaint.setAntiAlias(true);
			pathPaint.setDither(true);
			pathPaint.setColor(Color.RED);
			pathPaint.setStyle(Paint.Style.STROKE);
			pathPaint.setStrokeJoin(Paint.Join.ROUND);
			pathPaint.setStrokeCap(Paint.Cap.ROUND);
			pathPaint.setStrokeWidth(12);
		}

		@Override
		public void run() {
			while (keepRunning) {
				Canvas c = null;
				try {
					synchronized (thread) {
						c = mSurfaceHolder.lockCanvas();
						doDraw(c);
					}
				} finally {
					if (c != null) {
						mSurfaceHolder.unlockCanvasAndPost(c);
					}
				}
			}
		}

		public void setSurfaceSize(int width, int height, Rect rect) {
			synchronized (thread) {
				if (mBitmap == null) {
					mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
				} else {
					mBitmap = Bitmap.createScaledBitmap(mBitmap, width, height, true);
				}
				mCanvas = new Canvas();
				mCanvas.setBitmap(mBitmap);
				rectCanvas = rect;
			}
		}

		public void setRunning(boolean b) {
			keepRunning = b;
		}

		public void setPath(Path path) {
			if (!getPathCalled) {
				Log.e(TAG, "no getPath() prior to setPath()", new IllegalStateException());
			}
			getPathCalled = false;
			pathToDraw = path;
		}

		public Path getPath() {
			getPathCalled = true;
			return pathToDraw;
		}

		public void clearCanvas() {
			synchronized (thread) {
				pathToDraw.rewind();
				mCanvas.drawColor(Color.BLACK);
			}
		}

		private void doDraw(Canvas canvas) {
			//			Log.d(TAG, "doDraw");
			mCanvas.drawPath(pathToDraw, pathPaint);
			canvas.drawBitmap(mBitmap, rectCanvas, rectCanvas, null);
		}
	}

	public PaintView(Context context, AttributeSet attrs) {
		super(context, attrs);
		Log.w(TAG, "PaintView created");

		SurfaceHolder holder = getHolder();
		holder.addCallback(this);

		setFocusable(true);
		setOnTouchListener(this);
	}

	public PaintThread getThread() {
		return thread;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.d(TAG, "surfaceChanged");

		Rect rect = new Rect();
		rect.left = getLeft();
		rect.top = getTop();
		rect.right = getRight();
		rect.bottom = getBottom();

		thread.setSurfaceSize(width, height, rect);
	}

	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "surfaceCreated");
		thread = new PaintThread(holder);
		thread.setRunning(true);
		thread.start();
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG, "surfaceDestroyed");
		boolean retry = true;
		thread.setRunning(false);
		while (retry) {
			try {
				thread.join();
				retry = false;
			} catch (InterruptedException e) {
				Log.e(TAG, e.toString());
			}
		}
	}

	private float previousX = 0f;
	private float previousY = 0f;
	private boolean openPath = false;

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		synchronized (thread) {
			float xTouchCoordinate = event.getX();
			float yTouchCoordinate = event.getY();

			if (previousX == 0 && previousY == 0) {
				previousX = xTouchCoordinate;
				previousY = yTouchCoordinate;
			}

			float dx = Math.abs(xTouchCoordinate - previousX);
			float dy = Math.abs(yTouchCoordinate - previousY);

			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					startPath(xTouchCoordinate, yTouchCoordinate);
					return true;
				case MotionEvent.ACTION_MOVE:
					if (dx < UPDATE_PATH_TOLERANCE && dy < UPDATE_PATH_TOLERANCE) {
						return false;
					}
					updatePath(xTouchCoordinate, yTouchCoordinate);
					previousX = xTouchCoordinate;
					previousY = yTouchCoordinate;
					return true;
				case MotionEvent.ACTION_UP:
					if (openPath && dx != 0f && dy != 0f) {
						updatePath(xTouchCoordinate, yTouchCoordinate);
					}
					endPath();
					return true;
				default:
					return false;
			}
		}
	}

	private void startPath(float x, float y) {
		Log.d(TAG, "startPath x: " + x + " y: " + y);
		final Path path = thread.getPath();
		path.rewind();
		path.moveTo(x, y);
		thread.setPath(path);
	}

	private void updatePath(float x2, float y2) {
		openPath = true;
		float x1 = (previousX + x2) / 2;
		float y1 = (previousY + y2) / 2;
		Log.d(TAG, "updatePath x1: " + x1 + " y1: " + y1 + " x2: " + x2 + " y2: " + y2);
		final Path path = thread.getPath();
		path.quadTo(x1, y1, x2, y2);
		thread.setPath(path);
	}

	private void endPath() {
		Log.d(TAG, "endPath");
		previousX = 0f;
		previousY = 0f;
		openPath = false;
	}
}
