package cs377w.bettercaptions.lyrics;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.nuance.nmdp.speechkit.Prompt;
import com.nuance.nmdp.speechkit.Recognition;
import com.nuance.nmdp.speechkit.Recognizer;
import com.nuance.nmdp.speechkit.SpeechError;
import com.nuance.nmdp.speechkit.SpeechKit;

import cs377w.bettercaptions.R;
import cs377w.bettercaptions.R.id;
import cs377w.bettercaptions.R.layout;
import cs377w.bettercaptions.R.raw;
import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.ProgressBar;

public class FindMeLyricsBySpeechActivity extends Activity {
    private class LyricLine {
	public double seconds;
	public String text;
    }

    private static SpeechKit speechKit;
    private Recognizer.Listener listener;
    private Recognizer currentRecognizer;
    private GestureDetector mGestureDetector;
    private TextView tv;
    private ProgressBar progressBar;
    private Handler handler = null;
    private List<LyricLine> currentLyrics = null;
    private int currentLyricIndex;
    private Double currentSongElapsedTime = null;
    private LyricLine currentLyric = null;
    private Timer timer = null;
    private String currentText;
    private int progressAmount;

    // Allow other activities to access the SpeechKit instance.
    static SpeechKit getSpeechKit() {
	return speechKit;
    }
    
    private Handler progressHandler = new Handler();
    
    private void setCurrentSongElapsedTime(Double time) {
	currentSongElapsedTime = time;
	progressHandler.post(new Runnable() {
	    @Override
	    public void run() {
		LyricLine last = currentLyrics.get(currentLyrics.size() - 1);
		progressAmount = (int)((currentSongElapsedTime / last.seconds) * 100);
		System.out.println(progressAmount);
		progressBar.setProgress(progressAmount);
	    }
	});
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);

	setContentView(R.layout.lyrics_layout);
	tv = (TextView) findViewById(R.id.caption_content);
	progressBar = (ProgressBar) findViewById(R.id.progressBar);
	
	
	mGestureDetector = createGestureDetector(this);
	currentRecognizer = null;
	handler = new Handler();
	setVolumeControlStream(AudioManager.STREAM_MUSIC); // So that the 'Media Volume' applies to this activity
	
	// If this Activity is being recreated due to a config change (e.g.
	// screen rotation), check for the saved SpeechKit instance.
	// speechKit = (SpeechKit) getLastNonConfigurationInstance();
	
