package com.chaomixian.vflow.core.workflow.module.file

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import coil.Coil
import coil.request.ImageRequest
import com.chaomixian.vflow.R
import com.chaomixian.vflow.core.execution.ExecutionContext
import com.chaomixian.vflow.core.module.ActionMetadata
import com.chaomixian.vflow.core.module.AiModuleMetadata
import com.chaomixian.vflow.core.module.AiModuleRiskLevel
import com.chaomixian.vflow.core.module.AiModuleUsageScope
import com.chaomixian.vflow.core.module.BaseModule
import com.chaomixian.vflow.core.module.ExecutionResult
import com.chaomixian.vflow.core.module.InputDefinition
import com.chaomixian.vflow.core.module.OutputDefinition
import com.chaomixian.vflow.core.module.ParameterType
import com.chaomixian.vflow.core.module.ProgressUpdate
import com.chaomixian.vflow.core.types.VTypeRegistry
import com.chaomixian.vflow.core.types.complex.VImage
import com.chaomixian.vflow.core.workflow.model.ActionStep
import com.chaomixian.vflow.ui.workflow_editor.PillUtil
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.roundToInt

class ScaleImageModule : BaseModule() {
    override val id = "vflow.file.scale_image"
    override val metadata = ActionMetadata(
        nameStringRes = R.string.module_vflow_file_scale_image_name,
        descriptionStringRes = R.string.module_vflow_file_scale_image_desc,
        name = "缩放图片",
        description = "按百分比缩放图片。",
        iconRes = R.drawable.rounded_photo_24,
        category = "文件",
        categoryId = "file"
    )

    override val aiMetadata = AiModuleMetadata(
        usageScopes = setOf(AiModuleUsageScope.TEMPORARY_WORKFLOW),
        riskLevel = AiModuleRiskLevel.READ_ONLY,
        workflowStepDescription = "Scale an image by a percentage and output the resized image.",
        inputHints = mapOf(
            "image" to "Source image variable.",
            "scale_percent" to "Scale percentage such as 50 for half size or 200 for double size."
        ),
        requiredInputIds = setOf("image", "scale_percent")
    )

    override fun getInputs(): List<InputDefinition> = listOf(
        InputDefinition(
            id = "image",
            name = "源图像",
            staticType = ParameterType.ANY,
            acceptsMagicVariable = true,
            acceptedMagicVariableTypes = setOf(VTypeRegistry.IMAGE.id),
            nameStringRes = R.string.param_vflow_file_scale_image_image_name
        ),
        InputDefinition(
            id = "scale_percent",
            name = "缩放比例",
            staticType = ParameterType.NUMBER,
            defaultValue = 100.0,
            acceptsMagicVariable = true,
            acceptedMagicVariableTypes = setOf(VTypeRegistry.NUMBER.id),
            nameStringRes = R.string.param_vflow_file_scale_image_scale_percent_name
        )
    )

    override fun getOutputs(step: ActionStep?): List<OutputDefinition> = listOf(
        OutputDefinition("image", "缩放后的图像", VTypeRegistry.IMAGE.id)
    )

    override fun getSummary(context: Context, step: ActionStep): CharSequence {
        val inputs = getInputs()
        val imagePill = PillUtil.createPillFromParam(step.parameters["image"], inputs.find { it.id == "image" })
        val scalePill = PillUtil.createPillFromParam(step.parameters["scale_percent"], inputs.find { it.id == "scale_percent" })
        return PillUtil.buildSpannable(
            context,
            context.getString(R.string.summary_vflow_file_scale_image_prefix),
            imagePill,
            context.getString(R.string.summary_vflow_file_scale_image_middle),
            scalePill,
            context.getString(R.string.summary_vflow_file_scale_image_suffix)
        )
    }

    override suspend fun execute(
        context: ExecutionContext,
        onProgress: suspend (ProgressUpdate) -> Unit
    ): ExecutionResult {
        val imageVar = context.getVariableAsImage("image")
            ?: return ExecutionResult.Failure("参数错误", "需要一个图像变量作为输入。")
        val scalePercent = context.getVariableAsNumber("scale_percent")?.toFloat() ?: 100f

        if (scalePercent <= 0f) {
            return ExecutionResult.Failure("参数错误", "缩放比例必须大于 0。")
        }

        val appContext = context.applicationContext
        onProgress(ProgressUpdate("正在加载图像..."))

        return try {
            val request = ImageRequest.Builder(appContext)
                .data(Uri.parse(imageVar.uriString))
                .allowHardware(false)
                .build()
            val result = Coil.imageLoader(appContext).execute(request)
            val originalBitmap = result.drawable?.toBitmap()
                ?: return ExecutionResult.Failure("图像加载失败", "无法从 URI 加载位图: ${imageVar.uriString}")

            onProgress(ProgressUpdate("正在缩放图像..."))

            val targetWidth = (originalBitmap.width * scalePercent / 100f).roundToInt().coerceAtLeast(1)
            val targetHeight = (originalBitmap.height * scalePercent / 100f).roundToInt().coerceAtLeast(1)
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)

            onProgress(ProgressUpdate("正在保存处理后的图像..."))
            val outputFile = File(context.workDir, "scaled_${UUID.randomUUID()}.png")
            FileOutputStream(outputFile).use {
                scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }

            if (scaledBitmap != originalBitmap) {
                scaledBitmap.recycle()
            }

            ExecutionResult.Success(mapOf("image" to VImage(Uri.fromFile(outputFile).toString())))
        } catch (e: Exception) {
            ExecutionResult.Failure("图像处理异常", e.localizedMessage ?: "发生未知错误")
        }
    }
}
