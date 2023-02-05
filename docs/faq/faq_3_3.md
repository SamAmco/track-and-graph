# Statistics

Currently there are only a couple of supported statistics: "Average time between" and "Time since".

---

Both present you with the following interface: 

<img src="images/faq_3_3_1.png" width="50%">

First select a data set and then optionally add filters for label and value range. For example if you want to know the average time between tracking "Yes" for /Daily/Rest you can add the label filter and select "Yes".
 
<img src="images/faq_3_3_2.png" width="50%">

The statistics are calculated in the following ways:

- Average time between shows the duration between the first and last data point matching all filters in the feature divided by the number of data points matching all features minus 1 `(last-first)/(size - 1)`
- Time since last shows the time since the last data point matching all filters

---

<img src="images/faq_3_3_3.png" width="50%">

<img src="images/faq_3_3_1.gif" width="50%">
