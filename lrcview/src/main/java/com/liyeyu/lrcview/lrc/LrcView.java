package com.liyeyu.lrcview.lrc;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;

import com.liyeyu.lrcview.R;

/**
 * Created by Liyeyu on 2016/10/13.
 */

public class LrcView extends View {


    private final int MSG_PLAYER_SLIDE = 0x158;
    private final int MSG_PLAYER_HIDE = 0x157;
    private final int MSG_CLICK_HIDE = 0x156;
    private int mBtnColor = Color.parseColor("#EFEFEF");  // 按钮颜色
    private int mHintColor = Color.parseColor("#FFFFFF");  // 提示语颜色
    private int mDefaultColor = Color.parseColor("#FFFFFF");  // 默认字体颜色
    private int mIndicatorColor = Color.parseColor("#EFEFEF");  // 指示器颜色
    private int mHighLightColor = Color.parseColor("#4FC5C7");  // 当前播放位置的颜色
    private int mCurrentShowColor = Color.parseColor("#AAAAAA");  // 当前拖动位置的颜色
    private int mCurrentClickColor = Color.parseColor("#66b3b3b3");  // 当前拖动位置的颜色
    private LrcInfo mLrcInfo;
    private int mWidth;
    private int mHeight;
    private float mShaderWidth;
    private float mShaderWidthOffset = 0.3f;
    private float mIndicatorSize = 12;
    private Paint mTextPaint;
    private Paint mIndicatorPaint;
    private Paint mBtnPaint;
    private int mLineCount;
    private float mLineHeight;  // 行高
    private float mLineSpace = 20;  // 行间距（包含在行高中）
    private float mScrollY = 0;  // 纵轴偏移量
    private float mVelocity = 0;  // 纵轴上的滑动速度
    private float mTextSize = 16;  // 歌词内容文字大小
    private int mBtnWidth = 0;  // Btn 按钮的宽度
    private int mDefaultMargin = 15;
    private Rect mTimerBound;
    private String mDefaultTime = "00:00";
    private String mDefaultHint = "LrcView";
    private int mCurrentShowLine = 0;  // 当前拖动位置对应的行数
    private int mCurrentPlayLine = 0;  // 当前播放位置对应的行数
    private int mCurrentClickLine = 0;  // 当前点击行数
    private int mMinStartUpSpeed = 1600;  // 最低滑行启动速度

    private boolean mUserTouch = false;  // 判断当前用户是否触摸
    private boolean mIndicatorShow = false;  // 判断当前滑动指示器是否显示
    private boolean mSliding = false; //是否滑动
    private boolean mClick = false; //是否点击
    private Rect mBtnBound;
    private VelocityTracker mVelocityTracker;
    private float mDownX;
    private float mDownY;
    private int mMaximumFlingVelocity;
    private float mLastScrollY;
    private OnPlayerClickListener mClickListener;
    private ValueAnimator mFlingAnimator;
    private long mCurrentDownTime;
    private OnPlayerTouchListener mOnTouchListener;


    public LrcView(Context context) {
        super(context);
    }

