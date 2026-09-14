package com.spendr.app.kt.platform

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Quick Settings "Add spending" tile, ported from the RN app's
 * `AddSpendingTileService`: always STATE_INACTIVE, tap fires `spendrkt://add`
 * package-scoped so it never raises a chooser.
 */
class AddSpendingTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.let { tile ->
            tile.state = Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        if (isSecure) {
            unlockAndRun { launchAddSpending() }
        } else {
            launchAddSpending()
        }
    }

    @android.annotation.SuppressLint("StartActivityAndCollapseDeprecated")
    private fun launchAddSpending() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ADD_SPENDING_URI)).apply {
            setPackage(packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private companion object {
        const val ADD_SPENDING_URI = "spendrkt://add"
    }
}
