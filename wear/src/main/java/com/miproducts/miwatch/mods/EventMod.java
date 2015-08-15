package com.miproducts.miwatch.mods;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.miproducts.miwatch.R;
import com.miproducts.miwatch.hud.HudView;
import com.miproducts.miwatch.utilities.ConverterUtil;
import com.miproducts.miwatch.utilities.LoadMeetingTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ladam_000 on 6/29/2015.
 */
public class EventMod extends View {

    private final int EVENT_FORWARD_THRESHOLD = -10;
    private int EVENT_BACKWARD_THRESHOLD = 10;

    private int mIndex;
    private int mEventIndex;

    LoadMeetingTask mLoadMeetingTask;

    private Context mContext;
    // Paint for the text
    private Paint mPaint;
    private Paint mPaintInfo;

    // Rectangle of the view so we can tell if touches are within it's perimeter.
    private Rect locationRect;

    // to get the WatchFace SurfaceView's width/height
    private HudView mHudView;

    // keep track if we are animating - dont want to animate more than once at a time.
    private boolean isAnimating = false;

    // Event Strings to fill
    String mEventInfo = "";
    String mEventTitle = "";
    String mEventDesc = "";

    // keep track of what event we were and are viewing.
    private int eventIndex;

    // TEXT RELATED STUFF
    // Event text position, size; rect positions, size
    private int X_ORIGINAL_POSITION;
    private int Y_ORIGINAL_POSITION;
    private int RECT_LENGTH;
    private int RECT_WIDTH;

    private int width;
    private int height = 100;
    private float xText;
    private int textSize = 24;

    // FINGER TOUCH STUFF
    // finger location when initial press.
    float xDown;
    // finger location while moving.
    float xMove, xOffsetTouch;
    boolean isDragging = false;


    public void setEventIndex(int index){
        eventIndex = index;
    }

    public EventMod(Context context, HudView mHud) {
        super(context);
        this.mContext = context;
        this.mHudView = mHud;

        initPositions();
        initPaint();

    }


    private void initPositions() {
        // we always animate to this position, might as well save it.
        //TODO shouldnt be capitalized.
        X_ORIGINAL_POSITION = mContext.getWallpaperDesiredMinimumWidth()/10;
        Y_ORIGINAL_POSITION = (int)mHudView.getTopOfHud()+25;

        // set width
        width = mContext.getWallpaperDesiredMinimumWidth()-50;
        RECT_LENGTH = X_ORIGINAL_POSITION+width;
        RECT_WIDTH = Y_ORIGINAL_POSITION+height;
        locationRect = new Rect(X_ORIGINAL_POSITION, Y_ORIGINAL_POSITION, RECT_LENGTH, RECT_WIDTH);
        xText = X_ORIGINAL_POSITION;

        // set threshold
        EVENT_BACKWARD_THRESHOLD  = (int) (mContext.getWallpaperDesiredMinimumWidth() * .1); // 1/20
    }

    private void initPaint() {
        mPaint = new Paint();
        mPaint.setColor(getResources().getColor(android.R.color.white));
        mPaint.setTextSize(textSize);
        mPaint.setTypeface(Typeface.DEFAULT);
        mPaint.setAntiAlias(true);
        mPaint.setDither(false);
        mPaint.setTextAlign(Paint.Align.LEFT);

        mPaintInfo = new Paint();
        mPaintInfo.setColor(getResources().getColor(R.color.digital_time_blue));
        mPaintInfo.setTextSize(textSize);
        mPaintInfo.setTypeface(Typeface.DEFAULT);
        mPaintInfo.setAntiAlias(true);
        mPaintInfo.setDither(false);
        mPaintInfo.setTextAlign(Paint.Align.LEFT);
    }


    /**
     * I want to slide the Event right off
     *
     *
     * @param event - the event the user did. This will allow us to keep HudView lean and mean :-).
     * @return - nothing really, I am not relying on this boolean return, but I kept it for future
     * possibilities.
    */

