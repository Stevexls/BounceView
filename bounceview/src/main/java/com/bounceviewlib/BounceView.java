package com.bounceviewlib;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.RelativeLayout;

import com.bounceviewlib.utils.FastBlur;
import com.bounceviewlib.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 592172833@qq.com on 2019/3/22.
 */
public class BounceView extends RelativeLayout {

    // 动画类型
    public enum Style {
        NONE, TRANS, ROUND, ALPHA
    }

    private Style bgAnimStyle = Style.TRANS;    // 默认动画类型为平移

    private View ivBounceBg;
    private ViewGroup contentLayout;
    private View ivPub;

    private Path mPath;
    private long bgAnimDuration = 300;  // 背景动画时间
    private long contentAnimDuration = 300;     // 按钮动画时间
    private long pubAnimDuration = 250;     // 底部按钮动画时间
    private boolean windowBlur = false;     // 是否截取当前窗口并模糊,默认不截取
    private float curValue = 0;     // 当前背景动画的值
    private OnPubCloseListener pubCloseListener;    // 关闭操作

    // 动画关闭时操作
    public interface OnPubCloseListener {
        void onPubClose();
    }

    public BounceView(Context context) {
        super(context);
        init();
    }

    public BounceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BounceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPath = new Path();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ivBounceBg = findViewById(R.id.bounce_background);
        contentLayout = findViewById(R.id.bounce_content);
        ivPub = findViewById(R.id.bounce_pub);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (curValue == 0) {
            return;
        }

