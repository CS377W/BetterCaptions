package cs377w.bettercaptions.lyrics;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.view.MotionEvent;
import android.widget.TextView;
import cs377w.bettercaptions.R;

public class Send extends Activity {
    private TextView tv;
    private GestureDetector mGestureDetector;
    private Recorder recorder;
    private Thread recorderThread;
    private Timer timer = null;
    
    public RecognitionResult recognitionResult = null;
    
    private double curTimeInSeconds;
    private List<LyricLine> curLyrics;
    private int curLyricIndex;
    private LyricLine curLyric;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.audio_source_layout);
	tv = (TextView) findViewById(R.id.content);
	mGestureDetector = createGestureDetector(this);
    }

    @SuppressLint("HandlerLeak")
    public Handler recognitionResultHandler = new Handler() {
	public void handleMessage(Message msg) {
	    if (recognitionResult == null)
		return;
	    
	    tv.setText(recognitionResult.toString());
	    
	    curTimeInSeconds = recognitionResult.seconds;
	    scheduleLyrics();
	    
	    recorder = null;
	    recorderThread = null;
	}
    };
    
    private void displayLyric(LyricLine lyric) {
	if (tv == null) return;
	
	if (lyric == null) {
	    tv.setText("");
	    return;
	}

	tv.setText(Html.fromHtml(lyric.text));
    }
    
    private void scheduleLyrics() {
	// Cancel previously scheduled lyrics.
	if (timer != null) timer.cancel();
	
	//tv.setText("");
	
	curLyrics = recognitionResult.lyrics;
	if (curLyrics == null || curLyrics.size() == 0) return;
	
	int i = 0;
	while (i < curLyrics.size() && 
		curLyrics.get(i).seconds < curTimeInSeconds) {
	    i++;
	}
	if (i == curLyrics.size()) return;
	
	// Show the current subtitle ASAP
	curLyricIndex = i;
	displayLyric(curLyrics.get(i));

	// And, schedule upcoming ones...
	scheduleUpcomingLyrics(i + 1);
    }
    
    private void scheduleUpcomingLyrics(int index) {
	if (index >= curLyrics.size()) return;
	
	timer = new Timer();
	LyricTimerTask task = new LyricTimerTask(index);
	long milliDelta = (long) ((curLyrics.get(index).seconds - curTimeInSeconds) * 1000);
	if (milliDelta < 0) milliDelta = 0;
	timer.schedule(task, milliDelta);
    }
    
    private void handleTapGesture() {
	if (timer != null) timer.cancel();
		
	if (recorder == null) {
	    recorder = new Recorder();
	    
	    // TODO: think of a more elegant way to pass these along
	    recognitionResult = new RecognitionResult();
	    recorder.recognitionResult = recognitionResult;
	    recorder.handler = recognitionResultHandler;
	    recorderThread = new Thread(recorder);
	}

	if (!recorder.getIsRunning()) {
	    tv.setText("Streaming...");
	    recorderThread.start();
	} else {
	    recorder.cancel();
	    recorder = null;
	    recorderThread = null;
	    tv.setText("Stopped Streaming");
	}
    }
    
    private boolean isInScrollingMode = false;
    private float cumulDelta = 0;
    private static final int DELTA_THRESHOLD = 100;
    
    private void scrollLyrics(int direction) {
	if (curLyrics.size() == 0) return;
	
	if (direction < 0) {
	    curLyricIndex--;
	} else if (direction > 0) {
	    curLyricIndex++;
	}
	
	if (curLyricIndex >= curLyrics.size()) curLyricIndex = curLyrics.size() - 1;
	if (curLyricIndex < 0) curLyricIndex = 0;
	
	displayLyric(curLyrics.get(curLyricIndex));
    }
    
    private void scheduleSubtitlesByIndex() {
	if (timer != null) timer.cancel();
	timer = new Timer();
	tv.setText("");
	
	int i = curLyricIndex;
	displayLyric(curLyrics.get(i));
	curTimeInSeconds = curLyrics.get(i + 1).seconds;

	scheduleUpcomingLyrics(i + 1);
    }

    private GestureDetector createGestureDetector(Context context) {
	GestureDetector gestureDetector = new GestureDetector(context);
	// Create a base listener for generic gestures
	gestureDetector.setBaseListener(new GestureDetector.BaseListener() {
	    @Override
	    public boolean onGesture(Gesture gesture) {
		if (gesture == Gesture.TAP) {
		    handleTapGesture();
		    return true;
		} else if (gesture == Gesture.TWO_TAP) {
		    // do something on two finger tap
		    return true;
		} else if (gesture == Gesture.SWIPE_RIGHT) {
		    // do something on right (forward) swipe
		    return true;
		} else if (gesture == Gesture.SWIPE_LEFT) {
		    // do something on left (backwards) swipe
		    return true;
		}
		return false;
	    }
	});
	
	gestureDetector.setFingerListener(new GestureDetector.FingerListener() {
	    @Override
	    public void onFingerCountChanged(int previousCount, int currentCount) {
		// do something on finger count changes
		if (isInScrollingMode && previousCount > 0 && currentCount == 0) {
		    isInScrollingMode = false;
		    cumulDelta = 0;
		    scheduleSubtitlesByIndex();
		}
	    }
	});

	gestureDetector.setScrollListener(new GestureDetector.ScrollListener() {
	    @Override
	    public boolean onScroll(float displacement, float delta, float velocity) {
		// do something on scrolling
		cumulDelta += delta;
		if (!isInScrollingMode && timer != null) timer.cancel();
		isInScrollingMode = true;
		
		if (Math.abs(cumulDelta) > DELTA_THRESHOLD) {
		    if (cumulDelta < 0) scrollLyrics(-1);
		    if (cumulDelta > 0) scrollLyrics(1);
		    cumulDelta = 0;
		}
		
		return true;
	    }
	});
	
	return gestureDetector;
    }

    /*
     * Send generic motion events to the gesture detector
     */
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
	if (mGestureDetector != null) {
	    return mGestureDetector.onMotionEvent(event);
	}
	return false;
    }

    @Override
    public void onDestroy() {
	super.onDestroy();
	if (recorder != null) {
	    recorder.cancel();
	    recorder = null;
	}
    }
    
    public Handler mHandler = new Handler() {
	public void handleMessage(Message msg) {
	    displayLyric(curLyric);
	}
    };
    
    private class LyricTimerTask extends TimerTask {
	private int index;

	public LyricTimerTask(int index) {
	    this.index = index;
	}

	@Override
	public void run() {
	    // Off event
	    if (index == -1) {
		curLyric = null;
		mHandler.obtainMessage().sendToTarget();
		return;
	    }
	    
	    if (index >= curLyrics.size()) return;
	    curLyric = curLyrics.get(index);
	    mHandler.obtainMessage().sendToTarget();
	    
	    curLyricIndex = index;
	    curTimeInSeconds = curLyric.seconds;
	    
	    // Schedule next lyric
	    int next = index + 1;
	    if (next >= curLyrics.size()) return;
	    LyricLine nextLyric = curLyrics.get(next);
	    LyricTimerTask task = new LyricTimerTask(next);
	    long milliDelta = (long) ((nextLyric.seconds - curLyric.seconds) * 1000);
	    if (milliDelta >= 0) timer.schedule(task, milliDelta);
	}
    }
    
}
