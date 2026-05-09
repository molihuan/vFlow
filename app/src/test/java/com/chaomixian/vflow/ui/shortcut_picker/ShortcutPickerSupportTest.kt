package com.chaomixian.vflow.ui.shortcut_picker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcutPickerSupportTest {

    @Test
    fun buildLaunchCommand_parsesIntentAndExtras() {
        val command = ShortcutPickerSupport.buildLaunchCommand(
            "[Intent { act=com.tencent.mm.ui.ShortCutDispatchAction cmp=com.tencent.mm/com.tencent.mm.ui.LauncherUI flg=0x10000000 }/PersistableBundle[{LauncherUI.Shortcut.LaunchType=launch_type_scan_qrcode}]]"
        )

        requireNotNull(command)
        assertTrue(command.startsWith("am start"))
        assertTrue(command.contains("-a 'com.tencent.mm.ui.ShortCutDispatchAction'"))
        assertTrue(command.contains("-n 'com.tencent.mm/com.tencent.mm.ui.LauncherUI'"))
        assertTrue(command.contains("-f 0x10000000"))
        assertTrue(command.contains("--es 'LauncherUI.Shortcut.LaunchType' 'launch_type_scan_qrcode'"))
    }

    @Test
    fun readData_splitsKeyValuePairs() {
        val data = ShortcutPickerSupport.readData("act=test.action cmp=com.test/.Main flg=0x10000000")
        assertEquals("test.action", data["act"])
        assertEquals("com.test/.Main", data["cmp"])
        assertEquals("0x10000000", data["flg"])
    }
}
