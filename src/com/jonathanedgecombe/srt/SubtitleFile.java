package com.jonathanedgecombe.srt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SubtitleFile {
	private final List<Subtitle> subtitles;
	
	

	/* Create a new SubtitleFile. */
	public SubtitleFile() {
		subtitles = new ArrayList<>();
	}

	/* Load an existing SubtitleFile from a File. */
	public SubtitleFile(String in) throws InvalidTimestampFormatException {
		subtitles = new ArrayList<>();

		Scanner scanner = new Scanner(in);
		
		while (scanner.hasNextLine()) {
			/* We assign our own ID's, ignore the ID given in the file. */
			scanner.nextLine();

			/* Read the Timestamps from the file. */
			String[] timestamps = scanner.nextLine().split(" --> ");
			if (timestamps.length != 2) {
				scanner.close();
				throw new InvalidTimestampFormatException();
			}

			Timestamp startTime = new Timestamp(timestamps[0]);
			Timestamp endTime = new Timestamp(timestamps[1]);

			Subtitle subtitle = new Subtitle(startTime, endTime);

			String line = scanner.nextLine();
			while (!line.equals("")) {
				subtitle.addLine(line);

				if (!scanner.hasNextLine()) break;
				line = scanner.nextLine();
			}

			subtitles.add(subtitle);
		}

		scanner.close();
	}

	public void addSubtitle(Subtitle subtitle) {
		subtitles.add(subtitle);
	}

	public void clearSubtitles() {
		subtitles.clear();
	}

	public void removeSubtitle(Subtitle subtitle) {
		subtitles.remove(subtitle);
	}

	public void removeSubtitle(int index) {
		subtitles.remove(index);
	}

	public Subtitle getSubtitle(int index) {
		return subtitles.get(index);
	}

	public List<Subtitle> getSubtitles() {
		return subtitles;
	}
	
        public int indexOfSubtitleForSeconds(int seconds) {
            	int closestOvershot = -1;
            	int closestOvershotSec = Integer.MAX_VALUE;
            
        	int min = 0;
        	int max = subtitles.size() - 1;
        	while (min <= max) {
        	    int mid = (min + max) / 2;
        
        	    Subtitle subtitle = subtitles.get(mid);
        	    int secondsStart = subtitle.getStartTimeAsSeconds();
        	    int secondsEnd = subtitle.getEndTimeAsSeconds();
        
        	    if (secondsStart <= seconds && seconds <= secondsEnd) {
        		return mid;
        	    }
        
        	    if (seconds < secondsStart) {
        		max = mid - 1;
        	    } else if (seconds > secondsEnd) {
        		min = mid + 1;
        	    }
        	    
        	    if (seconds < secondsStart && secondsStart < closestOvershotSec) {
        		closestOvershotSec = secondsStart;
        		closestOvershot = mid;
        	    }
        	}
        	
        	return closestOvershot;
        }

	public String compile() {
		String string = "";

		/* Subtitle indexes start at 1 */
		int index = 1;

		for (Subtitle subtitle : subtitles) {
			string += subtitle.compile(index);
			index++;
		}

		return string;
	}

	public void save(File file) throws IOException {
		FileOutputStream out = new FileOutputStream(file);
		out.write(compile().getBytes(Charset.forName("UTF-8")));
		out.close();
	}
}
