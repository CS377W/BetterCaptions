package cs377w.bettercaptions;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.firebase.client.*;


public class StartCaptionsActivity extends Activity {
	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start_captions);
		
		TextView tv = (TextView)findViewById(R.id.caption_content);
		tv.setText("Hello world!");
		
        Firebase ref = new Firebase("https://fiery-fire-3139.firebaseio.com/current_subtitle");
        ref.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snap) {
                if (snap == null) return;
                if (snap.getValue() == null) return;
                TextView tv = (TextView)findViewById(R.id.caption_content);
                tv.setText(snap.getValue() + "");
            }

            @Override public void onCancelled(FirebaseError error) { }
        });

	}
}

