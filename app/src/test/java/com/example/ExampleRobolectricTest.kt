package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Chấm Công", appName)
  }

  @Test
  fun `test vietnamese month formatting`() {
    val sdfInput = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)
    val sdfOutput = java.text.SimpleDateFormat("'Tháng' MM / yyyy", java.util.Locale("vi", "VN"))
    val date = sdfInput.parse("2026-07")
    val result = date?.let { sdfOutput.format(it) }
    assertEquals("Tháng 07 / 2026", result)
  }
}
