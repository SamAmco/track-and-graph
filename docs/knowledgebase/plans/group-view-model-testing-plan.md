I would like to test the GroupViewModel on a couple of its core functionalities that are crucial where the implementation is a little complex. To make this possible we need to approach the task in phases. 

## Phase 1: Refactor ViewDataFactory
To make testing simpler you should start by splitting ViewDataFactory and GraphStatDataSourceAdapter into an interface and renaming the existing abstract classes to e.g. ViewDataFactoryImpl. This way in our tests we should be able to write concise fakes for the graph data calculators that don't need to depend on a full graph data calculator implementation. We can have a fake of the GraphStatInteractorProvider that provides fake GraphStatDataSourceAdapters and ViewDataFactorys.

## Phase 2: Split up the DI for DataInteractor

A decomp has already been started to split up the old monolithic DataInteractor into smaller interactors that are more focused on a single area of functionality. i.e.

interface DataInteractor : TrackerHelper, FunctionHelper, ReminderHelper, GroupHelper, GraphHelper {...}

however, the GroupViewModel still injects DataInteractor directly. It should instead inject the specific interactors it needs (GroupHelper, GraphHelper, etc.). This way we can write fakes for each of those specifically

TODO here i get stuck because we still fundamentally depend on the DataInteractor for emitting events. We need to do more decomposition to split up the event emitting functionality into a separate DataEventsInteractor or something before we can get rid of the DataInteractor.

## Phase 3: Write Unit Tests

Can you write me two unit test files that test the GroupViewModel.

1. One file should test display indices. Crucially we're testing that:

- When you first open the group screen the view data reflects the display indices from the data layer
- When the display indices change for a given group in the data module (and the data update event is emitted) the display data updates to reflect the new indices
- When the user starts dragging the indices of the view data update correctly as they drag
- When the user stops dragging the indices are written to the database, and no new updates are emitted to the view layer

2. The second file should test graph updates. Curcially we're testing:

- When the group view model is first opened the graph data for all graphs in that group is calculated and displayed
- When a specific graph is updated (we receive a GraphStatUpdated event) that graphs view data is re-calculated and displayed
- When a group of graphs are all updated at the same time, their graph data is re-calculated and displayed (it should only be re-calculated once for each graph and we should get both loading and complete states for the graph)
- When a graph is deleted it is removed from the group view, but no graph data is re-calculated
- When a graph is added its data is calculated (no other graphs data is calculated)
- When an unknown event is emitted, all graphs are re-calculated
- If a create/update/delete all happen at the same time, we calculate the data for the created and updated graphs and drop the deleted
- Two updates to the same graph in quick succession result in a single re-calculation of that graphs view data

