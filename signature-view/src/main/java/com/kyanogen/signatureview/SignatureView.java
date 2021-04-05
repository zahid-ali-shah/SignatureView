package com.kyanogen.signatureview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.kyanogen.signatureview.model.Point;

import java.io.ByteArrayOutputStream;

public class SignatureView extends View {

    public static final String TAG = SignatureView.class.getSimpleName();
    private Canvas canvasBmp;
    private boolean ignoreTouch;
    private Point previousPoint, startPoint, currentPoint;
    public static final float MIN_PEN_SIZE = 1f;
    private static final float MIN_INCREMENT = 0.01f;
    private static final float INCREMENT_CONSTANT = 0.0005f;
    private static final float DRAWING_CONSTANT = 0.0085f;
    public static final float MAX_VELOCITY_BOUND = 15f;
    private static final float MIN_VELOCITY_BOUND = 1.6f;
    private static final float STROKE_DES_VELOCITY = 1.0f;
    private static final float VELOCITY_FILTER_WEIGHT = 0.2f;
    private float lastVelocity, lastWidth;
    private Paint paint, paintBm;
    private Bitmap bmp;
    private int layoutLeft, layoutTop, layoutRight, layoutBottom;
    private Rect drawViewRect;
    private int penColor, backgroundColor;
    private boolean enableSignature;
    private float penSize;
    private Context context;

    @SuppressWarnings("deprecation")
    public SignatureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.setWillNotDraw(false);
        this.setDrawingCacheEnabled(true);

