package cs377w.bettercaptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.os.Bundle;
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

	private SubtitleFile subtitleFile;
	private String currentlyCaptioning;

	private String currentSongName;
	private List<LyricLine> currentLyrics;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start_captions);

		subtitleFile = null;
		currentlyCaptioning = null;
		currentSongName = null;
		currentLyrics = null;

		final TextView tv = (TextView) findViewById(R.id.caption_content);

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

		Firebase songRef = new Firebase(
				"https://fiery-fire-3139.firebaseio.com/current_song");
		songRef.addValueEventListener(new ValueEventListener() {

			@Override
			public void onDataChange(DataSnapshot snap) {
				if (snap == null)
					return;
				Object value = snap.getValue();
				if (value == null)
					return;

				String title = (String) ((Map) value).get("title");

				if (currentSongName != title) {
					currentLyrics = new ArrayList<>();

					try {
						InputStream is = getAssets().open(title + ".lrc");
						String str;
						BufferedReader br = new BufferedReader(
								new InputStreamReader(is));
						String line = null;

						Pattern pattern = Pattern
								.compile("\\[(\\d+):(\\d+)\\.(\\d+)\\] (.*)");
						while ((line = br.readLine()) != null) {
							Matcher m = pattern.matcher(line);
							if (m.find()) {
								int count = m.groupCount();

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
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					currentSongName = title;
				}

				Double currentTime = (Double) ((Map) value).get("current_time");
				
				LyricLine prevLyric = null;
				for (LyricLine lyric : currentLyrics) {
					if (lyric.seconds > currentTime) break;
					prevLyric = lyric;
				}
				if (prevLyric != null) {
					tv.setText(prevLyric.text);
				}
			}

			@Override
			public void onCancelled(FirebaseError error) {
			}
		});

	}
}
