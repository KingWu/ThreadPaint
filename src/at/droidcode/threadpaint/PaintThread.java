package at.droidcode.threadpaint;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.Log;
import android.view.SurfaceHolder;

public class PaintThread extends Thread {
	static final String TAG = "THREADPAINT";
	static final int BGCOLOR = Color.LTGRAY;

	private boolean keepRunning = false;
	private boolean getPathCalled = false;

	private Bitmap workingBitmap;
	private Canvas workingCanvas;
	private Path pathToDraw;
	private Paint pathPaint;

	private Rect rectCanvas;

	private final SurfaceHolder mSurfaceHolder;

	public PaintThread(SurfaceHolder surfaceHolder) {
		mSurfaceHolder = surfaceHolder;
		workingBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
		workingCanvas = new Canvas();
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
		clearCanvas();
		while (keepRunning) {
			Canvas canvas = null;
			try {
				synchronized (this) {
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
		synchronized (this) {
			workingBitmap = Bitmap.createScaledBitmap(workingBitmap, width, height, true);
			workingCanvas.setBitmap(workingBitmap);
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
		synchronized (this) {
			pathToDraw.rewind();
			workingCanvas.drawColor(BGCOLOR);
		}
	}

	private void doDraw(Canvas canvas) {
		workingCanvas.drawPath(pathToDraw, pathPaint);
		canvas.drawBitmap(workingBitmap, rectCanvas, rectCanvas, null);
	}
}
