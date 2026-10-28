package com.wendigo.tekrar.core

object Csv {

    fun encodeRow(values: List<String>): String = values.joinToString(";") { escape(it) }

    fun encode(rows: List<List<String>>): String =
        rows.joinToString("\r\n") { encodeRow(it) }

    fun parse(text: String): List<List<String>> {
        val source = text.removePrefix("\uFEFF")
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val field = StringBuilder()
        var quoted = false
        var index = 0
        var fieldStarted = false

        fun finishField() {
            row.add(field.toString())
            field.setLength(0)
            fieldStarted = false
        }

        fun finishRow() {
            finishField()
            if (row.isNotEmpty() && row.any { it.isNotEmpty() }) rows.add(row)
            row = ArrayList()
        }

        while (index < source.length) {
            val ch = source[index]
            if (quoted) {
                if (ch == '"') {
                    if (index + 1 < source.length && source[index + 1] == '"') {
                        field.append('"')
                        index += 2
                        continue
                    }
                    quoted = false
                    index++
                    continue
                }
                field.append(ch)
                index++
                continue
            }
            when (ch) {
                '"' -> {
                    quoted = true
                    fieldStarted = true
                    index++
                }
                ';' -> {
                    finishField()
                    index++
                }
                '\r' -> {
                    if (index + 1 < source.length && source[index + 1] == '\n') index++
                    finishRow()
                    index++
                }
                '\n' -> {
                    finishRow()
                    index++
                }
                else -> {
                    field.append(ch)
                    fieldStarted = true
                    index++
                }
            }
        }
        if (quoted || fieldStarted || field.isNotEmpty() || row.isNotEmpty()) {
            finishRow()
        }
        return rows
    }

    fun splitLine(line: String): List<String> = parse(line).firstOrNull().orEmpty()

    private fun escape(value: String): String {
        val needsQuotes = value.any { it == ';' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
}
