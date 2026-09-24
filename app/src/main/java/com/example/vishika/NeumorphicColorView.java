package com.example.vishika;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

public class NeumorphicColorView extends View {
    private int color;
    private Paint paintMain;
    private Paint paintOuterShadowDark;
    private Paint paintOuterShadowLight;
    private Paint paintInnerShadowDark;
    private Paint paintInnerShadowLight;
    
    public NeumorphicColorView(Context context) {
        super(context);
        init();
    }
    
    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null); // Required for shadows
        
        paintMain = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        paintOuterShadowDark = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintOuterShadowDark.setColor(Color.TRANSPARENT);
        paintOuterShadowDark.setShadowLayer(8f, 4f, 4f, Color.argb(153, 160, 160, 160));
        
        paintOuterShadowLight = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintOuterShadowLight.setColor(Color.TRANSPARENT);
        paintOuterShadowLight.setShadowLayer(8f, -4f, -4f, Color.argb(204, 255, 255, 255));
        
        paintInnerShadowDark = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintInnerShadowDark.setStyle(Paint.Style.STROKE);
        paintInnerShadowDark.setStrokeWidth(4f);
        
        paintInnerShadowLight = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintInnerShadowLight.setStyle(Paint.Style.STROKE);
        paintInnerShadowLight.setStrokeWidth(4f);
    }
    
    public void setColor(int color) {
        this.color = color;
        paintMain.setColor(color);
        
        // Inner shadow colors depend on the main color
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        
        // Darker inner shadow
        hsv[2] = Math.max(0f, hsv[2] - 0.2f);
        int darkColor = Color.HSVToColor(hsv);
        paintInnerShadowDark.setShadowLayer(4f, 2f, 2f, darkColor);
        paintInnerShadowDark.setColor(darkColor); // Stroke color
        
        // Lighter inner shadow
        Color.colorToHSV(color, hsv);
        hsv[2] = Math.min(1f, hsv[2] + 0.2f);
        int lightColor = Color.HSVToColor(hsv);
        paintInnerShadowLight.setShadowLayer(4f, -2f, -2f, lightColor);
        paintInnerShadowLight.setColor(lightColor); // Stroke color
        
        invalidate();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;
        
        float pad = 12f; // Padding for outer shadows
        RectF rect = new RectF(pad, pad, w - pad, h - pad);
        
        // Custom squircle path to match CSS border-radius: 41% 59% 45% 55% / 58% 44% 56% 42%
        Path path = new Path();
        float rw = rect.width();
        float rh = rect.height();
        
        // We will approximate the squircle using cubic beziers or just an oval for simplicity, 
        // but to match the CSS exactly, let's use a slightly distorted rounded rectangle.
        float[] radii = {
            rw * 0.41f, rh * 0.58f, // top left
            rw * 0.59f, rh * 0.44f, // top right
            rw * 0.55f, rh * 0.56f, // bottom right
            rw * 0.45f, rh * 0.42f  // bottom left
        };
        path.addRoundRect(rect, radii, Path.Direction.CW);
        
        // Draw outer shadows
        canvas.drawPath(path, paintOuterShadowDark);
        canvas.drawPath(path, paintOuterShadowLight);
        
        // Draw main color
        canvas.drawPath(path, paintMain);
        
        // Draw inner shadows by clipping
        canvas.save();
        canvas.clipPath(path);
        
        // Draw dark inner shadow
        canvas.drawPath(path, paintInnerShadowDark);
        
        // Draw light inner shadow
        canvas.drawPath(path, paintInnerShadowLight);
        
        canvas.restore();
    }
}
