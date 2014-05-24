package cs377w.bettercaptions;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;

public abstract class BaseActivity extends Activity {
    private GestureDetector mGestureDetector;
    private boolean isInScrollingMode = false;
    private float cumulDelta = 0;
    private static final int DELTA_THRESHOLD = 100;
    
    protected void handleOnTap() {}
    protected void handleOnTwoTap() {}
    protected void handleOnSwipeRight() {}
    protected void handleOnSwipeLeft() {}
    protected void handleOnScroll(int direction) {}
    protected void handleOnFinishedScrolling() {}
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	
	mGestureDetector = createGestureDetector(this);
    }
    
    protected GestureDetector createGestureDetector(Context context) {
	GestureDetector gestureDetector = new GestureDetector(context);
	gestureDetector.setBaseListener(new GestureDetector.BaseListener() {
	    @Override
	    public boolean onGesture(Gesture gesture) {
		if (gesture == Gesture.TAP) {
		    handleOnTap();
		    return true;
		} else if (gesture == Gesture.TWO_TAP) {
		    Log.d("CS377W", "TWO_TAP!");
		    handleOnTwoTap();
		    return true;
		} else if (gesture == Gesture.SWIPE_RIGHT) {
		    handleOnSwipeRight();
		    return true;
		} else if (gesture == Gesture.SWIPE_LEFT) {
		    handleOnSwipeLeft();
		    return true;
		}
		return false;
	    }
	});

	gestureDetector.setFingerListener(new GestureDetector.FingerListener() {
	    @Override
	    public void onFingerCountChanged(int previousCount, int currentCount) {
		Log.d("CS377W", "previousCount: " + previousCount + " currentCount: " + currentCount);
		
		if (isInScrollingMode && previousCount > 0 && currentCount == 0) {
		    isInScrollingMode = false;
		    cumulDelta = 0;
		    handleOnFinishedScrolling();
		}
	    }
	});

	gestureDetector.setScrollListener(new GestureDetector.ScrollListener() {
	    @Override
	    public boolean onScroll(float displacement, float delta, float velocity) {
		cumulDelta += delta;
		isInScrollingMode = true;
		
		if (Math.abs(cumulDelta) > DELTA_THRESHOLD) {
		    if (cumulDelta < 0) handleOnScroll(-1);
		    if (cumulDelta > 0) handleOnScroll(1);
		    cumulDelta = 0;
		}
		
		return true;
	    }
	});

	return gestureDetector;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
	if (mGestureDetector != null) {
	    return mGestureDetector.onMotionEvent(event);
	}
	return false;
    }
}
