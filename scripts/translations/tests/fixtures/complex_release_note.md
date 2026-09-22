---
version: "10.0.0-test"
audience: [play, foss]
---

# Track & Graph 10.0: Symlinks, actions, and more 🎉

**Version 10.0.0** introduces *Symlinks*. A Symlink is **not a copy**: changes made in one group appear everywhere. Tap `+`, choose **Symlink**, and select a tracker.

> **Important:** Existing items are not moved.
>
> They remain available from their original group.

## Data-point actions

You can now:

1. Add a point from **History**.
2. Long-press a point to enter multi-select mode.
   - Move or copy selected points.
   - Delete several points at once.
3. Keep tracking after saving:
   - [x] Lock a value
   - [ ] Lock a note

![The multi-select toolbar](https://raw.githubusercontent.com/SamAmco/track-and-graph/refs/heads/master/changelogs/10.0.0/data_point_actions.jpg "Data point actions")

Read the [function guide](https://github.com/SamAmco/track-and-graph/blob/master/docs/docs/lua/functions.md#examples), visit <https://github.com/SamAmco/track-and-graph>, or email <help@example.com>.

The literal characters \* and \_ are not emphasis. An entity remains&nbsp;unchanged.

### Configuration example

Use `tng.getDayData(featureId, epochDay)`; do **not** translate identifiers.

```lua
local value = tng.getNumber("weight")
-- Markdown-looking code must stay intact: **not bold**
return { timestamp = timestamp, value = value }
```

    Indented code is also immutable: [label](https://example.com)

### Compatibility table

| Feature | Before | Now |
| :-- | --: | :--: |
| Groups | One location | Many locations |
| Notes | Plain text | **Markdown** |

<details>
<summary>Technical detail</summary>

The database stores one component and several references.

</details>

<!-- This comment must remain byte-for-byte unchanged. -->

See the [full documentation][docs].[^privacy]

[docs]: https://example.com/docs?q=track%20graph#symlinks "Documentation"
[^privacy]: No tracking data is uploaded.

---

## Fixes & polish

- Fixed graphs that displayed `NaN` after floating-point rounding.
- Preserved names such as "R&D", apostrophes like "don't", and emoji 🔒.
- URLs without link markup stay intact: https://example.com/releases/10.0?from=app&mode=full#notes
- ~~Removed an obsolete workaround.~~

That’s everything—thanks for testing!
