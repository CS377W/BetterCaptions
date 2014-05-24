package cs377w.bettercaptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

public class LyricsActivity extends BaseActivity {
    protected List<LyricLine> currentLyrics = null;
    protected String currentSongName = null;
    protected Double currentSongElapsedTime = null;
    protected String lyricText = "";
    
    private Handler progressHandler = new Handler();
    private ProgressBar progressBar;
    private int progressAmount;
    
    protected int displayMode = 0;
    private final long LYRIC_SHOW_DELTA = 500;
    
    // A list of text
    protected ArrayList<Integer> colorList = new ArrayList<Integer>(
	    Arrays.asList(0xff37dd1c, 0xffff0000, 0xff0000ff));
    protected ArrayList<Integer> backgroundList = new ArrayList<Integer>(
	    Arrays.asList(0xff000000, 0xffffffff));
    protected ArrayList<Integer> remainingList = new ArrayList<Integer>(
	    Arrays.asList(0xffffffff, 0xff000000));

    protected int highlightColor;
    protected int backgroundColor;
    protected int remainingColor;

    // Current text index in list
    protected int currentTextIndex = 0;

    // Highlight index in current line
    protected int currentHighlightIndex = 0;
    protected boolean playLeft = true;
    protected TextView tv1;
    protected TextView tv2;
    protected Handler handler = new Handler();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_text_display);
	
	progressBar = (ProgressBar) findViewById(R.id.progressBar);

	// Initialize views
	tv1 = (TextView) findViewById(R.id.text1);
	tv2 = (TextView) findViewById(R.id.text2);
	setColors();
	
	// Set text views with initial text
	tv1.setText("");
	tv2.setText("");
    }
    
    
    protected void setColors() {
	switch (displayMode) {
	case 0:
	    backgroundColor = backgroundList.get(0);
	    remainingColor = remainingList.get(0);
	    highlightColor = colorList.get(0);
	    break;
	case 1:
	    backgroundColor = backgroundList.get(0);
	    remainingColor = remainingList.get(0);
	    highlightColor = colorList.get(1);
	    break;
	case 2:
	    backgroundColor = backgroundList.get(0);
	    remainingColor = remainingList.get(0);
	    highlightColor = colorList.get(2);
	    break;
	case 3:
	    backgroundColor = backgroundList.get(1);
	    remainingColor = remainingList.get(1);
	    highlightColor = colorList.get(0);
	    break;
	case 4:
	    backgroundColor = backgroundList.get(1);
	    remainingColor = remainingList.get(1);
	    highlightColor = colorList.get(1);
	    break;
	case 5:
	    backgroundColor = backgroundList.get(1);
	    remainingColor = remainingList.get(1);
	    highlightColor = colorList.get(2);
	    break;
	}


	tv1.setTypeface(null, Typeface.BOLD);
	tv2.setTypeface(null, Typeface.BOLD);
	
	tv1.setBackgroundColor(backgroundColor);
	tv2.setBackgroundColor(backgroundColor);
	progressBar.setBackgroundColor(backgroundColor);
	
	tv1.setTextColor(remainingColor);
	tv2.setTextColor(remainingColor);
    }
    
    @Override
    protected void handleOnTwoTap() {
	super.handleOnTwoTap();

	displayMode = (displayMode+1)%6;
	setColors();
    }
    
    protected void displayMessageText(String text) {
	tv1.setText(text);
	tv2.setText("");
    }
    
    protected void setCurrentSongElapsedTime(Double time) {
	currentSongElapsedTime = time;
	if (currentLyrics == null || currentLyrics.size() == 0) return;
	
	progressHandler.post(new Runnable() {
	    @Override
	    public void run() {
		LyricLine last = currentLyrics.get(currentLyrics.size() - 1);
		progressAmount = (int) ((currentSongElapsedTime / last.seconds) * 100);
		System.out.println(progressAmount);
		progressBar.setProgress(progressAmount);
	    }
	});
    }
    
    protected class LRCFetcher implements Runnable {
	public String speechResult;

	@Override
	public void run() {
	    String result = Util.getContentFromServer("http://cs377w.briebunge.com/lyrics.php?song=" + currentSongName);
	    if (result == null) return;

	    currentLyrics = LRCParser.parseLRC(result);
	    scheduleLyrics();
	}
    }

    public Handler mHandler = new Handler() {
	public void handleMessage(Message msg) {
	    // Run a repeated task
	    highlightRunnable.run();
	}
    };

    protected void scheduleLyrics() {
	if (currentSongElapsedTime == null) return;
	if (currentLyrics == null) return;
	
	Log.d("CS377W", "currentSongElapsedTime: " + currentSongElapsedTime);
	
	if (highlightRunnable != null)
	    highlightRunnable.isCancelled = true;
	highlightRunnable = new HighlightRunnable();
	
	int i = 0;
	for (i = 0; i < currentLyrics.size(); i++) {
	    LyricLine lyric = currentLyrics.get(i);
	    if (lyric.seconds >= currentSongElapsedTime) {
		break;
	    }
	}
	currentTextIndex = i - 1;
	if (currentTextIndex < 0) currentTextIndex = 0;
	
	LyricLine lyric = currentLyrics.get(currentTextIndex);
	if (lyric.text == null) return;
	playLeft = true;
	long milliDelta = (long) (lyric.seconds * 1000 - currentSongElapsedTime * 1000) - LYRIC_SHOW_DELTA;
	if (milliDelta < 0) milliDelta = 0;
	handler.postDelayed(highlightRunnable, milliDelta);
    }
    
    private HighlightRunnable highlightRunnable;
    
    private class HighlightRunnable implements Runnable {
	public boolean isCancelled = false;

	@Override
	public void run() {
	    if (isCancelled) return;

	    TextView tv = (playLeft) ? tv1 : tv2;

	    // When done highlighting this text
	    if (currentHighlightIndex > currentLyrics.get(currentTextIndex).text.length()) {
		Log.d("CS377W", "next lyric");
		currentHighlightIndex = 0;
		
		// Switch text views
		currentTextIndex++;
		if (currentTextIndex >= currentLyrics.size() - 1) return;

		final TextView nextTV = tv;
		handler.postDelayed(new Runnable() {
		    @Override
		    public void run() {
			Log.d("CS377W", "next TV");
			if (currentTextIndex + 1 < currentLyrics.size()) {
			    nextTV.setText(currentLyrics.get(currentTextIndex + 1).text);
			}
		    }
		}, 1000);

		playLeft = !playLeft;
		handler.postDelayed(this, 0); // pause a little when done
		return;
	    }

	    long interval = getInterval();
	    setCurrentSongElapsedTime(currentLyrics.get(currentTextIndex).seconds + currentHighlightIndex * interval/1000.0);
	    // Highlight the current text until the highlight index
	    highlight(tv, currentHighlightIndex++);
	    
	    if (currentTextIndex + 1 < currentLyrics.size()) {
	    	handler.postDelayed(this, interval);
	    } else {
		Log.d("CS377W", "else case");	
		handler.postDelayed(this, 100); // repeat TODO: change time to
						// variable
	    }
	}
    }
    
    private long getInterval() {
	LyricLine lyric = currentLyrics.get(currentTextIndex);
	LyricLine nextLyric = currentLyrics.get(currentTextIndex + 1);
    	double delta = (nextLyric.seconds - lyric.seconds) * 1000;
    	return (long)(delta / lyric.text.length());
    }

    
    // Highlight the middle textView
    private void highlight(TextView tv, int index) {
	Spannable text = new SpannableStringBuilder(
		currentLyrics.get(currentTextIndex).text);
	setSpan(text, index);
	tv.setText(text);
    }

    // Separate a line of text into two colors
    private void setSpan(Spannable text, int index) {
	ForegroundColorSpan highlight = new ForegroundColorSpan(highlightColor);
	ForegroundColorSpan remaining = new ForegroundColorSpan(remainingColor);
	if (index >= text.length()) {
	    text.setSpan(highlight, 0, text.length(), 0);
	} else {
	    text.setSpan(highlight, 0, index, 0);
	    text.setSpan(remaining, index, text.length(), 0);
	}
    }
    
    @Override
    protected void handleOnScroll(int direction) {
        super.handleOnScroll(direction);
        
        if (highlightRunnable != null)
	    highlightRunnable.isCancelled = true;
        
	if (currentLyrics.size() == 0) return;

	if (direction < 0) {
	    currentHighlightIndex--;
	    //currentTextIndex--;
	} else if (direction > 0) {
	    currentHighlightIndex++;
	    //currentTextIndex++;
	}
	
	if (currentHighlightIndex < 0) {
	    currentTextIndex--;
	    if (currentTextIndex < 0) {
		currentTextIndex = 0;
		currentHighlightIndex = 0;
	    }
	    else {
		currentHighlightIndex = currentLyrics.get(currentTextIndex).text.length() - 1;
	    }
	}

	else if (currentHighlightIndex >= currentLyrics.get(currentTextIndex).text.length()) {
	    currentTextIndex++;
	    if (currentTextIndex >= currentLyrics.size() - 1) {
		    currentTextIndex = currentLyrics.size() - 2;
		    currentHighlightIndex = currentLyrics.get(currentTextIndex).text.length() - 1;
	    } else {
		currentHighlightIndex = 0;
	    }
	}
	
	if (currentHighlightIndex < 0) {
	    currentHighlightIndex = 0;
	}
	
	setCurrentSongElapsedTime(currentLyrics.get(currentTextIndex).seconds + currentHighlightIndex * getInterval()/1000.0);
	
	handler.postDelayed(new Runnable() {
	    @Override
	    public void run() {
		tv2.setText("");
		highlight(tv1, currentHighlightIndex);
	    }
	}, 0);
    }
    
    @Override
    protected void handleOnFinishedScrolling() {
        super.handleOnFinishedScrolling();
        if (currentLyrics == null) return;
        Log.d("CS377W", "scheduling");
        scheduleLyrics();
    }

}
