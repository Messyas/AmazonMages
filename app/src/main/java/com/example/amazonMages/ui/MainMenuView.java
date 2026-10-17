package com.example.amazonMages.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.amazonMages.gamepanel.AtlasMenuButtonDrawable;
import com.example.amazonMages.gamepanel.PixelSceneDrawable;
import com.example.androidstudio2dgamedevelopment.R;

/** Title screen laid out from the measured safe content area, not full display metrics. */
public final class MainMenuView extends ViewGroup {
    private final View background;
    private final View shade;
    private final ImageView logo;
    private final TextView subtitle;
    private final MenuButton[] actions;
    private float scale;
    private int gap;
    private int logoGap;
    private int actionGap;
    private int contentHeight;

    public MainMenuView(Context context, OnClickListener play, OnClickListener options, OnClickListener exit) {
        super(context);
        setBackgroundColor(0xFF10241A);
        int inset = dp(12);
        setPadding(inset, inset, inset, inset);
        setClipToPadding(false);
        // Activity decor already excludes the navigation bar and landscape display cutout.
        // Keep future system-window fitting inside this view's measured bounds as well.
        setFitsSystemWindows(true);
        background = new View(context);
        background.setBackground(new PixelSceneDrawable(context));
        background.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(background);
        shade = new View(context);
        shade.setBackgroundColor(0x240B1A12);
        shade.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(shade);
        logo = new ImageView(context);
        logo.setImageResource(R.drawable.ui_logo);
        logo.setContentDescription("Amazon Mages");
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        addView(logo);
        Typeface font = getResources().getFont(R.font.pixelify_sans);
        subtitle = new TextView(context);
        subtitle.setText("Sua aventura na floresta começa aqui");
        subtitle.setTextColor(0xFFF4EBC8);
        subtitle.setTypeface(font);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setIncludeFontPadding(false);
        subtitle.setMaxLines(2);
        addView(subtitle);
        actions = new MenuButton[] {
            new MenuButton(context, "Jogar", font, play),
            new MenuButton(context, "Opções", font, options),
            new MenuButton(context, "Sair", font, exit)
        };
        for (MenuButton button : actions) addView(button);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int height = MeasureSpec.getSize(heightSpec);
        setMeasuredDimension(width, height);
        int availableWidth = Math.max(1, width - getPaddingLeft() - getPaddingRight());
        int availableHeight = Math.max(1, height - getPaddingTop() - getPaddingBottom());
        scale = Math.min(availableWidth / 1144f, availableHeight / 510f);
        float density = getResources().getDisplayMetrics().density;
        int buttonHeight = Math.max(dp(48), Math.round(56 * scale));
        int buttonWidth = Math.min(availableWidth, Math.max(dp(200), Math.round(320 * scale)));
        gap = Math.max(dp(8), Math.round(14 * scale));
        logoGap = Math.round(14 * scale);
        actionGap = Math.round(34 * scale);
        measureExactly(background, width, height);
        measureExactly(shade, width, height);
        measureExactly(logo, Math.min(availableWidth, Math.round(420 * scale)), Math.round(90 * scale));
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20 * scale / density);
        subtitle.setShadowLayer(Math.max(1, 2 * scale), 0, Math.max(1, scale), 0xFF1B1008);
        subtitle.measure(MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        for (MenuButton button : actions) {
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24 * scale / density);
            button.setArtScale(Math.max(1, Math.round(buttonHeight / 20f)));
            measureExactly(button, buttonWidth, buttonHeight);
        }
        contentHeight = logo.getMeasuredHeight() + logoGap + subtitle.getMeasuredHeight()
            + actionGap + 3 * buttonHeight + 2 * gap;
    }

    private void measureExactly(View child, int width, int height) {
        child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = r - l;
        int height = b - t;
        background.layout(0, 0, width, height);
        shade.layout(0, 0, width, height);
        int availableHeight = height - getPaddingTop() - getPaddingBottom();
        int y = getPaddingTop() + Math.max(0,
            Math.min(Math.round(70 * scale), availableHeight - contentHeight));
        int center = getPaddingLeft() + (width - getPaddingLeft() - getPaddingRight()) / 2;
        centerChild(logo, center, y);
        y += logo.getMeasuredHeight() + logoGap;
        centerChild(subtitle, center, y);
        y += subtitle.getMeasuredHeight() + actionGap;
        for (MenuButton button : actions) {
            centerChild(button, center, y);
            y += button.getMeasuredHeight() + gap;
        }
    }

    private void centerChild(View child, int center, int y) {
        int left = center - child.getMeasuredWidth() / 2;
        child.layout(left, y, left + child.getMeasuredWidth(), y + child.getMeasuredHeight());
    }

    private static final class MenuButton extends Button {
        private final AtlasMenuButtonDrawable artwork;
        private int artScale = 2;

        MenuButton(Context context, String label, Typeface font, OnClickListener action) {
            super(context);
            setText(label);
            setTypeface(font);
            setAllCaps(false);
            setIncludeFontPadding(false);
            setGravity(Gravity.CENTER);
            setPadding(0, 0, 0, 0);
            setMinWidth(0);
            setMinimumWidth(0);
            setStateListAnimator(null);
            setBackgroundTintList(null);
            artwork = new AtlasMenuButtonDrawable(context);
            setBackground(artwork);
            setTextColor(new ColorStateList(new int[][] {
                {-android.R.attr.state_enabled}, {}
            }, new int[] {0xFFB8C99A, 0xFFF4EBC8}));
            setShadowLayer(1, 0, 1, Color.rgb(27, 16, 8));
            setOnClickListener(action);
        }

        void setArtScale(int scale) {
            artScale = scale;
            artwork.setPixelScale(scale);
        }

        @Override protected void onDraw(Canvas canvas) {
            canvas.save();
            if (isEnabled() && isPressed()) canvas.translate(0, artScale);
            super.onDraw(canvas);
            canvas.restore();
        }
    }
}
