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
 * 
 * This file incorporates work covered by the following copyright:
 * 
 *     Copyright (C) 2007 The Android Open Source Project
 *
 */

package at.droidcode.threadpaint.dialog;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import at.droidcode.threadpaint.R;
import at.droidcode.threadpaint.TpApplication;
import at.droidcode.threadpaint.Utils;
import at.droidcode.threadpaint.dialog.ColorPickerDialog.OnPaintChangedListener;

/**
 * A donut-shaped dial selector to choose any color. Based on an API Demo by The Android Open Source Project.
 */
public class ColorDialView extends View {
	private final Context context;
	private final int centerX;
	private final int centerY;
	private final int centerRadius;
	private Cap centerShape;
	private final RectF ovalRect;
	private final RectF fullSquare;
	private final RectF frameSquare;

	private final Paint gradientPaint;
	private final Paint centerPaint;
	private int[] colorSpectrum;
	private OnPaintChangedListener paintListener;

	public ColorDialView(Context c, AttributeSet attrs) {
		super(c, attrs);
		context = c;

		final int maxStrokeWidth = ((TpApplication) context.getApplicationContext()).maxStrokeWidth();
		centerX = Math.round(maxStrokeWidth * 0.85f);
		centerY = centerX;
		centerRadius = maxStrokeWidth / 2;
		centerShape = Cap.ROUND;

		int highlightWidth = Utils.dp2px(context, 5);
		centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		centerPaint.setStrokeWidth(highlightWidth);

		gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		gradientPaint.setStyle(Paint.Style.STROKE);
		gradientPaint.setStrokeWidth(centerRadius / 2);

		float r = centerX - centerRadius * 0.25f;
		ovalRect = new RectF(-r, -r, r, r);

		r = Math.round(centerRadius * 0.8f);
		fullSquare = new RectF(-r, -r, r, r);

		r = Math.round(centerRadius * 0.8f) + highlightWidth;
		frameSquare = new RectF(-r, -r, r, r);

		setGreyscale(false);
	}

	/**
	 * Switches the color palette from colour to greyscale
	 * 
	 * @param b Greyscale if true, rgb colors if false
	 */
	final void setGreyscale(boolean b) {
		final TypedArray colorArray;
		if (b) {
			colorArray = context.getResources().obtainTypedArray(R.array.grey_spectrum);
			centerPaint.setColor(Color.WHITE);
		} else {
			colorArray = context.getResources().obtainTypedArray(R.array.color_spectrum);
			final int color = getContext().getResources().getColor(R.color.stroke_standard);
			centerPaint.setColor(color);
		}
		colorSpectrum = new int[colorArray.length()];
		for (int i = 0; i < colorArray.length(); i++) {
			colorSpectrum[i] = colorArray.getColor(i, Color.BLACK);
		}
		colorArray.recycle();

		gradientPaint.setShader(new SweepGradient(0, 0, colorSpectrum, null));
		invalidate();
	}

	/**
	 * @param l OnPaintChangedListener to be called when the color or stroke width changes.
	 */
	void setOnPaintChangedListener(OnPaintChangedListener l) {
		paintListener = l;
	}

	/**
	 * @param color Color to be active when the dial is shown for the first time.
	 */
	void setInitalColor(int color) {
		centerPaint.setColor(color);
	}

	Paint getCenterPaint() {
		return centerPaint;
	}

	void setCenterShape(Cap cap) {
		centerShape = cap;
		invalidate();
	}

	private boolean trackingCenter;
	private boolean highlightCenter;

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.translate(centerX, centerY);

		// color spectrum gradient
		canvas.drawOval(ovalRect, gradientPaint);

		// central solid shape
		if (centerShape == Cap.ROUND) {
			canvas.drawCircle(0, 0, centerRadius, centerPaint);
		} else {
			canvas.drawRect(fullSquare, centerPaint);
		}

		// highlight for central solid shape
		if (trackingCenter) {
			centerPaint.setStyle(Paint.Style.STROKE);

			if (!highlightCenter) {
				centerPaint.setAlpha(0x80);
			}
			if (centerShape == Cap.ROUND) {
				canvas.drawCircle(0, 0, centerRadius + centerPaint.getStrokeWidth(), centerPaint);
			} else {
				canvas.drawRect(frameSquare, centerPaint);
			}
			centerPaint.setStyle(Paint.Style.FILL);
			centerPaint.setAlpha(0xFF);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(centerX * 2, centerY * 2);
	}

	private static int ave(int s, int d, float p) {
		return s + java.lang.Math.round(p * (d - s));
	}

	private int interpColor(int colors[], float unit) {
		if (unit <= 0) {
			return colors[0];
		}
		if (unit >= 1) {
			return colors[colors.length - 1];
		}

		float p = unit * (colors.length - 1);
		int i = (int) p;
		p -= i;

		// now p is just the fractional part [0...1) and i is the index
		int c0 = colors[i];
		int c1 = colors[i + 1];
		int a = ave(Color.alpha(c0), Color.alpha(c1), p);
		int r = ave(Color.red(c0), Color.red(c1), p);
		int g = ave(Color.green(c0), Color.green(c1), p);
		int b = ave(Color.blue(c0), Color.blue(c1), p);

		return Color.argb(a, r, g, b);
	}

	private static final float PI = 3.1415926f;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float x = event.getX() - centerX;
		float y = event.getY() - centerY;
		boolean inCenter = java.lang.Math.sqrt(x * x + y * y) <= centerRadius;

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			trackingCenter = inCenter;
			if (inCenter) {
				highlightCenter = true;
				invalidate();
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (!trackingCenter) {
				float angle = (float) java.lang.Math.atan2(y, x);
				// need to turn angle [-PI ... PI] into unit [0....1]
				float unit = angle / (2 * PI);
				if (unit < 0) {
					unit += 1;
				}
				centerPaint.setColor(interpColor(colorSpectrum, unit));
				invalidate();
			}
			if (highlightCenter != inCenter) {
				highlightCenter = inCenter;
				invalidate();
			}
			break;
		case MotionEvent.ACTION_UP:
			if (trackingCenter) {
				if (inCenter) {
					paintListener.colorChanged(centerPaint.getColor());
				}
				trackingCenter = false; // so we draw w/o halo
			}
			break;
		}
		return true;
	}
}
