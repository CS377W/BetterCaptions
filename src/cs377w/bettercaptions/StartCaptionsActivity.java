package cs377w.bettercaptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

import com.firebase.client.*;
import com.jonathanedgecombe.srt.InvalidTimestampFormatException;
import com.jonathanedgecombe.srt.Subtitle;
import com.jonathanedgecombe.srt.SubtitleFile;

public class StartCaptionsActivity extends Activity {
	private class LyricLine {
		public double seconds;
		public String text;
	}

	private SubtitleFile subtitleFile = null;
	private String currentlyCaptioning = null;

	private String currentSongName = null;
	private List<LyricLine> currentLyrics = null;
	private Double currentSongElapsedTime = null;
	private Timer timer = null;
	private String lyricText = "";

	private TextView tv;
	
	public Handler mHandler = new Handler() {
	    public void handleMessage(Message msg) {
	    	if (tv == null)
	    		return;
	        tv.setText(lyricText);
	    }
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start_captions);

		tv = (TextView) findViewById(R.id.caption_content);

		addMovieListeners();
		addMusicListeners();
	}

	private void addMovieListeners() {
		Firebase nameRef = new Firebase(
				"https://fiery-fire-3139.firebaseio.com/current_name");
		nameRef.addValueEventListener(new ValueEventListener() {

			@Override
			public void onDataChange(DataSnapshot snap) {
				if (snap == null)
					return;
				if (snap.getValue() == null)
					return;

				String value = snap.getValue() + "";
				if (currentlyCaptioning == value)
					return;
				currentlyCaptioning = value;

				TextView tv = (TextView) findViewById(R.id.caption_content);
				tv.setText("Captioning " + value);

				try {
					subtitleFile = new SubtitleFile(getAssets().open(
							snap.getValue() + ".srt"));
				} catch (InvalidTimestampFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			@Override
			public void onCancelled(FirebaseError error) {
			}
		});

		Firebase timeRef = new Firebase(
				"https://fiery-fire-3139.firebaseio.com/current_time");
		timeRef.addValueEventListener(new ValueEventListener() {

			@Override
			public void onDataChange(DataSnapshot snap) {
				if (snap == null)
					return;
				if (snap.getValue() == null)
					return;
				if (subtitleFile == null) {
					// TODO: handle case where this fires, but there is no
					// subtitle file. queue? or, ignore?
					return;
				}

				// TODO: remove offset by 10 hack for aligning
				// superman_eleventh_hour srt
				// or, send an object to the api
				// (https://www.firebase.com/docs/rest-api.html)
				int seconds = Integer.parseInt(snap.getValue() + "");
				Subtitle subtitle = subtitleFile.getSubtitleForSeconds(seconds);
				if (subtitle == null) {
					tv.setText("");
					return;
				}

				String toShow = "";
				for (String str : subtitle.getLines()) {
					toShow += str + "\n";
				}
				tv.setText(toShow);
			}

			@Override
			public void onCancelled(FirebaseError error) {
			}
		});
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
			public void onCancelled(FirebaseError error) {
			}
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

	private class LyricTimerTask extends TimerTask {
		private int index = 0;

		public LyricTimerTask(int index) {
			this.index = index;
		}

		@Override
		public void run() {
			if (currentLyrics == null || currentLyrics.size() < index)
				return;

			LyricLine lyric = currentLyrics.get(index);
			currentSongElapsedTime = lyric.seconds;
			lyricText = lyric.text;
			mHandler.obtainMessage().sendToTarget();
		}
	}
	
	private final long LYRIC_SHOW_DELTA = 500;
	
	private void scheduleLyrics() {
		if (timer != null)
			timer.cancel();
		timer = new Timer();
		
		for (int i = 0; i < currentLyrics.size(); i++) {
			LyricLine lyric = currentLyrics.get(i);
			if (lyric.seconds < currentSongElapsedTime)
				continue;
			
			LyricTimerTask task = new LyricTimerTask(i);
			long milliDelta = (long) (lyric.seconds * 1000 - currentSongElapsedTime * 1000) - LYRIC_SHOW_DELTA;
			if (milliDelta < 0)
				continue;
			timer.schedule(task, milliDelta);
		}
	}

//	private void scheduleLyrics() {
//		int indexOfCurLyric = -1;
//		for (int i = 0; i < currentLyrics.size(); i++) {
//			LyricLine lyric = currentLyrics.get(i);
//			if (lyric.seconds > currentSongElapsedTime)
//				break;
//			indexOfCurLyric = i;
//		}
//		if (indexOfCurLyric == -1)
//			return;
//		
//		LyricLine lyric = currentLyrics.get(indexOfCurLyric);
//		lyricText = lyric.text;
//		mHandler.obtainMessage().sendToTarget();
//
//		if (timer != null)
//			timer.cancel();
//		timer = new Timer();
//		
//		LyricTimerTask firstTask = new LyricTimerTask(indexOfCurLyric);
//		timer.schedule(firstTask, 0);
//	}
}
