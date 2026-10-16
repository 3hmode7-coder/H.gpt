package com.hyperterm.app

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import java.util.concurrent.Executors

/** HyperTerm starter: a terminal-style interface using Android's app-sandbox shell. */
class MainActivity : Activity() {
    private val bg = Color.rgb(8, 11, 16)
    private val cyan = Color.rgb(34, 211, 238)
    private val green = Color.rgb(134, 239, 172)
    private val muted = Color.rgb(148, 163, 184)
    private lateinit var output: TextView
    private lateinit var input: EditText
    private lateinit var scroll: ScrollView
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var workDir: File
    private var english = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workDir = filesDir
        buildUi()
        append("HyperTerm 0.2.0\n", cyan)
        append("واجهة ثنائية اللغة | أوامر ضمن مساحة التطبيق\n", muted)
        append("اكتب help للمساعدة، أو lang للتبديل إلى الإنجليزية.\n\n", muted)
        append("${workDir.absolutePath}\n", green)
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 14, 18, 14)
            setBackgroundColor(bg)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = TextView(this).apply {
            text = "HYPERTERM"
            textSize = 18f
            typeface = Typeface.create("monospace", Typeface.BOLD)
            setTextColor(cyan)
        }
        val languageButton = Button(this).apply {
            text = "عربي / EN"
            setTextColor(cyan)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                english = !english
                input.hint = if (english) "Enter a command…" else "اكتب أمرًا…"
                append("\n${if (english) "Language: English" else "اللغة: العربية"}\n", muted)
            }
        }
        val status = TextView(this).apply {
            text = "  ● LOCAL SHELL"
            textSize = 10f
            setTextColor(green)
        }
        header.addView(brand)
        header.addView(status)
        header.addView(languageButton)
        root.addView(header)

        val rule = TextView(this).apply {
            text = "────────────────────────────────────"
            setTextColor(Color.rgb(51, 65, 85))
        }
        root.addView(rule)

        scroll = ScrollView(this).apply { isFillViewport = true }
        output = TextView(this).apply {
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setTextColor(green)
            setTextIsSelectable(true)
            setPadding(0, 10, 0, 14)
        }
        scroll.addView(output)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val prompt = TextView(this).apply {
            text = "❯ "
            textSize = 18f
            typeface = Typeface.MONOSPACE
            setTextColor(cyan)
        }
        input = EditText(this).apply {
            hint = "اكتب أمرًا…"
            setHintTextColor(muted)
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.MONOSPACE
            singleLine = true
            imeOptions = EditorInfo.IME_ACTION_DONE
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(4, 8, 4, 8)
        }
        val run = Button(this).apply {
            text = "RUN"
            setTextColor(bg)
            setBackgroundColor(cyan)
            setOnClickListener { runCommand() }
        }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { runCommand(); true } else false
        }
        row.addView(prompt)
        row.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(run)
        root.addView(row)
        setContentView(root)
    }

    private fun runCommand() {
        val command = input.text.toString().trim()
        if (command.isEmpty()) return
        input.setText("")
        append("\n$ ${command}\n", cyan)
        if (command == "clear") {
            output.text = ""
            return
        }
        if (command == "help") {
            append(if (english) "Built-ins: help, clear, pwd, ls, cd, echo, date, whoami, lang\n" else "الأوامر المضمنة: help, clear, pwd, ls, cd, echo, date, whoami, lang\n", muted)
            append(if (english) "Linux package manager (pkg/apt) and Python runtime are not bundled in this build. This shell runs with Android app permissions only.\n" else "مدير حزم Linux (pkg/apt) ومفسر Python غير مدمجين في هذه النسخة. تعمل الطرفية بصلاحيات تطبيق Android فقط.\n", muted)
            return
        }
        if (command == "lang") {
            english = !english
            append(if (english) "Language switched to English.\n" else "تم التبديل إلى العربية.\n", muted)
            return
        }
        if (command == "python" || command == "python3" || command == "pkg" || command.startsWith("pkg ") || command == "apt" || command.startsWith("apt ")) {
            val message = if (command.startsWith("python")) {
                if (english) "Python runtime is not bundled yet. It must be integrated before Python commands can run.\n" else "مفسر Python غير مدمج بعد؛ يجب إضافته إلى المشروع قبل تشغيل أوامر Python.\n"
            } else {
                if (english) "pkg/apt requires a compatible Linux userspace and package repository; neither is bundled in this starter.\n" else "يتطلب pkg/apt بيئة Linux متوافقة ومستودع حزم؛ وكلاهما غير مدمج في هذه النسخة المبدئية.\n"
            }
            append(message, Color.rgb(251, 191, 36))
            return
        }
        executor.execute {
            val result = try {
                when {
                    command == "pwd" -> workDir.absolutePath
                    command == "ls" -> (workDir.list()?.sorted()?.joinToString("\n") ?: "(فارغ)")
                    command.startsWith("cd ") -> {
                        val target = command.removePrefix("cd ").trim()
                        val candidate = if (target == "..") workDir.parentFile else File(workDir, target)
                        if (candidate != null && candidate.isDirectory && candidate.canonicalPath.startsWith(filesDir.canonicalPath)) {
                            workDir = candidate.canonicalFile
                            "${workDir.absolutePath}"
                        } else "cd: المسار غير موجود أو خارج مساحة التطبيق"
                    }
                    command.startsWith("echo ") -> command.removePrefix("echo ")
                    else -> {
                        val process = ProcessBuilder("/system/bin/sh", "-c", command)
                            .directory(workDir)
                            .redirectErrorStream(true)
                            .start()
                        val text = process.inputStream.bufferedReader().use { it.readText() }
                        val code = process.waitFor()
                        text.ifBlank { "(انتهى الأمر برمز $code)" }
                    }
                }
            } catch (e: Exception) {
                "خطأ: ${e.message ?: e.javaClass.simpleName}"
            }
            runOnUiThread { append(result.trimEnd() + "\n", if (result.startsWith("خطأ:")) Color.rgb(248, 113, 113) else green) }
        }
    }

    private fun append(text: String, color: Int) {
        val start = output.text.length
        output.append(text)
        output.text.setSpan(android.text.style.ForegroundColorSpan(color), start, output.text.length, 0)
        scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
