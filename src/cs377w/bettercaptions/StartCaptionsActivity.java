package cs377w.bettercaptions;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.firebase.client.*;
import com.jonathanedgecombe.srt.InvalidTimestampFormatException;
import com.jonathanedgecombe.srt.Subtitle;
import com.jonathanedgecombe.srt.SubtitleFile;

public class StartCaptionsActivity extends Activity {
	private SubtitleFile subtitleFile;
	private String currentlyCaptioning;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start_captions);

		subtitleFile = null;
		currentlyCaptioning  = null;

		final TextView tv = (TextView) findViewById(R.id.caption_content);
		tv.setText("Hello world!");

		Firebase nameRef = new Firebase(
				"https://fiery-fire-3139.firebaseio.com/current_name");
		nameRef.addValueEventListener(new ValueEventListener() {

			@Override
			public void onDataChange(DataSnapshot snap) {
				if (snap == null) return;
				if (snap.getValue() == null) return;
				
				String value = snap.getValue() + "";
				if (currentlyCaptioning == value) return;
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
				if (snap == null) return;
				if (snap.getValue() == null) return;
				if (subtitleFile == null) {
					// TODO: handle case where this fires, but there is no subtitle file. queue? or, ignore?
					return;
				}
				
				// TODO: remove offset by 10 hack for aligning superman_eleventh_hour srt
				// or, send an object to the api (https://www.firebase.com/docs/rest-api.html)
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
}
