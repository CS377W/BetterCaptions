package cs377w.bettercaptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import com.firebase.client.*;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.jonathanedgecombe.srt.InvalidTimestampFormatException;
import com.jonathanedgecombe.srt.Subtitle;
import com.jonathanedgecombe.srt.SubtitleFile;

public class StartCaptionsActivity extends Activity {
    
    private static final int NUDGE = 1;
    
    private SubtitleFile subtitleFile = null;
    private String movieTitle;
    private Integer movieCurTimeInSeconds;
    private TextView tv;
    private TextView tvIndicator;
    private Firebase movieRef;
    private Timer timer = null;
    private int curSubtitleIndex;
    private Subtitle curSubtitle;
    private List<Subtitle> curSubtitles;
    private GestureDetector mGestureDetector;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	
	setContentView(R.layout.activity_start_captions);
	
	tv = (TextView) findViewById(R.id.caption_content);
	tvIndicator = (TextView) findViewById(R.id.caption_indicator);
	
	movieRef = new Firebase("https://fiery-fire-3139.firebaseio.com/movie");
	movieTitle = null;
	movieCurTimeInSeconds = null;
	
	mGestureDetector = createGestureDetector(this);
	
	sync();
    }
    
    // TODO: there is a roughly second-long delay for the network...
    private void sync() {
	tv.setText("Syncing...");
	
	movieRef.addListenerForSingleValueEvent(new ValueEventListener() {
	     @Override
	     public void onDataChange(DataSnapshot snapshot) {
		 if (snapshot == null) return;
		 if (snapshot.getValue() == null) return;
		 
		 Object value = snapshot.getValue();
		 String newMovieTitle = (String)((Map)value).get("title");
	         Integer newMovieTimeInSeconds = Integer.valueOf(((Long) ((Map) value).get("time")).intValue());

	         Log.d("CS377W", "onDataChange complete");
	         if (!newMovieTitle.equals(movieTitle)) {
	             movieTitle = newMovieTitle;
	             subtitleFile = null;
	             Log.d("CS377W", "loading SRT");
	             loadSRT();
	         }
	         
	         movieCurTimeInSeconds = newMovieTimeInSeconds + NUDGE; //whatever...
	         if (subtitleFile != null) {
	             Log.d("CS377W", "scheduleSubtitles");
	             scheduleSubtitles();
	         }
	     }

	     @Override
	     public void onCancelled(FirebaseError error) {
	         System.err.println("Listener was cancelled");
	     }
	});
    }
    
    private void loadSRT() {
	(new Thread(new SRTFetcher())).start();
    }
    
    private void scheduleSubtitles() {
	// Cancel previously scheduled subtitles.
	if (timer != null) timer.cancel();
	
	tv.setText("");
	
	List<Subtitle> subtitles = subtitleFile.getSubtitles();
	curSubtitles = subtitles;
	
	int i = 0;
	while (subtitles.get(i).getStartTimeAsSeconds() < movieCurTimeInSeconds) {
	    i++;
	}
	//int i = subtitleFile.indexOfSubtitleForSeconds(movieCurTimeInSeconds);
	
	// Show the current subtitle ASAP
	curSubtitleIndex = i;
	displaySubtitle(curSubtitles.get(i));

	// And, schedule upcoming ones...
	scheduleUpcomingSubtitles(i + 1);
    }
    
    private void scheduleSubtitlesByIndex() {
	if (timer != null) timer.cancel();
	timer = new Timer();
	tv.setText("");
	
	int i = curSubtitleIndex;
	displaySubtitle(curSubtitles.get(i));
	movieCurTimeInSeconds = curSubtitles.get(i).getEndTimeAsSeconds();

	scheduleUpcomingSubtitles(i + 1);
    }
    
    private void scheduleUpcomingSubtitles(int index) {
	timer = new Timer();
	SubtitleTimerTask task = new SubtitleTimerTask(index);
	long milliDelta = (long) (curSubtitles.get(index).getStartTimeAsMilliseconds() - movieCurTimeInSeconds * 1000);
	if (milliDelta >= 0) timer.schedule(task, milliDelta);
    }
    
    private class SubtitleTimerTask extends TimerTask {
	private int index;

	public SubtitleTimerTask(int index) {
	    this.index = index;
	}

	@Override
	public void run() {
	    // Off event
	    if (index == -1) {
		curSubtitle = null;
		mHandler.obtainMessage().sendToTarget();
		return;
	    }
	    
	    if (index >= curSubtitles.size()) return;
	    curSubtitle = curSubtitles.get(index);
	    mHandler.obtainMessage().sendToTarget();
	    
	    curSubtitleIndex = index;
	    movieCurTimeInSeconds = curSubtitle.getStartTimeAsSeconds();
	    
	    // Schedule off task
	    SubtitleTimerTask taskEnd = new SubtitleTimerTask(-1);
	    long milliDeltaEnd = curSubtitle.getEndTimeAsMilliseconds() - curSubtitle.getStartTimeAsMilliseconds();
	    if (milliDeltaEnd >= 0) timer.schedule(taskEnd, milliDeltaEnd);
	    
	    // Schedule next
	    int next = index + 1;
	    if (next >= curSubtitles.size()) return;
	    Subtitle nextSubtitle = curSubtitles.get(next);
	    SubtitleTimerTask task = new SubtitleTimerTask(next);
	    long milliDelta = nextSubtitle.getStartTimeAsMilliseconds() - curSubtitle.getStartTimeAsMilliseconds();
	    if (milliDelta >= 0) timer.schedule(task, milliDelta);
	}
    }
    
    public Handler mHandler = new Handler() {
	public void handleMessage(Message msg) {
	    displaySubtitle(curSubtitle);
	}
    };
    
    private void displaySubtitle(Subtitle subtitle) {
	if (tv == null) return;
	
	if (subtitle == null) {
	    tv.setText("");
	    return;
	}

	String toShow = "";
	for (String str : subtitle.getLines()) {
	    toShow += str + "\n";
	}
	tv.setText(Html.fromHtml(toShow));
    }
    
    
    private void scrollSubtitles(int direction) {
	if (direction < 0) {
	    tvIndicator.setText("<<");
	    curSubtitleIndex--;
	} else if (direction > 0) {
	    tvIndicator.setText(">>");
	    curSubtitleIndex++;
	}
	
	displaySubtitle(curSubtitles.get(curSubtitleIndex));
    }
    
    
    private boolean isInScrollingMode = false;
    private float cumulDelta = 0;
    private static final int DELTA_THRESHOLD = 100;
    
    private GestureDetector createGestureDetector(Context context) {
	GestureDetector gestureDetector = new GestureDetector(context);
	// Create a base listener for generic gestures
	gestureDetector.setBaseListener(new GestureDetector.BaseListener() {
	    @Override
	    public boolean onGesture(Gesture gesture) {
		if (gesture == Gesture.TAP) {
		    sync();
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
		    tvIndicator.setText("");
		    scheduleSubtitlesByIndex();
		}
	    }
	});

	gestureDetector.setScrollListener(new GestureDetector.ScrollListener() {
	    @Override
	    public boolean onScroll(float displacement, float delta, float velocity) {
		// do something on scrolling
		cumulDelta += delta;
		isInScrollingMode = true;
		
		// tvIndicator.setText(cumulDelta + " " + displacement);
		
		if (Math.abs(cumulDelta) > DELTA_THRESHOLD) {
		    if (cumulDelta < 0) scrollSubtitles(-1);
		    if (cumulDelta > 0) scrollSubtitles(1);
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
    
    public class SRTFetcher implements Runnable {
	@Override
	public void run() {
	    try {
		Log.d("CS377W", "starting to run");
		
		String webPage = "http://cs377w.briebunge.com/assets/subtitles/" + movieTitle + ".srt";
		String name = "cs377w";
		String password = "wearables";

		String authString = name + ":" + password;
		System.out.println("auth string: " + authString);
		byte[] authEncBytes = Base64.encode(authString.getBytes(), Base64.DEFAULT);
		String authStringEnc = new String(authEncBytes);
		System.out.println("Base64 encoded auth string: " + authStringEnc);

		URL url = new URL(webPage);
		URLConnection urlConnection = url.openConnection();
		urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
		InputStream is = urlConnection.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);

		int numCharsRead;
		char[] charArray = new char[1024];
		StringBuffer sb = new StringBuffer();
		while ((numCharsRead = isr.read(charArray)) > 0) {
			sb.append(charArray, 0, numCharsRead);
		}

		Log.d("CS377W", "done downloading SRT");
		String result = sb.toString();
		subtitleFile = new SubtitleFile(result);
		Log.d("CS377W", "done creating SubtitleFile");
		subtitleLoadedHandler.obtainMessage().sendToTarget();
		
		
	    } catch (MalformedURLException e) {
		e.printStackTrace();
	    } catch (IOException e) {
		e.printStackTrace();
	    } catch (InvalidTimestampFormatException e) {
		e.printStackTrace();
	    }
	}
    }
    
    public Handler subtitleLoadedHandler = new Handler() {
	public void handleMessage(Message msg) {
	    Log.d("CS377W", "handle message");
	    sync();
	}
    };
}
