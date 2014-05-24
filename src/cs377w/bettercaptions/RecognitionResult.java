package cs377w.bettercaptions;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Handler;
import android.util.Base64;
import android.util.Log;


public class RecognitionResult {   
    public String songName;
    public double seconds;
    public List<LyricLine> lyrics;

    public RecognitionResult() {
	songName = null;
	seconds = -1;
	lyrics = null;
    }

    public String toString() {
	return "song:" + songName + " seconds:" + seconds;
    }
    
    public void fetchLRC(Handler handler) {
	LRCFetcher fetcher = new LRCFetcher();
	fetcher.handler = handler;
	(new Thread(fetcher)).start();
    }
    
    private void parseLRCString(String lrcStr) {
	lyrics = new ArrayList<LyricLine>();

	try {
	    Pattern pattern = Pattern.compile("\\[(\\d+):(\\d+)\\.(\\d+)\\] ?(.*)");
	    
	    InputStream is = new ByteArrayInputStream(lrcStr.getBytes());
	    BufferedReader br = new BufferedReader(new InputStreamReader(is));
	    String line = null;

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

		    lyrics.add(lyricLine);
		}
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
    
    private class LRCFetcher implements Runnable {
	public Handler handler;
	
	@Override
	public void run() {
	    try {
		Log.d("CS377W", "starting to run");
		
		String webPage = "http://cs377w.briebunge.com/assets/lyrics/" + songName + ".lrc";
		String name = "cs377w";
		String password = "wearables";

		String authString = name + ":" + password;
		System.out.println("auth string: " + authString);
		byte[] authEncBytes = Base64.encode(authString.getBytes(), Base64.DEFAULT);
		String authStringEnc = new String(authEncBytes);
		System.out.println("Base64 encoded auth string: " + authStringEnc);

		URL url = new URL(webPage);
		URLConnection urlConnection = url.openConnection();
		urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
		InputStream is = urlConnection.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);

		int numCharsRead;
		char[] charArray = new char[1024];
		StringBuffer sb = new StringBuffer();
		while ((numCharsRead = isr.read(charArray)) > 0) {
			sb.append(charArray, 0, numCharsRead);
		}

		Log.d("CS377W", "done downloading LRC");
		String result = sb.toString();
		parseLRCString(result);
		Log.d("CS377W", "done parsing LRC");
		Log.d("CS377W", this.toString());
		
	    } catch (MalformedURLException e) {
		e.printStackTrace();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	    
	    handler.obtainMessage().sendToTarget();
	}
    }
}
