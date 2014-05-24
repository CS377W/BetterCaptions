package cs377w.bettercaptions;

import java.util.Map;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import android.os.Bundle;
import android.util.Log;

public class FindMeLyricsFromSourceActivity extends LyricsActivity {
    private Firebase songTitleRef;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	songTitleRef = new Firebase("https://fiery-fire-3139.firebaseio.com/song");
	sync();
    }
    
    private void sync() {
        displayMessageText("Syncing...");

        songTitleRef.addListenerForSingleValueEvent(new ValueEventListener() {
	    @Override
	    public void onDataChange(DataSnapshot snapshot) {
		if (snapshot == null) return;
		if (snapshot.getValue() == null) return;

		Object value = snapshot.getValue();
		String title = (String) ((Map) value).get("name");
		setCurrentSongElapsedTime((Double) ((Map) value).get("time"));
		
		displayMessageText("");
		
		if (currentSongName != title) {
		    currentSongName = title;
		    (new Thread(new LRCFetcher())).start();
		} else {
		    scheduleLyrics();
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
        super.handleOnTap();
        sync();
    }

}
