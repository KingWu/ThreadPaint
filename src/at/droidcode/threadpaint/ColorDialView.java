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
 * This file incorporates work covered by the same license and the following copyright:
 * 
 *     Copyright (C) 2007 The Android Open Source Project
 *
 */

package at.droidcode.threadpaint;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import at.droidcode.threadpaint.ColorPickerDialog.OnColorChangedListener;

public class ColorDialView extends View {
	static final String TAG = "THREADPAINT";

	private final int centerX;
	private final int centerY;
	private final int stdCenterRadius;
	private int activeCenterRadius;

	private final Paint mPaint;
	private final Paint mCenterPaint;
	private final int[] colorSpectrum;
	private OnColorChangedListener mListener;

	public ColorDialView(Context context, AttributeSet attrs) {
		super(context, attrs);

		final TypedArray colorArray = context.getResources().obtainTypedArray(R.array.color_spectrum);
		colorSpectrum = new int[colorArray.length()];
		for (int i = 0; i < colorArray.length(); i++) {
			colorSpectrum[i] = colorArray.getColor(i, Color.RED);
		}
		colorArray.recycle();
		Shader s = new SweepGradient(0, 0, colorSpectrum, null);

		centerX = PaintView.dp2px(context, 125);
		centerY = centerX;
		stdCenterRadius = (PaintView.maxStrokeWidth() / 2) / 2;
		activeCenterRadius = stdCenterRadius;

		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaint.setShader(s);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeWidth(stdCenterRadius);

		mCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mCenterPaint.setColor(Color.BLACK);
		mCenterPaint.setStrokeWidth(PaintView.dp2px(context, 5));
	}

	public void setOnColorChangedListener(OnColorChangedListener l) {
		mListener = l;
	}

	public void setInitalColor(int color) {
		mCenterPaint.setColor(color);
	}

	public void setCenterRadius(int radius) {
		activeCenterRadius = radius;
		invalidate();
	}

	private boolean mTrackingCenter;
	private boolean mHighlightCenter;

	@Override
	protected void onDraw(Canvas canvas) {
		float r = centerX - mPaint.getStrokeWidth() * 0.5f;

		canvas.translate(centerX, centerX);

		canvas.drawOval(new RectF(-r, -r, r, r), mPaint);
		canvas.drawCircle(0, 0, activeCenterRadius, mCenterPaint);

		if (mTrackingCenter) {
			int c = mCenterPaint.getColor();
			mCenterPaint.setStyle(Paint.Style.STROKE);

			if (mHighlightCenter) {
				mCenterPaint.setAlpha(0xFF);
			} else {
				mCenterPaint.setAlpha(0x80);
			}
			canvas.drawCircle(0, 0, activeCenterRadius + mCenterPaint.getStrokeWidth(), mCenterPaint);

			mCenterPaint.setStyle(Paint.Style.FILL);
			mCenterPaint.setColor(c);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(centerX * 2, centerY * 2);
	}

	private int ave(int s, int d, float p) {
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
		boolean inCenter = java.lang.Math.sqrt(x * x + y * y) <= stdCenterRadius;

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mTrackingCenter = inCenter;
			if (inCenter) {
				mHighlightCenter = true;
				invalidate();
				break;
			}
		case MotionEvent.ACTION_MOVE:
			if (mTrackingCenter) {
				if (mHighlightCenter != inCenter) {
					mHighlightCenter = inCenter;
					invalidate();
				}
			} else {
				float angle = (float) java.lang.Math.atan2(y, x);
				// need to turn angle [-PI ... PI] into unit [0....1]
				float unit = angle / (2 * PI);
				if (unit < 0) {
					unit += 1;
				}
				mCenterPaint.setColor(interpColor(colorSpectrum, unit));
				invalidate();
			}
			break;
		case MotionEvent.ACTION_UP:
			if (mTrackingCenter) {
				if (inCenter) {
					if (mListener == null) {
						Log.e(TAG, "Error: No onColorChanged listener set!");
					} else {
						mListener.colorChanged(mCenterPaint.getColor());
					}
				}
				mTrackingCenter = false; // so we draw w/o halo
				invalidate();
			}
			break;
		}
		return true;
	}
}
