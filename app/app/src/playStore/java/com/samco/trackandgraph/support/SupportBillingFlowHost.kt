/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.support

import android.app.Activity

internal interface SupportBillingFlowHost

internal data class AndroidSupportBillingFlowHost(
    val activity: Activity,
) : SupportBillingFlowHost

internal fun Activity.asSupportBillingFlowHost(): SupportBillingFlowHost =
    AndroidSupportBillingFlowHost(this)
