package com.example.vishika;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.view.MotionEvent;
import android.view.View;

public class ColorWheelView extends View {
    private Paint huePaint;
    private Paint saturationPaint;
    private Paint thumbPaint;
    private Paint thumbBorderPaint;
    
    private int centerX, centerY, radius;
    private float selectedX, selectedY;
    private float hue = 0f;
    private float saturation = 1f;
    
    public interface OnColorChangeListener {
        void onColorChanged(int color);
    }
    
    private OnColorChangeListener listener;

    public ColorWheelView(Context context) {
        super(context);
        init();
    }
    
    private void init() {
        huePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        saturationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbPaint.setStyle(Paint.Style.FILL);
        
        thumbBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbBorderPaint.setStyle(Paint.Style.STROKE);
        thumbBorderPaint.setColor(Color.WHITE);
        thumbBorderPaint.setStrokeWidth(6f);
        thumbBorderPaint.setShadowLayer(4f, 0f, 2f, Color.BLACK);
        setLayerType(LAYER_TYPE_SOFTWARE, thumbBorderPaint);
    }
    
    public void setOnColorChangeListener(OnColorChangeListener listener) {
        this.listener = listener;
    }
    
    public void setColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hue = hsv[0];
        saturation = hsv[1];
        if (radius > 0) {
            updateThumbPositionFromHSV();
            invalidate();
        }
    }
    
    public int getCurrentColor() {
        return Color.HSVToColor(new float[]{hue, saturation, 1f});
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2;
        centerY = h / 2;
        radius = Math.min(centerX, centerY) - 30; // padding for thumb
        
        int[] hueColors = {Color.RED, Color.MAGENTA, Color.BLUE, Color.CYAN, Color.GREEN, Color.YELLOW, Color.RED};
        huePaint.setShader(new SweepGradient(centerX, centerY, hueColors, null));
        
        saturationPaint.setShader(new RadialGradient(centerX, centerY, radius, Color.WHITE, 0x00FFFFFF, Shader.TileMode.CLAMP));
        
        updateThumbPositionFromHSV();
    }
    
    private void updateThumbPositionFromHSV() {
        double angleRad = Math.toRadians(-hue); // SweepGradient draws clockwise, standard math is counter
        float dist = saturation * radius;
        selectedX = centerX + (float) (dist * Math.cos(angleRad));
        selectedY = centerY - (float) (dist * Math.sin(angleRad)); // Y is inverted in Android canvas
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        canvas.drawCircle(centerX, centerY, radius, huePaint);
        canvas.drawCircle(centerX, centerY, radius, saturationPaint);
        
        thumbPaint.setColor(getCurrentColor());
        canvas.drawCircle(selectedX, selectedY, 20f, thumbPaint);
        canvas.drawCircle(selectedX, selectedY, 20f, thumbBorderPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        
        float dx = x - centerX;
        float dy = y - centerY;
        float dist = (float) Math.hypot(dx, dy);
        
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
        }
        
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            if (dist > radius) {
                // Clamp to circle
                x = centerX + dx * radius / dist;
                y = centerY + dy * radius / dist;
                dist = radius;
            }
            
            selectedX = x;
            selectedY = y;
            
            saturation = dist / radius;
            double angleRad = Math.atan2(-dy, dx); // Y is inverted
            hue = (float) Math.toDegrees(angleRad);
            if (hue < 0) hue += 360f;
            
            invalidate();
            if (listener != null) {
                listener.onColorChanged(getCurrentColor());
            }
            return true;
        }
        return super.onTouchEvent(event);
    }
}
