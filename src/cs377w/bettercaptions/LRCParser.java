package cs377w.bettercaptions;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LRCParser {
    public static List<LyricLine> parseLRC(String lrcStr) {
	List<LyricLine> lyrics = new ArrayList<>();

	try {
	    InputStream is = new ByteArrayInputStream(lrcStr.getBytes());
	    BufferedReader br = new BufferedReader(new InputStreamReader(is));
	    String line = null;
	    
	    Pattern lyricPattern = Pattern.compile("\\[(\\d+):(\\d+)\\.(\\d+)\\] ?(.*)");
	    Pattern headerPattern = Pattern.compile("\\[([^\\d]+?):(.*?)\\]");
	    Pattern timeHeaderPattern = Pattern.compile("\\((\\d+):(\\d+)\\)");
	    
	    LyricLine endLyric = null;
	    
	    while ((line = br.readLine()) != null) {
		Matcher headerMatcher = headerPattern.matcher(line);
		if (headerMatcher.find()) {
		    if (headerMatcher.group(1).equals("t_time")) {
			Matcher timeHeaderMatcher = timeHeaderPattern.matcher(headerMatcher.group(2));
			if (timeHeaderMatcher.find()) {
			    int minute = Integer.parseInt(timeHeaderMatcher.group(1));
			    double seconds = Double.parseDouble(timeHeaderMatcher.group(2));
			    seconds += 60 * minute;
			    endLyric = new LyricLine();
			    endLyric.seconds = seconds;
			    endLyric.text = null;
			}
		    }
		} else {
		    break;
		}
	    }
		
	    while ((line = br.readLine()) != null) {
		Matcher lyricMatcher = lyricPattern.matcher(line);
		if (lyricMatcher.find()) {
		    int minute = Integer.parseInt(lyricMatcher.group(1));
		    double seconds = Double.parseDouble(lyricMatcher.group(2) + "."
			    + lyricMatcher.group(3));
		    seconds += 60 * minute;
		    String lyric = lyricMatcher.group(4);

		    LyricLine lyricLine = new LyricLine();
		    lyricLine.text = lyric;
		    lyricLine.seconds = seconds;

		    lyrics.add(lyricLine);
		}
	    }
	    
	    if (endLyric == null) {
		endLyric = new LyricLine();
		endLyric.seconds = lyrics.get(lyrics.size()-1).seconds + 20;
		endLyric.text = null;
	    }
	    
	    lyrics.add(endLyric);

	} catch (IOException e) {
	    e.printStackTrace();
	}
	
	return lyrics;
    }
}