        TypedArray typedArray = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.signature, 0, 0);

        try {
            backgroundColor = typedArray.getColor(R.styleable.signature_backgroundColor,
                    context.getResources().getColor(R.color.white));
            penColor = typedArray.getColor(R.styleable.signature_penColor,
                    context.getResources().getColor(R.color.penRoyalBlue));
            penSize = typedArray.getDimension(R.styleable.signature_penSize,
                    context.getResources().getDimension(R.dimen.pen_size));
            enableSignature = typedArray.getBoolean(R.styleable.signature_enableSignature, true);
        } finally {
            typedArray.recycle();
        }

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(penColor);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(penSize);

        paintBm = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBm.setAntiAlias(true);
        paintBm.setStyle(Paint.Style.STROKE);
        paintBm.setStrokeJoin(Paint.Join.ROUND);
        paintBm.setStrokeCap(Paint.Cap.ROUND);
        paintBm.setColor(Color.BLACK);
    }

    /**************** Getter/Setter *****************/

    /**
     * Get stoke size for signature creation
     *
     * @return float
     */
    public float getPenSize() {
        return penSize;
    }

    /**
     * Set stoke size for signature creation
     *
     * @param penSize float
     */
    public void setPenSize(float penSize) {
        this.penSize = penSize;
    }

    /**
     * Check if drawing on canvas is enabled or disabled
     *
     * @return boolean
     */
    public boolean isEnableSignature() {
        return enableSignature;
    }

    /**
     * Enable or disable drawing on canvas
     *
     * @param enableSignature boolean
     */
    public void setEnableSignature(boolean enableSignature) {
        this.enableSignature = enableSignature;
    }

    /**
     * Get stoke color for signature creation
     *
     * @return int
     */
    public int getPenColor() {
        return penColor;
    }

    /**
     * Set stoke color for signature creation
     *
     * @param penColor int
     */
    public void setPenColor(int penColor) {
        this.penColor = penColor;
        paint.setColor(penColor);
    }

    /**
     * Get background color
     *
     * @return int
     */
    public int getBackgroundColor() {
        return backgroundColor;
    }


    /**
     * Set background color
     *
     * @param backgroundColor int
     */
    @Override
    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    /**
     * Clear signature from canvas
     */
    public void clearCanvas() {
        previousPoint = null;
        startPoint = null;
        currentPoint = null;
        lastVelocity = 0;
        lastWidth = 0;

        newBitmapCanvas(layoutLeft, layoutTop, layoutRight, layoutBottom);
        postInvalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        layoutLeft = left;
        layoutTop = top;
        layoutRight = right;
        layoutBottom = bottom;
        if (bmp == null) {
            newBitmapCanvas(layoutLeft, layoutTop, layoutRight, layoutBottom);
        } else if (changed) {
            resizeBitmapCanvas(bmp, layoutLeft, layoutTop, layoutRight, layoutBottom);
        }
    }

    private void newBitmapCanvas(int left, int top, int right, int bottom) {
        bmp = null;
        canvasBmp = null;
        if ((right - left) > 0 && (bottom - top) > 0) {
            bmp = Bitmap.createBitmap(right - left, bottom - top, Bitmap.Config.ARGB_8888);
            canvasBmp = new Canvas(bmp);
            canvasBmp.drawColor(backgroundColor);
        }
    }

    private void resizeBitmapCanvas(Bitmap bmp, int left, int top, int right, int bottom) {
        int newBottom = Math.max(bottom, bmp.getHeight());
        int newRight = Math.max(right, bmp.getWidth());
        newBitmapCanvas(left, top, newRight, newBottom);
        canvasBmp.drawBitmap(bmp, 0, 0, null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnableSignature()) {
            return false;
        }

        if (event.getPointerCount() > 1) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                ignoreTouch = false;
                drawViewRect = new Rect(this.getLeft(), this.getTop(), this.getRight(),
                        this.getBottom());
                onTouchDownEvent(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                if (!drawViewRect.contains(getLeft() + (int) event.getX(),
                        this.getTop() + (int) event.getY())) {
                    //You are out of drawing area
                    if (!ignoreTouch) {
                        ignoreTouch = true;
                        onTouchUpEvent(event.getX(), event.getY());
                    }
                } else {
                    //You are in the drawing area
                    if (ignoreTouch) {
                        ignoreTouch = false;
                        onTouchDownEvent(event.getX(), event.getY());
                    } else {
                        onTouchMoveEvent(event.getX(), event.getY());
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                onTouchUpEvent(event.getX(), event.getY());
                break;
            default:
                break;
        }
        return true;// super.onTouchEvent(event);
    }

    private void onTouchDownEvent(float x, float y) {
        previousPoint = null;
        startPoint = null;
        currentPoint = null;
        lastVelocity = 0;
        lastWidth = penSize;

        currentPoint = new Point(x, y, System.currentTimeMillis());
        previousPoint = currentPoint;
        startPoint = previousPoint;
        postInvalidate();
    }

    private void onTouchMoveEvent(float x, float y) {
        if (previousPoint == null) {
            return;
        }
        startPoint = previousPoint;
        previousPoint = currentPoint;
        currentPoint = new Point(x, y, System.currentTimeMillis());

        float velocity = currentPoint.velocityFrom(previousPoint);
        velocity = VELOCITY_FILTER_WEIGHT * velocity + (1 - VELOCITY_FILTER_WEIGHT) * lastVelocity;

        float strokeWidth = getStrokeWidth(velocity);
        drawLine(lastWidth, strokeWidth, velocity);

        lastVelocity = velocity;
        lastWidth = strokeWidth;

        postInvalidate();
    }

    private void onTouchUpEvent(float x, float y) {
        if (previousPoint == null) {
            return;
        }
        startPoint = previousPoint;
        previousPoint = currentPoint;
        currentPoint = new Point(x, y, System.currentTimeMillis());

        drawLine(lastWidth, 0, lastVelocity);
        postInvalidate();
    }

    private float getStrokeWidth(float velocity) {
        return penSize - (velocity * STROKE_DES_VELOCITY);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(bmp, 0, 0, paintBm);
    }

    private void drawLine(final float lastWidth, final float currentWidth,
            final float velocity) {
        final Point mid1 = midPoint(previousPoint, startPoint);
        final Point mid2 = midPoint(currentPoint, previousPoint);

        draw(mid1, previousPoint, mid2, lastWidth,
                currentWidth, velocity);
    }

    private float getPt(float n1, float n2, float perc) {
        float diff = n2 - n1;
        return n1 + (diff * perc);
    }

    private void draw(Point p0, Point p1, Point p2, float lastWidth,
            float currentWidth, float velocity) {
        if (canvasBmp != null) {
            float xa, xb, ya, yb, x, y;
            float increment;
            if (velocity > MIN_VELOCITY_BOUND && velocity < MAX_VELOCITY_BOUND) {
                increment = DRAWING_CONSTANT - (velocity * INCREMENT_CONSTANT);
            } else {
                increment = MIN_INCREMENT;
            }

            for (float i = 0f; i < 1f; i += increment) {
                xa = getPt(p0.x, p1.x, i);
                ya = getPt(p0.y, p1.y, i);
                xb = getPt(p1.x, p2.x, i);
                yb = getPt(p1.y, p2.y, i);

                x = getPt(xa, xb, i);
                y = getPt(ya, yb, i);

                float strokeVal = lastWidth + (currentWidth - lastWidth) * (i);
                paint.setStrokeWidth(strokeVal < MIN_PEN_SIZE ? MIN_PEN_SIZE : strokeVal);
                canvasBmp.drawPoint(x, y, paint);
            }
        }
    }

    private Point midPoint(Point p1, Point p2) {
        return new Point((p1.x + p2.x) / 2.0f, (p1.y + p2.y) / 2, (p1.time + p2.time) / 2);
    }

    /**
     * Get signature as bitmap
     *
     * @return Bitmap
     */
    public Bitmap getSignatureBitmap() {
        if (bmp != null) {
            return Bitmap.createScaledBitmap(bmp, bmp.getWidth(), bmp.getHeight(), true);
        } else {
            return null;
        }
    }

    private Bitmap getSignatureBitmap(Bitmap bitmap) {
        return Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);
    }

    /**
     * Render bitmap in signature
     *
     * @param bitmap Bitmap
     */
    public void setBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            bmp = bitmap;
            canvasBmp = new Canvas(bitmap);
            postInvalidate();
        }
    }

    /**
     * Check is signature bitmap empty
     *
     * @return boolean
     */
    public boolean isBitmapEmpty() {
        if (bmp != null) {
            Bitmap emptyBitmap = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(),
                    bmp.getConfig());
            Canvas canvasBmp = new Canvas(emptyBitmap);
            canvasBmp.drawColor(backgroundColor);
            if (bmp.sameAs(emptyBitmap)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get library version name
     *
     * @return String
     */
    public String getVersionName() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, bmp);
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if(!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState)state;
        super.onRestoreInstanceState(ss.getSuperState());
        setBitmap(ss.bitmap);
    }

    static class SavedState extends BaseSavedState {

        Bitmap bitmap;

        SavedState(Parcelable superState, Bitmap bitmap) {
            super(superState);
            this.bitmap = bitmap;
        }

        private SavedState(Parcel in) {
            super(in);
            bitmap = deCompress(in.createByteArray());
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeByteArray(compress(bitmap));
        }

        private static byte[] compress(Bitmap bitmap){
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            return stream.toByteArray();
        }

        private static Bitmap deCompress(byte[] byteArray) {
            return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
