package com.github.tartaricacid.mcshelper.run

import com.github.tartaricacid.mcshelper.util.FileUtils
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.project.Project

private val PATTERN = Regex("""File "([a-zA-Z0-9_.]+)", line (\d+)""")

class PythonErrorFilter(private val project: Project) : Filter {
    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        val matchResult = PATTERN.find(line) ?: return null

        val modulePath = matchResult.groups[1]?.value ?: return null
        val lineNumber = matchResult.groups[2]?.value?.toIntOrNull() ?: return null

        val filePath = modulePath.replace('.', '/') + ".py"
        val virtualFile = FileUtils.findFile(project, filePath) ?: return null

        val startOffset = entireLength - line.length + matchResult.range.first
        val endOffset = entireLength - line.length + matchResult.range.last + 1

        return Filter.Result(
            startOffset, endOffset,
            OpenFileHyperlinkInfo(project, virtualFile, lineNumber - 1)
        )
    }
}
