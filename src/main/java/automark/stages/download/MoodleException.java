package automark.stages.download;

import org.jsoup.nodes.*;

public class MoodleException extends Exception {
	public MoodleException(String message) {
		super(message);
	}

	public MoodleException(String message, Document doc) {
		super(message + "; URL: " + doc.location() + "; HTML: " + doc.html());
	}
}
