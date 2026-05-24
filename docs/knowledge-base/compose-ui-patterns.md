---
title: Compose UI patterns — ViewModel binding, pure UI, previews, and chip state
description: The standard three-layer composable pattern used throughout the app — ViewModel-binding composable collects state and builds callbacks, pure-UI composable takes only state/callbacks, new screens and UI components include @Preview by default; reusable controls live in app/ui; selectable TextChip styling is driven only by selected state, with press feedback left to ripple indication.
topics:
  - ViewModel-binding layer collects state and calls the pure-UI composable
  - Pure-UI composable takes only state values and callbacks — no ViewModels
  - New screens and UI components should include @Preview functions by default
  - @Preview functions call the pure-UI composable with hardcoded data
  - Reuse shared app/ui controls before defining local Material wrappers
  - Naming conventions for each layer
  - TextChip/TngChip selected styling versus pressed-state feedback
keywords: [compose, composable, preview, ViewModel, pure UI, state, callbacks, pattern, split, shared-ui, app-ui, buttons, TextButton, SmallTextButton, RowCheckbox, Divider, spacing, PasswordTextField, GroupDeleteDialog, GroupScreen, TextChip, TngChip, chip, selected, pressed, ripple, interactionSource, collectIsPressedAsState]
---

# Compose UI Patterns

## Shared UI Module Boundary

Generic, reusable UI components live in `app/ui`. The app is fully Compose, so package names there do not need an extra `compose` segment; shared components are organized under packages such as `ui`, `theming`, and `utils`.

Move components into `app/ui` when they are UI-only and can be used without depending on app feature packages, ViewModels, `DataInteractor`, or data DTOs. Keep components in `app/app` when they still import app helpers, settings, feature callbacks, or data-layer DTOs. Prefer parameterizing copy/callbacks/resources for reusable components instead of moving feature-specific strings into `app/ui`.

When adding or migrating shared UI, add preview functions in the same file unless there is a specific blocker. Older shared UI files are not perfectly consistent yet, but the expected direction is that reusable UI has previews so changes can be inspected without wiring a feature screen.

Previews are the default for new screens, dialogs, and UI components. Skip them only when there is a concrete blocker such as an AndroidView/runtime dependency that cannot reasonably be faked; otherwise split the UI so a pure composable can be previewed with hardcoded state.

Before defining local wrappers around Material components, check `app/ui` for existing app-styled controls. Common examples include buttons (`TextButton`, `SmallTextButton`, `FullWidthIconTextButton`), text fields (`FullWidthTextField`, `PasswordTextField`), dividers (`Divider`, `GradientDivider`), spacing (`DialogInputSpacing`, `InputSpacingLarge`, `cardPadding`, etc.), and row controls (`RowCheckbox`). If a reusable control is missing and the need is generic, add it to `app/ui` with a preview rather than keeping a one-off feature-local version.

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
- For shared UI in `app/ui`, include previews for new components and for components migrated out of `app/app` unless a dependency makes previewing impractical
- For new screens, dialogs, and reusable controls, add previews in the initial change rather than as a later cleanup

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

## Selectable Chip Press Feedback

`TextChip`/`TngChip` is used for selectable chips as well as simple chip-shaped buttons. For selectable chips, primary color and primary border should be driven only by the committed `isSelected` state. Do not OR in `collectIsPressedAsState()` when deciding selected colors.

The reason is subtle when selection is persisted asynchronously (for example a chip toggling a DataStore-backed filter): press down can make the chip look selected, release can briefly return it to the old unselected state, then the persisted value emits and selects it again. That reads as an enabled → disabled → enabled flicker. The normal ripple/indication is enough press feedback; selected styling should represent actual selection state.