	if (speechKit == null) {
	    speechKit = SpeechKit.initialize(
		    getApplication().getApplicationContext(), AppInfo.SpeechKitAppId,
		    AppInfo.SpeechKitServer, AppInfo.SpeechKitPort,
		    AppInfo.SpeechKitSsl, AppInfo.SpeechKitApplicationKey);

	    speechKit.connect();

	    Prompt beep = speechKit.defineAudioPrompt(R.raw.beep);
	    speechKit.setDefaultRecognizerPrompts(beep, null, null, null);
	}
    }

    @Override
    protected void onDestroy() {
	super.onDestroy();
	
	if (currentRecognizer != null) {
	    currentRecognizer.cancel();
	    currentRecognizer = null;
	}
    }
    
    private void handleTapGesture() {
	if (timer != null) {
	    timer.cancel();
	    timer = null;
	}
	
	if (currentRecognizer == null) {
	    listener = createListener();
	    currentRecognizer = speechKit.createRecognizer(
		    Recognizer.RecognizerType.Dictation,
		    Recognizer.EndOfSpeechDetection.Short, "eng-USA",
		    listener, handler);
	    currentRecognizer.start();
	} else {
	    currentRecognizer.stopRecording();
	    currentRecognizer = null;
	}
    }

    private GestureDetector createGestureDetector(Context context) {
	GestureDetector gestureDetector = new GestureDetector(context);
	// Create a base listener for generic gestures
	gestureDetector.setBaseListener(new GestureDetector.BaseListener() {
	    @Override
	    public boolean onGesture(Gesture gesture) {
		if (gesture == Gesture.TAP) {
		    // do something on tap
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

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
	if (mGestureDetector != null) {
	    return mGestureDetector.onMotionEvent(event);
	}
	return false;
    }

    private Recognizer.Listener createListener() {
	return new Recognizer.Listener() {
	    @Override
	    public void onRecordingBegin(Recognizer recognizer) {
		tv.setText("Recording...");
	    }

	    @Override
	    public void onRecordingDone(Recognizer recognizer) {
		tv.setText("Processing...");
	    }

	    @Override
	    public void onError(Recognizer recognizer, SpeechError error) {
		if (recognizer != currentRecognizer)
		    return;
		currentRecognizer = null;

		// Display the error + suggestion in the edit box
		String detail = error.getErrorDetail();
		String suggestion = error.getSuggestion();

		if (suggestion == null) suggestion = "";
		setResult(detail + "\n" + suggestion);

		// for debugging purpose: printing out the speechkit session id
		android.util.Log.d("CS377W",
			"Recognizer.Listener.onError: session id [" + speechKit.getSessionId() + "]");
	    }

	    @Override
	    public void onResults(Recognizer recognizer, Recognition results) {
		currentRecognizer = null;
		
		int count = results.getResultCount();
		Recognition.Result[] rs = new Recognition.Result[count];
		for (int i = 0; i < count; i++) {
		    rs[i] = results.getResult(i);
		}
		setResults(rs);
		
		if (rs.length > 0) {
		    fetchLyrics(rs[0].getText());
		}
		
		// for debugging purpose: printing out the speechkit session id
		android.util.Log.d("CS377W",
			"Recognizer.Listener.onResults: session id [" + speechKit.getSessionId() + "]");
	    }
	};
    }

    private void setResult(String result) {
	tv.setText(result);
    }

    private void setResults(Recognition.Result[] results) {
	if (results.length > 0) {
	    setResult(results[0].getText());

	    // Log the other results
	    String otherResults = "";
	    for (int i = 0; i < results.length; i++)
		otherResults += "[" + results[i].getScore() + "]: " + results[i].getText() + "\n";
	    android.util.Log.d("CS377W", "otherResults: " + otherResults);
	} else {
	    setResult("");
	}
    }

    private void fetchLyrics(String speechResult) {
	Log.d("CS377W", "Fetching lyrics");
	LRCFetcher fetcher = new LRCFetcher();
	fetcher.speechResult = speechResult;
	(new Thread(fetcher)).start();
    }
	
    public class LRCFetcher implements Runnable {
	public String speechResult;
	
	@Override
	public void run() {
	    try {
		String url = "http://cs377w.briebunge.com:8983/solr/collection1/select" +
			"?rows=1&df=lrc_t&wt=json&hl=true&hl.fl=lrc_t&hl.simple.pre=<em>&hl.simple.post=<%2Fem>";
		String queryStr = speechResult.replaceAll("(\\w)($| )", "$1~ ");
		String query = String.format("q=%s",  URLEncoder.encode(queryStr, "UTF-8"));
		url += "&q=" + query;
		Log.d("CS377W", "Fetch url: " + url);

		URL restServiceURL = new URL(url);
		HttpURLConnection httpConnection = (HttpURLConnection) restServiceURL.openConnection();
		httpConnection.setRequestMethod("GET");
		httpConnection.setRequestProperty("Accept", "application/json");

		if (httpConnection.getResponseCode() != 200) {
		    throw new RuntimeException("HTTP GET Request Failed with Error code : " + httpConnection.getResponseCode());
		}

		BufferedReader responseBuffer = new BufferedReader(new InputStreamReader((httpConnection.getInputStream())));
		String responseStr = org.apache.commons.io.IOUtils.toString(responseBuffer);
		Log.d("CS377W", "output from server:\n" + responseStr);
		parseServerResponse(responseStr);

		httpConnection.disconnect();
	    } catch (MalformedURLException e) {
		e.printStackTrace();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    }
    
    private void parseServerResponse(String responseStr) {
	try {
	    JSONTokener tokener = new JSONTokener(responseStr);
	    JSONObject root = new JSONObject(tokener);
	    JSONObject response = root.getJSONObject("response");
	    JSONArray docs = response.getJSONArray("docs");
	    
	    if (docs.length() == 0) {
		currentText = "No lyrics found.";
		messageHandler.obtainMessage().sendToTarget();
		return;
	    }

	    JSONObject doc = docs.getJSONObject(0);
	    String id = doc.getString("id");
	    
	    currentText = doc.getString("name");
	    messageHandler.obtainMessage().sendToTarget();

	    parseLRC(doc.getString("lrc_t"));

	    JSONObject highlighting = root.getJSONObject("highlighting");
	    highlighting = highlighting.getJSONObject(id);
	    JSONArray highlights = highlighting.getJSONArray("lrc_t");
	    if (highlights.length() == 0) return;

	    String highlight = highlights.getString(0);

	    Pattern pattern = Pattern.compile("\\[(\\d+):(\\d+).(\\d+)\\]");
	    String[] lines = highlight.split("\n");
	    for (String line : lines) {
		Matcher m = pattern.matcher(line);
		if (m.find()) {
		    int minute = Integer.parseInt(m.group(1));
		    double seconds = Double.parseDouble(m.group(2) + "." + m.group(3));
		    seconds += 60 * minute;

		    setCurrentSongElapsedTime(seconds);
		    scheduleLyrics();
		    break;
		}
	    }
	} catch (JSONException e) {
	    e.printStackTrace();
	}
    }
    
    private void parseLRC(String lrcStr) {
	currentLyrics = new ArrayList<LyricLine>();

	try {
	    InputStream is = new ByteArrayInputStream(lrcStr.getBytes());
	    BufferedReader br = new BufferedReader(new InputStreamReader(is));
	    String line = null;

	    Pattern pattern = Pattern.compile("\\[(\\d+):(\\d+)\\.(\\d+)\\] ?(.*)");
	    while ((line = br.readLine()) != null) {
		Matcher m = pattern.matcher(line);
		if (m.find()) {
		    int minute = Integer.parseInt(m.group(1));
		    double seconds = Double.parseDouble(m.group(2) + "." + m.group(3));
		    seconds += 60 * minute;
		    String lyric = m.group(4);

		    LyricLine lyricLine = new LyricLine();
		    lyricLine.text = lyric;
		    lyricLine.seconds = seconds;

		    currentLyrics.add(lyricLine);
		}
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
    
    private void displayLyric(LyricLine lyric) {
	if (tv == null) return;
	
	if (lyric == null) {
	    tv.setText("");
	    return;
	}

	tv.setText(Html.fromHtml(lyric.text));
    }
    
    private Handler lyricHandler = new Handler() {
	public void handleMessage(Message msg) {
	    displayLyric(currentLyric);
	}
    };

    private Handler messageHandler = new Handler() {
	public void handleMessage(Message msg) {
	    tv.setText(currentText);
	}
    };
    
    private class LyricTimerTask extends TimerTask {
	private int index;

	public LyricTimerTask(int index) {
	    this.index = index;
	}

	@Override
	public void run() {
	    if (currentLyrics == null || index >= currentLyrics.size()) return;
	    
	    if (index == -1) {
		currentLyric = null;
		lyricHandler.obtainMessage().sendToTarget();
		return;
	    }
	    
	    currentLyric = currentLyrics.get(index);
	    lyricHandler.obtainMessage().sendToTarget();
	    
	    currentLyricIndex = index;
	    setCurrentSongElapsedTime(currentLyric.seconds);
	    
	    // Schedule next lyric
	    int next = index + 1;
	    if (next >= currentLyrics.size()) return;
	    LyricLine nextLyric = currentLyrics.get(next);
	    LyricTimerTask task = new LyricTimerTask(next);
	    long milliDelta = (long) ((nextLyric.seconds - currentSongElapsedTime) * 1000);
	    if (milliDelta >= 0) timer.schedule(task, milliDelta);
	}
    }

    private void scheduleLyrics() {
	Log.d("CS377W", "scheduling lyrics");
	
	// Cancel previously scheduled lyrics.
	if (timer != null) {
	    timer.cancel();
	    timer = null;
	}

	int i = 0;
	while (i < currentLyrics.size() && 
		currentLyrics.get(i).seconds < currentSongElapsedTime) {
	    i++;
	}
	if (i == currentLyrics.size()) return;

	// Show the current subtitle ASAP
	currentLyricIndex = i;
	currentLyric = currentLyrics.get(i);
	lyricHandler.obtainMessage().sendToTarget();

	// And, schedule upcoming ones...
	scheduleUpcomingLyrics(i + 1);
    }
    
    private void scheduleUpcomingLyrics(int index) {
	if (index >= currentLyrics.size()) return;
	
	timer = new Timer();
	LyricTimerTask task = new LyricTimerTask(index);
	long milliDelta = (long) ((currentLyrics.get(index).seconds - currentSongElapsedTime) * 1000);
	if (milliDelta < 0) milliDelta = 0;
	timer.schedule(task, milliDelta);
    }
    
    private boolean isInScrollingMode = false;
    private float cumulDelta = 0;
    private static final int DELTA_THRESHOLD = 100;
    
    private void scrollLyrics(int direction) {
	if (currentLyrics.size() == 0) return;
	
	if (direction < 0) {
	    currentLyricIndex--;
	} else if (direction > 0) {
	    currentLyricIndex++;
	}
	
	if (currentLyricIndex >= currentLyrics.size()) currentLyricIndex = currentLyrics.size() - 1;
	if (currentLyricIndex < 0) currentLyricIndex = 0;
	
	displayLyric(currentLyrics.get(currentLyricIndex));
    }
    
    private void scheduleSubtitlesByIndex() {
	if (timer != null) timer.cancel();
	timer = new Timer();
	tv.setText("");
	
	int i = currentLyricIndex;
	displayLyric(currentLyrics.get(i));
	if (i + 1 >= currentLyrics.size()) return;
	setCurrentSongElapsedTime(currentLyrics.get(i + 1).seconds);
	scheduleUpcomingLyrics(i + 1);
    }
}
