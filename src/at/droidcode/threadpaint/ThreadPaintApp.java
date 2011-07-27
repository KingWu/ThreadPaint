package at.droidcode.threadpaint;

import android.app.Application;
import android.view.Display;
import android.view.WindowManager;

public class ThreadPaintApp extends Application {
	private static final int MAX_STROKE_WIDTH_DP_VERT = 150;
	private static final int MAX_STROKE_WIDTH_DP_HORZ = 125;

	private int maxStrokeWidthPx;

	@Override
	public void onCreate() {
		super.onCreate();

		final Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
		if (display.getOrientation() == 0) {
			maxStrokeWidthPx = Utils.dp2px(getApplicationContext(), MAX_STROKE_WIDTH_DP_VERT);
		} else {
			maxStrokeWidthPx = Utils.dp2px(getApplicationContext(), MAX_STROKE_WIDTH_DP_HORZ);
		}
	}

	public int maxStrokeWidth() {
		return maxStrokeWidthPx;
	}
}
