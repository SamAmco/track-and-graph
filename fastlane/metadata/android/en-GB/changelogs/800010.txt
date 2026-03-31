## Symlinks! 🎉

**Version 10.0.0** introduces Symlinks. Symlinks allow you to have the same tracker, graph, function, or even group in multiple groups. It's not a duplicate, or a copy, it's a reference to the same component. Any changes you make to one will be reflected in all the others. To get started with Symlinks simply tap the + button in the top right of any group and select "Symlink".

## New Data Point Actions! 📝

You can now add data points to a tracker directly from the feature history screen (the one that opens when you tap the tracker card). You'll find the new floating action button at the bottom right of the screen.

![Data Point Actions](https://raw.githubusercontent.com/SamAmco/track-and-graph/refs/heads/master/changelogs/10.0.0/data_point_actions.jpg)

There is also a new multi-select mode in the feature history screen that allows you to move, copy, or delete multiple data points at once. You can even copy data points from a function to a tracker! Enter multi-select mode by long pressing on a data point, select the data points you want and look for the new actions fabs at the bottom right of the screen.

## Locked Tracking! 🔒

There's a new feature in the add data point dialog that allows you to add multiple data points for one tracker in succession. Look for the new lock icon at the end of the input fields:

![Locked Tracking](https://raw.githubusercontent.com/SamAmco/track-and-graph/refs/heads/master/changelogs/10.0.0/locked_tracking.jpg)

When any lock is enabled, the dialog will stay open after adding a data point, and any locked fields will be pre-filled with the same value as the previous data point.

## Bug Fixes and Improvements

- Fixed bug: graphs stuck infinite loading (sorry about that)
- Fixed bug: editing a reminder after updating a reminder fails to open the dialog
- Fixed bug: missing translations for reminders
- Fixed bug: forced unique work manager requests per reminder to try and avoid duplicate reminders
- Copied reminders are now scheduled immediately
- Fixed bug: copied reminders appearing in the wrong location
- Fixed bug: notes not showing under graphs
- Fixed bug: standard deviation returns NaN on floating point precision errors (in function nodes)
- Fixed bug: display indices not updating correctly when IDs clash
- Link lua script info button to developer guide in node selection dialog
- Improved reliability of track widgets after app updates
- Upgrade library dependencies for improved performance and stability
- Now targeting Android API level 36
            