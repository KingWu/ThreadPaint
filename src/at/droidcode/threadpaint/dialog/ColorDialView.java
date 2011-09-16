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
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import at.droidcode.threadpaint.R;

/**
 * A donut-shaped dial selector to choose any color. Based on an API Demo by The Android Open Source Project.
 */
public class ColorDialView extends ShapeView {
	private final RectF ovalRect;
	private final Paint gradientPaint;
	private int[] colorSpectrum;

	public ColorDialView(Context c, AttributeSet attrs) {
		super(c, attrs);

		gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		gradientPaint.setStyle(Paint.Style.STROKE);
		gradientPaint.setStrokeWidth(getShapeRadius() / 2);

		float r = getCenterX() - (gradientPaint.getStrokeWidth() / 2);
		ovalRect = new RectF(-r, -r, r, r);

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
			colorArray = getContext().getResources().obtainTypedArray(R.array.grey_spectrum);
			setShapeColor(Color.WHITE);
		} else {
			colorArray = getContext().getResources().obtainTypedArray(R.array.color_spectrum);
			final int color = getContext().getResources().getColor(R.color.stroke_standard);
			setShapeColor(color);
		}
		colorSpectrum = new int[colorArray.length()];
		for (int i = 0; i < colorArray.length(); i++) {
			colorSpectrum[i] = colorArray.getColor(i, Color.BLACK);
		}
		colorArray.recycle();

		gradientPaint.setShader(new SweepGradient(0, 0, colorSpectrum, null));
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		canvas.drawOval(ovalRect, gradientPaint);
	}

	private static int ave(int s, int d, float p) {
		return s + java.lang.Math.round(p * (d - s));
	}

	private int interpColor(float unit) {
		if (unit <= 0) {
			return colorSpectrum[0];
		}
		if (unit >= 1) {
			return colorSpectrum[colorSpectrum.length - 1];
		}

		float p = unit * (colorSpectrum.length - 1);
		int i = (int) p;
		p -= i;

		// now p is just the fractional part [0...1) and i is the index
		int c0 = colorSpectrum[i];
		int c1 = colorSpectrum[i + 1];
		int a = ave(Color.alpha(c0), Color.alpha(c1), p);
		int r = ave(Color.red(c0), Color.red(c1), p);
		int g = ave(Color.green(c0), Color.green(c1), p);
		int b = ave(Color.blue(c0), Color.blue(c1), p);

		return Color.argb(a, r, g, b);
	}

	private static final float PI = 3.1415926f;

	@Override
	protected void handleMotionEvent(int action, float x, float y) {
		if (action == MotionEvent.ACTION_MOVE && !trackingCenter()) {
			float angle = (float) java.lang.Math.atan2(y, x);
			// need to turn angle [-PI ... PI] into unit [0....1]
			float unit = angle / (2 * PI);
			if (unit < 0) {
				unit += 1;
			}
			super.setShapeColor(interpColor(unit));
			invalidate();
		}
	}
}
