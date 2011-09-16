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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import at.droidcode.threadpaint.Utils;

/**
 * A donut-shaped dial selector to choose any color. Based on an API Demo by The Android Open Source Project.
 */
public abstract class ShapeView extends View {
	interface ShapeClickedListener {
		void onShapeClicked();
	}

	private final int centerX;

	private float shapeDiameter;
	private float shapeRadius;

	private final RectF rectFull;
	private final RectF rectFrame;

	private final Paint shapePaint;
	private Shape shape;

	private boolean trackingCenter;
	private boolean highlightCenter;

	private final int indicatorFrameWidth;

	private ShapeClickedListener shapeClickedListener;

	enum Shape {
		CIRCLE, RECT
	};

	ShapeView(Context c, AttributeSet attrs) {
		super(c, attrs);

		shape = Shape.CIRCLE;

		shapeDiameter = 100f;
		shapeRadius = shapeDiameter / 2f;

		centerX = (int) (shapeDiameter * 1.5);

		indicatorFrameWidth = Utils.dp2px(c, 5);
		shapePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		shapePaint.setStrokeWidth(indicatorFrameWidth);

		float r = Math.round(shapeRadius * 0.8f);
		rectFull = new RectF(-r, -r, r, r);
		r += indicatorFrameWidth;
		rectFrame = new RectF(-r, -r, r, r);
	}

	final void setShapeClickedListener(ShapeClickedListener l) {
		shapeClickedListener = l;
	}

	final void setShape(Shape s) {
		shape = s;
		invalidate();
	}

	final void setShapeDiameter(float d) {
		shapeDiameter = d;
		shapeRadius = shapeDiameter / 2f;
		shapeDimensionsChanged();
	}

	final void setShapeRadius(float r) {
		shapeRadius = r;
		shapeDiameter = shapeRadius * 2f;
		shapeDimensionsChanged();
	}

	private void shapeDimensionsChanged() {
		float r = Math.round(shapeRadius * 0.8f);
		rectFull.set(-r, -r, r, r);
		r += indicatorFrameWidth;
		rectFrame.set(-r, -r, r, r);
		invalidate();
	}

	final void setShapeColor(int color) {
		shapePaint.setColor(color);
		invalidate();
	}

	final Paint getShapePaint() {
		return shapePaint;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.translate(centerX, centerX);

		// central shape
		if (shape == Shape.RECT) {
			canvas.drawRect(rectFull, shapePaint);
		} else {
			canvas.drawCircle(0, 0, shapeRadius, shapePaint);
		}

		// indicator frame for central shape
		if (trackingCenter) {
			shapePaint.setStyle(Paint.Style.STROKE);

			if (!highlightCenter) {
				shapePaint.setAlpha(0x80);
			}
			if (shape == Shape.RECT) {
				canvas.drawRect(rectFrame, shapePaint);
			} else {
				canvas.drawCircle(0, 0, shapeRadius + shapePaint.getStrokeWidth(), shapePaint);
			}
			// reset the paint for solid shape
			shapePaint.setStyle(Paint.Style.FILL);
			shapePaint.setAlpha(0xFF);
		}
	}

	@Override
	protected final void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(centerX * 2, centerX * 2);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		final int action = event.getAction();
		final float x = event.getX() - centerX;
		final float y = event.getY() - centerX;
		final boolean inCenter = Math.sqrt(x * x + y * y) <= shapeRadius;

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			trackingCenter = inCenter;
			if (inCenter) {
				highlightCenter = true;
				invalidate();
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (highlightCenter != inCenter) {
				highlightCenter = inCenter;
				invalidate();
			}
			break;
		case MotionEvent.ACTION_UP:
			if (trackingCenter && inCenter) {
				if (shapeClickedListener != null) {
					shapeClickedListener.onShapeClicked();
				}
			}
			trackingCenter = false;
			break;
		}
		return handleMotionEvent(action, x, y);
	}

	protected abstract boolean handleMotionEvent(int action, float x, float y);
}
