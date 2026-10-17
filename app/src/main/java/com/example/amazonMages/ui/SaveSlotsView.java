package com.example.amazonMages.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.amazonMages.gamepanel.AtlasMenuButtonDrawable;
import com.example.amazonMages.gamepanel.AtlasPixelDrawable;
import com.example.amazonMages.gamepanel.PixelSceneDrawable;
import com.example.amazonMages.save.PlayerPositionEntity;
import com.example.androidstudio2dgamedevelopment.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Three save cards sized from the safe landscape content area. */
public final class SaveSlotsView extends ViewGroup {
    private final View background;
    private final View shade;
    private final TextView title;
    private final SlotCard[] cards = new SlotCard[3];
    private final MenuButton back;
    private final Typeface font;
    private final PlayerPositionEntity[] saves;
    private final int[] cardLevels;
    private float scale;

    public SaveSlotsView(Context context, PlayerPositionEntity[] slots,
            int[] levels, View.OnClickListener onStart, View.OnClickListener onDelete,
            View.OnClickListener onBack) {
        super(context);
        setBackgroundColor(0xFF10241A);
        int inset = dp(12);
        setPadding(inset, inset, inset, inset);
        setClipToPadding(false);
        setFitsSystemWindows(true);
        font = getResources().getFont(R.font.pixelify_sans);
        saves = slots.clone();
        cardLevels = levels.clone();
        background = new View(context);
        background.setBackground(new PixelSceneDrawable(context));
        addView(background);
        shade = new View(context);
        shade.setBackgroundColor(0xA90B1A12);
        addView(shade);
        title = label("Suas Jogatinas", 30, 0xFFF2D36B);
        addView(title);
        for (int i = 0; i < cards.length; i++) {
            PlayerPositionEntity save = slots[i];
            cards[i] = new SlotCard(context, i + 1, save, levels[i], onStart, onDelete);
            addView(cards[i]);
        }
        back = new MenuButton(context, "Voltar", font, onBack);
        addView(back);
    }

