package com.samco.trackandgraph.reminders

import android.app.AlarmManager
import com.nhaarman.mockitokotlin2.*
import com.samco.trackandgraph.data.database.dto.CheckedDays
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.model.DataInteractor
import com.samco.trackandgraph.system.AlarmManagerWrapper
import com.samco.trackandgraph.system.ReminderPrefWrapper
import com.samco.trackandgraph.system.StoredAlarmInfo
import com.samco.trackandgraph.system.SystemInfoProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.threeten.bp.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
internal class RemindersHelperImplTest {
    private val dataInteractor: DataInteractor = mock()
    private val reminderPref: ReminderPrefWrapper = mock()
    private val alarmManager: AlarmManagerWrapper = mock()
    private val testScheduler = UnconfinedTestDispatcher()
    private val systemInfoProvider: SystemInfoProvider = mock()

    private val uut = RemindersHelperImpl(
        dataInteractor = dataInteractor,
        reminderPref = reminderPref,
        alarmManager = alarmManager,
        systemInfoProvider = systemInfoProvider,
        io = testScheduler
    )

    @Before
    fun before() {
        whenever(systemInfoProvider.buildVersionSdkInt).thenReturn(33)
        whenever(reminderPref.hasMigratedLegacyReminders).thenReturn(true)
    }

    @Test
    fun syncAlarmsClearsLegacyAlarms() = runTest(testScheduler) {
        //PREPARE
        var storedIntents: String? = "[]"

        val cancels = mutableListOf<Pair<Int, String>>()

        whenever(alarmManager.canScheduleExactAlarms()).thenReturn(false)
        whenever(reminderPref.hasMigratedLegacyReminders).thenReturn(false)
        whenever(dataInteractor.getAllRemindersSync()).thenReturn(
            listOf(
                Reminder(
                    id = 223L,
                    displayIndex = 0,
                    alarmName = "alarm name1",
                    time = LocalTime.of(0, 0),
                    checkedDays = CheckedDays.none().copy(monday = true, tuesday = true)
                ),
                Reminder(
                    id = 123L,
                    displayIndex = 0,
                    alarmName = "alarm name2",
                    time = LocalTime.of(0, 0),
                    checkedDays = CheckedDays.none().copy(monday = true, tuesday = true)
                ),
            )
        )
        whenever(alarmManager.cancelLegacyAlarm(any(), any())).thenAnswer {
            cancels.add(
                Pair(
                    it.arguments[0] as Int,
                    it.arguments[1] as String
                )
            )
        }

        whenever(reminderPref.getStoredIntents()).thenAnswer { storedIntents }
        whenever(reminderPref.putStoredIntents(any())).thenAnswer {
            storedIntents = it.arguments[0] as String
            return@thenAnswer Unit
        }

        //EXECUTE
        //Sync alarms should debounce excess calls
        for (i in 1..100) uut.syncAlarms()
        delay(205)

        //VERIFY
        assertEquals(14, cancels.size)
        assertEquals(
            listOf(2231, 2232, 2233, 2234, 2235, 2236, 2237),
            cancels.take(7).map { it.first }
        )
        assertEquals(
            listOf(1231, 1232, 1233, 1234, 1235, 1236, 1237),
            cancels.drop(7).map { it.first }
        )
        assert(cancels.take(7).all { it.second == "alarm name1" })
        assert(cancels.drop(7).all { it.second == "alarm name2" })
    }

