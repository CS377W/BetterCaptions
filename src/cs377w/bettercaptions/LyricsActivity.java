package cs377w.bettercaptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.TextView;

public class LyricsActivity extends Activity {
    private class LyricLine {
	public double seconds;
	public String text;
    }

    private String currentSongName = null;
    private List<LyricLine> currentLyrics = null;
    private Double currentSongElapsedTime = null;
    private Timer timer = null;
    private String lyricText = "";

    private TextView tv;

    private static final int DISPLAY_MODE = 0;
    // A list of text
    private ArrayList<Integer> colorList = new ArrayList<Integer>(
	    Arrays.asList(0xff37dd1c, 0xff37dd1c, 0xff37dd1c));
    private ArrayList<Integer> backgroundList = new ArrayList<Integer>(
	    Arrays.asList(0xff000000, 0xffffffff));

    private int highlightColor;
    private int backgroundColor;
    // Current text index in list
    private int currentTextIndex = 0;

    // Highlight index in current line
    private int currentHighlightIndex = 0;

    private boolean playLeft = true;

    TextView tv1;
    TextView tv2;
    private Handler handler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_text_display);

	switch (DISPLAY_MODE) {
	case 0:
	    backgroundColor = backgroundList.get(0);
	    highlightColor = colorList.get(0);
	    break;
	case 1:
	    backgroundColor = backgroundList.get(0);
	    highlightColor = colorList.get(1);
	    break;
	case 2:
	    backgroundColor = backgroundList.get(0);
	    highlightColor = colorList.get(2);
	    break;
	case 3:
	    backgroundColor = backgroundList.get(1);
	    highlightColor = colorList.get(0);
	    break;
	case 4:
	    backgroundColor = backgroundList.get(1);
	    highlightColor = colorList.get(1);
	    break;
	case 5:
	    backgroundColor = backgroundList.get(1);
	    highlightColor = colorList.get(2);
	    break;
	}

	// Initialize views
	tv1 = (TextView) findViewById(R.id.text1);
	tv2 = (TextView) findViewById(R.id.text2);
	tv1.setTypeface(null, Typeface.BOLD);
	tv2.setTypeface(null, Typeface.BOLD);

	// Set text views with initial text
	tv1.setText("");
	tv2.setText("");

	addMusicListeners();
    }

    private void addMusicListeners() {
	Firebase songTitleRef = new Firebase(
		"https://fiery-fire-3139.firebaseio.com/current_song/title");
	songTitleRef.addValueEventListener(new ValueEventListener() {

	    @Override
	    public void onDataChange(DataSnapshot snap) {
		if (snap == null)
		    return;
		Object value = snap.getValue();
		if (value == null)
		    return;

		String title = (String) value;

		if (currentSongName != title) {
		    currentSongName = title;
		    parseLRC();
		}
	    }

	    @Override
	    public void onCancelled(FirebaseError error) { }
	});

	Firebase songTimeRef = new Firebase(
		"https://fiery-fire-3139.firebaseio.com/current_song/current_time");
	songTimeRef.addValueEventListener(new ValueEventListener() {
	    @Override
	    public void onDataChange(DataSnapshot snapshot) {
		if (snapshot == null || snapshot.getValue() == null)
		    return;

		if (currentLyrics == null)
		    return;

		Double currentTime = (Double) snapshot.getValue();
		System.out.println("MYLOG: currentTime: " + currentTime);
		currentSongElapsedTime = currentTime;
		scheduleLyrics();
	    }

	    @Override
	    public void onCancelled(FirebaseError error) {
		System.err.println("Listener was cancelled");
	    }
	});
    }

    private void parseLRC() {
	currentLyrics = new ArrayList<>();

	try {
	    InputStream is = getAssets().open(currentSongName + ".lrc");
	    BufferedReader br = new BufferedReader(new InputStreamReader(is));
	    String line = null;

	    Pattern pattern = Pattern
		    .compile("\\[(\\d+):(\\d+)\\.(\\d+)\\] (.*)");
	    while ((line = br.readLine()) != null) {
		Matcher m = pattern.matcher(line);
		if (m.find()) {
		    int minute = Integer.parseInt(m.group(1));
		    double seconds = Double.parseDouble(m.group(2) + "."
			    + m.group(3));
		    seconds += 60 * minute;
		    String lyric = m.group(4);

		    LyricLine lyricLine = new LyricLine();
		    lyricLine.text = lyric;
		    lyricLine.seconds = seconds;

		    currentLyrics.add(lyricLine);
		}
	    }

	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }
    
    public Handler mHandler = new Handler() {
	public void handleMessage(Message msg) {
	    if (tv == null)
		return;
	    //tv.setText(lyricText);
	    // Run a repeated task
	    highlightRunnable.run();
	}
    };

//    private class LyricTimerTask extends TimerTask {
//	private int index = 0;
//
//	public LyricTimerTask(int index) {
//	    this.index = index;
//	}
//
//	@Override
//	public void run() {
//	    if (currentLyrics == null || currentLyrics.size() < index)
//		return;
//
//	    LyricLine lyric = currentLyrics.get(index);
//	    currentSongElapsedTime = lyric.seconds;
//	    lyricText = lyric.text;
//	    mHandler.obtainMessage().sendToTarget();
//	}
//    }
//
    private final long LYRIC_SHOW_DELTA = 500;

    private void scheduleLyrics() {
	if (highlightRunnable != null)
	    highlightRunnable.isCancelled = true;
	highlightRunnable = new HighlightRunnable();
	
	for (int i = 0; i < currentLyrics.size(); i++) {
	    LyricLine lyric = currentLyrics.get(i);
	    if (lyric.seconds >= currentSongElapsedTime) {
		currentTextIndex = i;
		
		long milliDelta = (long) (lyric.seconds * 1000 - currentSongElapsedTime * 1000) - LYRIC_SHOW_DELTA;
		if (milliDelta < 0) milliDelta = 0;
		handler.postDelayed(highlightRunnable, milliDelta);
		
		break;
	    }
	}
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
		if (currentTextIndex >= currentLyrics.size()) return;

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

	    // Highlight the current text until the highlight index
	    highlight(tv, currentHighlightIndex++);
	    
	    if (currentTextIndex + 1 < currentLyrics.size()) {
		LyricLine lyric = currentLyrics.get(currentTextIndex);
	    	LyricLine nextLyric = currentLyrics.get(currentTextIndex+1);
	    	double delta = (nextLyric.seconds - lyric.seconds) * 1000;
	    	Log.d("CS377W", "interval: " + (long)(delta / lyric.text.length()));
	    	handler.postDelayed(this, (long)(delta / lyric.text.length()));
	    } else {
		Log.d("CS377W", "else case");	
		handler.postDelayed(this, 100); // repeat TODO: change time to
						// variable
	    }
	}
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
	int highlightColor = 0xff37dd1c;
	int remainingColor = 0xffffffff;
	ForegroundColorSpan highlight = new ForegroundColorSpan(highlightColor);
	ForegroundColorSpan remaining = new ForegroundColorSpan(remainingColor);
	if (index >= text.length()) {
	    text.setSpan(highlight, 0, text.length(), 0);
	} else {
	    text.setSpan(highlight, 0, index, 0);
	    text.setSpan(remaining, index, text.length(), 0);
	}
    }
}