    private TextView label(String text, float size, int color) {
        TextView view = new TextView(getContext());
        view.setText(text);
        view.setTypeface(font);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER);
        view.setIncludeFontPadding(false);
        return view;
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec), height = MeasureSpec.getSize(heightSpec);
        setMeasuredDimension(width, height);
        int availableWidth = Math.max(1, width - getPaddingLeft() - getPaddingRight());
        int availableHeight = Math.max(1, height - getPaddingTop() - getPaddingBottom());
        scale = Math.min(availableWidth / 1144f, availableHeight / 510f);
        measureExactly(background, width, height);
        measureExactly(shade, width, height);
        measureExactly(title, Math.round(700 * scale), Math.round(48 * scale));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30 * scale / getResources().getDisplayMetrics().density);
        int gap = Math.round(18 * scale);
        int cardWidth = Math.min(Math.round(340 * scale), (availableWidth - 2 * gap) / 3);
        int cardHeight = Math.min(Math.round(326 * scale), Math.round(348 * scale));
        for (SlotCard card : cards) card.measure(
            MeasureSpec.makeMeasureSpec(cardWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(cardHeight, MeasureSpec.EXACTLY));
        back.setArtScale(Math.max(1, Math.round(20 * scale / 20f)));
        back.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20 * scale / getResources().getDisplayMetrics().density);
        measureExactly(back, Math.round(152 * scale), Math.max(dp(48), Math.round(48 * scale)));
    }

    private void measureExactly(View view, int width, int height) {
        view.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    public void updateSlotLevel(int slot, int level) {
        if (slot < 1 || slot > cards.length || saves[slot - 1] == null) return;
        cardLevels[slot - 1] = Math.max(1, level);
        cards[slot - 1].updateLevel(cardLevels[slot - 1]);
    }

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = r - l, height = b - t;
        background.layout(0, 0, width, height);
        shade.layout(0, 0, width, height);
        int left = getPaddingLeft(), top = getPaddingTop();
        int availableWidth = width - getPaddingLeft() - getPaddingRight();
        title.layout(left, top + Math.round(24 * scale), left + title.getMeasuredWidth(),
            top + Math.round(24 * scale) + title.getMeasuredHeight());
        int gap = Math.round(18 * scale);
        int groupWidth = cards.length * cards[0].getMeasuredWidth() + 2 * gap;
        int x = left + (availableWidth - groupWidth) / 2;
        int y = top + Math.round(92 * scale);
        for (SlotCard card : cards) {
            card.layout(x, y, x + card.getMeasuredWidth(), y + card.getMeasuredHeight());
            x += card.getMeasuredWidth() + gap;
        }
        int backY = Math.min(height - getPaddingBottom() - back.getMeasuredHeight(),
            top + Math.round(452 * scale));
        back.layout(left, backY, left + back.getMeasuredWidth(), backY + back.getMeasuredHeight());
    }

    private final class SlotCard extends ViewGroup {
        private final int slot;
        private final PlayerPositionEntity save;
        private final TextView name;
        private final ImageView portrait;
        private final View silhouette;
        private final TextView detail;
        private final Button action;
        private final View delete;

        SlotCard(Context context, int slot, PlayerPositionEntity save, int level,
                View.OnClickListener onStart, View.OnClickListener onDelete) {
            super(context);
            this.slot = slot;
            this.save = save;
            setBackground(new AtlasPixelDrawable(context, R.drawable.ui_panel, 32, 32, 0,
                10, 10, 10, 10));
            setClipChildren(false);
            name = label(save == null ? "Nova aventura" : "Jogatina " + slot,
                22, 0xFFF2D36B);
            name.setGravity(Gravity.CENTER);
            addView(name);
            if (save == null) {
                portrait = null;
                silhouette = new MageSilhouette(context);
                addView(silhouette);
                detail = label("Espaço livre", 16, 0xFFB8C99A);
            } else {
                silhouette = null;
                portrait = new ImageView(context);
                portrait.setImageResource(R.drawable.portrait_player);
                portrait.setScaleType(ImageView.ScaleType.FIT_CENTER);
                portrait.setContentDescription("Retrato do personagem da jogatina " + slot);
                addView(portrait);
                detail = label("", 15, 0xFFF4EBC8);
                setDetail(level);
            }
            addView(detail);
            action = new Button(context);
            action.setText(save == null ? "Começar" : "Continuar");
            action.setTextColor(0xFFF4EBC8);
            action.setTypeface(font);
            action.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            action.setIncludeFontPadding(false);
            action.setAllCaps(false);
            action.setPadding(0, 0, 0, 0);
            action.setBackground(new AtlasMenuButtonDrawable(context));
            action.setOnClickListener(v -> onStart.onClick(v));
            action.setContentDescription((save == null ? "Começar" : "Continuar") + " jogatina " + slot);
            addView(action);
            if (save != null) {
                delete = new TrashButton(context);
                delete.setContentDescription("Apagar jogatina " + slot);
                delete.setOnClickListener(v -> onDelete.onClick(v));
                addView(delete);
            } else delete = null;
        }

        void updateLevel(int level) { setDetail(level); }

        private void setDetail(int level) {
            if (save == null) return;
            detail.setText("Nível " + Math.max(1, level) + "  ·  Vida " + save.health
                + "\n" + new SimpleDateFormat("dd/MM/yy HH:mm", Locale.forLanguageTag("pt-BR"))
                    .format(new Date(save.updatedAt)));
        }

        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            int w = MeasureSpec.getSize(widthSpec), h = MeasureSpec.getSize(heightSpec);
            setMeasuredDimension(w, h);
            int pad = Math.round(16 * scale), innerW = w - pad * 2;
            measureExactly(name, innerW, Math.round(34 * scale));
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22 * scale / getResources().getDisplayMetrics().density);
            int artW = Math.round(104 * scale), artH = Math.round(124 * scale);
            if (portrait != null) measureExactly(portrait, artW, artH);
            if (silhouette != null) measureExactly(silhouette, artW, artH);
            measureExactly(detail, innerW, Math.round((save == null ? 30 : 48) * scale));
            detail.setTextSize(TypedValue.COMPLEX_UNIT_SP, (save == null ? 16 : 15) * scale /
                getResources().getDisplayMetrics().density);
            int btnH = Math.max(dp(48), Math.round(50 * scale));
            if (delete == null) measureExactly(action, innerW, btnH);
            else {
                int deleteSize = Math.max(dp(48), Math.round(50 * scale));
                measureExactly(action, innerW - deleteSize - Math.round(8 * scale), btnH);
                measureExactly(delete, deleteSize, btnH);
            }
            action.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18 * scale /
                getResources().getDisplayMetrics().density);
        }

        @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
            int pad = Math.round(16 * scale), y = Math.round(14 * scale), center = getMeasuredWidth() / 2;
            name.layout(pad, y, getMeasuredWidth() - pad, y + name.getMeasuredHeight());
            y += name.getMeasuredHeight() + Math.round(5 * scale);
            View art = portrait != null ? portrait : silhouette;
            int ax = center - art.getMeasuredWidth() / 2;
            art.layout(ax, y, ax + art.getMeasuredWidth(), y + art.getMeasuredHeight());
            y += art.getMeasuredHeight() + Math.round(3 * scale);
            detail.layout(pad, y, getMeasuredWidth() - pad, y + detail.getMeasuredHeight());
            int actionY = getMeasuredHeight() - Math.round(16 * scale) - action.getMeasuredHeight();
            if (delete == null) action.layout(pad, actionY, getMeasuredWidth() - pad, actionY + action.getMeasuredHeight());
            else {
                int right = getMeasuredWidth() - pad;
                delete.layout(right - delete.getMeasuredWidth(), actionY,
                    right, actionY + delete.getMeasuredHeight());
                int actionRight = delete.getLeft() - Math.round(8 * scale);
                action.layout(pad, actionY, actionRight, actionY + action.getMeasuredHeight());
            }
        }

        private void measureExactly(View view, int width, int height) {
            view.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        }
    }

    private final class MenuButton extends Button {
        private final AtlasMenuButtonDrawable artwork;
        MenuButton(Context context, String text, Typeface typeface, View.OnClickListener listener) {
            super(context);
            setText(text);
            setTypeface(typeface);
            setAllCaps(false);
            setIncludeFontPadding(false);
            setGravity(Gravity.CENTER);
            setPadding(0, 0, 0, 0);
            setMinWidth(0);
            setMinimumWidth(0);
            setBackgroundTintList(null);
            setStateListAnimator(null);
            artwork = new AtlasMenuButtonDrawable(context);
            setBackground(artwork);
            setTextColor(0xFFF4EBC8);
            setOnClickListener(listener);
        }
        void setArtScale(int value) { artwork.setPixelScale(value); }
    }

    private static final class MageSilhouette extends View {
        private final Paint paint = new Paint();
        MageSilhouette(Context context) { super(context); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float unit = Math.max(2, getWidth() / 24f);
            paint.setColor(0xFF6F8A52);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(unit);
            paint.setPathEffect(new android.graphics.DashPathEffect(new float[]{unit * 2, unit * 2}, 0));
            float cx = getWidth() / 2f, top = unit * 2;
            canvas.drawCircle(cx, top + unit * 5, unit * 4, paint);
            canvas.drawRoundRect(cx - unit * 6, top + unit * 10, cx + unit * 6,
                top + unit * 21, unit * 2, unit * 2, paint);
            canvas.drawPath(new android.graphics.Path() {{
                moveTo(cx - unit * 7, top + unit * 10); lineTo(cx, top + unit * 2);
                lineTo(cx + unit * 7, top + unit * 10);
            }}, paint);
            paint.setPathEffect(null);
        }
    }

    private final class TrashButton extends View {
        private final Paint paint = new Paint();
        TrashButton(Context context) {
            super(context);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xFF5A1E1A);
            bg.setStroke(dp(2), 0xFFE06A4E);
            bg.setCornerRadius(dp(8));
            setBackground(bg);
            setClickable(true);
            setFocusable(true);
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float u = Math.max(2, getWidth() / 20f), cx = getWidth() / 2f, cy = getHeight() / 2f;
            paint.setColor(0xFFF4EBC8);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(u * 1.5f);
            paint.setStrokeCap(Paint.Cap.SQUARE);
            canvas.drawLine(cx - 4*u, cy - 4*u, cx + 4*u, cy - 4*u, paint);
            canvas.drawLine(cx - 2*u, cy - 6*u, cx + 2*u, cy - 6*u, paint);
            canvas.drawLine(cx - 3*u, cy - 3*u, cx - 2*u, cy + 5*u, paint);
            canvas.drawLine(cx + 3*u, cy - 3*u, cx + 2*u, cy + 5*u, paint);
            canvas.drawLine(cx - 2*u, cy + 5*u, cx + 2*u, cy + 5*u, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }
}
