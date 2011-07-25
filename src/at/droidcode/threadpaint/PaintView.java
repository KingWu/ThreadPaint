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

	private PaintThread paintThread;

	public PaintView(Context context, AttributeSet attrs) {
		super(context, attrs);
		Log.d(TAG, "PaintView created");

		SurfaceHolder holder = getHolder();
		holder.addCallback(this);

		setFocusable(true);
		setOnTouchListener(this);
	}

	public PaintThread getThread() {
		return paintThread;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.w(TAG, "surfaceChanged");

		Rect rect = new Rect();
		rect.left = getLeft();
		rect.top = getTop();
		rect.right = getRight();
		rect.bottom = getBottom();

		paintThread.setSurfaceSize(width, height, rect);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.w(TAG, "surfaceCreated");
		paintThread = new PaintThread(holder);
		paintThread.setRunning(true);
		paintThread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.w(TAG, "surfaceDestroyed");
		boolean retry = true;
		paintThread.setRunning(false);
		while (retry) {
			try {
				paintThread.join();
				retry = false;
			} catch (InterruptedException e) {
				Log.e(TAG, e.toString());
			}
		}
		paintThread = null;
	}

	private float previousX = 0f;
	private float previousY = 0f;
	private boolean openPath = false;

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		synchronized (paintThread) {
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
		final Path path = paintThread.getPath();
		path.rewind();
		path.moveTo(x, y);
		paintThread.setPath(path);
	}

	private void updatePath(float x2, float y2) {
		openPath = true;
		float x1 = (previousX + x2) / 2;
		float y1 = (previousY + y2) / 2;
		Log.d(TAG, "updatePath x1: " + x1 + " y1: " + y1 + " x2: " + x2 + " y2: " + y2);
		final Path path = paintThread.getPath();
		path.quadTo(x1, y1, x2, y2);
		paintThread.setPath(path);
	}

	private void endPath() {
		Log.d(TAG, "endPath");
		previousX = 0f;
		previousY = 0f;
		openPath = false;
	}
}
