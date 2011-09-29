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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import at.droidcode.threadpaint.R;
import at.droidcode.threadpaint.Utils;

/**
 * A donut-shaped dial selector to choose any color. Based on an API Demo by The Android Open Source
 * Project.
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
	private final Paint framePaint;
	private final Paint checkeredPattern;
	private Shape shape;

	private boolean trackingCenter;
	private boolean highlightCenter;

	private final int indicatorFrameWidth;

	private ShapeClickedListener shapeClickedListener;

	enum Shape {
		CIRCLE, RECT
	};

	protected ShapeView(Context c, AttributeSet attrs) {
		super(c, attrs);

		shape = Shape.CIRCLE;

		shapeDiameter = Utils.dp2px(c, 100);
		shapeRadius = shapeDiameter / 2f;

		centerX = Utils.sreenWidthPx(c, 0.75f) / 2;

		indicatorFrameWidth = Utils.dp2px(c, 5);
		shapePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		framePaint.setStrokeWidth(indicatorFrameWidth);
		framePaint.setStyle(Style.STROKE);

		float r = Math.round(shapeRadius * 0.8f);
		rectFull = new RectF(-r, -r, r, r);
		r += indicatorFrameWidth;
		rectFrame = new RectF(-r, -r, r, r);

		Bitmap checkerboard = BitmapFactory
				.decodeResource(c.getResources(), R.drawable.transparent);
		BitmapShader shader = new BitmapShader(checkerboard, Shader.TileMode.REPEAT,
				Shader.TileMode.REPEAT);
		checkeredPattern = new Paint();
		checkeredPattern.setShader(shader);
	}

	final int getCenterX() {
		return centerX;
	}

	final float getShapeRadius() {
		return shapeRadius;
	}

	final boolean trackingCenter() {
		return trackingCenter;
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

	/**
	 * @param a [0..255]
	 */
	void setShapeColorAlpha(int a) {
		shapePaint.setAlpha(a);
		invalidate();
	}

	final void setShapeColor(int color) {
		shapePaint.setColor(color);
		framePaint.setColor(color);
		invalidate();
	}

	final int getShapeColor() {
		return shapePaint.getColor();
	}

	private static final int FULL = 0xFF;
	private static final int DIMMED = 0x88;

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.translate(centerX, centerX);

		// central shape
		if (shape == Shape.RECT) {
			canvas.drawRect(rectFull, checkeredPattern);
			canvas.drawRect(rectFull, shapePaint);
		} else {
			canvas.drawCircle(0, 0, shapeRadius, checkeredPattern);
			canvas.drawCircle(0, 0, shapeRadius, shapePaint);
		}

		// indicator frame for central shape
		if (trackingCenter) {
			if (!highlightCenter) {
				framePaint.setAlpha(DIMMED);
			} else {
				framePaint.setAlpha(FULL);
			}
			if (shape == Shape.RECT) {
				canvas.drawRect(rectFrame, framePaint);
			} else {
				canvas.drawCircle(0, 0, shapeRadius + framePaint.getStrokeWidth(), framePaint);
			}
		}
	}

	@Override
	protected final void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(centerX * 2, centerX * 2);
	}

	@Override
	public final boolean onTouchEvent(MotionEvent event) {
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
			if (trackingCenter && inCenter && shapeClickedListener != null) {
				shapeClickedListener.onShapeClicked();
			}
			trackingCenter = false;
			break;
		}
		handleMotionEvent(action, x, y);
		return true;
	}

	protected abstract void handleMotionEvent(int action, float x, float y);
}
