package com.chaomixian.vflow.core.workflow.module.system

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.chaomixian.vflow.R
import com.chaomixian.vflow.core.module.CustomEditorViewHolder
import com.chaomixian.vflow.core.module.ModuleUIProvider
import com.chaomixian.vflow.core.workflow.model.ActionStep
import com.chaomixian.vflow.ui.shortcut_picker.UnifiedShortcutPickerSheet

private class LaunchShortcutViewHolder(
    view: View,
    val summaryTextView: TextView,
    val pickButton: Button
) : CustomEditorViewHolder(view) {
    var selectedPackageName: String? = null
    var selectedShortcutLabel: String? = null
    var selectedLaunchCommand: String? = null
}

class LaunchShortcutUIProvider : ModuleUIProvider {
    override fun getHandledInputIds(): Set<String> =
        setOf("shortcutLabel", "packageName", "launchCommand")

    override fun createEditor(
        context: Context,
        parent: ViewGroup,
        currentParameters: Map<String, Any?>,
        onParametersChanged: () -> Unit,
        onMagicVariableRequested: ((String) -> Unit)?,
        allSteps: List<ActionStep>?,
        onStartActivityForResult: ((Intent, (Int, Intent?) -> Unit) -> Unit)?
    ): CustomEditorViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.partial_launch_shortcut_editor, parent, false)
        val holder = LaunchShortcutViewHolder(
            view,
            view.findViewById(R.id.text_selected_shortcut_summary),
            view.findViewById(R.id.button_pick_shortcut)
        )

        holder.selectedPackageName = currentParameters["packageName"] as? String
        holder.selectedShortcutLabel = currentParameters["shortcutLabel"] as? String
        holder.selectedLaunchCommand = currentParameters["launchCommand"] as? String
        updateSummaryText(context, holder.summaryTextView, currentParameters)

        holder.pickButton.setOnClickListener {
            val intent = Intent().apply {
                putExtra("shortcut_picker", true)
            }
            onStartActivityForResult?.invoke(intent) { resultCode, data ->
                if (resultCode == android.app.Activity.RESULT_OK && data != null) {
                    val packageName = data.getStringExtra(UnifiedShortcutPickerSheet.EXTRA_SELECTED_PACKAGE_NAME)
                    val shortcutLabel = data.getStringExtra(UnifiedShortcutPickerSheet.EXTRA_SELECTED_SHORTCUT_LABEL)
                    val launchCommand = data.getStringExtra(UnifiedShortcutPickerSheet.EXTRA_SELECTED_LAUNCH_COMMAND)
                    if (!packageName.isNullOrBlank() && !shortcutLabel.isNullOrBlank() && !launchCommand.isNullOrBlank()) {
                        holder.selectedPackageName = packageName
                        holder.selectedShortcutLabel = shortcutLabel
                        holder.selectedLaunchCommand = launchCommand
                        updateSummaryText(
                            context,
                            holder.summaryTextView,
                            mapOf(
                                "packageName" to packageName,
                                "shortcutLabel" to shortcutLabel
                            )
                        )
                        onParametersChanged()
                    }
                }
            }
        }

        return holder
    }

    override fun readFromEditor(holder: CustomEditorViewHolder): Map<String, Any?> {
        val shortcutHolder = holder as? LaunchShortcutViewHolder ?: return emptyMap()
        return mapOf(
            "packageName" to shortcutHolder.selectedPackageName,
            "shortcutLabel" to shortcutHolder.selectedShortcutLabel,
            "launchCommand" to shortcutHolder.selectedLaunchCommand
        ).filterValues { it != null }
    }

    private fun updateSummaryText(context: Context, textView: TextView, parameters: Map<String, Any?>) {
        val packageName = parameters["packageName"] as? String
        val shortcutLabel = parameters["shortcutLabel"] as? String
        textView.text = when {
            shortcutLabel.isNullOrBlank() -> context.getString(R.string.text_not_selected)
            packageName.isNullOrBlank() -> context.getString(R.string.text_shortcut_selected_only, shortcutLabel)
            else -> context.getString(R.string.text_shortcut_selected, shortcutLabel, packageName)
        }
    }

    override fun createPreview(
        context: Context,
        parent: ViewGroup,
        step: ActionStep,
        allSteps: List<ActionStep>,
        onStartActivityForResult: ((Intent, (resultCode: Int, data: Intent?) -> Unit) -> Unit)?
    ): View? = null
}
