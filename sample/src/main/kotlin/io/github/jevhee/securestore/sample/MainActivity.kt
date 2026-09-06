package io.github.jevhee.securestore.sample

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class MainActivity : Activity() {
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var runner: ScenarioRunner
    private lateinit var runAllButton: Button
    private lateinit var clearLogButton: Button
    private lateinit var summaryView: TextView
    private lateinit var logView: TextView
    private val scenarioButtons = mutableListOf<Button>()
    private var activeJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runner = ScenarioRunner(applicationContext)
        setContentView(buildContent())
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            setBackgroundColor(Color.rgb(246, 248, 250))
        }

        root.addView(text("SecureStore Playground", 26f, Typeface.BOLD))
        root.addView(
            text(
                "Run scenarios against Android Keystore, AES-GCM, HMAC identifiers, and DataStore.",
                15f,
                Typeface.NORMAL,
            ).withBottomMargin(14),
        )

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        runAllButton = Button(this).apply {
            text = "Run all scenarios"
            setOnClickListener { runAll() }
        }
        clearLogButton = Button(this).apply {
            text = "Clear log"
            setOnClickListener {
                summaryView.text = "Ready"
                logView.text = ""
            }
        }
        actions.addView(runAllButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        actions.addView(clearLogButton)
        root.addView(actions)

        summaryView = text("Ready", 17f, Typeface.BOLD).apply {
            setPadding(0, dp(10), 0, dp(10))
        }
        root.addView(summaryView)

        root.addView(text("Individual scenarios", 18f, Typeface.BOLD).withBottomMargin(6))
        runner.scenarios.forEach { scenario -> root.addView(scenarioCard(scenario)) }

        root.addView(text("Execution log", 18f, Typeface.BOLD).withTopMargin(14))
        logView = text("", 13f, Typeface.NORMAL).apply {
            typeface = Typeface.MONOSPACE
            setTextColor(Color.rgb(35, 40, 45))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.WHITE)
            setTextIsSelectable(true)
        }
        root.addView(
            logView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        return ScrollView(this).apply { addView(root) }
    }

    private fun scenarioCard(scenario: Scenario): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(10), dp(12), dp(10))
        setBackgroundColor(Color.WHITE)

        addView(text(scenario.name, 16f, Typeface.BOLD))
        addView(text(scenario.description, 13f, Typeface.NORMAL))
        addView(
            Button(this@MainActivity).apply {
                text = "Run"
                scenarioButtons += this
                setOnClickListener { runOne(scenario) }
            },
        )
    }.withBottomMargin(8)

    private fun runOne(scenario: Scenario) {
        execute {
            summaryView.text = "Running: ${scenario.name}"
            appendLog("RUN  ${scenario.name}")
            val result = runner.run(scenario)
            renderResult(result)
            summaryView.text = if (result.passed) "1 passed" else "1 failed"
        }
    }

    private fun runAll() {
        execute {
            logView.text = ""
            var passed = 0
            var failed = 0
            for ((index, scenario) in runner.scenarios.withIndex()) {
                summaryView.text = "Running ${index + 1}/${runner.scenarios.size}: ${scenario.name}"
                appendLog("RUN  ${scenario.name}")
                val result = runner.run(scenario)
                renderResult(result)
                if (result.passed) passed++ else failed++
            }
            summaryView.text = "Completed: $passed passed, $failed failed"
        }
    }

    private fun execute(block: suspend () -> Unit) {
        if (activeJob?.isActive == true) return
        setControlsEnabled(false)
        activeJob = activityScope.launch {
            try {
                block()
            } catch (exception: CancellationException) {
                throw exception
            } catch (throwable: Throwable) {
                summaryView.text = "Runner failed"
                appendLog("FAIL Runner: ${throwable::class.java.simpleName}")
            } finally {
                setControlsEnabled(true)
            }
        }
    }

    private fun renderResult(result: ScenarioResult) {
        val status = if (result.passed) "PASS" else "FAIL"
        appendLog("$status ${result.durationMillis} ms — ${result.detail}")
    }

    private fun appendLog(message: String) {
        val time = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date())
        logView.append("[$time] $message\n")
    }

    private fun setControlsEnabled(enabled: Boolean) {
        runAllButton.isEnabled = enabled
        clearLogButton.isEnabled = enabled
        scenarioButtons.forEach { it.isEnabled = enabled }
    }

    private fun text(value: String, sizeSp: Float, style: Int): TextView = TextView(this).apply {
        text = value
        textSize = sizeSp
        setTextColor(Color.rgb(20, 24, 28))
        setTypeface(typeface, style)
    }

    private fun View.withBottomMargin(valueDp: Int): View = apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = dp(valueDp) }
    }

    private fun View.withTopMargin(valueDp: Int): View = apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(valueDp) }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
