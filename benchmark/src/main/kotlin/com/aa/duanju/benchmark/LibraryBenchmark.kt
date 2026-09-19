package com.aa.duanju.benchmark

import android.content.Intent
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE_NAME = "com.aa.duanju"

@LargeTest
@RunWith(AndroidJUnit4::class)
class LibraryBenchmark {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test fun startup() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD,
        setupBlock = { seedLibrary() },
    ) {
        startActivityAndWait()
    }

    @Test fun scrollTwoHundredDramas() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = { seedLibrary() },
    ) {
        startActivityAndWait()
        val list = device.wait(Until.findObject(By.scrollable(true)), 5_000)
        repeat(5) { list?.fling(Direction.DOWN) }
        repeat(5) { list?.fling(Direction.UP) }
    }

    private fun MacrobenchmarkScope.seedLibrary() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)!!
            .putExtra("seed_benchmark_data", true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivityAndWait(intent)
        device.wait(Until.hasObject(By.textContains("共有 200 部短剧")), 30_000)
        device.pressHome()
    }
}

@LargeTest
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule val baselineProfileRule = BaselineProfileRule()

    @Test fun generate() = baselineProfileRule.collect(PACKAGE_NAME) {
        startActivityAndWait()
        val list = device.wait(Until.findObject(By.scrollable(true)), 5_000)
        repeat(3) { list?.fling(Direction.DOWN) }
    }
}
