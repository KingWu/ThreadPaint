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
	private final PaintRunner paintRunner;
	private ToolButtonAnimator toolButtonAnimator;
	private static final String STATE_BITMAP = "WORKING_BITMAP";

	public PaintView(Context context, AttributeSet attrs) {
		super(context, attrs);
		Log.d(TAG, "PaintView created");

		SurfaceHolder holder = getHolder();
		holder.addCallback(this);

		setFocusable(true);
		setOnTouchListener(this);

		selectedTool = Tool.BRUSH;

		moveThreshold = 1.0f;

		paintRunner = new PaintRunner(this);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.w(TAG, "surfaceChanged");
		paintRunner.setSurfaceSize(width, height);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.w(TAG, "surfaceCreated");
		paintRunner.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.w(TAG, "surfaceDestroyed");
		paintRunner.setPaused(true);
	}

	/**
	 * Invokes getBitmap() to store a copy of the current Bitmap in the indicated Bundle.
	 * 
	 * @param b Bundle to store attributes in.
	 */
	public synchronized void saveState(Bundle b) {
		b.putParcelable(STATE_BITMAP, getBitmap());
	}

	/**
	 * @param savedState Bundle containing saved attributes.
	 */
	public synchronized void restoreState(Bundle savedState) {
		Log.d(TAG, "restore state");
		Bitmap savedBmp = (Bitmap) savedState.getParcelable(STATE_BITMAP);
		if (!savedBmp.isRecycled()) {
			paintRunner.setBitmap(savedBmp);
		}
	}

	/**
	 * Typically called when ThreadPaintActivity is being destroyed.
	 */
	public void stopPaintThread() {
		paintRunner.stop();
	}

	/**
	 * @param animator Object that animates the tool buttons.
	 */
	public void setToolButtonAnimator(ToolButtonAnimator animator) {
		toolButtonAnimator = animator;
	}

	/**
	 * @param f Distance a user must drag her finger to create a path.
	 */
	public void setMoveThreshold(float f) {
		moveThreshold = f;
	}

	/**
	 * @return Currently selected Tool.
	 */
	public Tool selectedTool() {
		return selectedTool;
	}

	/**
	 * @param tool Tool to use.
	 */
	public void selectTool(Tool tool) {
		selectedTool = tool;
	}

	/**
	 * @return Actual Bitmap that is drawn onto, a copy might be too big.
	 */
	public Bitmap getBitmap() {
		return paintRunner.getBitmap();
	}

	/**
	 * @return Paint currently in use.
	 */
	public Paint getPathPaint() {
		return paintRunner.getPaint();
	}

	/**
	 * @param bitmap The Bitmap to draw on.
	 */
	public synchronized void setBitmap(Bitmap bitmap) {
		Log.d(TAG, "setBitmap");
		paintRunner.setBitmap(bitmap);
	}

	/**
	 * @param color Color used to draw on the Bitmap.
	 */
	public void setPaintColor(int color) {
		paintRunner.colorChanged(color);
	}

	/**
	 * Fill the Bitmap with the currently used paint (and color).
	 */
	public void fillWithPaint() {
		paintRunner.fillWithPaint();
	}

	/**
	 * Create an empty new Canvas with reset perspective.
	 */
	public void resetCanvas() {
		paintRunner.resetCanvas();
	}

	/**
	 * Undo one step.
	 */
	public void undo() {
		paintRunner.undo();
	}

	/**
	 * Redo one step.
	 */
	public void redo() {
		paintRunner.redo();
	}

	/**
	 * @return OnPaintChangedListener, usually the PaintRunner.
	 */
	public OnPaintChangedListener getOnPaintChangedListener() {
		return paintRunner;
	}

	/**
	 * @return OnBrushChangedListener, usually the PaintRunner.
	 */
	public OnBrushChangedListener getOnBrushChangedListener() {
		return paintRunner;
	}

	private float xTouchCoordinate;
	private float yTouchCoordinate;
	private float previousX;
	private float previousY;
	private float oldDist;
	private boolean hasMoved;
	private boolean pinchToZoom;

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
		// always allow pinch to zoom
		handlePinchToZoom(event);
		// if zooming, don't handle tools
		if (!pinchToZoom) {
			switch (selectedTool) {
			case ERASE:
			case BRUSH:
				handleBrushTool(event);
				break;
			case MOVE:
				handleMoveTool(event);
				break;
			}
		}
		previousX = xTouchCoordinate;
		previousY = yTouchCoordinate;
		return true;
	}

	/**
	 * Draw path or point.
	 */
	private void handleBrushTool(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			paintRunner.startPath(xTouchCoordinate, yTouchCoordinate);
			toolButtonAnimator.fadeOutToolButtons();
			break;
		case MotionEvent.ACTION_MOVE:
			if (hasMoved) {
				paintRunner.updatePath(previousX, previousY, xTouchCoordinate, yTouchCoordinate);
			}
			break;
		case MotionEvent.ACTION_UP:
			if (hasMoved) {
				paintRunner.finishPath();
			} else {
				paintRunner.drawPoint(xTouchCoordinate, yTouchCoordinate);
			}
			toolButtonAnimator.fadeInToolButtons();
			break;
		}
	}

	/**
	 * @return Distance between two points.
	 */
	private float spacing(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return FloatMath.sqrt(x * x + y * y);
	}

	/**
	 * Scroll the picture.
	 */
	private void handleMoveTool(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_MOVE:
			int dx = Math.round(xTouchCoordinate - previousX);
			int dy = Math.round(yTouchCoordinate - previousY);
			paintRunner.scroll(dx, dy);
			break;
		}
	}

	/**
	 * Zoom into the picture or out of it.
	 */
	private void handlePinchToZoom(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_POINTER_2_DOWN:
			oldDist = spacing(event) / paintRunner.getZoom();
			pinchToZoom = true;
			break;
		case MotionEvent.ACTION_MOVE:
			if (pinchToZoom) {
				float newDist = spacing(event);
				if (newDist > 10) {
					float scale = newDist / oldDist;
					paintRunner.zoom(scale);
				}
			}
			break;
		default:
			pinchToZoom = false;
		}
	}
}
