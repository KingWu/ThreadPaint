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
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import at.droidcode.threadpaint.api.ToolButtonAnimator;
import at.droidcode.threadpaint.dialog.BrushPickerDialog.OnBrushChangedListener;
import at.droidcode.threadpaint.dialog.ColorPickerDialog.OnPaintChangedListener;

/**
 * View that holds the surface onto which a user can draw. Has an OnTouchListener to turn user input
 * into paths and points on a canvas.
 */
public class PaintView extends SurfaceView implements SurfaceHolder.Callback, View.OnTouchListener {
	public enum Tool {
		BRUSH, MOVE, ERASE
	};

	private Tool selectedTool;
	private float moveThreshold;
	private PaintThread paintThread;
	private ToolButtonAnimator toolButtonAnimator;
	private static final String STATE_WORKING_BITMAP = "WORKING_BITMAP";

	public PaintView(Context context, AttributeSet attrs) {
		super(context, attrs);
		Log.d(TAG, "PaintView created");

		SurfaceHolder holder = getHolder();
		holder.addCallback(this);

		setFocusable(true);
		setOnTouchListener(this);

		selectedTool = Tool.BRUSH;

		moveThreshold = 1.0f;

		Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
		paintThread = new PaintThread(this);
		paintThread.setDaemon(true);
		paintThread.setBitmap(bitmap);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.w(TAG, "surfaceChanged");

		paintThread.setSurfaceSize(width, height);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.w(TAG, "surfaceCreated");

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
			Log.d(TAG, "setPaused true");
			paintThread.setPaused(true);
		}
	}

	/**
	 * Invokes getBitmap() to store a copy of the current Bitmap in the indicated Bundle.
	 * 
	 * @param b Bundle to store attributes in
	 */
	public synchronized void saveState(Bundle b) {
		b.putParcelable(STATE_WORKING_BITMAP, getBitmap());
	}

	/**
	 * If paintThread is null a new thread with the Bitmap stored in the Bundle is created.
	 * Typically called when ThreadPaintActivity is being restored after having been previously
	 * destroyed and thus the thread was terminated.
	 * 
	 * @param savedState Bundle containing saved attributes
	 */
	public synchronized void restoreState(Bundle savedState) {
		if (paintThread != null) {
			terminatePaintThread();
		}
		Bitmap savedBmp = (Bitmap) savedState.getParcelable(STATE_WORKING_BITMAP);
		paintThread = new PaintThread(this);
		paintThread.setDaemon(true);
		paintThread.setBitmap(savedBmp);
	}

	/**
	 * Stops the PaintThread and nulls the reference. Typically called when ThreadPaintActivity is
	 * being destroyed.
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

	public Tool selectedTool() {
		return selectedTool;
	}

	public void selectTool(Tool tool) {
		selectedTool = tool;
	}

	/**
	 * @return Actual Bitmap that is drawn onto, a copy might be to big.
	 */
	public Bitmap getBitmap() {
		return paintThread.getBitmap();
	}

	/**
	 * @return Paint the PaintThread is drawing the path with
	 */
	public Paint getPathPaint() {
		return paintThread.getPaint();
	}

	/**
	 * Set the Bitmap for the PaintThread to draw.
	 * 
	 * @param bitmap The Bitmap the PaintThread will draw.
	 */
	public void setBitmap(Bitmap bitmap) {
		paintThread.setBitmap(bitmap);
	}

	/**
	 * @param color Color used to draw on the Bitmap.
	 */
	public void setPaintColor(int color) {
		paintThread.colorChanged(color);
	}

	/**
	 * Fill the Bitmap with the currently used paint (and color).
	 */
	public void fillWithPaint() {
		paintThread.fillWithPaint();
	}

	/**
	 * Create an empty new Canvas with reset perspective.
	 */
	public void resetCanvas() {
		paintThread.resetCanvas();
	}

	public void undo() {
		paintThread.undo();
	}

	public void redo() {
		paintThread.redo();
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

	private float xTouchCoordinate;
	private float yTouchCoordinate;
	private float previousX;
	private float previousY;
	private boolean hasMoved;

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		xTouchCoordinate = event.getX();
		yTouchCoordinate = event.getY();

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			previousX = xTouchCoordinate;
			previousY = yTouchCoordinate;
			hasMoved = false;
			break;
		case MotionEvent.ACTION_MOVE:
			float dx = Math.abs(xTouchCoordinate - previousX);
			float dy = Math.abs(yTouchCoordinate - previousY);
			if (dx > moveThreshold || dy > moveThreshold) {
				hasMoved = true;
			}
			break;
		}
		switch (selectedTool) {
		case ERASE:
		case BRUSH:
			handleBrushTool(event);
			break;
		case MOVE:
			handleMoveTool(event);
			break;
		}
		previousX = xTouchCoordinate;
		previousY = yTouchCoordinate;
		return true;
	}

	private void handleBrushTool(MotionEvent event) {

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			paintThread.startPath(xTouchCoordinate, yTouchCoordinate);
			toolButtonAnimator.fadeOutToolButtons();
			break;
		case MotionEvent.ACTION_MOVE:
			if (hasMoved) {
				paintThread.updatePath(previousX, previousY, xTouchCoordinate, yTouchCoordinate);
			}
			break;
		case MotionEvent.ACTION_UP:
			if (hasMoved) {
				paintThread.finishPath();
			} else {
				paintThread.drawPoint(xTouchCoordinate, yTouchCoordinate);
			}
			toolButtonAnimator.fadeInToolButtons();
			break;
		}
	}

	private boolean pinchToZoom;
	private float oldDist;

	private float spacing(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return FloatMath.sqrt(x * x + y * y);
	}

	private void handleMoveTool(MotionEvent event) {

		switch (event.getAction()) {
		case MotionEvent.ACTION_POINTER_2_DOWN:
			oldDist = spacing(event) / paintThread.getZoom();
			pinchToZoom = true;
			break;
		case MotionEvent.ACTION_MOVE:
			if (pinchToZoom) {
				float newDist = spacing(event);
				if (newDist > 10) {
					float scale = newDist / oldDist;
					paintThread.zoom(scale);
				}
			} else {
				int dx = Math.round(xTouchCoordinate - previousX);
				int dy = Math.round(yTouchCoordinate - previousY);
				paintThread.scroll(dx, dy);
			}
			break;
		default:
			pinchToZoom = false;
			break;
		}
	}
}
