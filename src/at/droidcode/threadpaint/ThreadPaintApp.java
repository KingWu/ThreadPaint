package at.droidcode.threadpaint;

import android.app.Application;

public class ThreadPaintApp extends Application {
	private static final int MAX_STROKE_WIDTH_DP = 150;
	private int maxStrokeWidthPx = 150;

	@Override
	public void onCreate() {
		super.onCreate();

		maxStrokeWidthPx = Utils.dp2px(getApplicationContext(), MAX_STROKE_WIDTH_DP);

	}

	public int maxStrokeWidth() {
		return maxStrokeWidthPx;
	}
}
