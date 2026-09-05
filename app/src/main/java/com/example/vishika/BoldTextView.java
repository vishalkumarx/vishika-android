package com.example.vishika;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.res.ResourcesCompat;

public class BoldTextView extends AppCompatTextView {
    public BoldTextView(Context context) {
        super(context);
        init(context);
    }

    public BoldTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BoldTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        if (!isInEditMode()) {
            Typeface typeface = ResourcesCompat.getFont(context, R.font.fredoka_bold);
            if (typeface != null) {
                setTypeface(typeface);
            }
        }
    }
}
