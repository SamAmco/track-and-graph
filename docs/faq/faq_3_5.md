# How do bar charts work?

Bar charts in Track & Graph provide a visual representation of how your tracked data fluctuates over time. While they share similarities with line graphs, bar charts offer an additional feature: displaying the ratio of each label, similar to a Pie Chart, within each bar. The disadvantage is that bar charts do not support showing multiple tracker on the same graphs where as line graphs do.

Let's consider an example where you sell T-Shirts in different sizes: Small, Medium, and Large. Suppose you want to track the daily sales and the distribution of each size. To set up your bar chart select the "Daily" option for the bar period like so: 

<img src="images/faq_3_5_1.png" width="50%">

Now you can create a bar chart like the one below:

<img src="images/faq_3_5_2.png" width="50%">

However, if your tracking system assigns a value of 1 for Small, 2 for Medium, and 3 for Large T-Shirts, the bar chart will display the sales of a Large T-Shirt as 3 instead of 1. To rectify this, check the box labeled "Check here to count the number of data points tracked rather than the total of their values." This option ignores the assigned values and counts each data point as 1 on the bar chart.

Additionally, you can utilize the scale variable to multiply all the values by a constant factor.

After creating your bar chart, you can tap on it to view it in full-screen mode. The notes section below the chart displays any relevant notes from the data points used. Tapping on a note highlights the corresponding bar on the chart. Moreover, tapping directly on a bar provides further details about it.

<img src="images/faq_3_5_3.png" width="50%">

The labels are ordered such that the label with the highest cumulative value is first (at the bottom of the bar chart) and the label with the smallest cumulative value is last (at the top of the bar chart)
