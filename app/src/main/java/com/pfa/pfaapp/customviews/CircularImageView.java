package com.pfa.pfaapp.customviews;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.pfa.pfaapp.R;


/**
 * Custom ImageView for circular images in Android while maintaining the best draw performance and supporting custom
 * borders & selectors.
 */
public class CircularImageView extends CustomNetworkImageView {
    // For logging purposes
    private static final String TAG = CircularImageView.class.getSimpleName();

    // Border & Selector configuration variables
    private boolean hasBorder;
    private boolean hasSelector;
    private boolean isSelected;
    private int borderWidth;
    private int canvasSize;
    private int selectorStrokeWidth;

    // Objects used for the actual drawing
    private BitmapShader shader;
    private Bitmap image;
    private Paint paint;
    private Paint paintBorder;
    private Paint paintSelectorBorder;
    private ColorFilter selectorFilter;

    public CircularImageView(Context context) {
        this(context, null, R.styleable.CircularImageViewStyle_circularImageViewDefault);
    }

    public CircularImageView(Context context, AttributeSet attrs) {
        this(context, attrs, R.styleable.CircularImageViewStyle_circularImageViewDefault);
    }

    public CircularImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    /**
     * Initializes paint objects and sets desired attributes.
     *
     * @param context  Context
     * @param attrs    Attributes
     * @param defStyle Default Style
     */
    private void init(Context context, AttributeSet attrs, int defStyle) {
        // Initialize paint objects
        paint = new Paint();
        paint.setAntiAlias(true);
        paintBorder = new Paint();
        paintBorder.setAntiAlias(true);
        paintSelectorBorder = new Paint();
        paintSelectorBorder.setAntiAlias(true);

        // Attempt applying shadow layers
        applyShadow();

        // load the styled attributes and set their properties
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.CircularImageView, defStyle, 0);

        // Check if border and/or border is enabled
        hasBorder = attributes.getBoolean(R.styleable.CircularImageView_civ_border, false);
        hasSelector = attributes.getBoolean(R.styleable.CircularImageView_civ_selector, false);

        // Set border properties if enabled
        if (hasBorder) {
            int defaultBorderSize = (int) (2 * context.getResources().getDisplayMetrics().density + 0.5f);
            setBorderWidth(attributes.getDimensionPixelOffset(R.styleable.CircularImageView_civ_border_width, defaultBorderSize));
            setBorderColor(attributes.getColor(R.styleable.CircularImageView_civ_border_color, Color.WHITE));
        }

        // Set selector properties if enabled
        if (hasSelector) {
            int defaultSelectorSize = (int) (2 * context.getResources().getDisplayMetrics().density + 0.5f);
            setSelectorColor(attributes.getColor(R.styleable.CircularImageView_civ_selector_color, Color.TRANSPARENT));
            setSelectorStrokeWidth(attributes.getDimensionPixelOffset(R.styleable.CircularImageView_civ_selector_stroke_width, defaultSelectorSize));
            setSelectorStrokeColor(attributes.getColor(R.styleable.CircularImageView_civ_selector_stroke_color, Color.BLUE));
        }

        // Add shadow if enabled
        if (attributes.getBoolean(R.styleable.CircularImageView_civ_shadow, false))
            setShadow(true);

