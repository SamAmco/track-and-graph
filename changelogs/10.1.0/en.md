## Search! 🎉

**Version 10.1.0** introduces Search! You will find it in 3 places:

### Search for components

You can now search for components using the new search icon in the top app bar. Searching from within a group will only show results under that group in the hierarchy. The search algorithm uses fuzzy matching to help you find things even if you can't remember the exact name. It will also find Trackers and Functions with matching descriptions. You can use Trackers directly in the search results or tap any component card to navigate to that component in the hierarchy.

You will find the import/export dialogs are now compacted into a single dialog with tabs. This dialog remembers the last tab you were on for each group to help keep the workflow streamlined.

### Search for datapoints

You will also find the search icon in the top app bar of the Tracker/Function history screen. Here, datapoints are filtered using a case-insensitive substring match against the datapoint's value, description, and notes. They keep their reverse chronological order.

### Search for notes

You can now also search from the notes screen. This behaves the same way as the history screen. You will also find 2 new filters on the notes screen to toggle between global and datapoint notes.


## Bug Fixes and Improvements

- Fixed bug: graphs disabled too aggressively when starting with Lua disabled
- Fixed bug: Lua function catalog dialog could get stuck loading indefinitely under poor network conditions
- Fixed bug: History screen top bar not animating in on enter
- Fixed bug: bar chart incorrectly stacked data points in the last bar
- Fixed bug: IME action ignored at the end of duration input
- Fixed bug: custom Lua functions not showing enum options with key lookup names			
