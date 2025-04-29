package com.example.agrohive_1

import androidx.test.ext.junit.runners.AndroidJUnit4 // For AndroidJUnit4 runner
import androidx.test.platform.app.InstrumentationRegistry // For InstrumentationRegistry
import org.junit.Assert.assertEquals // For assertEquals
import org.junit.Test // For @Test annotation
import org.junit.runner.RunWith // For @RunWith annotation

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.agrohive_1", appContext.packageName)
    }
}