        if (bgAnimStyle == Style.ROUND) {
            drawRoundAnim(canvas);
        } else if (bgAnimStyle == Style.TRANS) {
            drawTransAnim(canvas);
        } else if (bgAnimStyle == Style.ALPHA) {
            ivBounceBg.setAlpha(curValue);
            super.dispatchDraw(canvas);
        } else if (bgAnimStyle == Style.NONE) {
            super.dispatchDraw(canvas);
        }
    }

    // 圆形动画
    private void drawRoundAnim(Canvas canvas) {
        canvas.save();
        // 确定圆心坐标及大小
        float mCenterX = getWidth() / 2;
        float mCenterY = getHeight();
        float radius = getRadius(mCenterX, mCenterY);
        mPath.reset();
        mPath.addCircle(mCenterX, mCenterY, radius, Path.Direction.CW);
        canvas.clipPath(mPath);
        super.dispatchDraw(canvas);
        canvas.restore();
    }

    // calculate max radius
    private float getRadius(float mCenterX, float mCenterY) {
        return (float) (Math.sqrt(mCenterX * mCenterX + mCenterY * mCenterY) * curValue);
    }

    // 平移动画
    private void drawTransAnim(Canvas canvas) {
        canvas.save();
        float height = getHeight() * curValue;  // 计算当前高度
        mPath.reset();
        mPath.addRect(0, getHeight() - height, getWidth(), getHeight(), Path.Direction.CW);
        canvas.clipPath(mPath);
        super.dispatchDraw(canvas);
        canvas.restore();
    }

    public void startAnim() {
        if (windowBlur) { // 如果需要截取当前背景并模糊
            setBounceBgDrawable(new BitmapDrawable(getResources(), blur()));
        }
        setVisibility(VISIBLE);
        postDelayed(new Runnable() {
            @Override
            public void run() {
                startBgAnim();
                startContentAnim();
                startPubAnim();
            }
        }, 50);
    }

    // 开始背景动画
    private void startBgAnim() {
        ValueAnimator bgAnimator = ValueAnimator.ofFloat(0, 1);
        bgAnimator.setInterpolator(new LinearInterpolator());
        bgAnimator.setDuration(bgAnimDuration);
        bgAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                curValue = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        bgAnimator.start();
    }

    // 开始按钮动画
    private void startContentAnim() {
        if (contentLayout == null) {
            throw new NullPointerException("contentLayout is null!");
        }

        for (int i = 0; i < contentLayout.getChildCount(); i++) {
            View child = contentLayout.getChildAt(i);
            child.setVisibility(VISIBLE);
            child.setTranslationY(contentLayout.getHeight() - child.getY());
            ValueAnimator childAnimator = ObjectAnimator.ofFloat(child, "translationY", child.getTranslationY(), 0);
            childAnimator.setDuration(contentAnimDuration);
            childAnimator.setInterpolator(new AnticipateOvershootInterpolator());
            childAnimator.setStartDelay(i * 50);
            childAnimator.start();
        }
    }

    // 开始底部按钮动画
    private void startPubAnim() {
        if (ivPub == null) {
            return;
        }
        ivPub.animate().rotation(135).setDuration(pubAnimDuration).start();
    }

    public void closeAnim() {
        closeBgAnim();
        closeContentAnim();
        closePubAnim();
    }

    private void closeBgAnim() {
        ValueAnimator bgAnimator = ValueAnimator.ofFloat(1, 0);
        bgAnimator.setInterpolator(new LinearInterpolator());
        bgAnimator.setDuration(bgAnimDuration);
        bgAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                curValue = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        bgAnimator.start();
    }

    private void closeContentAnim() {
        if (contentLayout == null) {
            throw new NullPointerException("contentLayout is null!");
        }

        List<Animator> mList = new ArrayList<>();
        for (int i = 0; i < contentLayout.getChildCount(); i++) {
            final View child = contentLayout.getChildAt(i);
            ValueAnimator childAnimator = ObjectAnimator.ofFloat(child, "translationY", 0, getHeight() - child.getY());
            childAnimator.setDuration(contentAnimDuration);
            childAnimator.setInterpolator(new AnticipateOvershootInterpolator());
            childAnimator.setStartDelay((getChildCount() - i) * 40);
            childAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    child.setVisibility(GONE);
                    child.setTranslationY(0);   // reset translation
                }
            });
            mList.add(childAnimator);
        }
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(mList);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                setVisibility(GONE);
                if (pubCloseListener != null) {
                    pubCloseListener.onPubClose();  // 一般此动画时间最长，把最后的关闭操作放在这里执行
                }
            }
        });
        animatorSet.start();
    }

    private void closePubAnim() {
        if (ivPub == null) {
            return;
        }
        ivPub.animate().rotation(0f).setDuration(pubAnimDuration).start();
    }

    // 设置背景动画类型
    public void setBgAnimStyle(Style style) {
        this.bgAnimStyle = style;
    }

    public void setBounceBgDrawable(Drawable drawable) {
        ivBounceBg.setBackgroundDrawable(drawable);
    }

    public void setBounceBgColor(int color) {
        ivBounceBg.setBackgroundColor(color);
    }

    public void setBounceBgResource(int resId) {
        ivBounceBg.setBackgroundResource(resId);
    }

    // 设置背景动画时间
    public void setBgAnimDuration(long duration) {
        this.bgAnimDuration = duration;
    }

    // 设置按钮动画时间
    public void setContentAnimDuration(long duration) {
        this.contentAnimDuration = duration;
    }

    // 设置底部按钮动画时间
    public void setPubAnimDuration(long duration) {
        this.pubAnimDuration = duration;
    }

    // 是否获取当前窗口内容并模糊
    public void setBounceBgBlur(boolean windowBlur) {
        this.windowBlur = windowBlur;
    }

    public void setOnPubCloseListener(OnPubCloseListener listener) {
        this.pubCloseListener = listener;
    }

    // 截取当前窗口并模糊
    public Bitmap blur() {
        View view = ((Activity) getContext()).getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache(true);
        Bitmap mBitmap = view.getDrawingCache();

        float scaleFactor = 7;//图片缩放比例
        float radius = 20;//模糊程度
        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight();
        // bitmap高度需要减去虚拟键盘高度
        Bitmap overlay = Bitmap.createBitmap(mBitmap, 0, 0, (int) (width / scaleFactor), (int) ((height - ScreenUtils.getNavigationBarHeight(getContext())) / scaleFactor));

        Canvas canvas = new Canvas(overlay);
        canvas.scale(1 / scaleFactor, 1 / scaleFactor);
        Paint paint = new Paint();
        paint.setFlags(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(mBitmap, 0, 0, paint);

        overlay = FastBlur.blur(overlay, (int) radius, true);

        view.setDrawingCacheEnabled(false);
        view.destroyDrawingCache();
        mBitmap.recycle();
        mBitmap = null;
        return overlay;
    }
}
