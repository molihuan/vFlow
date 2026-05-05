package com.chaomixian.vflow.core.workflow.module.data

import android.content.Context
import com.chaomixian.vflow.R
import com.chaomixian.vflow.core.execution.ExecutionContext
import com.chaomixian.vflow.core.module.ActionMetadata
import com.chaomixian.vflow.core.module.AiModuleMetadata
import com.chaomixian.vflow.core.module.AiModuleRiskLevel
import com.chaomixian.vflow.core.module.AiModuleUsageScope
import com.chaomixian.vflow.core.module.BaseModule
import com.chaomixian.vflow.core.module.ProgressUpdate
import com.chaomixian.vflow.core.workflow.model.ActionStep
import com.chaomixian.vflow.ui.workflow_editor.PillUtil

class Rc4CryptoModule : BaseModule() {
    override val id = "vflow.data.rc4"
    override val metadata = ActionMetadata(
        nameStringRes = R.string.module_vflow_data_rc4_name,
        descriptionStringRes = R.string.module_vflow_data_rc4_desc,
        name = "RC4 加解密",
        description = "使用 RC4 对文本进行加密或解密",
        iconRes = R.drawable.rounded_chef_hat_24,
        category = "数据处理",
        categoryId = "data"
    )

    override val aiMetadata = AiModuleMetadata(
        usageScopes = setOf(AiModuleUsageScope.TEMPORARY_WORKFLOW),
        riskLevel = AiModuleRiskLevel.READ_ONLY,
        workflowStepDescription = "Encrypt or decrypt text with RC4.",
        requiredInputIds = setOf("operation", "source_text", "key")
    )

    override fun getInputs() = CryptoModuleSupport.buildCipherInputs(
        includeMode = false,
        includePadding = false,
        includeIv = false
    )

    override fun getOutputs(step: ActionStep?) = CryptoModuleSupport.resultOutput()

    override fun getSummary(context: Context, step: ActionStep): CharSequence {
        val inputs = getInputs()
        val operationPill = PillUtil.createPillFromParam(step.parameters["operation"], inputs.find { it.id == "operation" }, true)
        val sourcePill = PillUtil.createPillFromParam(step.parameters["source_text"], inputs.find { it.id == "source_text" })
        return PillUtil.buildSpannable(context, context.getString(R.string.summary_vflow_data_rc4_prefix), operationPill, " ", sourcePill)
    }

    override suspend fun execute(
        context: ExecutionContext,
        onProgress: suspend (ProgressUpdate) -> Unit
    ) = CryptoModuleSupport.executeRc4(
        context = appContext,
        keyText = CryptoModuleSupport.resolveText(context, "key"),
        keyEncoding = context.getVariableAsString("key_encoding", CryptoModuleSupport.ENCODING_HEX),
        sourceText = CryptoModuleSupport.resolveText(context, "source_text"),
        textEncoding = context.getVariableAsString("text_encoding", CryptoModuleSupport.ENCODING_UTF8),
        operation = CryptoModuleSupport.operationLegacyMap[context.getVariableAsString("operation", CryptoModuleSupport.OP_ENCRYPT)]
            ?: context.getVariableAsString("operation", CryptoModuleSupport.OP_ENCRYPT),
        ciphertextEncoding = context.getVariableAsString("ciphertext_encoding", CryptoModuleSupport.ENCODING_BASE64)
    )
}
