---
title: Play Store developer support billing
description: Play-only Google Play Billing architecture and Console contract: one product with multiple purchase options, lazy dialog loading, consumable recovery, offer-tag highlighting, and flavor isolation.
topics:
  - Distribution-flavor isolation between Play Billing and Buy Me a Coffee
  - Play Console product and purchase-option contract
  - Lazy loading and support-dialog state
  - Consumption and recovery of completed purchases
  - Release-notes dialog navigation
keywords: [billing, Google Play Billing, playStore, support, tip, consumable, one-time product, purchase option, offer token, highlighted, queryPurchasesAsync, consumeAsync, developer_support, release notes, drawer]
---

# Play Store developer support billing

The `playStore` flavor offers voluntary developer support through Google Play Billing. The `foss` flavor keeps the external Buy Me a Coffee flow. The Billing dependency is `playStoreImplementation`, so its merged `com.android.vending.BILLING` permission and version metadata must be absent from FOSS artifacts.

Google Play support confirmed that direct support for the app developer is not the independent-creator peer-to-peer exemption: the Play-distributed app must use Play Billing and must not link to Stripe, Buy Me a Coffee, or another external payment method.

## Play Console contract

The client knows one product ID, `developer_support`. Configure several active **Buy** purchase options on that product to provide different amounts. The app obtains the localized description, formatted prices, eligibility, and offer tokens from Play; it does not hard-code amounts or currency formatting. The product name is deliberately not displayed: the available state leads with centered price buttons and shows the description below them.

Add the tag `highlighted` to any purchase option that should receive an accent border. Tags are inherited into the offer details returned by the Billing Library. Options are ordered by `priceAmountMicros` for display.

Draft or inactive purchase options are not returned. Each intended option needs pricing, regional availability, and an active state. The app shows an unavailable message when Play returns no eligible options.

## Lazy dialog lifecycle

Billing calls start only when the support dialog opens. Both the drawer item and release-notes support action use the same Play-only billing manager and previewable UI:

1. Connect to Billing and show a spinner-only loading UI.
2. Query `INAPP` owned purchases and silently retry consumption of completed purchases.
3. Leave pending purchases unconsumed and show their pending status.
4. Query `developer_support` and render every eligible Buy purchase option.
5. Launch Play's flow immediately when an option is selected.
6. Consume a `PURCHASED` result; show thanks only after consumption succeeds.

Play retains non-consumed purchases, so consumption recovery deliberately uses `queryPurchasesAsync()` rather than app preferences. If consumption fails after a successful charge, do not report “payment failed”: the charge may be real, and the retained purchase will be retried on a later dialog load. Actual purchase-flow failures show an inline message; selecting any option again is the retry action. User cancellation is silent.

The release-notes dialog animates between changelog and support content inside the same dialog. Returning from the Google Play purchase sheet reveals the support content; backing out of that content returns to the changelog.

The pure price-options content is shared from the `ui` module. The `changelog-viewer` uses it with fake prices and callbacks to exercise the complete dialog transition without depending on or calling Google Play Billing.
