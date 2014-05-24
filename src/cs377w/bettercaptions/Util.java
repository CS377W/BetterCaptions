package cs377w.bettercaptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.util.Base64;
import android.util.Log;

public class Util {

    public static String getContentFromServer(String webPage) {
	return getContentFromServer(webPage, true);
    }

    public static String getContentFromServer(String webPage, boolean doAuth) {
	try {
	    Log.d("CS377W", "starting to getContentFromServer");

	    URL url = new URL(webPage);
	    URLConnection urlConnection = url.openConnection();

	    if (doAuth) {
		String name = "cs377w";
		String password = "wearables";

		String authString = name + ":" + password;
		System.out.println("auth string: " + authString);
		byte[] authEncBytes = Base64.encode(authString.getBytes(),
			Base64.DEFAULT);
		String authStringEnc = new String(authEncBytes);

		urlConnection.setRequestProperty("Authorization", "Basic "
			+ authStringEnc);
	    }

	    InputStream is = urlConnection.getInputStream();
	    InputStreamReader isr = new InputStreamReader(is);

	    int numCharsRead;
	    char[] charArray = new char[1024];
	    StringBuffer sb = new StringBuffer();
	    while ((numCharsRead = isr.read(charArray)) > 0) {
		sb.append(charArray, 0, numCharsRead);
	    }
	    
	    Log.d("CS377W", "done downloading");
	    return sb.toString();
	    
	} catch (MalformedURLException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}

	return null;
    }
}
