---
title: Compose dialog wrappers, footer padding, scrolling, and previews
description: How to choose the shared dialog wrapper, avoid doubled space below action buttons, structure adaptive content, and make dialog previews render reliably.
topics:
  - Choosing CustomDialog versus CustomContinueCancelDialog
  - Bottom padding for text-button action rows
  - Content-only dialog screens embedded in animated hosts
  - Adaptive scrolling and height
  - Previewing dialog bodies without a Dialog window
keywords: [dialog, CustomDialog, CustomContinueCancelDialog, ContinueCancelDialogContent, ContinueCancelButtons, padding, bottom padding, halfDialogInputSpacing, scrolling, adaptive height, preview, Dialog window]
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

Content-only screens embedded in an animated dialog host should not create another `Dialog`. They render their body and footer into the existing host and use callbacks for back navigation. A standalone entry point can wrap that same content-only screen in `CustomDialog`; both hosts must honor the footer padding contract.

## Previews

A preview whose root emits only Compose `Dialog` may appear blank because the modal is hosted in a separate window while the preview root has no measurable content. Extract a pure dialog body and preview it inside a dialog-shaped `Surface` on a bounded preview canvas. Production should continue to wrap that body in the real dialog; fixed canvas dimensions belong only to the preview.
