package com.chaomixian.vflow.core.workflow.module.core

import android.content.Context
import com.chaomixian.vflow.R
import com.chaomixian.vflow.core.execution.ExecutionContext
import com.chaomixian.vflow.core.module.ActionMetadata
import com.chaomixian.vflow.core.module.BaseModule
import com.chaomixian.vflow.core.module.ExecutionResult
import com.chaomixian.vflow.core.module.InputDefinition
import com.chaomixian.vflow.core.module.OutputDefinition
import com.chaomixian.vflow.core.module.ParameterType
import com.chaomixian.vflow.core.module.ProgressUpdate
import com.chaomixian.vflow.core.types.VTypeRegistry
import com.chaomixian.vflow.core.types.basic.VBoolean
import com.chaomixian.vflow.core.workflow.model.ActionStep
import com.chaomixian.vflow.permissions.Permission
import com.chaomixian.vflow.permissions.PermissionManager
import com.chaomixian.vflow.services.VFlowCoreBridge
import com.chaomixian.vflow.ui.workflow_editor.PillUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CoreHotspotModule : BaseModule() {
    companion object {
        private const val ACTION_ENABLE = "enable"
        private const val ACTION_DISABLE = "disable"
        private const val ACTION_TOGGLE = "toggle"
    }

    override val id = "vflow.core.hotspot"
    override val metadata = ActionMetadata(
        name = "热点控制",
        nameStringRes = R.string.module_vflow_core_hotspot_name,
        description = "使用 vFlow Core 控制 Wi-Fi 热点开关状态（开启/关闭/切换）。",
        descriptionStringRes = R.string.module_vflow_core_hotspot_desc,
        iconRes = R.drawable.rounded_wifi_tethering_24,
        category = "Core (Beta)",
        categoryId = "core"
    )

    override fun getRequiredPermissions(step: ActionStep?): List<Permission> {
        return listOf(PermissionManager.CORE_ROOT)
    }

    private val actionOptions = listOf(ACTION_ENABLE, ACTION_DISABLE, ACTION_TOGGLE)

    override fun getInputs(): List<InputDefinition> = listOf(
        InputDefinition(
            id = "action",
            name = "操作",
            staticType = ParameterType.ENUM,
            defaultValue = ACTION_TOGGLE,
            options = actionOptions,
            optionsStringRes = listOf(
                R.string.option_vflow_core_hotspot_enable,
                R.string.option_vflow_core_hotspot_disable,
                R.string.option_vflow_core_hotspot_toggle
            ),
            legacyValueMap = mapOf(
                "开启" to ACTION_ENABLE,
                "Enable" to ACTION_ENABLE,
                "关闭" to ACTION_DISABLE,
                "Disable" to ACTION_DISABLE,
                "切换" to ACTION_TOGGLE,
                "Toggle" to ACTION_TOGGLE
            ),
            acceptsMagicVariable = false,
            nameStringRes = R.string.param_vflow_core_hotspot_action_name
        )
    )

    override fun getOutputs(step: ActionStep?): List<OutputDefinition> = listOf(
        OutputDefinition("success", "是否成功", VTypeRegistry.BOOLEAN.id, nameStringRes = R.string.output_vflow_core_hotspot_success_name),
        OutputDefinition("enabled", "切换后的状态", VTypeRegistry.BOOLEAN.id, nameStringRes = R.string.output_vflow_core_hotspot_enabled_name)
    )

    override fun getSummary(context: Context, step: ActionStep): CharSequence {
        val actionPill = PillUtil.createPillFromParam(
            step.parameters["action"],
            getInputs().find { it.id == "action" },
            isModuleOption = true
        )
        return PillUtil.buildSpannable(context, actionPill, context.getString(R.string.summary_vflow_core_set_hotspot))
    }

    override suspend fun execute(
        context: ExecutionContext,
        onProgress: suspend (ProgressUpdate) -> Unit
    ): ExecutionResult {
        val connected = withContext(Dispatchers.IO) {
            VFlowCoreBridge.connect(context.applicationContext)
        }
        if (!connected) {
            return ExecutionResult.Failure(
                appContext.getString(R.string.error_vflow_core_not_connected),
                appContext.getString(R.string.error_vflow_core_service_not_running)
            )
        }

        if (VFlowCoreBridge.privilegeMode != VFlowCoreBridge.PrivilegeMode.ROOT) {
            return ExecutionResult.Failure(
                appContext.getString(R.string.error_vflow_core_hotspot_permission_denied),
                appContext.getString(R.string.error_vflow_core_hotspot_root_required)
            )
        }

        val actionInput = getInputs().first { it.id == "action" }
        val rawAction = context.getVariableAsString("action", ACTION_TOGGLE)
        val action = actionInput.normalizeEnumValue(rawAction) ?: rawAction

        val (success, newState) = when (action) {
            ACTION_ENABLE -> {
                onProgress(ProgressUpdate(appContext.getString(R.string.msg_vflow_core_hotspot_enabling)))
                val result = VFlowCoreBridge.setHotspotEnabled(true)
                Pair(result, result)
            }
            ACTION_DISABLE -> {
                onProgress(ProgressUpdate(appContext.getString(R.string.msg_vflow_core_hotspot_disabling)))
                val result = VFlowCoreBridge.setHotspotEnabled(false)
                Pair(result, !result)
            }
            ACTION_TOGGLE -> {
                onProgress(ProgressUpdate(appContext.getString(R.string.msg_vflow_core_hotspot_toggling)))
                val newState = VFlowCoreBridge.toggleHotspot()
                Pair(true, newState)
            }
            else -> {
                return ExecutionResult.Failure(
                    appContext.getString(R.string.error_vflow_interaction_operit_param_error),
                    appContext.getString(R.string.error_vflow_core_hotspot_invalid_action, action)
                )
            }
        }

        return if (success) {
            val stateText = if (newState) {
                appContext.getString(R.string.msg_vflow_core_bluetooth_state_enabled)
            } else {
                appContext.getString(R.string.msg_vflow_core_bluetooth_state_disabled)
            }
            onProgress(ProgressUpdate(appContext.getString(R.string.msg_vflow_core_hotspot_state_changed, stateText)))
            ExecutionResult.Success(mapOf(
                "success" to VBoolean(true),
                "enabled" to VBoolean(newState)
            ))
        } else {
            ExecutionResult.Failure(
                appContext.getString(R.string.error_vflow_shizuku_shell_command_failed),
                appContext.getString(R.string.error_vflow_core_hotspot_failed)
            )
        }
    }
}
