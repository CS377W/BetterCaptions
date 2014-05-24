package cs377w.bettercaptions;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class FindMeLyricsByListeningActivity extends LyricsActivity {
    private Recorder recorder;
    private Thread recorderThread;
    public RecognitionResult recognitionResult = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
    }

    @SuppressLint("HandlerLeak")
    public Handler recognitionResultHandler = new Handler() {
	public void handleMessage(Message msg) {
	    if (recognitionResult == null)
		return;

	    displayMessageText(recognitionResult.toString());

	    currentLyrics = recognitionResult.lyrics;
	    currentSongName = recognitionResult.songName;
	    setCurrentSongElapsedTime(recognitionResult.seconds);
	    
	    scheduleLyrics();

	    recorder = null;
	    recorderThread = null;
	}
    };

    private void startListening() {
	if (recorder != null) {
	    RecorderSingleton.instance.stop();
	    recorder.cancel();
	}
	
	recorder = new Recorder();

	// TODO: think of a more elegant way to pass these along
	recognitionResult = new RecognitionResult();
	recorder.recognitionResult = recognitionResult;
	recorder.handler = recognitionResultHandler;
	recorderThread = new Thread(recorder);

	recorderThread.start();
	displayMessageText("Streaming...");
    }

    private void stopListening() {
	if (recorder != null) {
	    recorder.cancel();
	    recorder = null;
	}

	recorderThread = null;
	displayMessageText("Stopped Streaming");
    }

    @Override
    protected void handleOnTap() {
	super.handleOnTap();

	if (recorder == null || !recorder.getIsRunning()) {
	    startListening();
	} else {
	    stopListening();
	}
    }

    @Override
    public void onDestroy() {
	super.onDestroy();
	if (recorder != null) {
	    recorder.cancel();
	    recorder = null;
	}
    }
}