        // We no longer need our attributes TypedArray, give it back to cache
        attributes.recycle();
    }

    /**
     * Sets the CircularImageView's border width in pixels.
     *
     * @param borderWidth Width in pixels for the border.
     */
    public void setBorderWidth(int borderWidth) {
        this.borderWidth = borderWidth;
        this.requestLayout();
        this.invalidate();
    }

    /**
     * Sets the CircularImageView's basic border color.
     *
     * @param borderColor The new color (including alpha) to set the border.
     */
    public void setBorderColor(int borderColor) {
        if (paintBorder != null)
            paintBorder.setColor(borderColor);
        this.invalidate();
    }

    /**
     * Sets the color of the selector to be draw over the CircularImageView. Be sure to provide some opacity.
     *
     * @param selectorColor The color (including alpha) to set for the selector overlay.
     */
    public void setSelectorColor(int selectorColor) {
        this.selectorFilter = new PorterDuffColorFilter(selectorColor, PorterDuff.Mode.SRC_ATOP);
        this.invalidate();
    }

    /**
     * Sets the stroke width to be drawn around the CircularImageView during click events when the selector is enabled.
     *
     * @param selectorStrokeWidth Width in pixels for the selector stroke.
     */
    public void setSelectorStrokeWidth(int selectorStrokeWidth) {
        this.selectorStrokeWidth = selectorStrokeWidth;
        this.requestLayout();
        this.invalidate();
    }

    /**
     * Sets the stroke color to be drawn around the CircularImageView during click events when the selector is enabled.
     *
     * @param selectorStrokeColor The color (including alpha) to set for the selector stroke.
     */
    public void setSelectorStrokeColor(int selectorStrokeColor) {
        if (paintSelectorBorder != null)
            paintSelectorBorder.setColor(selectorStrokeColor);
        this.invalidate();
    }

    /**
     * Enables a dark shadow for this CircularImageView.
     *
     * @param shadowEnabled Set to true to render a shadow or false to disable it.
     */
    public void setShadow(boolean shadowEnabled) {
        if (shadowEnabled) {
            // paint.setShadowLayer(4.0f, 0.0f, 2.0f, Color.BLACK);
            paintBorder.setShadowLayer(4.0f, 0.0f, 2.0f, Color.BLACK);
            paintSelectorBorder.setShadowLayer(4.0f, 0.0f, 2.0f, Color.BLACK);
        } else {
            // paint.setShadowLayer(0, 0.0f, 2.0f, Color.BLACK);
            paintBorder.setShadowLayer(0, 0.0f, 2.0f, Color.BLACK);
            paintSelectorBorder.setShadowLayer(0, 0.0f, 2.0f, Color.BLACK);
        }
    }


    @Override
    public void onDraw(Canvas canvas) {
        // Don't draw anything without an image
        if (image == null)
            return;

        // Nothing to draw (Empty bounds)
        if (image.getHeight() == 0 || image.getWidth() == 0)
            return;

        // We'll need this later
        int oldCanvasSize = canvasSize;

        canvasSize = getWidth() < getHeight() ? getWidth() : getHeight();

        // Reinitialize shader, if necessary
        if (oldCanvasSize != canvasSize)
            updateBitmapShader();

        // Apply shader to paint
        paint.setShader(shader);

        // Keep track of selectorStroke/border width
        int outerWidth = 0;

        // Get the exact X/Y axis of the view
        int center = canvasSize / 2;

        if (hasSelector && isSelected) { // Draw the selector stroke & apply the selector filter, if applicable
            outerWidth = selectorStrokeWidth;
            center = (canvasSize - (outerWidth * 2)) / 2;

            paint.setColorFilter(selectorFilter);
            canvas.drawCircle(center + outerWidth, center + outerWidth, ((canvasSize - (outerWidth * 2)) >> 1) + outerWidth - 4.0f, paintSelectorBorder);
        } else if (hasBorder) { // If no selector was drawn, draw a border and clear the filter instead... if enabled   >> is used instead of / (divider)
            outerWidth = borderWidth;
            center = (canvasSize - (outerWidth * 2)) / 2;

            paint.setColorFilter(null);
            canvas.drawCircle(center + outerWidth, center + outerWidth, ((canvasSize - (outerWidth << 1)) >> 1) + outerWidth - 4.0f, paintBorder);
        } else
            // Clear the color filter if no selector nor border were drawn
            paint.setColorFilter(null);

        // Draw the circular image itself
        canvas.drawCircle(center + outerWidth, center + outerWidth, ((canvasSize - (outerWidth * 2)) / 2) - 4.0f, paint);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Check for clickable state and do nothing if disabled
        if (!this.isClickable()) {
            this.isSelected = false;
            return super.onTouchEvent(event);
        }

        // Set selected state based on Motion Event
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                this.isSelected = true;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_SCROLL:
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_CANCEL:
                this.isSelected = false;
                break;
        }

        // Redraw image and return super type
        this.invalidate();
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void setImageURI(Uri uri) {
        super.setImageURI(uri);

        // Extract a Bitmap out of the drawable & set it as the main shader
        image = drawableToBitmap(getDrawable());
        if (canvasSize > 0)
            updateBitmapShader();
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);

        // Extract a Bitmap out of the drawable & set it as the main shader
        image = drawableToBitmap(getDrawable());
        if (canvasSize > 0)
            updateBitmapShader();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);

        // Extract a Bitmap out of the drawable & set it as the main shader
        image = drawableToBitmap(getDrawable());
        if (canvasSize > 0)
            updateBitmapShader();
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);

        // Extract a Bitmap out of the drawable & set it as the main shader
        image = bm;
        if (canvasSize > 0)
            updateBitmapShader();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = measureWidth(widthMeasureSpec);
        int height = measureHeight(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    private int measureWidth(int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // The parent has determined an exact size for the child.
            result = specSize;
        } else if (specMode == MeasureSpec.AT_MOST) {
            // The child can be as large as it wants up to the specified size.
            result = specSize;
        } else {
            // The parent has not imposed any constraint on the child.
            result = canvasSize;
        }

        return result;
    }

    private int measureHeight(int measureSpecHeight) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpecHeight);
        int specSize = MeasureSpec.getSize(measureSpecHeight);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else if (specMode == MeasureSpec.AT_MOST) {
            // The child can be as large as it wants up to the specified size.
            result = specSize;
        } else {
            // Measure the text (beware: ascent is a negative number)
            result = canvasSize;
        }

        return (result + 2);
    }

    /**
     * Disable this view's hardware acceleration on Honeycomb and up, as long as edit mode is disabled. (Required for
     * shadow effect)
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void applyShadow() {
        setLayerType(LAYER_TYPE_SOFTWARE, paintBorder);
    }

    /**
     * Convert a drawable object into a Bitmap.
     *
     * @param drawable Drawable to extract a Bitmap from.
     * @return A Bitmap created from the drawable parameter.
     */
    public Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable == null) // Don't do anything without a proper drawable
            return null;
        else if (drawable instanceof BitmapDrawable) { // Use the getBitmap() method instead if BitmapDrawable
            Log.i(TAG, "Bitmap drawable!");
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();

        if (!(intrinsicWidth > 0 && intrinsicHeight > 0))
            return null;

        try {
            // Create Bitmap object out of the drawable
            Bitmap bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } catch (OutOfMemoryError e) {
            // Simply return null of failed bitmap creations
            Log.e(TAG, "Encountered OutOfMemoryError while generating bitmap!");
            return null;
        }
    }

    /**
     * Re-initializes the shader texture used to fill in the Circle upon drawing.
     */
    public void updateBitmapShader() {
        if (image == null)
            return;

        shader = new BitmapShader(image, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        if (canvasSize != image.getWidth() || canvasSize != image.getHeight()) {
            Matrix matrix = new Matrix();
            float scale = (float) canvasSize / (float) image.getWidth();
            matrix.setScale(scale, scale);
            shader.setLocalMatrix(matrix);
        }
    }

    /**
     * @return Whether or not this view is currently in its selected state.
     */
    public boolean isSelected() {
        return this.isSelected;
    }
}