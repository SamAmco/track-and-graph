Following #40, this is a proposal adding options to how the current count is shown in the below screen:

```
  +---------------------+
  |                  [:]|
  |    Cigarets         |
  |                     |
  |                     |
  | 🕖 20/06/20 13:09   |
  | ------------------  |
  | 153 Tracked     [+] |
  +---------------------+
```

In short the purpose is to show a count for today, this week or this month, while also working with `Multiple choice` type of data, and the potential future time ranges proposed in #15.
The default would be to show the total count in the data series, but user could optionally choose to display it differently for each data series, for instance below using "Total today":

```
  +---------------------+
  |                  [:]|
  |    Cigarets         |
  |                     |
  |                     |
  | 🕖 20/06/20 13:09   |
  | ------------------  |
  | 3 Today         [+] |
  +---------------------+
```

# Affected screens

Since "[Edit]" and "Track something:" use the same screen, that's one.

The count in the cards on the "tracked" screen would also change, depending on the chosen type of data (i.e. `Numerical` or `Multiple choices`).

# Mockup

This is only a text mockup for now, as it uses the same components as before (label, checkbox, pulldown menu):

```
What would you like to track?
_Cigarets___________


What type of data is this?

  Numerical v
  Multiple

 [x] Set a default value

 _____________1.0___________

 [x] Show custom count

  Tracked (default)  v
  Total Today
  Total this week
  Total this month

```

When the `[ ] Show custom count` checkbox is off, the pulldown menu below does not appear.

# Use cases per data types

* For **numerical**, it can be used to show total, daily, weekly or monthly count.
* For **start and stop tracking** (idea proposed in #15), if it gets implemented, we could use this value to show the total amount of time ranges. In case of a running time range it would mean the total value shown would be updated every minute (e.g. "24 min today"). If it is a problem we can easily decide to not have the checkbox clickable when the data type is not numerical or multiple choice.
* For **multiple choice**, there seems to be 2 distinct cases:

    1. index doesn't matter (e.g. a `{blue=0, red=1, yellow=2}` partition): in that case the user seems to have no reason to want to change from "Tracked (Default)" to another value. It would work, but giving a total for the week has no reason to be useful. 
    2. index has a significant value (e.g. drinking ∈ `{none=0, light=1, heavy=2}`), in that case the following values seem useful:
        "Tracked (Default)" (as it is now)
        "Total today"
        "Total this week"
        "Total this month"
        
A general setting to offset the time for day changing (the "midnight") can be imagined but I prefer to leave it for another proposal to keep this short.


