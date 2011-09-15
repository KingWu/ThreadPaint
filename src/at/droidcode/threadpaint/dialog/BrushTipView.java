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

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import at.droidcode.threadpaint.R;
import at.droidcode.threadpaint.TpApplication;
import at.droidcode.threadpaint.Utils;

public class BrushTipView extends View {
	private final int centerX;
	private final int centerY;
	private final int stdCenterRadius;
	private int activeCenterRadius;
	private Cap centerShape;
	private final RectF squareRect;

	private final Paint centerPaint;
	private Dialog parentDialog;

	public BrushTipView(Context c, AttributeSet attrs) {
		super(c, attrs);

		final int maxStrokeWidth = ((TpApplication) c.getApplicationContext()).maxStrokeWidth();

		centerX = maxStrokeWidth - (maxStrokeWidth / 6);
		centerY = centerX;
		stdCenterRadius = (maxStrokeWidth / 2) / 2;
		activeCenterRadius = stdCenterRadius;
		centerShape = Cap.ROUND;

		centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		centerPaint.setStrokeWidth(Utils.dp2px(c, 5));

		squareRect = new RectF();

		final int color = getContext().getResources().getColor(R.color.stroke_standard);
		centerPaint.setColor(color);
	}

	/**
	 * @param d Dialog containing this view
	 */
	void setParentDialog(Dialog d) {
		parentDialog = d;
	}

	/**
	 * @param color Color of the central brush shape.
	 */
	void setCenterColor(int color) {
		centerPaint.setColor(color);
		invalidate();
	}

	/**
	 * @param radius Radius of the central circle, representing the stroke width.
	 */
	void setCenterRadius(int radius) {
		activeCenterRadius = radius;
		invalidate();
	}

	void setCenterShape(Cap cap) {
		centerShape = cap;
		invalidate();
	}

	Paint getCenterPaint() {
		return centerPaint;
	}

	private boolean trackingCenter;
	private boolean highlightCenter;

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.translate(centerX, centerX);

		if (centerShape == Cap.ROUND) {
			canvas.drawCircle(0, 0, activeCenterRadius, centerPaint);
		} else {
			squareRect.set(-activeCenterRadius, -activeCenterRadius, activeCenterRadius, activeCenterRadius);
			canvas.drawRect(squareRect, centerPaint);
		}

		if (trackingCenter) {
			int c = centerPaint.getColor();
			centerPaint.setStyle(Paint.Style.STROKE);

			if (highlightCenter) {
				centerPaint.setAlpha(0xFF);
			} else {
				centerPaint.setAlpha(0x80);
			}
			canvas.drawCircle(0, 0, activeCenterRadius + centerPaint.getStrokeWidth(), centerPaint);

			centerPaint.setStyle(Paint.Style.FILL);
			centerPaint.setColor(c);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(centerX * 2, centerY * 2);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float x = event.getX() - centerX;
		float y = event.getY() - centerY;
		boolean inCenter = java.lang.Math.sqrt(x * x + y * y) <= stdCenterRadius;

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			trackingCenter = inCenter;
			if (inCenter) {
				highlightCenter = true;
				invalidate();
				break;
			}
		case MotionEvent.ACTION_MOVE:
			if (trackingCenter && highlightCenter != inCenter) {
				highlightCenter = inCenter;
				invalidate();
			}
			break;
		case MotionEvent.ACTION_UP:
			if (trackingCenter) {
				if (inCenter) {
					parentDialog.dismiss();
				}
				trackingCenter = false; // so we draw w/o halo
				invalidate();
			}
			break;
		}
		return true;
	}
}
