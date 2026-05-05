package com.chaomixian.vflow.core.workflow.module.data

import android.content.Context
import com.chaomixian.vflow.R
import com.chaomixian.vflow.core.execution.ExecutionContext
import com.chaomixian.vflow.core.module.ActionMetadata
import com.chaomixian.vflow.core.module.AiModuleMetadata
import com.chaomixian.vflow.core.module.AiModuleRiskLevel
import com.chaomixian.vflow.core.module.AiModuleUsageScope
import com.chaomixian.vflow.core.module.BaseModule
import com.chaomixian.vflow.core.module.InputDefinition
import com.chaomixian.vflow.core.module.InputStyle
import com.chaomixian.vflow.core.module.ParameterType
import com.chaomixian.vflow.core.module.ProgressUpdate
import com.chaomixian.vflow.core.workflow.model.ActionStep
import com.chaomixian.vflow.ui.workflow_editor.PillUtil

class UrlEncodeOrDecodeModule : BaseModule() {
    override val id = "vflow.data.url_codec"
    override val metadata = ActionMetadata(
        nameStringRes = R.string.module_vflow_data_url_codec_name,
        descriptionStringRes = R.string.module_vflow_data_url_codec_desc,
        name = "URL 编解码",
        description = "对文本执行 URL 编码或解码",
        iconRes = R.drawable.rounded_chef_hat_24,
        category = "数据处理",
        categoryId = "data"
    )

    override val aiMetadata = AiModuleMetadata(
        usageScopes = setOf(AiModuleUsageScope.TEMPORARY_WORKFLOW),
        riskLevel = AiModuleRiskLevel.READ_ONLY,
        workflowStepDescription = "Encode or decode text using URL encoding.",
        inputHints = mapOf(
            "operation" to "Use encode or decode.",
            "source_text" to "Text to transform."
        ),
        requiredInputIds = setOf("operation", "source_text")
    )

    override fun getInputs(): List<InputDefinition> = listOf(
        InputDefinition(
            id = "operation",
            nameStringRes = R.string.param_vflow_data_url_codec_operation_name,
            name = "操作",
            staticType = ParameterType.ENUM,
            defaultValue = CryptoModuleSupport.OP_ENCODE,
            options = listOf(CryptoModuleSupport.OP_ENCODE, CryptoModuleSupport.OP_DECODE),
            optionsStringRes = listOf(
                R.string.option_vflow_data_base64_encode,
                R.string.option_vflow_data_base64_decode
            ),
            legacyValueMap = CryptoModuleSupport.operationLegacyMap,
            inputStyle = InputStyle.CHIP_GROUP
        ),
        InputDefinition(
            id = "source_text",
            nameStringRes = R.string.param_vflow_data_url_codec_source_text_name,
            name = "源文本",
            staticType = ParameterType.STRING,
            defaultValue = "",
            acceptsMagicVariable = true,
            supportsRichText = true
        ),
        InputDefinition(
            id = "charset",
            nameStringRes = R.string.param_vflow_data_url_codec_charset_name,
            name = "字符集",
            staticType = ParameterType.ENUM,
            defaultValue = "UTF-8",
            options = listOf("UTF-8", "GBK", "GB2312", "ISO-8859-1"),
            inputStyle = InputStyle.CHIP_GROUP
        )
    )

    override fun getOutputs(step: ActionStep?) = CryptoModuleSupport.resultOutput()

    override fun getSummary(context: Context, step: ActionStep): CharSequence {
        val inputs = getInputs()
        val operationPill = PillUtil.createPillFromParam(step.parameters["operation"], inputs.find { it.id == "operation" }, true)
        val sourcePill = PillUtil.createPillFromParam(step.parameters["source_text"], inputs.find { it.id == "source_text" })
        return PillUtil.buildSpannable(context, operationPill, " ", sourcePill)
    }

    override suspend fun execute(
        context: ExecutionContext,
        onProgress: suspend (ProgressUpdate) -> Unit
    ) = when (context.getVariableAsString("operation", CryptoModuleSupport.OP_ENCODE).let {
        CryptoModuleSupport.operationLegacyMap[it] ?: it
    }) {
        CryptoModuleSupport.OP_DECODE -> CryptoModuleSupport.urlDecode(
            context = appContext,
            CryptoModuleSupport.resolveText(context, "source_text"),
            context.getVariableAsString("charset", "UTF-8")
        )
        else -> CryptoModuleSupport.urlEncode(
            context = appContext,
            CryptoModuleSupport.resolveText(context, "source_text"),
            context.getVariableAsString("charset", "UTF-8")
        )
    }
}
