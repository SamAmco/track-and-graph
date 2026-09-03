/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.support

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SupportBillingModule {
    @Binds
    abstract fun bindSupportBillingCoordinator(
        implementation: SupportBillingCoordinatorImpl,
    ): SupportBillingCoordinator

    @Binds
    abstract fun bindSupportBillingGateway(
        implementation: GooglePlaySupportBillingGateway,
    ): SupportBillingGateway

    @Binds
    abstract fun bindSupportBillingRecovery(
        implementation: PlaySupportBillingRecovery,
    ): SupportBillingRecovery

    @Binds
    abstract fun bindSupportBillingRecoveryStore(
        implementation: SupportBillingRecoveryStoreImpl,
    ): SupportBillingRecoveryStore
}
