package me.recycle;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by benge on 3/25/2018.
 */

public class Progress extends View {
    public Progress(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Rect rec = new Rect();
        rec.set(0, 0, R.id.prog, canvas.getHeight());

        Paint green = new Paint();
        green.setColor(Color.GREEN);
        green.setStyle(Paint.Style.FILL);

        canvas.drawRect(rec, green);
    }
}
