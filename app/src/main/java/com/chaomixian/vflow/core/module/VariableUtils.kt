// 文件：VariableUtils.kt
// 描述：变量相关的工具函数和扩展
package com.chaomixian.vflow.core.module

import com.chaomixian.vflow.core.types.parser.TemplateParser
import com.chaomixian.vflow.core.types.parser.TemplateSegment

/**
 * 检查字符串是否为魔法变量引用 (来自步骤输出)。
 * e.g., "{{stepId.outputId}}"
 *
 * 注意：多个连续的变量引用 ({{...}}{{...}}) 不视为魔法变量，
 * 它们应该走 VariableResolver 的混合解析逻辑。
 */
fun String?.isMagicVariable(): Boolean {
    return isSingleVariableReference(expectNamedVariable = false)
}

/**
 * 检查字符串是否为命名变量引用。
 * e.g., "[[myCounter]]"
 */
fun String?.isNamedVariable(): Boolean = isSingleVariableReference(expectNamedVariable = true)

private fun String?.isSingleVariableReference(expectNamedVariable: Boolean): Boolean {
    val value = this ?: return false
    val segments = TemplateParser(value).parse()
    if (segments.size != 1) return false

    val variable = segments.single() as? TemplateSegment.Variable ?: return false
    return variable.isNamedVariable == expectNamedVariable && variable.rawExpression == value
}
