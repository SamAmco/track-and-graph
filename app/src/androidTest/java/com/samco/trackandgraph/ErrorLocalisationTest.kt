package com.samco.trackandgraph;

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.samco.trackandgraph.antlr.evaluation.*
import org.junit.Assert.assertEquals
import org.junit.Test

public class ErrorLocalisationTest {

    @Test
    fun argMissingText() {
        val context: Context =  InstrumentationRegistry.getInstrumentation().targetContext

        val error = ArgMissingError("testFun", 0, NumberValue::class)
        val msg = error.fullLocalizedMessage(context::getString)

        assertEquals(
            "ArgMissingError at Line 0, Column 0: Missing argument for function 'testFun'. Expected an argument of type Number at position 1",
            msg
        )
    }

    @Test
    fun wrongDatatypeTest() {
        val context: Context =  InstrumentationRegistry.getInstrumentation().targetContext

        val error = WrongArgDatatypeError("testFunWrongArg", 0, NumberValue::class, listOf(TimeValue::class, DatapointsValue::class))
        val msg = error.fullLocalizedMessage(context::getString)

        assertEquals(
            "WrongArgDatatypeError at Line 0, Column 0: Argument 1 of function 'testFunWrongArg' has to be of type Time/Duration or Datapoints, but was of type Number",
            msg
        )
    }

    @Test
    fun datapointsWrongTypeTest() {
        val context: Context =  InstrumentationRegistry.getInstrumentation().targetContext

        val error = DatapointsWrongTypeError(listOf(DataType.NUMERICAL, DataType.TIME), DataType.CATEGORICAL)
        val msg = error.fullLocalizedMessage(context::getString)

        assertEquals("DatapointsWrongTypeError at Line 0, Column 0: Datapoints had the wrong type! Expected Numerical or Time, but got Categorical.", msg)
    }

}
