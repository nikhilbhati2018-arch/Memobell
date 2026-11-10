# MemoBell 2.0 Android project

This is an early Android build of MemoBell.

Included: date-wise multiple reminder notes, add-date action, three-dot menu, search, one-time reminders, daily repeating reminders, 10/30/50-day and custom habit challenges, completion tracking, and tap-to-speak voice input with basic AM/PM and daily-repeat parsing.

Limitations: voice is tap-to-speak, not an always-listening wake phrase; voice parsing is basic and not full AI; meeting lead-time reminders such as 10 minutes before and spoken alarm audio are not implemented yet. Exact alarm behavior depends on Android settings. Please test the APK on your phone before relying on reminders.

## Build
GitHub Actions builds a debug APK when code is pushed to main. Open Actions and download the `MemoBell-2.0-debug-apk` artifact from the successful run.
