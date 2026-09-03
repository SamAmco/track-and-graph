/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.support

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SupportBillingViewModelTest {
    private lateinit var coordinator: FakeSupportBillingCoordinator
    private lateinit var viewModel: SupportBillingViewModel
    private val host = object : SupportBillingFlowHost {}
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coordinator = FakeSupportBillingCoordinator()
        viewModel = SupportBillingViewModel(coordinator)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() {
        assertEquals(SupportBillingState.Loading, viewModel.state.value)
    }

    @Test
    fun `available load result becomes idle available state`() {
        viewModel.load()
        coordinator.completeLoad(availableResult())
        assertEquals(availableState(), viewModel.state.value)
    }

    @Test
    fun `pending load result becomes pending available state`() {
        viewModel.load()
        coordinator.completeLoad(availableResult(hasPendingPurchase = true))
        assertEquals(
            availableState(checkoutState = SupportCheckoutState.PaymentPending),
            viewModel.state.value,
        )
    }

    @Test
    fun `unavailable load result preserves pending status`() {
        viewModel.load()
        coordinator.completeLoad(SupportLoadResult.Unavailable(hasPendingPurchase = true))
        assertEquals(SupportBillingState.Unavailable(true), viewModel.state.value)
    }

    @Test
    fun `reloading cancels old load and ignores its callback`() {
        viewModel.load()
        val first = coordinator.loadCalls.single()

        viewModel.load()
        first.complete(availableResult())

        assertTrue(first.canceled)
        assertEquals(SupportBillingState.Loading, viewModel.state.value)
        coordinator.completeLoad(SupportLoadResult.Unavailable(false), index = 1)
        assertEquals(SupportBillingState.Unavailable(false), viewModel.state.value)
    }

    @Test
    fun `dismiss cancels requests and resets state`() {
        viewModel.load()
        coordinator.completeLoad(availableResult())
        viewModel.purchase(host, OPTION_ID)
        val purchaseCall = coordinator.purchaseCalls.single()

        viewModel.dismiss()

        assertTrue(purchaseCall.request.canceled)
        assertEquals(SupportBillingState.Loading, viewModel.state.value)
    }

    @Test
    fun `unknown option reports payment failure without starting purchase`() {
        loadAvailable()
        viewModel.purchase(host, "unknown")
        assertEquals(
            availableState(checkoutState = SupportCheckoutState.PaymentFailed),
            viewModel.state.value,
        )
        assertTrue(coordinator.purchaseCalls.isEmpty())
    }

    @Test
    fun `purchase enters progress and forwards host and option`() {
        loadAvailable()
        viewModel.purchase(host, OPTION_ID)
        assertEquals(
            availableState(checkoutState = SupportCheckoutState.InProgress),
            viewModel.state.value,
        )
        assertSame(host, coordinator.purchaseCalls.single().host)
        assertEquals(OPTION_ID, coordinator.purchaseCalls.single().optionId)
    }

    @Test
    fun `second tap during progress does not start another purchase`() {
        loadAvailable()
        viewModel.purchase(host, OPTION_ID)
        viewModel.purchase(host, OPTION_ID)
        assertEquals(1, coordinator.purchaseCalls.size)
    }

    @Test
    fun `purchase is disabled while an earlier payment remains pending`() {
        viewModel.load()
        coordinator.completeLoad(availableResult(hasPendingPurchase = true))

        viewModel.purchase(host, OPTION_ID)

        assertTrue(coordinator.purchaseCalls.isEmpty())
        assertEquals(
            availableState(checkoutState = SupportCheckoutState.PaymentPending),
            viewModel.state.value,
        )
    }

    @Test
    fun `completed purchase shows thank you`() {
        startPurchase()
        coordinator.completePurchase(SupportPurchaseResult.Completed)
        assertEquals(SupportBillingState.ThankYou, viewModel.state.value)
    }

    @Test
    fun `pending purchase shows pending message`() {
        startPurchase()
        coordinator.completePurchase(SupportPurchaseResult.Pending)
        assertEquals(
            availableState(checkoutState = SupportCheckoutState.PaymentPending),
            viewModel.state.value,
        )
    }

    @Test
    fun `failed purchase shows failure message`() {
        startPurchase()
        coordinator.completePurchase(SupportPurchaseResult.Failed)
        assertEquals(
            availableState(checkoutState = SupportCheckoutState.PaymentFailed),
            viewModel.state.value,
        )
    }

    @Test
    fun `canceled purchase returns to idle`() {
        assertPurchaseResultReturnsToIdle(SupportPurchaseResult.Canceled)
    }

    @Test
    fun `deferred consumption returns to idle without payment failure`() {
        assertPurchaseResultReturnsToIdle(SupportPurchaseResult.ConsumptionDeferred)
    }

    @Test
    fun `recovered purchase returns to idle`() {
        assertPurchaseResultReturnsToIdle(SupportPurchaseResult.Recovered)
    }

    @Test
    fun `dismissed screen ignores late purchase result`() {
        startPurchase()
        val call = coordinator.purchaseCalls.single()

        viewModel.dismiss()
        call.request.complete(SupportPurchaseResult.Completed)

        assertTrue(call.request.canceled)
        assertEquals(SupportBillingState.Loading, viewModel.state.value)
    }

    @Test
    fun `reload cancels active purchase`() {
        startPurchase()
        val purchaseRequest = coordinator.purchaseCalls.single().request

        viewModel.load()

        assertTrue(purchaseRequest.canceled)
        assertEquals(SupportBillingState.Loading, viewModel.state.value)
        assertEquals(2, coordinator.loadCalls.size)
    }

    @Test
    fun `purchase outside available state is ignored`() {
        viewModel.purchase(host, OPTION_ID)
        assertTrue(coordinator.purchaseCalls.isEmpty())
        assertEquals(SupportBillingState.Loading, viewModel.state.value)
    }

    private fun loadAvailable() {
        viewModel.load()
        coordinator.completeLoad(availableResult())
    }

    private fun startPurchase() {
        loadAvailable()
        viewModel.purchase(host, OPTION_ID)
    }

    private fun assertPurchaseResultReturnsToIdle(result: SupportPurchaseResult) {
        startPurchase()
        coordinator.completePurchase(result)
        assertEquals(availableState(), viewModel.state.value)
    }

    private fun availableResult(hasPendingPurchase: Boolean = false) =
        SupportLoadResult.Available(
            description = DESCRIPTION,
            options = listOf(OPTION),
            hasPendingPurchase = hasPendingPurchase,
        )

    private fun availableState(
        checkoutState: SupportCheckoutState = SupportCheckoutState.Idle,
    ) = SupportBillingState.Available(
        description = DESCRIPTION,
        options = listOf(OPTION),
        checkoutState = checkoutState,
    )

    private companion object {
        const val OPTION_ID = "small"
        const val DESCRIPTION = "Support development"
        val OPTION = SupportPurchaseOption(OPTION_ID, "£1.00", 1_000_000, false)
    }
}
