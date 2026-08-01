package com.kurostream.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = "com.kurostream.app",
        profileBlock = {
            pressHome()
            startActivityAndWait()
            device.wait(Until.hasObject(By.res("home_screen")), 5_000)
            device.findObject(By.res("media_card_0"))?.click()
            device.wait(Until.hasObject(By.res("detail_screen")), 3_000)
            device.pressBack()
        }
    )
}
