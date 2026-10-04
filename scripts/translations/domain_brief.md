# Track & Graph domain brief

## Core context — do not remove

Track & Graph is an Android app for recording, deriving, organising, and visualising personal data. A Tracker stores a time-ordered series of data points entered by the user. Each data point has a timestamp and value and may also have a label or note. A Function is a derived data source that transforms or combines data from Trackers or other Functions; it is not an app feature in the general software sense. Graphs and statistics visualise data from Trackers and Functions. Groups organise Trackers, Functions, Graphs, Reminders, and other Groups. Reminders prompt the user to record data or react to tracking history.

## Terminology and current concepts

In internal and release-note terminology, a “feature” is a data source: either a Tracker or a Function. The feature-history screen displays the data points belonging to one Tracker or Function; “feature history” must not be interpreted as “function history.”

A Symlink places a reference to the same Tracker, Function, Graph, or Group in more than one Group. It does not duplicate or copy that component, and edits made through any placement affect the same underlying component.

“Locked tracking” means repeatedly adding data points while the add-data-point dialog remains open. Locked input fields retain the previous data point’s value. It is unrelated to security, authentication, or preventing edits.

Track & Graph, Lua, Android, API, ID, NaN, and WorkManager are technical or product names. Preserve them unless the target language has a firmly established typographic convention.

A One-Time Reminder (also called “One Time” in release notes) is a standalone reminder, independent of any Tracker or Function. Its first notification can be set to a date and time or to a delay from saving the reminder. It may optionally repeat after the first notification; “one-time” is the reminder type name, not a claim that repeats are impossible. A Time Since Last Reminder measures an interval from the latest data point in a Tracker or Function. “Last” means the last recorded entry, not the previous reminder notification. Its time-of-day option chooses a clock time for the first notification when using calendar intervals. Translate these names as app UI concepts.

“App language” is the Android per-app language setting, reached from Track & Graph’s navigation drawer on Android 13 and newer. It changes this app’s language independently of the phone’s language. Language options include separate Simplified and Traditional Chinese choices.

Graph canvas code draws charts on screen; “canvas” here is a graphics drawing surface, not an art material. AndroidPlot and Vico are chart-library product names and must remain unchanged. A farewell such as “R.I.P.” thanks the retired libraries humorously. X-axis labels identify positions along the horizontal data/time axis. Panning moves the visible range, zooming changes its scale, and subdividing labels means showing finer label intervals, not splitting words or changing stored data.

Keyboard focus means which input field receives typing. Dialog resizing animations are visual transitions as a dialog changes size. CSV export writes selected data to a file; database backup restoration replaces the app’s data from a backup and is distinct from importing CSV data. GitHub is a product name; a GitHub issue is a report submitted to the project’s issue tracker.

App UI labels and domain nouns are human-facing prose, not protected technical identifiers. Translate “App language”, “One Time”, “Time Since Last”, “reminder”, “tracker”, and “function” naturally into the target language, including inside bold spans and headings. Use a complete, meaningful reminder-type name rather than a dangling literal equivalent of “since last”. Preserve Markdown emphasis but adapt surrounding grammar. Likewise translate navigation drawer, keyboard focus, dialog resizing, backup restoration, and import/export; do not leave English phrases in otherwise translated sentences. Use established local technical loanwords where natural, but do not treat English capitalization as a reason to retain English. Product names such as Track & Graph, AndroidPlot, Vico, GitHub, and Android remain unchanged.
