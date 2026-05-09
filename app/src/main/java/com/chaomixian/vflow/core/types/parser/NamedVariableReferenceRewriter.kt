package com.chaomixian.vflow.core.types.parser

object NamedVariableReferenceRewriter {
    data class RewriteResult(
        val value: Any?,
        val changed: Boolean
    )

    fun rewrite(value: Any?, oldVariableName: String, newVariableName: String): RewriteResult {
        if (oldVariableName.isBlank() || newVariableName.isBlank()) {
            return RewriteResult(value, false)
        }

        val oldBracketReference = "[[$oldVariableName]]"
        val oldCanonicalReference = VariablePathParser.buildNamedVariableReference(oldVariableName)
        val newCanonicalReference = VariablePathParser.buildNamedVariableReference(newVariableName)

        return rewriteInternal(value, oldBracketReference, oldCanonicalReference, newCanonicalReference)
    }

    private fun rewriteInternal(
        value: Any?,
        oldBracketReference: String,
        oldCanonicalReference: String,
        newCanonicalReference: String
    ): RewriteResult {
        return when (value) {
            null -> RewriteResult(null, false)
            is String -> rewriteString(value, oldBracketReference, oldCanonicalReference, newCanonicalReference)
            is Map<*, *> -> rewriteMap(value, oldBracketReference, oldCanonicalReference, newCanonicalReference)
            is List<*> -> rewriteList(value, oldBracketReference, oldCanonicalReference, newCanonicalReference)
            is Set<*> -> rewriteSet(value, oldBracketReference, oldCanonicalReference, newCanonicalReference)
            is Array<*> -> rewriteArray(value, oldBracketReference, oldCanonicalReference, newCanonicalReference)
            else -> RewriteResult(value, false)
        }
    }

    private fun rewriteString(
        value: String,
        oldBracketReference: String,
        oldCanonicalReference: String,
        newCanonicalReference: String
    ): RewriteResult {
        var updated = value
        var changed = false

        if (updated.contains(oldBracketReference)) {
            updated = updated.replace(oldBracketReference, newCanonicalReference)
            changed = true
        }
        if (updated.contains(oldCanonicalReference)) {
            updated = updated.replace(oldCanonicalReference, newCanonicalReference)
            changed = true
        }

        return RewriteResult(updated, changed)
    }

    private fun rewriteMap(
        value: Map<*, *>,
        oldBracketReference: String,
        oldCanonicalReference: String,
        newCanonicalReference: String
    ): RewriteResult {
        var changed = false
        val updated = LinkedHashMap<Any?, Any?>()

        value.forEach { (key, item) ->
            val rewritten = rewriteInternal(item, oldBracketReference, oldCanonicalReference, newCanonicalReference)
            updated[key] = rewritten.value
            changed = changed || rewritten.changed
        }

        return RewriteResult(if (changed) updated else value, changed)
    }

    private fun rewriteList(
        value: List<*>,
        oldBracketReference: String,
        oldCanonicalReference: String,
        newCanonicalReference: String
    ): RewriteResult {
        var changed = false
        val updated = ArrayList<Any?>(value.size)

        value.forEach { item ->
            val rewritten = rewriteInternal(item, oldBracketReference, oldCanonicalReference, newCanonicalReference)
            updated.add(rewritten.value)
            changed = changed || rewritten.changed
        }

        return RewriteResult(if (changed) updated else value, changed)
    }

    private fun rewriteSet(
        value: Set<*>,
        oldBracketReference: String,
        oldCanonicalReference: String,
        newCanonicalReference: String
    ): RewriteResult {
        var changed = false
        val updated = LinkedHashSet<Any?>()

        value.forEach { item ->
            val rewritten = rewriteInternal(item, oldBracketReference, oldCanonicalReference, newCanonicalReference)
            updated.add(rewritten.value)
            changed = changed || rewritten.changed
        }

        return RewriteResult(if (changed) updated else value, changed)
    }

    private fun rewriteArray(
        value: Array<*>,
        oldBracketReference: String,
        oldCanonicalReference: String,
        newCanonicalReference: String
    ): RewriteResult {
        var changed = false
        val updated = arrayOfNulls<Any?>(value.size)

        value.forEachIndexed { index, item ->
            val rewritten = rewriteInternal(item, oldBracketReference, oldCanonicalReference, newCanonicalReference)
            updated[index] = rewritten.value
            changed = changed || rewritten.changed
        }

        return RewriteResult(if (changed) updated else value, changed)
    }
}