    @Test
    fun syncAlarms() = runTest(testScheduler) {
        //PREPARE
        val reminderId1 = 123L
        val alarmName1 = "alarm name1"
        val reminderId2 = 456L
        val alarmName2 = "alarm name2"

        var storedIntents: String? = """
            [
                {"reminderId":123,"reminderName":"alarm name","pendingIntentId":181612233},
                {"reminderId":123,"reminderName":"alarm name","pendingIntentId":181621083},
                {"reminderId":456,"reminderName":"alarm name2","pendingIntentId":181621907}
            ]
        """.trimIndent()

        val storedAlarmInfos = mutableListOf<StoredAlarmInfo?>()

        whenever(alarmManager.canScheduleExactAlarms()).thenReturn(false)
        whenever(dataInteractor.getAllRemindersSync()).thenReturn(
            listOf(
                Reminder(
                    id = reminderId1,
                    displayIndex = 0,
                    alarmName = alarmName1,
                    time = LocalTime.of(0, 0),
                    checkedDays = CheckedDays.none().copy(monday = true, tuesday = true)
                ),
                Reminder(
                    id = reminderId2,
                    displayIndex = 1,
                    alarmName = alarmName2,
                    time = LocalTime.of(1, 0),
                    checkedDays = CheckedDays.none().copy(thursday = true, friday = true)
                )
            )
        )
        whenever(alarmManager.set(any(), any(), any())).thenAnswer {
            storedAlarmInfos.add(it.arguments[2] as? StoredAlarmInfo)
            return@thenAnswer Unit
        }

        whenever(reminderPref.getStoredIntents()).thenAnswer { storedIntents }
        whenever(reminderPref.putStoredIntents(any())).thenAnswer {
            storedIntents = it.arguments[0] as String
            return@thenAnswer Unit
        }

        //EXECUTE
        //Sync alarms should debounce excess calls
        for (i in 1..100) uut.syncAlarms()
        delay(201)

        //VERIFY
        verify(reminderPref, times(3)).putStoredIntents(any())

        verify(alarmManager, times(4)).set(
            eq(AlarmManager.RTC_WAKEUP),
            any(),
            any()
        )

        assertEquals(4, storedAlarmInfos.size)
        assert(storedAlarmInfos.take(2).all { it?.reminderId == reminderId1 })
        assert(storedAlarmInfos.drop(2).all { it?.reminderId == reminderId2 })
        val pendingIntentIds = storedAlarmInfos.map { it?.pendingIntentId }.toSet()
        assertEquals(4, pendingIntentIds.size)
        pendingIntentIds.forEach {
            val toMatch = "\"pendingIntentId\":$it"
            assertTrue(
                "$storedIntents does not contain the string: $toMatch",
                storedIntents?.contains(toMatch) == true
            )
        }
        verify(alarmManager, times(1)).cancel(
            eq(
                StoredAlarmInfo(
                    reminderId = 123L,
                    reminderName = "alarm name",
                    pendingIntentId = 181612233,
                )
            )
        )
        verify(alarmManager, times(1)).cancel(
            eq(
                StoredAlarmInfo(
                    reminderId = 123L,
                    reminderName = "alarm name",
                    pendingIntentId = 181621083,
                )
            )
        )
        verify(alarmManager, times(1)).cancel(
            eq(
                StoredAlarmInfo(
                    reminderId = 456L,
                    reminderName = "alarm name2",
                    pendingIntentId = 181621907,
                )
            )
        )
        verify(alarmManager, never()).cancelLegacyAlarm(any(), any())
    }

    @Test
    fun clearAlarms() = runTest {
        //PREPARE
        var putStoredIntents: String? = ""
        whenever(reminderPref.getStoredIntents()).thenReturn(
            """
            [
                {"reminderId":123,"reminderName":"alarm name","pendingIntentId":181612233},
                {"reminderId":123,"reminderName":"alarm name","pendingIntentId":181621083},
                {"reminderId":456,"reminderName":"alarm name2","pendingIntentId":181621907}
            ]
        """.trimIndent()
        )
        whenever(reminderPref.putStoredIntents(any())).thenAnswer {
            putStoredIntents = it.arguments[0] as String?
            return@thenAnswer Unit
        }

        //EXECUTE
        uut.clearAlarms()

        //VERIFY
        assertEquals("[]", putStoredIntents)
        verify(alarmManager, times(1)).cancel(
            eq(
                StoredAlarmInfo(
                    reminderId = 123L,
                    reminderName = "alarm name",
                    pendingIntentId = 181612233,
                )
            )
        )
        verify(alarmManager, times(1)).cancel(
            eq(
                StoredAlarmInfo(
                    reminderId = 123L,
                    reminderName = "alarm name",
                    pendingIntentId = 181621083,
                )
            )
        )
        verify(alarmManager, times(1)).cancel(
            eq(
                StoredAlarmInfo(
                    reminderId = 456L,
                    reminderName = "alarm name2",
                    pendingIntentId = 181621907,
                )
            )
        )
    }