    public boolean touchInside(MotionEvent event){
        if(!locationRect.contains((int)event.getX(),(int)event.getY())) return false;
        else {
            //log("touch is inside");
            // touch has come in

            switch(event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    //log("down");
                    // no need to move view if animating.
                    if(!isAnimating)
                        xDown = event.getX();

                    return true;

                case MotionEvent.ACTION_MOVE:
                    // no adjustments if we are animating
                    if(!isAnimating) {
                        //log("moving");
                        xMove = event.getX();
                        xOffsetTouch = xMove - xDown;
                        isDragging = true;
                        xText = (int) xOffsetTouch;
                        //log("position of xText == " + xText);
                        mHudView.invalidate();
                        // if finger has dragged forward or backwards enough to instigate a "EventChange"
                        if(xOffsetTouch > 50
                                || xOffsetTouch < -50){
                            isAnimating = true;
                            fingerOff();
                        }
                    }
                    return true;
                // no need to call finger off if we are aniamting, animating, because of ACTION_MOVE
                case MotionEvent.ACTION_UP:
                    if(!isAnimating)
                        fingerOff();
                    return true;

                case MotionEvent.ACTION_CANCEL:
                    if(!isAnimating)
                        fingerOff();
                    return true;
            }




            return true;
        }
    }

    private void sendConfigurationChange() {


    }


    private void fingerOff() {
        //log("finger off and I was dragging = " + isDragging);
        if(isDragging){
            isDragging = false;
            // View was dragged far enough to animate into the next Event.
            if(xOffsetTouch < -50){
                //log("next Event forward");
                animateChangeInEvent(true);
                return;

            }
            // finger went far enough to go backwards
            else if(xOffsetTouch > 50){
                log("next Event backwards.");
                animateChangeInEvent(false);
                return;
            }


            else {
                //TODO make a setup for going the other way too. that way we can grab events forward and backwards
                Log.d("threshold", "not met");
                isDragging = false;
                xDown = 0;
                xMove = 0;

                xText= X_ORIGINAL_POSITION;

                mHudView.invalidate();
                return;
            }

        }
        // was not dragging bring the xText back.
        else{
            xText= X_ORIGINAL_POSITION;
        }


    }

    /**
     *
     * @param nextForwardEvent - tell us if we are going forward with next event, or backwards with previous event.
     */
    private void animateChangeInEvent(final boolean nextForwardEvent) {
        // set these based on the user's gesture
        float destination = 0;
        float from = 0;
        int duration = 500;

        // get next Event and moving the view from right to left.
        if(nextForwardEvent){
            destination = X_ORIGINAL_POSITION;
            from = mContext.getWallpaperDesiredMinimumWidth();
        }
        // get previous Event and move the view from left to right.
        else {
            from = -500;
            destination = X_ORIGINAL_POSITION;
        }

        isAnimating = true;
        animateEventChange(from, destination, duration, nextForwardEvent);

    }

