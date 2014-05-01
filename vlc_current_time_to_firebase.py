#!/usr/bin/python

import subprocess
import httplib
import time

def asrun(ascript):
  "Run the given AppleScript and return the standard output and error."

  osa = subprocess.Popen(['osascript', '-'],
                         stdin=subprocess.PIPE,
                         stdout=subprocess.PIPE)
  return osa.communicate(ascript)[0]


commandTime = """
tell application "VLC"
    current time
end tell
"""

commandName = """
tell application "VLC"
    name of current item
end tell
"""

while True:
    currentTime = int(asrun(commandTime))
    connection =  httplib.HTTPSConnection("fiery-fire-3139.firebaseio.com")
    connection.request("PUT", "/current_time.json", str(currentTime))
    result = connection.getresponse()
    print result.read()
    connection.close()

    currentName = asrun(commandName).strip()
    connection =  httplib.HTTPSConnection("fiery-fire-3139.firebaseio.com")
    connection.request("PUT", "/current_name.json", "\"" + currentName + "\"")
    result = connection.getresponse()
    print result.read()
    connection.close()

    time.sleep(0.5)

