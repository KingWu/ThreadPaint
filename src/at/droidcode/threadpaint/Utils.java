package at.droidcode.threadpaint;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

public class Utils {
	public static int dp2px(Context context, int dp) {
		final Resources r = context.getResources();
		float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
		return Math.round(px);
	}
}
