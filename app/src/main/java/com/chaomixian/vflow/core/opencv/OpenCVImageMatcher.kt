// 文件: OpenCVImageMatcher.kt
package com.chaomixian.vflow.core.opencv

import android.graphics.Bitmap
import com.chaomixian.vflow.core.logging.DebugLogger
import com.chaomixian.vflow.core.workflow.module.interaction.ImageMatcher
import com.chaomixian.vflow.core.workflow.module.interaction.MatchResult
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

/**
 * OpenCV 图片匹配器
 * 使用 OpenCV 的模板匹配算法，比原始 RGB 像素差异匹配更快、更准确
 */
object OpenCVImageMatcher {
    private const val TAG = "OpenCVImageMatcher"

    /**
     * 在源图片中查找所有与模板匹配的位置
     * @param source 源图片（屏幕截图）
     * @param template 模板图片
     * @param maxDiffPercent 允许的最大差异百分比 (0.0-1.0)
     * @return 匹配结果列表，已按相似度排序（最相似的在前面）
     */
    fun findAll(
        source: Bitmap,
        template: Bitmap,
        maxDiffPercent: Double
    ): List<MatchResult> {

        // 检查 OpenCV 是否已初始化
        if (!OpenCVManager.isInitialized) {
            DebugLogger.w(TAG, "OpenCV not initialized, using legacy matcher")
            return ImageMatcher.findAll(source, template, maxDiffPercent)
        }

        // 基本验证
        if (template.width > source.width || template.height > source.height) {
            return emptyList()
        }

        try {
            val sourceMat = Mat()
            val templateMat = Mat()

            Utils.bitmapToMat(source, sourceMat)
            Utils.bitmapToMat(template, templateMat)

            // 转灰度提升匹配效果
            val sourceGray = Mat()
            val templateGray = Mat()
            Imgproc.cvtColor(sourceMat, sourceGray, Imgproc.COLOR_RGB2GRAY)
            Imgproc.cvtColor(templateMat, templateGray, Imgproc.COLOR_RGB2GRAY)

            val resultCols = sourceGray.cols() - templateGray.cols() + 1
            val resultRows = sourceGray.rows() - templateGray.rows() + 1
            val result = Mat(resultRows, resultCols, CvType.CV_32FC1)

            // 使用归一化相关系数 (对光照变化鲁棒)
            Imgproc.matchTemplate(sourceGray, templateGray, result, Imgproc.TM_CCOEFF_NORMED)

            val thresholdPercent = (maxDiffPercent * 100.0).coerceIn(0.0, 100.0)
            val confidenceThreshold = ((100.0 - thresholdPercent) / 100.0).coerceIn(0.0, 1.0)

            val matches = findAllMatches(
                result = result,
                sourceColor = sourceMat,
                templateColor = templateMat,
                confidenceThreshold = confidenceThreshold,
                colorDiffThreshold = thresholdPercent,
                templateWidth = templateGray.width(),
                templateHeight = templateGray.height()
            )

            // 释放资源
            sourceMat.release()
            templateMat.release()
            sourceGray.release()
            templateGray.release()
            result.release()

            DebugLogger.d(TAG, "Found ${matches.size} matches with OpenCV")
            return matches

        } catch (e: Exception) {
            DebugLogger.e(TAG, "OpenCV matching failed, falling back to legacy matcher", e)
            return ImageMatcher.findAll(source, template, maxDiffPercent)
        }
    }

    /**
     * 从匹配结果矩阵中提取所有匹配位置
     * 使用非极大值抑制避免重复检测
     */
    private fun findAllMatches(
        result: Mat,
        sourceColor: Mat,
        templateColor: Mat,
        confidenceThreshold: Double,
        colorDiffThreshold: Double,
        templateWidth: Int,
        templateHeight: Int
    ): List<MatchResult> {

        val matches = mutableListOf<MatchResult>()
        val resultCopy = Mat()
        result.copyTo(resultCopy)
        val templateMean = Core.mean(templateColor)

        while (true) {
            val minMaxLoc = Core.minMaxLoc(resultCopy)

            if (minMaxLoc.maxVal <= confidenceThreshold) break

            val loc = minMaxLoc.maxLoc
            val x = loc.x.toInt()
            val y = loc.y.toInt()
            val diffRatio = 1.0 - minMaxLoc.maxVal
            val colorDiff = getColorDiff(sourceColor, x, y, templateWidth, templateHeight, templateMean)

            if (colorDiff < colorDiffThreshold) {
                matches.add(
                    MatchResult(
                        x = x,
                        y = y,
                        width = templateWidth,
                        height = templateHeight,
                        diffRatio = diffRatio.coerceIn(0.0, 1.0)
                    )
                )
            }

            // 无论颜色复核是否通过，都屏蔽当前最佳候选，继续找下一个
            suppressMatch(resultCopy, x, y, templateWidth, templateHeight)

            if (Core.countNonZero(resultCopy) == 0) break
        }

        resultCopy.release()

        return matches.sortedBy { it.diffRatio }
    }

    private fun getColorDiff(
        sourceColor: Mat,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        templateMean: Scalar
    ): Double {
        val left = x.coerceAtLeast(0)
        val top = y.coerceAtLeast(0)
        val right = (x + width).coerceAtMost(sourceColor.cols())
        val bottom = (y + height).coerceAtMost(sourceColor.rows())
        val validRect = org.opencv.core.Rect(
            left,
            top,
            (right - left).coerceAtLeast(0),
            (bottom - top).coerceAtLeast(0)
        )
        if (validRect.width <= 0 || validRect.height <= 0) return 100.0

        val candidate = Mat(sourceColor, validRect)
        return try {
            val candidateMean = Core.mean(candidate)
            var diff = 0.0
            for (channel in 0..2) {
                diff += kotlin.math.abs(candidateMean.`val`[channel] - templateMean.`val`[channel])
            }
            (diff * 100.0) / (255.0 * 3.0)
        } finally {
            candidate.release()
        }
    }

    private fun suppressMatch(
        result: Mat,
        x: Int,
        y: Int,
        templateWidth: Int,
        templateHeight: Int
    ) {
        val left = x.coerceAtLeast(0)
        val top = y.coerceAtLeast(0)
        val right = (x + templateWidth).coerceAtMost(result.cols())
        val bottom = (y + templateHeight).coerceAtMost(result.rows())
        if (right <= left || bottom <= top) return

        val mask = Mat.zeros(result.size(), CvType.CV_8UC1)
        try {
            Imgproc.rectangle(
                mask,
                Point(left.toDouble(), top.toDouble()),
                Point((right - 1).toDouble(), (bottom - 1).toDouble()),
                Scalar(255.0),
                -1
            )
            result.setTo(Scalar(0.0), mask)
        } finally {
            mask.release()
        }
    }
}
