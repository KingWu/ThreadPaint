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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import at.droidcode.threadpaint.api.ToolButtonAnimator;
import at.droidcode.threadpaint.dialog.BrushPickerDialog.OnBrushChangedListener;
import at.droidcode.threadpaint.dialog.ColorPickerDialog.OnPaintChangedListener;

/**
 * View that holds the surface onto which a user can draw. Has an OnTouchListener to turn user input into paths and
 * points on a canvas.
 */
public class PaintView extends SurfaceView implements SurfaceHolder.Callback, View.OnTouchListener {
	/**
	 * Observable to notify observers about changes to fields of the PaintView or PaintThread, e.g. to inform the
	 * ColorPickerDialog that the cap has changed from round to squared.
	 */
	private class SurfaceObservable extends Observable {
		@Override
		public boolean hasChanged() {
			return true;
		}
	}

	private PaintThread paintThread;
	private final Observable observable;
	private ToolButtonAnimator toolButtonAnimator;
	private float moveThreshold;

	private static final String STATE_WORKING_BITMAP = "WORKING_BITMAP";

	public PaintView(Context context, AttributeSet attrs) {
		super(context, attrs);
		Log.d(TAG, "PaintView created");

		observable = new SurfaceObservable();
		moveThreshold = 1.0f;

		SurfaceHolder holder = getHolder();
		holder.addCallback(this);

		setFocusable(true);
		setOnTouchListener(this);
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

		if (paintThread == null) {
			Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
			paintThread = new PaintThread(this, bitmap);
			paintThread.setDaemon(true);
			paintThread.fillBackground();
		}
		if (paintThread.isAlive()) {
			paintThread.setPaused(false);
		} else {
			paintThread.setRunning(true);
			paintThread.start();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.w(TAG, "surfaceDestroyed");

		if (paintThread != null) {
			paintThread.setPaused(true);
		}
	}

	/**
	 * Invokes getBitmap() to store a copy of the current Bitmap in the indicated Bundle.
	 * 
	 * @param b Bundle to store attributes in
	 */
	public synchronized void saveState(Bundle b) {
		Bitmap copy = getBitmap().copy(Bitmap.Config.ARGB_8888, true);
		b.putParcelable(PaintView.STATE_WORKING_BITMAP, copy);
	}

	/**
	 * If paintThread is null a new thread with the Bitmap stored in the Bundle is created. Typically called when
	 * ThreadPaintActivity is being restored after having been previously destroyed and thus the thread was terminated.
	 * 
	 * @param savedState Bundle containing saved attributes
	 */
	public synchronized void restoreState(Bundle savedState) {
		if (paintThread == null) {
			Bitmap saved = (Bitmap) savedState.getParcelable(STATE_WORKING_BITMAP);
			paintThread = new PaintThread(this, saved);
			paintThread.setDaemon(true);
		} else {
			Log.e(TAG, "ERROR: restoring state failed becuase thread was not null.");
		}
	}

	/**
	 * Stops the PaintThread and nulls the reference. Typically called when ThreadPaintActivity is being destroyed.
	 */
	public synchronized void terminatePaintThread() {
		Log.d(TAG, "terminatePaintThread");
		if (paintThread.isAlive()) {
			boolean retry = true;
			paintThread.setRunning(false);
			paintThread.setPaused(false);
			while (retry) {
				try {
					paintThread.join();
					retry = false;
				} catch (InterruptedException e) {
					Log.e(TAG, "ERROR ", e);
				}
			}
		}
		paintThread = null;
	}

	/**
	 * @param animator Object that animates the tool buttons
	 */
	public void setToolButtonAnimator(ToolButtonAnimator animator) {
		toolButtonAnimator = animator;
	}

	/**
	 * @param f Distance a user must drag her finger to create a path
	 */
	public void setMoveThreshold(float f) {
		moveThreshold = f;
	}

	/**
	 * @return Observable associated with this View
	 */
	public Observable getObservable() {
		return observable;
	}

	public Bitmap getBitmap() {
		return paintThread.getBitmap();
	}

	/**
	 * Set the Bitmap for the PainThread to draw.
	 * 
	 * @param bitmap The Bitmap the PaintThread will draw.
	 */
	public void setBitmap(Bitmap bitmap) {
		paintThread.setBitmap(bitmap);
	}

	/**
	 * @return OnPaintChangedListener, usually the PaintThread
	 */
	public OnPaintChangedListener getOnPaintChangedListener() {
		return paintThread;
	}

	/**
	 * @return OnBrushChangedListener, usually the PaintThread
	 */
	public OnBrushChangedListener getOnBrushChangedListener() {
		return paintThread;
	}

	/**
	 * Fills the whole Canvas with the current background color.
	 */
	public void fillWithBackgroundColor() {
		paintThread.fillBackground();
	}

	private float previousX;
	private float previousY;

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		float xTouchCoordinate = event.getX();
		float yTouchCoordinate = event.getY();

		final float dx = Math.abs(xTouchCoordinate - previousX);
		final float dy = Math.abs(yTouchCoordinate - previousY);

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			paintThread.startPath(xTouchCoordinate, yTouchCoordinate);
			toolButtonAnimator.fadeOutToolButtons();
			previousX = xTouchCoordinate;
			previousY = yTouchCoordinate;
			return true;
		case MotionEvent.ACTION_MOVE:
			if (dx > moveThreshold || dy > moveThreshold) {
				paintThread.updatePath(previousX, previousY, xTouchCoordinate, yTouchCoordinate);
			}
			previousX = xTouchCoordinate;
			previousY = yTouchCoordinate;
			return true;
		case MotionEvent.ACTION_UP:
			if (dx > moveThreshold || dy > moveThreshold) {
				paintThread.updatePath(previousX, previousY, xTouchCoordinate, yTouchCoordinate);
			} else {
				paintThread.drawPoint(xTouchCoordinate, yTouchCoordinate);
			}
			toolButtonAnimator.fadeInToolButtons();
			return true;
		default:
			return false;
		}
	}
}
