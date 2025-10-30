# Functions

Functions allow you to create custom data sources by transforming and combining existing trackers. Using a visual node editor, you can build graphs that process your data in powerful ways without writing any code.

## What are Functions?

A Function is a new data source that derives its data from one or more trackers or functions. Once created, you can use a Function just like a tracker - add it to graphs, view the history as a list, or use it in other Functions.

Think of Functions as pipelines: data flows from your trackers (inputs) through processing steps (function nodes) to produce a new data source (output).

## Creating Your First Function

To create a Function, tap the + icon in a group and select "Function" from the menu.

<!-- TODO: Screenshot of creating a function -->

### The Output Node

When you open the Function editor, you'll see an **Output Node** card. This represents the data source you're creating:

- **Name**: Give your Function a name (required)
- **Description**: Add an optional description to explain what this Function does in more detail
- **Duration checkbox**: Check this if the output values should be interpreted as durations (in seconds). You may want to check this box if your input data sources are timer/duration trackers for example.

The Output Node has an **input connector** on the side. This is where you'll connect the data you want to output.

### Adding Function Nodes

To add nodes to your Function, **long press anywhere in the empty space**. This opens the **Function Catalog**, which shows all available function nodes organized by category.

Each function node typically has:

- **Input connectors**: Where data flows in
- **Output connector**: Where processed data flows out

### Connecting Nodes

To connect nodes, **drag from an output connector to an input connector**. You can connect multiple output connectors to the same input connector - their data will be merged in chronological order.

**Note:** You cannot create connections that would form a cycle in the graph.

## Example 1: Merging Multiple Trackers

The simplest use of Functions is combining data from multiple trackers into a single data source.

Let's say you track exercise in two separate trackers - one for running and one for cycling. You can merge them into a single "All Exercise" data source:

<!-- TODO: Screenshot or GIF showing merging two data sources -->

Now your "All Exercise" Function contains all data points from both trackers, merged in chronological order. You can use this in graphs or statistics to see your total exercise activity.

## Example 2: Converting Units with Transformations

Functions can transform your data using simple operations. Let's say you track your weight in pounds, but want to see it in kilograms for a specific graph.

You can create a "Weight (kg)" Function by connecting your "Weight" tracker to a **Multiply Values** node (set to 0.453592), then connecting that to the Output Node.

<!-- TODO: Screenshot showing data source -> multiply -> output -->

Now you have a data source showing your weight in kilograms without changing your original tracker. This same approach works for other conversions - calories to kilojoules, miles to kilometers, or any other mathematical transformation.

## Example 3: This Week's Activity

For a more advanced example, let's create a Function that automatically shows only the current week's data from your exercise tracker.

This Function combines a **Periodic Data Points** generator node with a **Filter After Last** node. The periodic generator creates a data point with value=1 at the start of each week. The filter then outputs only exercise data that comes after the most recent weekly marker.

<!-- TODO: Screenshot or GIF of complete weekly filter setup -->

The result is a data source that automatically resets each week, showing only your current week's activity. This is useful for weekly progress tracking or creating "this week" summary statistics.
