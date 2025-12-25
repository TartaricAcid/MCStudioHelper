package com.github.tartaricacid.mcshelper.run

import com.github.tartaricacid.mcshelper.util.FileUtils
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.project.Project

// 捕获文件路径，后面可选的 ":*" 以及可选的 "Line <n>, Column <m>"
private val PATTERN = Regex("""\s([A-Za-z0-9_\-./\\]+\.json)(?::\*?)?(?:\s*Line\s+(\d+),\s*Column\s+(\d+))?""")

/**
 * 用来过滤基岩版原生引擎对 JSON 解析错误的文件，进行快捷跳转
 */
class JsonErrorFilter(private val project: Project) : Filter {
    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        if (!line.contains("[ERROR][Engine]")) {
            return null
        }

        val matchResult = PATTERN.find(line) ?: return null
        val filePath = matchResult.groups[1]?.value ?: return null
        val virtualFile = FileUtils.findFile(project, filePath) ?: return null

        // 解析可选的行列号，行号转换为 0-based
        val lineIndex = (matchResult.groups[2]?.value?.toIntOrNull() ?: 1).let {
            if (it > 0) it - 1 else 0
        }

        val startOffset = entireLength - line.length + matchResult.range.first + 1
        val endOffset = entireLength - line.length + matchResult.range.last + 1

        // 传入行号（0-based），列号通常无法直接传入该构造函数
        return Filter.Result(
            startOffset, endOffset,
            OpenFileHyperlinkInfo(project, virtualFile, lineIndex)
        )
    }
}
