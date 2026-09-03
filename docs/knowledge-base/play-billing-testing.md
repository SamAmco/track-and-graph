---
title: Play Billing setup and manual test script
description: Console, internal-track, license-tester, and device test procedure for repeatable developer-support purchases, including success, decline, pending, recovery, regional, and artifact-isolation checks.
topics:
  - Uploading the first Billing-enabled internal release
  - Configuring developer_support and its Buy purchase options
  - License testing and installation through Google Play
  - Successful, declined, canceled, pending, and interrupted purchase tests
  - Regional pricing and FOSS/Play artifact isolation
keywords: [billing, Google Play Billing, testing, Play Console, internal testing, license tester, Play Billing Lab, consumable, pending, slow test card, developer_support, purchase recovery, test script]
---

# Play Billing setup and manual test script

Use this after changes to the developer-support purchase flow and before its first production rollout. The architecture and client behavior are documented in `play-billing.md`.

## Prerequisite implementation checks

- Play Billing is isolated to the `playStore` flavor.
- The Play artifact includes `com.android.vending.BILLING`; the FOSS artifact does not.
- `developer_support` is the only hard-coded product ID.
- Completed purchases are consumed, not merely acknowledged, so the product can be purchased repeatedly.
- Pending purchases are neither consumed nor treated as successful.
- A checkout attempt persists a targeted recovery marker before Play's purchase flow starts. Resuming the app reconciles only while that marker exists; opening the support dialog also performs recovery.

## Publish a Billing-enabled internal release

1. Ensure the version code is higher than the version currently uploaded to Play.
2. Build the signed Play bundle using the normal release configuration:

   ```bash
   cd app
   ./gradlew clean :app:bundlePlayStoreRelease
   ```

3. In Play Console, open **Test and release → Testing → Internal testing**.
4. Upload the Play Store AAB, complete the release, and roll it out to internal testers.
5. Wait for Play to process the artifact. The one-time-products page should then recognise that the app supports Billing.

Use an internal-track installation for normal end-to-end testing. `playStoreDebug` has an `.debug` application-ID suffix and therefore does not match the production Play Console package.

## Configure the product

In **Monetize with Play → Products → One-time products**, create:

- Product ID: `developer_support`
- Accurate support-oriented name and description
- Several purchase options, each with type **Buy**
- Regional availability and pricing for every intended market
- Active status for every option intended to appear in the app

Example purchase-option IDs are `support_small`, `support_medium`, and `support_large`. These IDs are not hard-coded by the client. Add the tag `highlighted` to the option that should receive the accent border. Do not configure Rent, pre-order, or multi-quantity behavior for this flow.

The app displays Play's localized formatted prices and the product description. Draft, inactive, regionally unavailable, or user-ineligible options should not appear.

## Configure the tester

1. Add the Google account to the internal-test tester list.
2. In **Play Console → Settings → License testing**, add the same account as a license tester. The publisher account is already treated as licensed, but it still needs access to the internal release.
3. Open the internal-test opt-in link using that account.
4. Install the app from Google Play, rather than sideloading it.
5. On a device with several Google accounts, confirm that the purchasing account is the account that installed the app.

License testers see test payment instruments and are not charged. A tester who is only on the internal track, but not the license-testing list, can incur real charges.

## Manual test cases

### Product loading

1. Open support from the drawer and from release notes.
2. Confirm that a spinner is shown while loading.
3. Confirm that all active eligible prices appear in ascending order.
4. Confirm localized currency formatting and the Console-supplied description.
5. Confirm only tagged options have the highlighted border.
6. With no eligible active options, confirm the unavailable state is shown without a crash.

### Successful and repeatable purchase

1. Choose an amount and use **Test instrument, always approves**.
2. Confirm options disable and the progress indicator appears while checkout is active.
3. Complete Google's purchase sheet.
4. Confirm the thank-you content appears only after consumption succeeds.
5. Close and reopen support.
6. Purchase the same option again. This must succeed; otherwise the previous purchase was not consumed.
7. Repeat for every configured amount.

### User cancellation

1. Open Google's purchase sheet and cancel or press Back.
2. Confirm the support options return to idle.
3. Confirm there is no payment-failed message and no thank-you content.

### Declined payment

1. Use **Test instrument, always declines**.
2. Confirm an inline payment-failed message appears.
3. Confirm no thank-you content appears.
4. Select an option again and confirm that this acts as the retry.

### Pending payment that approves

1. Use **Slow test card, approves after a few minutes**.
2. Confirm the pending message appears and every option remains disabled.
3. Confirm the purchase is not consumed while its state is `PENDING`.
4. Test once while leaving the app running until Play changes it to `PURCHASED`.
5. Test again after terminating the app while pending, then relaunch after approval.
6. Confirm reconciliation consumes the completed purchase and the option becomes purchasable again.

### Pending payment that declines

1. Use **Slow test card, declines after a few minutes**.
2. Confirm the purchase initially remains pending with options disabled.
3. Confirm there is no thank-you content.
4. After Play cancels the transaction, foreground the app or reopen support and confirm options return to idle.

### Process death and failed consumption

1. Complete an approved purchase and terminate the app immediately as Google's sheet closes.
2. Relaunch the app; foreground reconciliation should run automatically.
3. Confirm the retained completed purchase is consumed.
4. Confirm the same option can be purchased again.
5. Repeat with connectivity unavailable during reconciliation, then restore connectivity and background/foreground the app to retry.

### Dialog and concurrency behavior

- Rapidly tap an option and confirm only one Google purchase sheet launches.
- Dismiss and reopen while products are loading; visible options must remain launchable and must not produce “Unknown support offer.”
- In release notes, Cancel/Back from support returns to the changelog.
- Close from thank-you dismisses the entire release-notes flow.
- In the standalone dialog, Cancel and thank-you Close both dismiss it.

### Regions and localization

Using Play Billing Lab with the same license-tester account:

- Change Play country and confirm currency and formatting.
- Confirm only options available in that region appear.
- Confirm a region with no eligible options produces the unavailable state.

Billing Lab country/test configurations are temporary. If changes are slow to appear, re-authenticate Billing Lab and clear the Play Store cache on the test device.

## Artifact isolation check

Build both variants and inspect the resulting artifacts:

```bash
cd app
./gradlew :app:assemblePlayStoreDebug :app:assembleFossDebug
```

- The Play artifact must not contain Buy Me a Coffee links, code, or `bmc_logo`.
- The FOSS artifact must not contain the Play Billing dependency or Billing permission.

## Final validation

After license testing passes, make one controlled smallest-value purchase with a non-license-test account on a test track. This is a real charge; verify the order in Play Console and refund it if appropriate. Then use a staged production rollout and monitor crashes, Billing response failures, refunds, and support orders.

## Official references

- [Test Google Play Billing integrations](https://developer.android.com/google/play/billing/test)
- [One-time purchase lifecycle](https://developer.android.com/google/play/billing/lifecycle/one-time)
- [Create and manage one-time products](https://support.google.com/googleplay/android-developer/answer/16430488)
- [Set up application licensing](https://support.google.com/googleplay/android-developer/answer/6062777)
- [Set up an internal test](https://support.google.com/googleplay/android-developer/answer/9845334)