    /**
     *
     * @param from - position we will be animating from.
     * @param destination - position we will be animating to.
     * @param duration - the time it will take for animation to be complete, indirectly effects speed of animation as well.
     * @param nextForwardEvent - true = fetch next Event; false = fetch prior Event.
     */
    private void animateEventChange(float from, float destination, int duration, final boolean nextForwardEvent) {

        ValueAnimator animator = ValueAnimator.ofFloat(from, destination);

        //log("Event Index we are using is " + mEventIndex);
        // It will take 1000ms for the animator to go from the width of the canvas to the

        // original position.
        animator.setDuration(duration); // .5 second

        // Callback that executes on animation steps.
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            boolean alreadySetEvent = false;
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = ((Float) (animation.getAnimatedValue())).floatValue();
                //Log.d("ValueAnimator", "value=" + value);
                while(!alreadySetEvent) {
                    // dont want to do this more than once.
                    alreadySetEvent = true;

                    // Next Event
                    if(nextForwardEvent){
                        // lets set the new events while we animate.
//                        log("current Event Size = " + mHudView.getEventSize());
                        if (mHudView.getEventSize() > eventIndex + 1) {
  //                          log("next Event");
                            eventIndex += 1;
                            setNextEvent(eventIndex);
                        }
                        // we have reached out limit and we want now to go back
                        // to original position
                        else {
    //                        log("first event");
                            eventIndex = 0;
                            setNextEvent(eventIndex);
                        }
                    }
                    // Prior Event
                    else {
                        // lets set the new events while we animate.
               //         log("current Event Size = " + mHudView.getEventSize());
                        if (eventIndex - 1 > 0) {
                 //           log("next Event");
                            eventIndex -= 1;
                            setNextEvent(eventIndex);
                        }
                        // we have reached out limit and we want now to go back
                        // to original position
                        else {
             //               log("first event");
                            eventIndex = mHudView.getEventSize();
                            setNextEvent(eventIndex);
                        }
                    }
                }
                 xText = value;
                 mHudView.invalidate();

                // we have finished.
                if (value == X_ORIGINAL_POSITION) {
                    //log("animation done");
                    // reset values
                    isDragging = false;
                    xDown = 0;
                    xMove = 0;
                    xText = X_ORIGINAL_POSITION;
                    isAnimating = false;
                    mHudView.invalidate();
                }

            }
        });

        //log("animation start");

        animator.start();
    }


    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        //canvas.drawRect(locationRect, mPaint);
        canvas.drawText(mEventInfo, xText, Y_ORIGINAL_POSITION + textSize, mPaint);
        canvas.drawText(mEventTitle, xText, Y_ORIGINAL_POSITION + (textSize * 2), mPaintInfo);
    //        canvas.drawText(mEventDesc, xText, y + (textSize * 3), mPaint);
        }

    public void log(String s){
        Log.d("EventMod", s);
    }

    public EventMod(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EventMod(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public EventMod(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Called by the HudView
     */
    public void initASyncTask(){
        mLoadMeetingTask = null;
        mLoadMeetingTask = new LoadMeetingTask(mContext, EventMod.this, mHudView);
        mLoadMeetingTask.execute();
    }
    /**
     * Called by the HudView
     */
    public void cancelTasks() {
        mLoadMeetingTask.cancel(true);
    }

    public void justAdded() {
        //log("mLoadMeetingTask status = " + mLoadMeetingTask.getStatus().toString());
        // lets fetch new events if we got any
        xText = X_ORIGINAL_POSITION;
    }
    public int setNextEvent(int index) {
        //log("setNextEvent");
        //isTaskRunning = false;
        if(mHudView.getEventSize() > index){// calendar[0] = Wed

            String dayOfMonth = mHudView.getEvent(index).dayOfMonth;
            String time = ConverterUtil.normalizeTime(mHudView.getEvent(index).time);
            String title = mHudView.getEvent(index).title;
            String description = mHudView.getEvent(index).desc;
            String month = mHudView.getEvent(index).month;

            //tvTitleRow.setText(month + " " + dayOfMonth + "  " + time);
            mEventInfo = (month + " " + dayOfMonth + "  " + time);
            mEventTitle = title;
            mEventDesc =  description;

            mEventIndex = index;
            Log.d("initCalendarHud", "events larger than index and it is " + mEventIndex);

        }
        // nothing has changed but the index that has been passed through is larger than
        // the events size so we go back to 0
        else if (mHudView.getEventSize() > 0) {
            Log.d("initCalendarHud", "events smaller than index and greater than 0");

            String dayOfMonth = mHudView.getEvent(0).dayOfMonth;
            String time = ConverterUtil.normalizeTime(mHudView.getEvent(0).time);
            String title = mHudView.getEvent(0).title;
            String description = mHudView.getEvent(0).desc;
            String month = mHudView.getEvent(0).month;

            //tvTitleRow.setText(month + " " + dayOfMonth + "  " + time);
            mEventInfo = (month + " " + dayOfMonth + "  " + time);
            mEventTitle = title;
            mEventDesc =  description;

            mEventIndex = 0;
        }
        //log("mEvents.size = " + mHudView.getEventSize());
        //log("return " + mEventIndex);
        mHudView.invalidate();
        return mEventIndex;

    }

    public int getEventsSize() {
        return mHudView.getEventSize();
    }

}
