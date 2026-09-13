# Compose screenshot tests

This source set has two distinct uses:

- `regression/graphs/` contains visual regression tests. Their references under
  `src/screenshotTestFossDebug/reference/.../regression/graphs/` are committed to source
  control and reviewed when graph rendering intentionally changes.
- `playstore/` and `tutorial/` are asset-generation previews. The recording scripts select
  those packages and copy their output into the appropriate published resource locations.

Update all FOSS debug references after an intentional visual change:

```bash
cd app
./gradlew :app:updateFossDebugScreenshotTest
```

Validate the committed references without replacing them:

```bash
cd app
./gradlew :app:validateFossDebugScreenshotTest
```

Graph regression previews render production graph composables in list mode at a representative
380dp card-content width. Preview height is deliberately unspecified, so each golden captures the
composable's wrapped height; high-cardinality legend cases verify that the chart expands rather
than clipping its legend. Structural configurations render once in English. A smaller set that
covers localized date tiers, histogram units, percentages, and empty-label text renders in every
supported locale. Keep the set focused on distinct layout, data-range, density, localization, and
series-composition risks rather than every possible permutation.
