---
title: Play Store developer support billing
description: Play-only Google Play Billing architecture and Console contract: one product with multiple purchase options, lazy dialog loading, consumable recovery, offer-tag highlighting, and flavor isolation.
topics:
  - Distribution-flavor isolation between Play Billing and Buy Me a Coffee
  - Play Console product and purchase-option contract
  - Lazy loading and support-dialog state
  - Consumption and recovery of completed purchases
  - Callback serialization and dialog-session invalidation
  - Release-notes dialog navigation
keywords: [billing, Google Play Billing, BillingClient, playStore, support, tip, consumable, one-time product, purchase option, offer token, highlighted, callback, race, session, queryPurchasesAsync, consumeAsync, developer_support, release notes, drawer]
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

## Billing boundary and callback safety

The manager depends on a Play-only, logic-free `SupportBillingClient` facade rather than constructing or calling `BillingClient` directly. Its production implementation delegates to the SDK and is bound with Hilt; tests use a callback-controllable fake. Keep policy, state transitions, recovery, and callback-ordering decisions in the manager, not the facade.

Billing callbacks may arrive late or concurrently. The manager therefore serializes all mutable billing state, assigns a generation to each dialog load, and ignores UI updates from older or dismissed generations. Connection attempts have a separate generation so an old disconnect cannot fail a newer load. A purchase can still be consumed after its dialog closes, but it must not open a late thank-you state.

Consumption is coalesced by Play purchase token. This matters when recovery queries overlap or Play delivers the same completed purchase more than once: only one `consumeAsync()` call is made, while every waiting recovery continuation is completed. The purchase-in-progress flag also guards against rapid repeated taps launching multiple Billing flows.

The Play-flavor unit tests drive SDK callbacks explicitly and cover stale, overlapping, duplicate, and concurrent callback orders. Add regression cases there whenever callback sequencing changes.

The release-notes dialog animates between changelog and support content inside the same dialog. Returning from the Google Play purchase sheet reveals the support content. Back or Cancel from the purchase-options screen returns to the changelog, but Close from the successful thank-you state dismisses the entire release-notes dialog so the user does not have to dismiss the support prompt again.

The pure price-options content is shared from the `ui` module. The `changelog-viewer` uses it with fake prices and callbacks to exercise the complete dialog transition without depending on or calling Google Play Billing.
