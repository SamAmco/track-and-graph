# Get started with tracking

To begin tracking you will need to create a Tracker. As a minimum all you will need to give your tracker is a name. You can also optionally add a longer description. 

### Data Point Structure

A Tracker holds a list of data points ordered by time. Each data point has the following information:

- Timestamp
- Value 
- Label (optional)
- Note (optional)

The value is usually just a number, but it can also be a time or duration. If you check the box "This is a time or duration" when setting up the tracker you will be asked for values in hours, minutes and seconds and you will be able to start a timer using that tracker. Internally this information is still stored as a number (specifically the total number of seconds tracked).

Labels and notes are both text inputs. Labels are useful for categorising data. For example if you wanted to track calories for breakfast, lunch, and dinner, you could track the calories in the value field and the word "breakfast", "lunch" or "dinner" in the label field. Use the note field to add more contextual information like "Today was my birthday so I ate lots of cake." Labels are often crucial data for building a graph, notes are added in a list at the bottom underneath when you view the graph. 

### Quick Tracking in Groups

Use groups to organise your trackers. You can move a tracker to a group using the trackers context menu. You can reorder items in a group by holding down and dragging them. When you have multiple trackers in a group you can use the quick track button to quickly track them all at once.

If you are tracking data points for multiple trackers in a group and you change the date/time on one of them, the selected date/time will persist for all subsequent trackers in the group. Data points are added to the trackers as soon as you click add, so cancelling out of the dialog won't lose any data points you have already input. If you swipe back to a previous tracker and change the input data, it will overwrite the data point you added for that tracker unless you change the date/time first. 

Tapping the lock icon at the end of any input field allows you to track multiple data points at once. Any locked fields will not be cleared after tracking each data point. 

### Label Suggestions

By default any previous labels you have tracked will later appear as suggestions on the tracker input dialog. If you press the label button the label field is filled with the selected label. If you long press the button a data point is added immediately with that label. You can change the behaviour of the suggestions in the tracker settings under the advanced options. If you are using "Value and Label" or "Value only" then long pressing the button will fill out the fields without tracking immediately. If you are using "Label only" then long pressing will track the label immediately with a value of 1.

Also in the advanced options you can set a "default value" for a tracker. If you do this you will not be asked for a value when you tap the + button. Instead the default value will immediately be tracked at the current time. You can still track a custom value by long pressing the + button on the tracker.

### Bulk Editing Data Points

To view, edit and delete your data points tap on the center of the tracker card. You can edit all the data points for a tracker at once using the edit button in the top right. 

**WARNING: It is recommended that you back up your data before doing so as this will change all your data points at once and can not be undone.**

The "Where" section allows you to specify which data points you want to update. Any data points that match any of the input data will be updated. For example if you check value and input 56, then any data point that has a value of 56 will be updated. If you check label and input "Small" then any data point with the label "Small" will be updated (this is case sensitive so "Small" will not match "small"). If you check both value and label, then only data points with both the given value and the given label will be updated. 

