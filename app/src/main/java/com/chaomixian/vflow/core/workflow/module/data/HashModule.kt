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

class HashModule : BaseModule() {
    companion object {
        private const val ALG_MD5 = "MD5"
        private const val ALG_SHA1 = "SHA-1"
        private const val ALG_SHA224 = "SHA-224"
        private const val ALG_SHA256 = "SHA-256"
        private const val ALG_SHA384 = "SHA-384"
        private const val ALG_SHA512 = "SHA-512"
        private const val ALG_SM3 = "SM3"

        private val algorithmLegacyMap = mapOf(
            "md5" to ALG_MD5,
            "sha1" to ALG_SHA1,
            "sha224" to ALG_SHA224,
            "sha256" to ALG_SHA256,
            "sha384" to ALG_SHA384,
            "sha512" to ALG_SHA512,
            "sm3" to ALG_SM3
        )
    }

    override val id = "vflow.data.hash"
    override val metadata = ActionMetadata(
        nameStringRes = R.string.module_vflow_data_hash_name,
        descriptionStringRes = R.string.module_vflow_data_hash_desc,
        name = "Hash 计算",
        description = "计算文本的 MD5、SHA、SM3 等哈希值",
        iconRes = R.drawable.rounded_chef_hat_24,
        category = "数据处理",
        categoryId = "data"
    )

    override val aiMetadata = AiModuleMetadata(
        usageScopes = setOf(AiModuleUsageScope.TEMPORARY_WORKFLOW),
        riskLevel = AiModuleRiskLevel.READ_ONLY,
        workflowStepDescription = "Hash text with common algorithms such as MD5, SHA-256, SHA-512, or SM3.",
        inputHints = mapOf(
            "algorithm" to "Use one of MD5, SHA-1, SHA-224, SHA-256, SHA-384, SHA-512, or SM3.",
            "source_text" to "Text to hash."
        ),
        requiredInputIds = setOf("algorithm", "source_text")
    )

    private val algorithms = listOf(
        ALG_MD5, ALG_SHA1, ALG_SHA224, ALG_SHA256, ALG_SHA384, ALG_SHA512, ALG_SM3
    )

    override fun getInputs(): List<InputDefinition> = listOf(
        InputDefinition(
            id = "algorithm",
            nameStringRes = R.string.param_vflow_data_hash_algorithm_name,
            name = "算法",
            staticType = ParameterType.ENUM,
            defaultValue = ALG_SHA256,
            options = algorithms,
            optionsStringRes = listOf(
                R.string.option_vflow_data_hash_md5,
                R.string.option_vflow_data_hash_sha1,
                R.string.option_vflow_data_hash_sha224,
                R.string.option_vflow_data_hash_sha256,
                R.string.option_vflow_data_hash_sha384,
                R.string.option_vflow_data_hash_sha512,
                R.string.option_vflow_data_hash_sm3
            ),
            legacyValueMap = algorithmLegacyMap,
            inputStyle = InputStyle.CHIP_GROUP
        ),
        InputDefinition(
            id = "source_text",
            nameStringRes = R.string.param_vflow_data_hash_source_text_name,
            name = "源文本",
            staticType = ParameterType.STRING,
            defaultValue = "",
            acceptsMagicVariable = true,
            supportsRichText = true
        ),
        InputDefinition(
            id = "input_encoding",
            nameStringRes = R.string.param_vflow_data_hash_input_encoding_name,
            name = "输入编码",
            staticType = ParameterType.ENUM,
            defaultValue = CryptoModuleSupport.ENCODING_UTF8,
            options = listOf(
                CryptoModuleSupport.ENCODING_UTF8,
                CryptoModuleSupport.ENCODING_HEX,
                CryptoModuleSupport.ENCODING_BASE64,
                CryptoModuleSupport.ENCODING_LATIN1
            ),
            optionsStringRes = listOf(
                R.string.option_vflow_data_crypto_text_encoding_utf8,
                R.string.option_vflow_data_crypto_text_encoding_hex,
                R.string.option_vflow_data_crypto_text_encoding_base64,
                R.string.option_vflow_data_crypto_text_encoding_latin1
            ),
            legacyValueMap = CryptoModuleSupport.textEncodingLegacyMap,
            inputStyle = InputStyle.CHIP_GROUP
        ),
        InputDefinition(
            id = "output_encoding",
            nameStringRes = R.string.param_vflow_data_hash_output_encoding_name,
            name = "输出编码",
            staticType = ParameterType.ENUM,
            defaultValue = CryptoModuleSupport.ENCODING_HEX,
            options = listOf(CryptoModuleSupport.ENCODING_HEX, CryptoModuleSupport.ENCODING_BASE64),
            optionsStringRes = listOf(
                R.string.option_vflow_data_crypto_binary_encoding_hex,
                R.string.option_vflow_data_crypto_binary_encoding_base64
            ),
            legacyValueMap = CryptoModuleSupport.binaryEncodingLegacyMap,
            inputStyle = InputStyle.CHIP_GROUP
        )
    )

    override fun getOutputs(step: ActionStep?) = CryptoModuleSupport.resultOutput(
        name = "哈希结果",
        nameStringRes = R.string.output_vflow_data_hash_result_text_name
    )

    override fun getSummary(context: Context, step: ActionStep): CharSequence {
        val inputs = getInputs()
        val algorithmPill = PillUtil.createPillFromParam(step.parameters["algorithm"], inputs.find { it.id == "algorithm" }, true)
        val sourcePill = PillUtil.createPillFromParam(step.parameters["source_text"], inputs.find { it.id == "source_text" })
        return PillUtil.buildSpannable(context, algorithmPill, " ", sourcePill)
    }

    override suspend fun execute(
        context: ExecutionContext,
        onProgress: suspend (ProgressUpdate) -> Unit
    ) = CryptoModuleSupport.digest(
        context = appContext,
        algorithm = algorithmLegacyMap[context.getVariableAsString("algorithm", ALG_SHA256)] ?: context.getVariableAsString("algorithm", ALG_SHA256),
        sourceBytes = CryptoModuleSupport.decodeByEncoding(
            CryptoModuleSupport.resolveText(context, "source_text"),
            context.getVariableAsString("input_encoding", CryptoModuleSupport.ENCODING_UTF8)
        ),
        outputEncoding = context.getVariableAsString("output_encoding", CryptoModuleSupport.ENCODING_HEX)
    )
}
