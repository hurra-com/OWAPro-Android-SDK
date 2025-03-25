package com.hurra.s2s

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(
    manifest = Config.NONE,
    sdk = [28], // Downgrade to API 28 (Android P) to avoid the resource mode issue
    shadows = [],
    application = TestApplication::class
)
@RunWith(RobolectricTestRunner::class)
abstract class BaseRobolectricTest

// A minimal application for testing
class TestApplication : android.app.Application() 