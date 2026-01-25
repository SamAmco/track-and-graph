package com.samco.trackandgraph.settings

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Duration
import org.threeten.bp.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

//TODO this is a copy paste of the AggregationPreferences .. Need to align this so it's all
// in one place. I think that place is here, but we need to be able to inject this where it's
// needed in the factories which might require some refactoring, so there's a duplicate for now.
class TngSettingsImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : TngSettings {
    override val firstDayOfWeek: DayOfWeek
        get() = WeekFields.of(getLocale()).firstDayOfWeek
    override val startTimeOfDay: Duration
        get() = Duration.ofSeconds(0)

    private fun getLocale(): Locale =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
}