package cs377w.bettercaptions;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.nuance.nmdp.speechkit.Prompt;
import com.nuance.nmdp.speechkit.Recognition;
import com.nuance.nmdp.speechkit.Recognizer;
import com.nuance.nmdp.speechkit.SpeechError;
import com.nuance.nmdp.speechkit.SpeechKit;

import cs377w.bettercaptions.R;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class FindMeLyricsBySpeechActivity extends LyricsActivity {
    private static SpeechKit speechKit;
    private Recognizer.Listener listener;
    private Recognizer currentRecognizer;
    private String currentText;
    private Handler handler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);

	currentRecognizer = null;
	handler = new Handler();
	
	// So that the 'Media Volume' applies to this activity
	setVolumeControlStream(AudioManager.STREAM_MUSIC);
	
	if (speechKit == null) {
	    speechKit = SpeechKit.initialize(getApplication()
		    .getApplicationContext(), SpeechAppInfo.SpeechKitAppId,
		    SpeechAppInfo.SpeechKitServer, SpeechAppInfo.SpeechKitPort,
		    SpeechAppInfo.SpeechKitSsl,
		    SpeechAppInfo.SpeechKitApplicationKey);

	    speechKit.connect();

	    Prompt beep = speechKit.defineAudioPrompt(R.raw.beep);
	    speechKit.setDefaultRecognizerPrompts(beep, null, null, null);
	}
	
	startListening();
    }

    @Override
    protected void onDestroy() {
	super.onDestroy();

	if (currentRecognizer != null) {
	    stopListening();
	    currentRecognizer.cancel();
	    currentRecognizer = null;
	    speechKit.cancelCurrent();
	}
    }
    
    private void startListening() {
	listener = createListener();
	currentRecognizer = speechKit.createRecognizer(
		Recognizer.RecognizerType.Dictation,
		Recognizer.EndOfSpeechDetection.Short, "eng-USA", listener,
		handler);
	currentRecognizer.start();
    }
    
    private void stopListening() {
	currentRecognizer.stopRecording();
	currentRecognizer = null;
    }

    @Override
    protected void handleOnTap() {
	super.handleOnTap();

	if (currentRecognizer == null) {
	    startListening();
	} else {
	    stopListening();
	}
    }

    private Recognizer.Listener createListener() {
	return new Recognizer.Listener() {
	    @Override
	    public void onRecordingBegin(Recognizer recognizer) {
		displayMessageText("Recording...");
	    }

	    @Override
	    public void onRecordingDone(Recognizer recognizer) {
		displayMessageText("Processing...");
	    }

	    @Override
	    public void onError(Recognizer recognizer, SpeechError error) {
		if (recognizer != currentRecognizer)
		    return;
		currentRecognizer = null;

		// Display the error + suggestion in the edit box
		String detail = error.getErrorDetail();
		String suggestion = error.getSuggestion();

		if (suggestion == null)
		    suggestion = "";
		setResult(detail + "\n" + suggestion);

		// for debugging purpose: printing out the speechkit session id
		android.util.Log.d(
			"CS377W",
			"Recognizer.Listener.onError: session id ["
				+ speechKit.getSessionId() + "]");
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
			"Recognizer.Listener.onResults: session id ["
				+ speechKit.getSessionId() + "]");
	    }
	};
    }

    private void setResult(String result) {
	displayMessageText(result);
    }

    private void setResults(Recognition.Result[] results) {
	if (results.length > 0) {
	    setResult(results[0].getText());

	    // Log the other results
	    String otherResults = "";
	    for (int i = 0; i < results.length; i++)
		otherResults += "[" + results[i].getScore() + "]: "
			+ results[i].getText() + "\n";
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

    protected class LRCFetcher implements Runnable {
	public String speechResult;

	@Override
	public void run() {
	    try {
		String url = "http://cs377w.briebunge.com:8983/solr/collection1/select"
			+ "?rows=1&df=lrc_t&wt=json&hl=true&hl.fl=lrc_t&hl.simple.pre=<em>&hl.simple.post=<%2Fem>";
		String queryStr = speechResult.replaceAll("(\\w)($| )", "$1~ ");

		String query = String.format("q=%s",
			URLEncoder.encode(queryStr, "UTF-8"));
		url += "&q=" + query;
		String responseStr = Util.getContentFromServer(url);
		
		Log.d("CS377W", "output from server:\n" + responseStr);
		parseServerResponse(responseStr);
	    } catch (UnsupportedEncodingException e) {
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

	    currentLyrics = LRCParser.parseLRC(doc.getString("lrc_t"));

	    JSONObject highlighting = root.getJSONObject("highlighting");
	    highlighting = highlighting.getJSONObject(id);
	    JSONArray highlights = highlighting.getJSONArray("lrc_t");
	    if (highlights.length() == 0)
		return;

	    String highlight = highlights.getString(0);

	    Pattern pattern = Pattern.compile("\\[(\\d+):(\\d+).(\\d+)\\]");
	    String[] lines = highlight.split("\n");
	    for (String line : lines) {
		Matcher m = pattern.matcher(line);
		if (m.find()) {
		    int minute = Integer.parseInt(m.group(1));
		    double seconds = Double.parseDouble(m.group(2) + "."
			    + m.group(3));
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

    private Handler messageHandler = new Handler() {
	public void handleMessage(Message msg) {
	    displayMessageText(currentText);
	}
    };

}
