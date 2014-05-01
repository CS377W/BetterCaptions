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
	public SubtitleFile(InputStream in) throws IOException,
			InvalidTimestampFormatException {
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

	public Subtitle getSubtitleForSeconds(int seconds) {
		// TODO: do we need to make this more efficient?
		for (Subtitle subtitle : subtitles) {
			if (subtitle.getStartTimeAsSeconds() <= seconds
					&& seconds < subtitle.getEndTimeAsSeconds()) {
				return subtitle;
			}
		}
		return null;
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
