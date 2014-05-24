package cs377w.bettercaptions;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.client.*;
import com.jonathanedgecombe.srt.InvalidTimestampFormatException;
import com.jonathanedgecombe.srt.Subtitle;
import com.jonathanedgecombe.srt.SubtitleFile;

public class StartCaptionsActivity extends BaseActivity {

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
    private ProgressBar progressBar;
    private Handler progressHandler = new Handler();

    private void setMovieCurTimeInSeconds(Integer time) {
	movieCurTimeInSeconds = time;

	if (curSubtitles == null)
	    return;

	progressHandler.post(new Runnable() {
	    @Override
	    public void run() {
		Subtitle last = curSubtitles.get(curSubtitles.size() - 1);
		int progressAmount = (int) (((double) movieCurTimeInSeconds / last
			.getEndTimeAsSeconds()) * 100);
		System.out.println(progressAmount);
		progressBar.setProgress(progressAmount);
	    }
	});
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);

	setContentView(R.layout.activity_start_captions);

	tv = (TextView) findViewById(R.id.caption_content);
	tvIndicator = (TextView) findViewById(R.id.caption_indicator);
	progressBar = (ProgressBar) findViewById(R.id.progressBar);

	movieRef = new Firebase("https://fiery-fire-3139.firebaseio.com/movie");
	movieTitle = null;
	movieCurTimeInSeconds = null;

	sync();
    }

    // TODO: there is a roughly second-long delay for the network...
    private void sync() {
	tv.setText("Syncing...");

	movieRef.addListenerForSingleValueEvent(new ValueEventListener() {
	    @Override
	    public void onDataChange(DataSnapshot snapshot) {
		if (snapshot == null)
		    return;
		if (snapshot.getValue() == null)
		    return;

		Object value = snapshot.getValue();
		String newMovieTitle = (String) ((Map) value).get("title");
		Integer newMovieTimeInSeconds = Integer
			.valueOf(((Long) ((Map) value).get("time")).intValue());

		Log.d("CS377W", "onDataChange complete");
		if (!newMovieTitle.equals(movieTitle)) {
		    movieTitle = newMovieTitle;
		    subtitleFile = null;
		    Log.d("CS377W", "loading SRT");
		    loadSRT();
		}

		setMovieCurTimeInSeconds(newMovieTimeInSeconds + NUDGE); // whatever...
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

    @Override
    protected void handleOnTap() {
	sync();
    }

    private void loadSRT() {
	(new Thread(new SRTFetcher())).start();
    }

    private void scheduleSubtitles() {
	// Cancel previously scheduled subtitles.
	if (timer != null)
	    timer.cancel();

	tv.setText("");

	List<Subtitle> subtitles = subtitleFile.getSubtitles();
	curSubtitles = subtitles;

	int i = 0;
	while (subtitles.get(i).getStartTimeAsSeconds() < movieCurTimeInSeconds) {
	    i++;
	}
	// int i =
	// subtitleFile.indexOfSubtitleForSeconds(movieCurTimeInSeconds);

	// Show the current subtitle ASAP
	curSubtitleIndex = i;
	displaySubtitle(curSubtitles.get(i));

	// And, schedule upcoming ones...
	scheduleUpcomingSubtitles(i + 1);
    }

    private void scheduleSubtitlesByIndex() {
	if (timer != null)
	    timer.cancel();
	timer = new Timer();
	tv.setText("");

	int i = curSubtitleIndex;
	displaySubtitle(curSubtitles.get(i));
	setMovieCurTimeInSeconds(curSubtitles.get(i).getEndTimeAsSeconds());

	scheduleUpcomingSubtitles(i + 1);
    }

    private void scheduleUpcomingSubtitles(int index) {
	timer = new Timer();
	SubtitleTimerTask task = new SubtitleTimerTask(index);
	long milliDelta = (long) (curSubtitles.get(index)
		.getStartTimeAsMilliseconds() - movieCurTimeInSeconds * 1000);
	if (milliDelta >= 0)
	    timer.schedule(task, milliDelta);
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

	    if (index >= curSubtitles.size())
		return;
	    curSubtitle = curSubtitles.get(index);
	    mHandler.obtainMessage().sendToTarget();

	    curSubtitleIndex = index;
	    setMovieCurTimeInSeconds(curSubtitle.getStartTimeAsSeconds());

	    // Schedule off task
	    SubtitleTimerTask taskEnd = new SubtitleTimerTask(-1);
	    long milliDeltaEnd = curSubtitle.getEndTimeAsMilliseconds()
		    - curSubtitle.getStartTimeAsMilliseconds();
	    if (milliDeltaEnd >= 0)
		timer.schedule(taskEnd, milliDeltaEnd);

	    // Schedule next
	    int next = index + 1;
	    if (next >= curSubtitles.size())
		return;
	    Subtitle nextSubtitle = curSubtitles.get(next);
	    SubtitleTimerTask task = new SubtitleTimerTask(next);
	    long milliDelta = nextSubtitle.getStartTimeAsMilliseconds()
		    - curSubtitle.getStartTimeAsMilliseconds();
	    if (milliDelta >= 0)
		timer.schedule(task, milliDelta);
	}
    }

    public Handler mHandler = new Handler() {
	public void handleMessage(Message msg) {
	    displaySubtitle(curSubtitle);
	}
    };

    private void displaySubtitle(Subtitle subtitle) {
	if (tv == null)
	    return;

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

    protected void handleOnScroll(int direction) {
	if (direction < 0) {
	    tvIndicator.setText("<<");
	    curSubtitleIndex--;
	} else if (direction > 0) {
	    tvIndicator.setText(">>");
	    curSubtitleIndex++;
	}

	displaySubtitle(curSubtitles.get(curSubtitleIndex));
    }

    public class SRTFetcher implements Runnable {
	@Override
	public void run() {
	    String result = Util
		    .getContentFromServer("http://cs377w.briebunge.com/subtitle.php?movie="
			    + movieTitle);
	    if (result == null)
		return;

	    try {
		subtitleFile = new SubtitleFile(result);
		Log.d("CS377W", "done creating SubtitleFile");
		subtitleLoadedHandler.obtainMessage().sendToTarget();
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

    protected void handleOnFinishedScrolling() {
	tvIndicator.setText("");
	scheduleSubtitlesByIndex();
    }
}
