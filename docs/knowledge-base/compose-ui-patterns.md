---
title: Compose UI patterns — ViewModel binding, pure UI, and previews
description: The standard three-layer composable pattern used throughout the app — ViewModel-binding composable collects state and builds callbacks, pure-UI composable takes only state/callbacks, @Preview calls the pure-UI composable.
topics:
  - ViewModel-binding layer collects state and calls the pure-UI composable
  - Pure-UI composable takes only state values and callbacks — no ViewModels
  - @Preview functions call the pure-UI composable with hardcoded data
  - Naming conventions for each layer
keywords: [compose, composable, preview, ViewModel, pure UI, state, callbacks, pattern, split, GroupDeleteDialog, GroupScreen]
---

# Compose UI Patterns

## Three-Layer Composable Pattern

Every non-trivial screen or dialog follows this structure:

### 1. ViewModel-binding composable
- Takes ViewModel(s) as parameters
- Collects state via `collectAsStateWithLifecycle()`
- Builds callback lambdas that call ViewModel methods
- Calls the pure-UI composable, passing derived state and callbacks
- No rendering logic — just wiring

```kotlin
@Composable
internal fun GroupDeleteDialog(
    groupDialogsViewModel: GroupDialogsViewModel,
    groupViewModel: GroupViewModel,
) {
    val itemForDeletion = groupDialogsViewModel.itemForDeletion.collectAsStateWithLifecycle().value
        ?: return
    GroupDeleteDialogContent(
        itemForDeletion = itemForDeletion,
        onDismiss = { groupDialogsViewModel.hideDeleteDialog() },
        onDeleteEverywhere = { /* call groupViewModel methods */ },
        onRemoveFromGroup = { /* call groupViewModel methods */ },
    )
}
```

### 2. Pure-UI composable
- Takes **only** state values (DTOs, primitives, lists) and callback lambdas
- No ViewModels, no `hiltViewModel()`, no `collectAsStateWithLifecycle()`
- All parameters have sensible defaults (empty lambdas, empty lists)
- Renders UI using Compose Material3 components
- May hold local UI-only state (`remember { mutableStateOf(...) }`)
- This is the function that gets previewed

```kotlin
@Composable
private fun GroupDeleteDialogContent(
    itemForDeletion: DeleteItemDto,
    onDismiss: () -> Unit,
    onDeleteEverywhere: () -> Unit,
    onRemoveFromGroup: () -> Unit,
) { /* all rendering here */ }
```

### 3. Preview function(s)
- Marked `@Preview` (or `@Preview(showBackground = true)`)
- Wrapped in `TnGComposeTheme { }`
- Calls the **pure-UI composable** directly with hardcoded values
- No context, no Hilt, no coroutines
- Provide multiple previews for different states (e.g. unique vs non-unique)

```kotlin
@Preview
@Composable
private fun GroupDeleteDialogUniquePreview() {
    TnGComposeTheme {
        GroupDeleteDialogContent(
            itemForDeletion = DeleteItemDto(id = 1L, type = DeleteType.TRACKER, unique = true),
            onDismiss = {},
            onDeleteEverywhere = {},
            onRemoveFromGroup = {},
        )
    }
}
```

## Naming Conventions

| Layer | Example names |
|---|---|
| ViewModel-binding | `GroupDeleteDialog`, `GroupScreen`, `RemindersScreen` |
| Pure-UI | `GroupDeleteDialogContent`, `GroupScreenView`, overloaded `RemindersScreen` |
| Preview | `GroupDeleteDialogUniquePreview`, `GroupScreenViewEmptyPreview` |

There is no single enforced suffix — some files use `Content`, some use `View`, and some overload the same name with a different signature. Prefer `Content` for dialogs and `View` for screens.

## Where to Find Examples

- `group/GroupDeleteDialog.kt` — clean dialog example (ViewModel-binding + Content + 2 previews)
- `group/GroupScreen.kt` — large screen example (`GroupScreen` → `GroupScreenContent` → `GroupScreenView`)
- `group/Group.kt` — leaf component with no ViewModel layer needed (pure UI + preview only)
- `group/Tracker.kt` — complex leaf component with multiple sub-composables and previews
