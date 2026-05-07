package com.chaomixian.vflow.core.workflow.module.system

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import com.chaomixian.vflow.R
import com.chaomixian.vflow.core.execution.ExecutionContext
import com.chaomixian.vflow.core.module.ActionMetadata
import com.chaomixian.vflow.core.module.AiModuleRiskLevel
import com.chaomixian.vflow.core.module.BaseModule
import com.chaomixian.vflow.core.module.ExecutionResult
import com.chaomixian.vflow.core.module.InputDefinition
import com.chaomixian.vflow.core.module.OutputDefinition
import com.chaomixian.vflow.core.module.ProgressUpdate
import com.chaomixian.vflow.core.module.directToolMetadata
import com.chaomixian.vflow.core.types.VTypeRegistry
import com.chaomixian.vflow.core.types.basic.VBoolean
import com.chaomixian.vflow.core.workflow.model.ActionStep

/**
 * 读取当前屏幕与系统锁定状态的普通系统模块。
 * 不依赖 vFlow Core、Shizuku 或 Root。
 */
class GetScreenStateModule : BaseModule() {

    override val id = "vflow.system.get_screen_state"
    override val metadata = ActionMetadata(
        name = "获取屏幕状态",
        nameStringRes = R.string.module_vflow_system_get_screen_state_name,
        description = "读取屏幕是否已唤醒，以及系统是否已解锁。",
        descriptionStringRes = R.string.module_vflow_system_get_screen_state_desc,
        iconRes = R.drawable.rounded_fullscreen_portrait_24,
        category = "应用与系统",
        categoryId = "device"
    )
    override val aiMetadata = directToolMetadata(
        riskLevel = AiModuleRiskLevel.READ_ONLY,
        directToolDescription = "Read whether the screen is awake and whether the device is unlocked.",
        workflowStepDescription = "Read screen wake and unlock state.",
    )

    override fun getInputs(): List<InputDefinition> = emptyList()

    override fun getOutputs(step: ActionStep?): List<OutputDefinition> = listOf(
        OutputDefinition(
            id = "enabled",
            name = "屏幕状态",
            typeName = VTypeRegistry.BOOLEAN.id,
            nameStringRes = R.string.output_vflow_system_get_screen_state_enabled_name
        ),
        OutputDefinition(
            id = "screen_on",
            name = "屏幕是否已唤醒",
            typeName = VTypeRegistry.BOOLEAN.id,
            nameStringRes = R.string.output_vflow_system_get_screen_state_screen_on_name
        ),
        OutputDefinition(
            id = "unlocked",
            name = "系统是否已解锁",
            typeName = VTypeRegistry.BOOLEAN.id,
            nameStringRes = R.string.output_vflow_system_get_screen_state_unlocked_name
        )
    )

    override fun getSummary(context: Context, step: ActionStep): CharSequence {
        return context.getString(R.string.summary_vflow_system_get_screen_state)
    }

    override suspend fun execute(
        context: ExecutionContext,
        onProgress: suspend (ProgressUpdate) -> Unit
    ): ExecutionResult {
        val appContext = context.applicationContext
        onProgress(ProgressUpdate(appContext.getString(R.string.msg_vflow_system_get_screen_state_reading)))

        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val keyguardManager = appContext.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        val isScreenOn = powerManager.isInteractive
        val isUnlocked = !keyguardManager.isDeviceLocked

        onProgress(
            ProgressUpdate(
                appContext.getString(
                    R.string.msg_vflow_system_get_screen_state_result,
                    if (isScreenOn) {
                        appContext.getString(R.string.value_vflow_system_get_screen_state_screen_on)
                    } else {
                        appContext.getString(R.string.value_vflow_system_get_screen_state_screen_off)
                    },
                    if (isUnlocked) {
                        appContext.getString(R.string.value_vflow_system_get_screen_state_unlocked)
                    } else {
                        appContext.getString(R.string.value_vflow_system_get_screen_state_locked)
                    }
                )
            )
        )

        return ExecutionResult.Success(
            mapOf(
                "enabled" to VBoolean(isScreenOn),
                "screen_on" to VBoolean(isScreenOn),
                "unlocked" to VBoolean(isUnlocked)
            )
        )
    }
}
