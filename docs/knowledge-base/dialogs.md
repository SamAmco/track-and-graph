---
title: Compose dialog wrappers, footer padding, scrolling, and previews
description: How to choose the shared dialog wrapper, avoid doubled space below action buttons, structure adaptive content, scope Navigation 3 ViewModels, and make dialog previews render reliably.
topics:
  - Choosing CustomDialog versus CustomContinueCancelDialog
  - Bottom padding for text-button action rows
  - Content-only dialog screens embedded in animated hosts
  - Dialog-scoped Navigation 3 ViewModels and configuration changes
  - Non-navigation dialog scopes with rememberViewModelStoreOwner
  - Adaptive scrolling and height
  - Previewing dialog bodies without a Dialog window
keywords: [dialog, CustomDialog, CustomContinueCancelDialog, ContinueCancelButtons, padding, halfDialogInputSpacing, scrolling, adaptive height, preview, Navigation 3, ViewModelStore, rememberViewModelStoreOwner, hiltViewModel, configuration-change, onPop]
---

# Compose dialogs

## Choose the wrapper first

Use `CustomContinueCancelDialog` for a conventional dialog with body content followed by Continue and/or Cancel actions. It owns the scrolling boundary, spacing before the footer, action alignment, and the reduced bottom inset expected below Material text buttons.

Use `CustomDialog` when the structure is genuinely different: animated navigation between content screens, a custom footer, tabs that reach the dialog edge, or another layout that cannot be expressed by the standard wrapper. Reuse `ContinueCancelButtons` for a standard action row inside that custom structure.

## Footer padding contract

Material text buttons already occupy a generous touch target. When `ContinueCancelButtons` or an equivalent text-button row is the final content in a `CustomDialog`, use:

- `inputSpacingLarge` for the top, start, and end insets.
- `halfDialogInputSpacing` for the bottom inset.

Using `inputSpacingLarge` below the action row creates the recurring “double padding” appearance. `CustomContinueCancelDialog` already implements the correct insets and should be preferred when possible. If a dialog animates between a normal content ending and an action-footer screen, the outer bottom inset may need to follow the displayed screen: normal content keeps the regular bottom inset, while the action-footer screen uses the reduced inset.

## Scrolling and adaptive height

Do not give dialog content a fixed height merely to make scrolling work. Let the dialog wrap content until constrained by the window. For a screen with a scrollable middle and fixed actions, use `CustomDialog(scrollContent = false)`, constrain the middle content with `weight(fill = false)`, and keep the action row outside that scrolling region. The shared continue/cancel wrapper already follows this pattern.

## Dialog navigation

Content-only screens embedded in an animated dialog host should not create another `Dialog`. They render their body and footer into the existing host. Keep non-terminal navigation and terminal completion callbacks distinct: Back or Cancel may return to the previous screen inside the host, while an explicit Close after completing the journey should dismiss the host dialog. A standalone entry point can wrap that same content-only screen in `CustomDialog`; both hosts must honor the footer padding contract.

Reusable picker dialogs should expose both a standalone wrapper and a content-only form. For example, `SelectItemDialog` owns the modal window for ordinary callers, while `SelectItemDialogContent` allows a navigation destination to reuse the same selection state, list, and action footer inside an existing dialog window.

## Dialog-scoped ViewModels with Navigation 3

Calling `hiltViewModel()` inside a conditionally composed `Dialog` does not make the ViewModel dialog-scoped. It uses the nearest `ViewModelStoreOwner`, normally the enclosing screen's navigation entry, so removing only the dialog from composition leaves that ViewModel alive.

For a multi-destination dialog that should retain in-progress state across configuration changes but release it when the user closes the dialog:

- Keep its `rememberNavBackStack`, `rememberDecoratedNavEntries`, saveable-state decorator, and ViewModel-store decorator in a host that remains composed while the screen exists.
- Render the actual `Dialog` window only while the workflow is visible and the decorated entries are non-empty.
- On close, clear or pop the dialog back stack before hiding the window. The decorators then observe real pops and clear the destination ViewModels through `onPop`.
- Do not manually create a `ViewModelStoreOwner` with `remember` and clear it in `DisposableEffect`; that also clears during configuration recreation and loses in-progress form state.

Apply the same decorators to nested navigation flows whose individual forms own ViewModels. Popping a nested form entry should destroy that form's ViewModel, so returning to the selector and choosing the same form starts fresh. Closing the outer session destroys the nested stores as part of its ownership tree.

If a standalone dialog needs one ViewModel scope but has no actual navigation, do not invent a one-entry `NavDisplay`. With Lifecycle 2.11+, conditionally compose `rememberViewModelStoreOwner()` at the dialog boundary and provide it through `LocalViewModelStoreOwner`. This purpose-built owner survives configuration changes, inherits the parent's ViewModel factory/creation extras for Hilt, and clears automatically when the visible dialog branch permanently leaves composition. Real nested navigation inside that dialog should still use navigation-entry decorators for its individual form lifetimes.

Do not compensate for screen-owned form ViewModels by registering child `reset()` callbacks with the dialog host. That approach is manual lifecycle emulation, is easy to miss on a Back path, and makes the host aware of child implementation details. Scope each form to the navigation entry that represents its lifetime instead.

Any terminal persistence launched in a destination's `viewModelScope` must complete before that destination is popped. Signal completion from the ViewModel and dismiss afterward; dismissing immediately can cancel the write when the scoped ViewModel is cleared.

## Previews

A preview whose root emits only Compose `Dialog` may appear blank because the modal is hosted in a separate window while the preview root has no measurable content. Extract a pure dialog body and preview it inside a dialog-shaped `Surface` on a bounded preview canvas. Production should continue to wrap that body in the real dialog; fixed canvas dimensions belong only to the preview.