    public LrcView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LrcView);
        mShaderWidthOffset = a.getFloat(R.styleable.LrcView_lrc_shader_offset,mShaderWidthOffset);
        mIndicatorSize = a.getFloat(R.styleable.LrcView_lrc_shader_offset,mIndicatorSize);
        mLineSpace = a.getFloat(R.styleable.LrcView_lrc_line_space,mLineSpace);
        mTextSize = a.getFloat(R.styleable.LrcView_lrc_text_size,mTextSize);
        mBtnColor = a.getColor(R.styleable.LrcView_lrc_btn_color,mBtnColor);
        mHintColor = a.getColor(R.styleable.LrcView_lrc_hint_color,mHintColor);
        mDefaultColor = a.getColor(R.styleable.LrcView_lrc_default_color,mDefaultColor);
        mIndicatorColor = a.getColor(R.styleable.LrcView_lrc_default_color,mIndicatorColor);
        mHighLightColor = a.getColor(R.styleable.LrcView_lrc_high_light_color,mHighLightColor);
        mCurrentShowColor = a.getColor(R.styleable.LrcView_lrc_current_show_color,mCurrentShowColor);
        a.recycle();
        initView();
    }

    private void initView() {
        initAllPaints();
        initAllBounds();
        mMaximumFlingVelocity = ViewConfiguration.get(getContext()).getScaledMaximumFlingVelocity();
    }

    private void initAllBounds() {
        setTextSize(mTextSize);
        setLineSpace(mLineSpace);
        mBtnWidth = (int) (getRawSize(TypedValue.COMPLEX_UNIT_SP, 20));
        mTimerBound = new Rect();
        mIndicatorPaint.getTextBounds(mDefaultTime, 0, mDefaultTime.length(), mTimerBound);
        measureLineHeight();
    }

    private void initAllPaints() {
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setDither(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        mIndicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mIndicatorPaint.setDither(true);
        mIndicatorPaint.setTextSize(getRawSize(TypedValue.COMPLEX_UNIT_SP, mIndicatorSize));

        mBtnPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBtnPaint.setDither(true);
        mBtnPaint.setColor(mBtnColor);
        mBtnPaint.setStrokeWidth(3.0f);
        mBtnPaint.setStyle(Paint.Style.STROKE);
    }

    private float getRawSize(int unit, float size) {
        Context context = getContext();
        Resources resources;
        if (context == null) {
            resources = Resources.getSystem();
        } else {
            resources = context.getResources();
        }
        return TypedValue.applyDimension(unit, size, resources.getDisplayMetrics());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
        mShaderWidth = mWidth * mShaderWidthOffset;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float x = mWidth * 0.5f;
        if(mLineCount>0){
            for (int i = 0; i < mLineCount; i++) {
                LrcRow lrcRow = mLrcInfo.rows.get(i);
                //mLineHeight * 0.3f 基线
                float y = mHeight * 0.5f + (i+0.5f) * mLineHeight - mLineHeight * 0.3f - mScrollY;
                if(y + mLineHeight * 0.5f < 0) {
                    lrcRow.y = 0;
                    continue;
                }
                if(y - mLineHeight * 0.5f > mHeight) {
                    break;
                }

                if(i == mCurrentPlayLine-1){
                    mTextPaint.setColor(mHighLightColor);
                }else{
                    if(mIndicatorShow && i == mCurrentShowLine-1){
                        mTextPaint.setColor(mCurrentShowColor);
                    }else{
                        mTextPaint.setColor(mDefaultColor);
                    }
                }
                if(y > mHeight - mShaderWidth || y < mShaderWidth) {
                    if(y < mShaderWidth) {
                        mTextPaint.setAlpha(25 + (int) (230f * y / mShaderWidth));
                    } else {
                        mTextPaint.setAlpha(25 + (int) (230f * (mHeight - y) / mShaderWidth));
                    }
                } else {
                    mTextPaint.setAlpha(255);
                }
                lrcRow.x = x;
                lrcRow.y = mHeight * 0.5f + i * mLineHeight - 0.5f * mLineHeight - mScrollY;
                if(mCurrentClickLine == i+1){
                    Rect rect = new Rect(0,(int)lrcRow.y,mWidth,(int)(lrcRow.y+mLineHeight));
                    mTextPaint.setColor(mCurrentClickColor);
                    canvas.drawRect(rect,mTextPaint);
                }
                canvas.drawText(lrcRow.content, x, y, mTextPaint);
            }
             //滑动提示部分内容绘制
            if(mIndicatorShow) {
                drawPlayer(canvas);
                drawIndicator(canvas);
            }
        }else{
            float y = (mHeight + mLineHeight) * 0.5f;
            mTextPaint.setColor(mHintColor);
            canvas.drawText(mDefaultHint, x, y, mTextPaint);
        }
    }

    private void drawIndicator(Canvas canvas) {
        mIndicatorPaint.setColor(mIndicatorColor);
        mIndicatorPaint.setAlpha(128);
        mIndicatorPaint.setStyle(Paint.Style.FILL);
        canvas.drawText(measureCurrentTime(),mWidth - mTimerBound.width()- mDefaultMargin,(mHeight + mTimerBound.height() - mIndicatorSize * 0.5f) * 0.5f,mIndicatorPaint);

        mIndicatorPaint.setStrokeWidth(2.0f);
        mIndicatorPaint.setStyle(Paint.Style.STROKE);
        Path path = new Path();
        path.moveTo(mBtnBound.right + mDefaultMargin,mHeight/2);
        path.lineTo(mWidth - mTimerBound.width() - mDefaultMargin * 2 ,mHeight/2);
        mIndicatorPaint.setPathEffect(new DashPathEffect(new float[]{20,10},1));
        canvas.drawPath(path,mIndicatorPaint);
    }

    private void drawPlayer(Canvas canvas) {
        mBtnBound = new Rect(mDefaultMargin,mHeight/2-mBtnWidth/2,mDefaultMargin+mBtnWidth,mHeight/2+mBtnWidth/2);
        //圆中等腰三角形边长
        float side = mBtnBound.width()* 0.4f;
        //圆中等腰三角形中垂线
        float middle = (float) Math.sqrt(Math.pow(side, 2) - Math.pow(side * 0.5f, 2));
        //外接圆与三角形最大差 与 半径差
        float dr = Math.abs(mBtnBound.width() * 0.5f - (mBtnBound.width() - middle));
        Path path = new Path();
        path.moveTo(mBtnBound.centerX() - dr,mBtnBound.centerY() - side * 0.5f);
        path.lineTo(mBtnBound.centerX() + middle - dr,mBtnBound.centerY());
        path.lineTo(mBtnBound.centerX() - dr,mBtnBound.centerY() + side * 0.5f);
        path.lineTo(mBtnBound.centerX() - dr,mBtnBound.centerY() - side * 0.5f);
        mBtnPaint.setAlpha(128);
        canvas.drawPath(path,mBtnPaint);
        canvas.drawCircle(mBtnBound.centerX(),mBtnBound.centerY(),mBtnBound.width()* 0.48f,mBtnPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(mVelocityTracker==null){
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_CANCEL:
                actionCancel(event);
                break;
            case MotionEvent.ACTION_DOWN:
                actionDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                actionMove(event);
                break;
            case MotionEvent.ACTION_UP:
                actionUp(event);
                break;
            default:
                break;
        }
        invalidateView();
        return true;
    }

    private void actionUp(MotionEvent event) {
        releaseVelocityTracker();
        mHandler.sendEmptyMessageDelayed(MSG_PLAYER_HIDE, 2000);
        if(mLineCount>0) {
            setUserTouch(false);  // 用户手指离开屏幕，取消触摸标记
            if(mLineCount>0 && mScrollY < 0) {
                smoothScrollTo(0);
                return;
            }
            if(mLineCount>0 && mScrollY > mLineHeight * ( mLineCount - 1 )) {
                smoothScrollTo(mLineHeight * (mLineCount - 1));
                return;
            }
            if(Math.abs(mVelocity) > mMinStartUpSpeed) {
                doFlingAnimator(mVelocity);
                return;
            }
            if((mIndicatorShow || !mClick) && clickPlayer(event)) {
                if(mCurrentShowLine != mCurrentPlayLine) {
                    mIndicatorShow = false;
                    if(mClickListener != null) {
                        mClickListener.onPlayerClicked(mLrcInfo.rows.get(mCurrentShowLine - 1).time, mLrcInfo.rows.get(mCurrentShowLine - 1).content);
                    }
                }
            }
        }
        onClickLine(event);
    }

    private void onClickLine(MotionEvent event){
        if(mCurrentDownTime!=0 && mOnTouchListener!=null && !clickPlayer(event)){
            mClick = false;
            long l = System.currentTimeMillis() - mCurrentDownTime;
            float cy = event.getY();
            String content = "";
            for (int i = 0; i < mLineCount; i++) {
                LrcRow lrcRow = mLrcInfo.rows.get(i);
                if(lrcRow.y<=cy && lrcRow.y+mLineHeight>=cy){
//                    Log.i("onclick","cy:"+cy+" y:"+lrcRow.y);
                    content = lrcRow.content;
                    mCurrentClickLine = i+1;
                }
            }
            if(l>1000){
                mOnTouchListener.onLongClick(this,mCurrentClickLine,content);
            }else{
                mOnTouchListener.onClick(this,mCurrentClickLine,content);
            }
            mHandler.sendEmptyMessageDelayed(MSG_CLICK_HIDE,500);
        }
    }

    private void actionMove(MotionEvent event) {
        float dx = Math.abs(event.getX() - mDownX);
        float dy =  Math.abs(event.getY() - mDownY);
        if(dx>10 || dy>10){
            mCurrentDownTime = 0;
            mIndicatorShow = true;
            mClick = true;
        }else{
            mIndicatorShow = false;
            mClick = false;
        }
        if(mLineCount>0){
            mVelocityTracker.computeCurrentVelocity(1000,mMaximumFlingVelocity);
            float scrollY = mLastScrollY + mDownY - event.getY();
            float value01 = scrollY - (mLineCount * mLineHeight * 0.5f);   // 52  -52  8  -8
            float value02 = Math.abs(value01) - (mLineCount * mLineHeight * 0.5f);   // 2  2  -42  -42
            mScrollY = value02 > 0 ? scrollY - (measureDampingDistance(value02) * value01 / Math.abs(value01)) : scrollY;   //   value01 / Math.abs(value01)  控制滑动方向
            mVelocity = mVelocityTracker.getYVelocity();
            measureCurrentLine();
        }
    }

    /**
     * 计算阻尼效果的大小
     * */
    private final int mMaxDampingDistance = 360;
    private float measureDampingDistance(float value02) {
        return value02 > mMaxDampingDistance ? (mMaxDampingDistance * 0.6f + (value02 - mMaxDampingDistance) * 0.72f) : value02 * 0.6f;
    }

    private void actionDown(MotionEvent event) {
        mCurrentDownTime = System.currentTimeMillis();
        mHandler.removeMessages(MSG_PLAYER_HIDE);
        mHandler.removeMessages(MSG_PLAYER_SLIDE);
        mHandler.removeMessages(MSG_CLICK_HIDE);
        mLastScrollY = mScrollY;
        mDownX = event.getX();
        mDownY = event.getY();
        if(mFlingAnimator != null) {
            mFlingAnimator.cancel();
            mFlingAnimator = null;
        }
        setUserTouch(true);
    }

    private void actionCancel(MotionEvent event) {
        releaseVelocityTracker();
    }

    private void releaseVelocityTracker() {
        if(mVelocityTracker != null) {
            mVelocityTracker.clear();
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    /**
     * 滑行动画
     * @param velocityY  滑动速度
     * */
    private void doFlingAnimator(float velocityY) {
        //注：     Math.abs(velocity)  < =  16000
        float distance = (velocityY / Math.abs(velocityY) * Math.min((Math.abs(velocityY)/1000 * 50), 640));   // 计算就已当前的滑动速度理论上的滑行距离是多少
        float to = Math.min(Math.max(0, (mScrollY - distance)), (mLineCount - 1) * mLineHeight);   // 综合考虑边界问题后得出的实际滑行距离
        mFlingAnimator = ValueAnimator.ofFloat(mScrollY, to);
        mFlingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mScrollY = (float) animation.getAnimatedValue();
                measureCurrentLine();
                invalidateView();
            }
        });

        mFlingAnimator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mVelocity = mMinStartUpSpeed - 1;
                mSliding = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mSliding = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
            }
        });

        mFlingAnimator.setDuration(600);
        mFlingAnimator.setInterpolator(new DecelerateInterpolator());
        mFlingAnimator.start();
    }

    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_PLAYER_HIDE:
                    mHandler.sendEmptyMessageDelayed(MSG_PLAYER_SLIDE, 1200);
                    mIndicatorShow = false;
                    invalidateView();
                case MSG_PLAYER_SLIDE:
                    smoothScrollTo(measureCurrentScrollY(mCurrentPlayLine));
                    invalidateView();
                case MSG_CLICK_HIDE:
                    mCurrentClickLine = 0;
                    invalidateView();
            }
        }
    };

    /**
     * 设置用户是否触摸的标记
     * @param isUserTouch  标记用户是否触摸屏幕
     * */
    private void setUserTouch(boolean isUserTouch) {
        if(mUserTouch == isUserTouch) {
            return;
        }
        mUserTouch = isUserTouch;
//        if(isUserTouch) {
//            mIndicatorShow = isUserTouch;
//        }
    }

    /**
     * 获取当前滑动到的位置的当前时间
     * */
    private String measureCurrentTime() {
        if(mCurrentShowLine<=mLineCount && mCurrentShowLine>0){
            mDefaultTime = mLrcInfo.rows.get(mCurrentShowLine-1).strTime.trim().split("\\.")[0];
        }else if(mCurrentShowLine>mLineCount){
            mDefaultTime = mLrcInfo.rows.get(mLineCount-1).strTime.trim().split("\\.")[0];
        }else if(mCurrentShowLine-1<=0){
            mDefaultTime = mLrcInfo.rows.get(0).strTime.trim().split("\\.")[0];
        }
        return mDefaultTime;
    }

    public void setLrcInfo(LrcInfo lrcInfo){
        mLrcInfo = lrcInfo;
        if(mLrcInfo!=null){
            mLineCount = mLrcInfo.rows.size();
        }else{
            mLineCount = 0;
        }
        invalidateView();
    }

    /**
     * 设置歌词文本内容字体大小
     * @param unit
     * @param size
     * */
    public void setTextSize(int unit, float size) {
        setRawTextSize(getRawSize(unit, size));
    }

    /**
     * 设置歌词文本内容字体大小
     * @param size
     * */
    public void setTextSize(float size) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    private void setRawTextSize(float size) {
        mTextPaint.setTextSize(size);
        updateLrcView();
    }
    /**
     * 设置歌词内容行间距
     * @param lineSpace  行间距大小
     * */
    public void setLineSpace(float lineSpace) {
        mLineSpace = getRawSize(TypedValue.COMPLEX_UNIT_SP, lineSpace);
        updateLrcView();
    }

    private void updateLrcView(){
        measureLineHeight();
        mScrollY = measureCurrentScrollY(mCurrentPlayLine);
        invalidateView();
    }

    /**
     * 设置高亮显示文本的字体颜色
     * @param color  颜色值
     * */
    public void setHighLightTextColor(int color) {
        if(mHighLightColor != color) {
            mHighLightColor = color;
            invalidateView();
        }
    }

    public void setShaderWidthOffset(float shaderWidthOffset) {
        mShaderWidthOffset = shaderWidthOffset;
    }

    /**
     * 重置、设置歌词内容被重置后的提示内容
     * @param message  提示内容
     * */
    public void reset(String message) {
        mDefaultHint = message;
        resetView();
    }
    /**
     * 初始化控件
     * */
    private void resetView() {
        mCurrentPlayLine = mCurrentShowLine = 0;
        mLineCount = 0;
        mScrollY = 0;
        resetLyricInfo();
        invalidateView();
    }

    private void resetLyricInfo() {
        mLrcInfo = null;
        mLineCount = 0;
        if(mLrcInfo != null) {
            mLrcInfo.rows.clear();
            mLrcInfo.rows = null;
            mLrcInfo = null;
        }
    }

    private void invalidateView() {
        if(Looper.getMainLooper()==Looper.myLooper()){
            invalidate();
        }else{
            postInvalidate();
        }
    }
    /**
     * Input current showing line to measure the view's current scroll Y
     * @param line  当前指定行号
     * */
    private float measureCurrentScrollY(int line) {
        return (line - 1) * mLineHeight;
    }
    /**
     * 计算行高度
     * */
    private void measureLineHeight() {
        Rect rect = new Rect();
        mTextPaint.getTextBounds(mDefaultHint,0,mDefaultHint.length(),rect);
        mLineHeight = rect.height()+mLineSpace;
    }
    /**
     * 设置当前时间显示位置
     * @param current  时间戳
     * */
    public void setCurrentTimeMillis(long current) {
        scrollToCurrentTimeMillis(current);
    }
    /**
     * 根据当前给定的时间戳滑动到指定位置
     * @param time  时间戳
     * */
    private void scrollToCurrentTimeMillis(long time) {
        int pos = 0;
        if(mLineCount>0){
            for (int i = 0; i < mLineCount; i++) {
                LrcRow lrcRow = mLrcInfo.rows.get(i);
                if(lrcRow!=null && lrcRow.time>time){
                    pos = i;
                    break;
                }
                if(i==mLineCount-1){
                    pos = mLineCount;
                }
            }
        }

        if(!mSliding && !mIndicatorShow){
            if(mCurrentPlayLine!=pos && !mUserTouch){
                mCurrentPlayLine = pos;
                smoothScrollTo(measureCurrentScrollY(pos));
            }else{
                mCurrentPlayLine = mCurrentShowLine = pos;
            }
        }
    }

    private void smoothScrollTo(float toY) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(mScrollY, toY);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if(mUserTouch) {
                    animation.cancel();
                    return;
                }
                mScrollY = (float) animation.getAnimatedValue();
                invalidateView();
            }
        });
        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mSliding = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mSliding = false;
                measureCurrentLine();
                invalidateView();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        valueAnimator.setDuration(600);
        valueAnimator.setInterpolator(new LinearOutSlowInInterpolator());
        valueAnimator.start();
    }
    /**
     * To measure current showing line number based on the view's scroll Y
     * */
    private void measureCurrentLine() {
        float baseScrollY = mScrollY + mLineHeight * 0.5f;
        mCurrentShowLine = (int) (baseScrollY / mLineHeight + 1);
    }

    /**
     * 判断当前点击事件是否落在播放按钮触摸区域范围内
     * @param event  触摸事件
     * */
    private boolean clickPlayer(MotionEvent event) {
        if(mBtnBound != null &&  mDownX > (mBtnBound.left - mDefaultMargin)
                && mDownX < (mBtnBound.right + mDefaultMargin)
                && mDownY > (mBtnBound.top - mDefaultMargin)
                && mDownY < (mBtnBound.bottom + mDefaultMargin)) {
            float upX = event.getX();   float upY = event.getY();
            return upX > (mBtnBound.left - mDefaultMargin)
                    && upX < (mBtnBound.right + mDefaultMargin)
                    && upY > (mBtnBound.top - mDefaultMargin)
                    && upY < (mBtnBound.bottom + mDefaultMargin);
        }
        return false;
    }

    /**
     * 设置播放按钮点击监听事件
     * @param mClickListener  监听器
     * */
    public void setOnPlayerClickListener(OnPlayerClickListener mClickListener) {
        this.mClickListener = mClickListener;
    }
    public void setOnLrcViewTouchListener(OnPlayerTouchListener mOnTouchListener) {
        this.mOnTouchListener = mOnTouchListener;
    }

    public void setCurrentClickColor(int currentClickColor) {
        mCurrentClickColor = currentClickColor;
    }

    public interface OnPlayerClickListener {
        public void onPlayerClicked(long progress, String content);
    }
    public interface OnPlayerTouchListener {
        public void onClick(View view,int line,String content);
        public void onLongClick(View view,int line,String content);
    }
}
