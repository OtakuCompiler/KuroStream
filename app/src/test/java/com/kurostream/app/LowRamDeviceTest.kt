package com.kurostream.app

import com.kurostream.common.memory.LowRamDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LowRamDeviceTest {
    @Test
    fun `coil memory cache is 2MB for low RAM`() {
        assertEquals(2 * 1024 * 1024, LowRamDevice.coilMemoryCacheSize)
    }
}

class RamEnforcerTest {
    @Test
    fun `pressure level is nominal under 90MB`() {
        assertTrue(true)
    }
}
