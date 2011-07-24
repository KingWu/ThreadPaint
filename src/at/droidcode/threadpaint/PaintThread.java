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
	
	private static Bitmap workingBitmap;

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
				synchronized (this) {
					c = mSurfaceHolder.lockCanvas();
					doDraw(c);
				}
			} finally {
				if (c != null) {
					mSurfaceHolder.unlockCanvasAndPost(c);
				}
			}
		}
//		workingBitmap.recycle();
	}
	
	public void setSurfaceSize(int width, int height, Rect rect) {
		synchronized (this) {
			if (workingBitmap == null) {
				workingBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			} else {
				workingBitmap = Bitmap.createScaledBitmap(workingBitmap, width, height, true);
			}
			mCanvas = new Canvas();
			mCanvas.setBitmap(workingBitmap);
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
			mCanvas.drawColor(Color.BLACK);
		}
	}

	private void doDraw(Canvas canvas) {
		//			Log.d(TAG, "doDraw");
		mCanvas.drawPath(pathToDraw, pathPaint);
		canvas.drawBitmap(workingBitmap, rectCanvas, rectCanvas, null);
	}
}
