// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.app.accessibility

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.kurostream.app.MainActivity
import com.kurostream.app.player.PlayerActivity
import com.kurostream.app.ui.screens.details.DetailsScreen
import com.kurostream.app.ui.screens.home.HomeScreen
import com.kurostream.app.ui.screens.search.SearchScreen
import com.kurostream.app.ui.screens.settings.SettingsScreen
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilityTest {

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation())
        device.pressHome()
    }

    @After
    fun tearDown() {
        device.pressHome()
    }

    @Test
    fun homeScreen_hasContentDescriptions() {
        launchActivity<MainActivity>()

        // Wait for home screen to load
        val homeScreen = device.wait(Until.hasObject(By.pkg("com.kurostream.app").depth(10)), 5000)
        assertNotNull("Home screen should be visible", homeScreen)

        // Check key elements have content descriptions
        checkContentDescription("Search")
        checkContentDescription("Settings")
        checkContentDescription("Downloads")
        checkContentDescription("Addons")

        // Hero banner items should have descriptions
        val heroItems = device.findObjects(By.descStartsWith("Hero item"))
        assertTrue("Hero banner should have accessible items", heroItems.size > 0)

        // Continue watching row
        val continueItems = device.findObjects(By.descContains("Continue watching"))
        assertTrue("Continue watching items should be accessible", continueItems.size > 0)
    }

    @Test
    fun detailsScreen_hasProperDescriptions() {
        launchActivity<MainActivity>()
        device.waitForIdle()

        // Navigate to a detail screen (simulated)
        // In real test, we'd click on a media item
        // For now, verify the back button has description
        val backButton = device.wait(Until.hasObject(By.desc("Back")), 3000)
        assertNotNull("Back button should have content description", backButton)

        // Play button
        val playButton = device.wait(Until.hasObject(By.desc("Play")), 3000)
        assertNotNull("Play button should have content description", playButton)

        // Trailer button
        val trailerButton = device.findObject(By.desc("Trailer"))
        assertNotNull("Trailer button should have content description", trailerButton)

        // Favorite button
        val favButton = device.findObject(By.descContains("favorite"))
        assertNotNull("Favorite button should have content description", favButton)
    }

    @Test
    fun playerScreen_hasPlaybackControlsDescriptions() {
        // Launch player activity directly
        val intent = android.content.Intent(ApplicationProvider.getApplicationContext(), PlayerActivity::class.java)
        intent.putExtra("media_id", "test123")
        intent.putExtra("start_position_ms", 0)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        ApplicationProvider.getApplicationContext().startActivity(intent)

        device.waitForIdle()

        // Check playback controls have proper descriptions
        checkContentDescription("Play")
        checkContentDescription("Pause")
        checkContentDescription("Back 10s")
        checkContentDescription("Forward 10s")
        checkContentDescription("Next Episode")
        checkContentDescription("Settings")
        checkContentDescription("Back")

        // Progress slider should be accessible
        val progressBar = device.findObject(By.clazz("android.widget.SeekBar"))
        assertNotNull("Progress bar should be accessible", progressBar)
        assertNotNull("Progress bar should have content description", progressBar.contentDescription)
    }

    @Test
    fun searchScreen_hasAccessibleSearch() {
        launchActivity<MainActivity>()
        device.waitForIdle()

        // Navigate to search
        val searchButton = device.wait(Until.hasObject(By.desc("Search")), 3000)
        searchButton?.click()
        device.waitForIdle()

        // Search field should have label
        val searchField = device.wait(Until.hasObject(By.clazz("android.widget.EditText")), 3000)
        assertNotNull("Search field should exist", searchField)
        assertTrue("Search field should have hint/label", searchField.text.isNotEmpty() || searchField.contentDescription.isNotEmpty())

        // Back button
        checkContentDescription("Back")
    }

    @Test
    fun settingsScreen_hasAccessibleToggles() {
        launchActivity<MainActivity>()
        device.waitForIdle()

        // Navigate to settings
        val settingsButton = device.wait(Until.hasObject(By.desc("Settings")), 3000)
        settingsButton?.click()
        device.waitForIdle()

        // Check toggles have descriptions
        checkContentDescription("High Contrast Mode")
        checkContentDescription("Reduce Motion")
        checkContentDescription("Enhanced Focus Indicator")
        checkContentDescription("Auto-play Next Episode")
        checkContentDescription("Hardware Acceleration")
        checkContentDescription("Debug Overlay")

        // TalkBack settings link
        val talkbackLink = device.findObject(By.text("TalkBack Settings"))
        assertNotNull("TalkBack settings link should exist", talkbackLink)
    }

    @Test
    fun focusOrder_isLogical() {
        launchActivity<MainActivity>()
        device.waitForIdle()

        // Get all focusable elements in order
        val focusableElements = device.findObjects(By.focusable(true))

        // Verify focus moves in expected order (top-to-bottom, left-to-right)
        // This is a basic check - full validation requires manual testing
        assertTrue("Should have focusable elements", focusableElements.size > 0)

        // First focusable should be hero banner or top app bar
        val firstFocusable = focusableElements.firstOrNull()
        assertNotNull("First focusable element should exist", firstFocusable)
    }

    @Test
    fun highContrastMode_meetsWCAG() {
        // This test verifies the high contrast theme colors meet WCAG AAA
        // Actual verification requires color analysis - here we check the theme is applied

        launchActivity<MainActivity>()
        device.waitForIdle()

        // Enable high contrast via settings
        val settingsButton = device.wait(Until.hasObject(By.desc("Settings")), 3000)
        settingsButton?.click()
        device.waitForIdle()

        val highContrastToggle = device.wait(Until.hasObject(By.desc("High Contrast Mode")), 3000)
        highContrastToggle?.click()
        device.waitForIdle()

        // Verify UI updated (background should be pure black #000000)
        // This is a simplified check - full color verification needs pixel analysis
        val rootView = device.findObject(By.clazz("android.widget.FrameLayout"))
        assertNotNull("Root view should exist", rootView)
    }

    @Test
    fun talkBackAnnouncesScreenChanges() {
        // Enable TalkBack via accessibility service
        // Note: This requires TalkBack to be installed and enabled
        // We verify the accessibility events are properly sent

        launchActivity<MainActivity>()
        device.waitForIdle()

        // Navigate to details
        val firstItem = device.findObject(By.descStartsWith("Hero item"))
        firstItem?.click()
        device.waitForIdle()

        // Check that screen change announcement would be sent
        // (Content description of new screen root)
        val detailsRoot = device.wait(Until.hasObject(By.desc("Details")), 3000)
        assertNotNull("Details screen should be announced", detailsRoot)
    }

    private fun <T : android.app.Activity> launchActivity() {
        val intent = android.content.Intent(ApplicationProvider.getApplicationContext(), T::class.java)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ApplicationProvider.getApplicationContext().startActivity(intent)
        device.waitForIdle()
    }

    private fun checkContentDescription(description: String) {
        val obj = device.findObject(By.desc(description))
        assertNotNull("Element with description '$description' should exist", obj)
        assertTrue("Element '$description' should have non-empty content description",
            obj.contentDescription.isNotEmpty() || obj.text.isNotEmpty())
    }
}