    @Test
    fun createAlarms() {
        //PREPARE
        val reminderId = 123L
        val alarmName = "alarm name"

        val storedAlarmInfos = mutableListOf<StoredAlarmInfo?>()
        var putStoredIntents = ""

        whenever(reminderPref.getStoredIntents()).thenReturn(null)
        whenever(alarmManager.canScheduleExactAlarms()).thenReturn(true)
        whenever(alarmManager.setExact(any(), any(), any())).thenAnswer {
            storedAlarmInfos.add(it.arguments[2] as? StoredAlarmInfo)
            return@thenAnswer Unit
        }
        whenever(reminderPref.putStoredIntents(any())).thenAnswer {
            putStoredIntents = it.arguments[0] as String
            return@thenAnswer Unit
        }

        //EXECUTE
        uut.createAlarms(
            Reminder(
                id = reminderId,
                displayIndex = 0,
                alarmName = alarmName,
                time = LocalTime.of(0, 0),
                checkedDays = CheckedDays.all()
            )
        )

        //VERIFY
        verify(reminderPref, times(1)).putStoredIntents(any())

        verify(alarmManager, times(7)).setExact(
            eq(AlarmManager.RTC_WAKEUP),
            any(),
            any()
        )

        assertEquals(7, storedAlarmInfos.size)
        assert(storedAlarmInfos.all { it?.reminderId == reminderId })
        val pendingIntentIds = storedAlarmInfos.map { it?.pendingIntentId }.toSet()
        assertEquals(7, pendingIntentIds.size)
        pendingIntentIds.forEach {
            val toMatch = "\"pendingIntentId\":$it"
            assertTrue(
                "$putStoredIntents does not contain the string: $toMatch",
                putStoredIntents.contains(toMatch)
            )
        }
    }

    @Test
    fun deleteAlarms() {
        //PREPARE
        val reminderId = 123L
        val alarmName = "alarm name"

        val cancelledAlarms = mutableListOf<StoredAlarmInfo?>()
        var putStoredIntents = ""

        whenever(reminderPref.getStoredIntents()).thenReturn(
            """
            [
                {"reminderId":123,"reminderName":"alarm name","pendingIntentId":181612233},
                {"reminderId":123,"reminderName":"alarm name","pendingIntentId":181621083},
                {"reminderId":456,"reminderName":"alarm name2","pendingIntentId":181621907},
                {"reminderId":456,"reminderName":"alarm name2","pendingIntentId":181622404},
                {"reminderId":456,"reminderName":"alarm name2","pendingIntentId":181622881},
                {"reminderId":456,"reminderName":"alarm name2","pendingIntentId":181623382},
                {"reminderId":456,"reminderName":"alarm name2","pendingIntentId":181623828}
            ]
        """.trimIndent()
        )
        whenever(alarmManager.cancel(any())).thenAnswer {
            cancelledAlarms.add(it.arguments[0] as? StoredAlarmInfo)
            return@thenAnswer Unit
        }
        whenever(reminderPref.putStoredIntents(any())).thenAnswer {
            putStoredIntents = it.arguments[0] as String
            return@thenAnswer Unit
        }

        //EXECUTE
        uut.deleteAlarms(
            Reminder(
                id = reminderId,
                displayIndex = 0,
                alarmName = alarmName,
                time = LocalTime.of(0, 0),
                checkedDays = CheckedDays.all()
            )
        )

        //VERIFY
        assertEquals(
            """[{"reminderId":456,"reminderName":"alarm name2","pendingIntentId":181621907},{"reminderId":456,"reminderName":"alarm name2","pendingIntentId":181622404},{"reminderId":456,"reminderName":"alarm name2","pendingIntentId":181622881},{"reminderId":456,"reminderName":"alarm name2","pendingIntentId":181623382},{"reminderId":456,"reminderName":"alarm name2","pendingIntentId":181623828}]""",
            putStoredIntents
        )
        verify(alarmManager, times(1)).cancel(
            eq(
                StoredAlarmInfo(
                    reminderId = 123L,
                    reminderName = "alarm name",
                    pendingIntentId = 181612233,
                )
            )
        )
        verify(alarmManager, times(1)).cancel(
            eq(
                StoredAlarmInfo(
                    reminderId = 123L,
                    reminderName = "alarm name",
                    pendingIntentId = 181621083,
                )
            )
        )
    }
}