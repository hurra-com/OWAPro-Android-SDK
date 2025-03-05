package com.hurra.s2s

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic

/**
 * Utility class to redirect Android Log calls to System.out during tests
 */
object TestLogger {
    /**
     * Set up logging redirection for tests
     */
    fun setup() {
        mockkStatic(Log::class)
//        println("Setting up test logger(2)")
        every { Log.v(any(), any<String>()) } answers {
            println("VERBOSE: ${arg<String>(0)}: ${arg<String>(1)}")
            0
        }
        
        every { Log.d(any(), any<String>()) } answers {
            println("DEBUG: ${arg<String>(0)}: ${arg<String>(1)}")
            0
        }
        
        every { Log.i(any(), any<String>()) } answers {
            println("INFO: ${arg<String>(0)}: ${arg<String>(1)}")
            0
        }
        
        every { Log.w(any(), any<String>()) } answers {
            println("WARN: ${arg<String>(0)}: ${arg<String>(1)}")
            0
        }
        
        every { Log.w(any(), any<Throwable>()) } answers {
            println("WARN: ${arg<String>(0)}: ${arg<Throwable>(1)}")
            0
        }
        
        every { Log.e(any(), any<String>()) } answers {
            println("ERROR: ${arg<String>(0)}: ${arg<String>(1)}")
            0
        }
        
        every { Log.e(any(), any<String>(), any<Throwable>()) } answers {
            println("ERROR: ${arg<String>(0)}: ${arg<String>(1)}")
            println("EXCEPTION: ${arg<Throwable>(2)}")
            0
        }
    }
} 