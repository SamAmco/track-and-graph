---
title: Play Store developer support billing
description: Play-only Google Play Billing architecture and Console contract: one product with multiple purchase options, lazy dialog loading, consumable recovery, offer-tag highlighting, and flavor isolation.
topics:
  - Distribution-flavor isolation between Play Billing and Buy Me a Coffee
  - Play Console product and purchase-option contract
  - Lazy loading and support-dialog state
  - Consumption and recovery of completed purchases
  - Billing boundaries, callback serialization, and coroutine cancellation
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

In the normal unmarked state, Billing calls start only when the support dialog opens. The only background-to-dialog exception is the targeted foreground recovery described below. Both the drawer item and release-notes support action use the same Play-only coordinator and previewable UI:

1. Connect to Billing and show a spinner-only loading UI.
2. Query `INAPP` owned purchases and silently retry consumption of completed purchases.
3. Leave pending purchases unconsumed, show their pending status, and disable further support purchases until Play reports a terminal state.
4. Query `developer_support` and render every eligible Buy purchase option.
5. Launch Play's flow immediately when an option is selected.
6. Consume a `PURCHASED` result; show thanks only after consumption succeeds.

Play retains non-consumed purchases, so Play remains the source of truth for recovery: reconciliation uses `queryPurchasesAsync()` and never persists purchase details or assumes that a locally recorded checkout was paid. If consumption fails after a successful charge, do not report “payment failed”: the charge may be real, and the retained purchase will be retried. Actual purchase-flow failures show an inline message; selecting any option again is the retry action. User cancellation is silent.

Before starting checkout, the Play flavor persists only a reconciliation-needed generation in `PrefsPersistenceProvider`. Whenever `MainActivity` resumes, it reads that marker; Billing is contacted only when the marker exists. Reconciliation consumes completed purchases, retains the marker for pending purchases and query/consume failures, and clears it only when Play successfully reports no pending or unconsumed support purchases. Clearing is conditional on the generation observed at the start of the query, so a late query cannot erase the marker for a newer checkout. Opening the support dialog still performs the same recovery before loading offers. The FOSS lifecycle hook is a no-op and contains no Billing dependency.

## Billing boundary and callback safety

The implementation has three deliberately narrow layers:

1. The process-scoped `SupportBillingGateway` owns the real `BillingClient`, translates SDK objects and response codes into app-owned values, and retains the SDK offer objects required to launch purchases. It contains no support-flow policy.
2. The process-scoped `SupportBillingCoordinator` owns the long-lived connection, purchase settlement, recovery, consume coalescing, and callback ordering. Its public operations are suspending functions returning app-owned results; it does not expose callbacks or UI state.
3. `SupportBillingViewModel` owns the dialog's sealed loading/available/unavailable/thank-you state and maps coordinator results to presentation decisions.

This is called a coordinator, not an Android service or a data interactor: its role is coordinating an asynchronous platform protocol. Only infrastructure that may need to settle a purchase after the dialog disappears is singleton-scoped. Never put dialog sessions, messages, or thank-you visibility back into that singleton.

Keep the single SDK gateway callback-shaped because it mirrors BillingClient. Convert callbacks to suspension at the coordinator boundary; the ViewModel should only launch and cancel jobs. Do not add another pass-through wrapper or a parallel custom request/cancellation abstraction.

Billing callbacks may arrive late or concurrently. The coordinator serializes its mutable protocol state, coalesces connection waiters, and assigns generations to connection attempts so an old disconnect cannot fail a newer attempt. The gateway separately assigns generations to product queries before mutating its SDK-offer cache: a superseded query must never clear or replace offers returned by a newer query. The ViewModel cancels its previous coroutine when reloading or dismissing, so late results cannot mutate a new or closed screen. Canceling presentation delivery does not cancel settlement: a completed purchase is still consumed after its dialog closes, but it cannot open a late thank-you state.

Purchase launch reconnects if Play disconnected after products were loaded. Once a launch is reserved, another launch is rejected until the existing flow reaches a terminal callback; this prevents an old Play callback being attributed to a newer request. An immediate `ITEM_ALREADY_OWNED` response follows the same recovery path as the asynchronous response.

If `ITEM_ALREADY_OWNED` recovery finds a pending purchase, preserve the pending result rather than treating recovery as idle. Both the ViewModel and the rendered purchase options guard against a second checkout while pending; keep both checks so non-UI callers and future UI changes cannot bypass the rule.

Consumption is coalesced by Play purchase token. This matters when recovery queries overlap or Play delivers the same completed purchase more than once: only one `consumeAsync()` call is made, while every waiting recovery continuation is completed. The ViewModel also guards against rapid repeated taps while checkout is in progress.

Do not remove the persistent foreground marker just because immediate settlement normally succeeds. It covers process death during the Play sheet, a pending payment changing state while the app is absent, and failed consumption. The marker may intentionally survive a canceled or already-settled checkout until the next foreground query proves that nothing remains; that harmless extra query is preferable to clearing based only on an app-side callback.

The Play-flavor coordinator tests use a callback-controllable gateway fake and cover stale, overlapping, duplicate, canceled, and concurrent callback orders without mocking final SDK objects. Separate ViewModel tests cover all presentation transitions and dismissal/reload invalidation. Add regression cases at the layer that owns the behavior whenever sequencing changes.

The release-notes dialog animates between changelog and support content inside the same dialog. Returning from the Google Play purchase sheet reveals the support content. Back or Cancel from the purchase-options screen returns to the changelog, but Close from the successful thank-you state dismisses the entire release-notes dialog so the user does not have to dismiss the support prompt again.

The pure price-options content is shared from the `ui` module. The `changelog-viewer` uses it with fake prices and callbacks to exercise the complete dialog transition without depending on or calling Google Play Billing.
