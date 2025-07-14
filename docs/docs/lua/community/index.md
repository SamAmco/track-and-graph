# Community Scripts

## Line Graphs
- [Merge Inputs](./line-graphs/merge-inputs/README.md): Plots all provided data sources in a single line where all datapoints are sorted by their timestamp.
- [Cumulative](./line-graphs/cumulative/README.md): Generates a cumulative line graph from the provided data sources.
- [Difference](./line-graphs/difference/README.md): Generates a line graph that shows the difference between each tracked value from the previous value for each provided data source.

## Bar Charts
- [Cumulative](./bar-charts/cumulative/README.md): Generates a cumulative bar chart from the provided data source.
- [Merge Inputs](./bar-charts/merge-inputs/README.md): Plots all provided data sources in a single bar chart where all data points are sorted by their timestamp.

## Text
- [Total this period](./text/total-this-period/README.md): Displays the total value of from the provided data sources in the given period e.g. the total this week.
- [Fraction](./text/fraction/README.md): Displays a fraction representing the ratio of values with specified labels to total values. 
- [Average in duration](./text/average-in-duration/README.md): Calculates the average value of a data source over a specified duration, such as the last 24 hours or the last week.

## Datapoint
- [Last Value Above Threshold](./datapoint/last-value-above-threshold/README.md): Returns the last datapoint with a value above a given threshold. It can accept multiple data sources.
- [Last Value Below Threshold](./datapoint/last-value-below-threshold/README.md): Returns the last datapoint with a value below a given threshold. It can accept multiple data sources.

## Pie Charts
- [Moving Pie Chart](./pie-charts/moving-pie-chart/README.md): Merges all given data sources into one pie chart including all data, or all data in the given period prior to now.
- [Periodic Pie Chart](./pie-charts/periodic-pie-chart/README.md): Displays data for a specific time period such as the current day, week, month, etc. The pie chart resets at the beginning of each new period and accumulates data throughout that period. This script can accept multiple data sources and merges them all into one pie chart.
