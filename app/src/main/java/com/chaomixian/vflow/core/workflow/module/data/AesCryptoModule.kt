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

class AesCryptoModule : BaseModule() {
    override val id = "vflow.data.aes"
    override val metadata = ActionMetadata(
        nameStringRes = R.string.module_vflow_data_aes_name,
        descriptionStringRes = R.string.module_vflow_data_aes_desc,
        name = "AES 加解密",
        description = "使用 AES 对文本进行加密或解密",
        iconRes = R.drawable.rounded_chef_hat_24,
        category = "数据处理",
        categoryId = "data"
    )

    override val aiMetadata = AiModuleMetadata(
        usageScopes = setOf(AiModuleUsageScope.TEMPORARY_WORKFLOW),
        riskLevel = AiModuleRiskLevel.READ_ONLY,
        workflowStepDescription = "Encrypt or decrypt text with AES.",
        inputHints = mapOf(
            "operation" to "Use encrypt or decrypt.",
            "source_text" to "Plaintext for encryption or encoded ciphertext for decryption.",
            "key" to "AES key with 16, 24, or 32 bytes."
        ),
        requiredInputIds = setOf("operation", "source_text", "key")
    )

    override fun getInputs() = CryptoModuleSupport.buildCipherInputs()
    override fun getOutputs(step: ActionStep?) = CryptoModuleSupport.resultOutput()

    override fun getSummary(context: Context, step: ActionStep): CharSequence {
        val inputs = getInputs()
        val operationPill = PillUtil.createPillFromParam(step.parameters["operation"], inputs.find { it.id == "operation" }, true)
        val sourcePill = PillUtil.createPillFromParam(step.parameters["source_text"], inputs.find { it.id == "source_text" })
        return PillUtil.buildSpannable(context, context.getString(R.string.summary_vflow_data_aes_prefix), operationPill, " ", sourcePill)
    }

    override suspend fun execute(
        context: ExecutionContext,
        onProgress: suspend (ProgressUpdate) -> Unit
    ) = CryptoModuleSupport.executeCipher(
        context = appContext,
        algorithm = "AES",
        keyText = CryptoModuleSupport.resolveText(context, "key"),
        keyEncoding = context.getVariableAsString("key_encoding", CryptoModuleSupport.ENCODING_HEX),
        sourceText = CryptoModuleSupport.resolveText(context, "source_text"),
        textEncoding = context.getVariableAsString("text_encoding", CryptoModuleSupport.ENCODING_UTF8),
        operation = CryptoModuleSupport.operationLegacyMap[context.getVariableAsString("operation", CryptoModuleSupport.OP_ENCRYPT)]
            ?: context.getVariableAsString("operation", CryptoModuleSupport.OP_ENCRYPT),
        ciphertextEncoding = context.getVariableAsString("ciphertext_encoding", CryptoModuleSupport.ENCODING_BASE64),
        expectedKeyLengths = setOf(16, 24, 32),
        blockSize = 16,
        mode = context.getVariableAsString("cipher_mode", CryptoModuleSupport.MODE_ECB),
        padding = context.getVariableAsString("padding", CryptoModuleSupport.PADDING_PKCS5),
        ivText = CryptoModuleSupport.resolveText(context, "iv"),
        ivEncoding = context.getVariableAsString("iv_encoding", CryptoModuleSupport.ENCODING_HEX)
    )
}
