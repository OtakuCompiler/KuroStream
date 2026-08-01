package com.kurostream.app.controller

import android.view.InputDevice
import android.view.KeyEvent

object GamepadController {
    fun isGamepadEvent(event: android.view.KeyEvent): Boolean {
        return event.source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
               event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
    }

    fun mapToDirection(event: android.view.KeyEvent): Direction? {
        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> Direction.UP
            KeyEvent.KEYCODE_DPAD_DOWN -> Direction.DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> Direction.LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> Direction.RIGHT
            KeyEvent.KEYCODE_BUTTON_A -> Direction.SELECT
            KeyEvent.KEYCODE_BUTTON_B -> Direction.BACK
            else -> null
        }
    }

    enum class Direction { UP, DOWN, LEFT, RIGHT, SELECT, BACK }
}
