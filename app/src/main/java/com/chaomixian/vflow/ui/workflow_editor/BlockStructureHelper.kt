package com.chaomixian.vflow.ui.workflow_editor

import com.chaomixian.vflow.core.module.BaseBlockModule
import com.chaomixian.vflow.core.module.BlockType
import com.chaomixian.vflow.core.module.ModuleRegistry
import com.chaomixian.vflow.core.workflow.model.ActionStep

object BlockStructureHelper {

    fun findBlockRange(steps: List<ActionStep>, position: Int): Pair<Int, Int> {
        if (position !in steps.indices) return position to position

        val initialModule = ModuleRegistry.getModule(steps[position].moduleId) ?: return position to position
        val behavior = initialModule.blockBehavior
        if (behavior.type == BlockType.NONE || behavior.pairingId == null) {
            return position to position
        }

        var blockStart = position
        var openCount = if (behavior.type == BlockType.BLOCK_END) 1 else 0
        val searchStart = if (behavior.type == BlockType.BLOCK_END) position - 1 else position
        for (i in searchStart downTo 0) {
            val module = ModuleRegistry.getModule(steps[i].moduleId) ?: continue
            val currentBehavior = module.blockBehavior
            if (currentBehavior.pairingId != behavior.pairingId) continue

            when (currentBehavior.type) {
                BlockType.BLOCK_END -> openCount++
                BlockType.BLOCK_START -> {
                    val shouldMatch = when (behavior.type) {
                        BlockType.BLOCK_END -> openCount <= 1
                        else -> openCount == 0
                    }
                    if (shouldMatch) {
                        blockStart = i
                        break
                    }
                    openCount--
                }
                else -> Unit
            }
        }

        var blockEnd = blockStart
        openCount = 0
        for (i in blockStart until steps.size) {
            val module = ModuleRegistry.getModule(steps[i].moduleId) ?: continue
            val currentBehavior = module.blockBehavior
            if (currentBehavior.pairingId != behavior.pairingId) continue

            when (currentBehavior.type) {
                BlockType.BLOCK_START -> openCount++
                BlockType.BLOCK_END -> {
                    openCount--
                    if (openCount == 0) {
                        blockEnd = i
                        break
                    }
                }
                else -> Unit
            }
        }
        return blockStart to blockEnd
    }

    fun isStepEffectivelyDisabled(steps: List<ActionStep>, position: Int): Boolean {
        if (position !in steps.indices) return false
        steps.forEachIndexed { index, step ->
            if (!step.isDisabled) return@forEachIndexed

            val module = ModuleRegistry.getModule(step.moduleId) ?: return@forEachIndexed
            val behavior = module.blockBehavior
            if (behavior.type == BlockType.NONE || behavior.pairingId == null) {
                if (index == position) return true
            } else {
                val (start, end) = findBlockRange(steps, index)
                if (position in start..end) return true
            }
        }
        return false
    }

    fun hasDisabledAncestor(steps: List<ActionStep>, position: Int): Boolean {
        if (position !in steps.indices) return false
        steps.forEachIndexed { index, step ->
            if (index == position || !step.isDisabled) return@forEachIndexed

            val module = ModuleRegistry.getModule(step.moduleId) ?: return@forEachIndexed
            val behavior = module.blockBehavior
            if (behavior.type == BlockType.NONE || behavior.pairingId == null) {
                return@forEachIndexed
            }

            val (start, end) = findBlockRange(steps, index)
            if (position in start..end && position != start) return true
        }
        return false
    }

    fun getMissingMiddleModuleIds(steps: List<ActionStep>, position: Int): List<String> {
        if (position !in steps.indices) return emptyList()
        val (blockStart, blockEnd) = findBlockRange(steps, position)
        val blockStartStep = steps.getOrNull(blockStart) ?: return emptyList()
        val blockStartModule = ModuleRegistry.getModule(blockStartStep.moduleId) as? BaseBlockModule ?: return emptyList()

        val expectedMiddleIds = blockStartModule.stepIdsInBlock.drop(1).dropLast(1)
        if (expectedMiddleIds.isEmpty()) return emptyList()

        val pairingId = blockStartModule.pairingId
        val directMiddleIds = mutableSetOf<String>()
        var nestedSamePairingDepth = 0

        for (i in (blockStart + 1) until blockEnd) {
            val module = ModuleRegistry.getModule(steps[i].moduleId) ?: continue
            val behavior = module.blockBehavior
            if (behavior.pairingId != pairingId) continue

            when (behavior.type) {
                BlockType.BLOCK_START -> nestedSamePairingDepth++
                BlockType.BLOCK_END -> {
                    if (nestedSamePairingDepth > 0) {
                        nestedSamePairingDepth--
                    }
                }
                BlockType.BLOCK_MIDDLE -> {
                    if (nestedSamePairingDepth == 0) {
                        directMiddleIds += module.id
                    }
                }
                else -> Unit
            }
        }

        return expectedMiddleIds.filterNot(directMiddleIds::contains)
    }
}
