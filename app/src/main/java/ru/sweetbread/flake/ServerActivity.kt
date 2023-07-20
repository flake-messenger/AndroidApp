package ru.sweetbread.flake

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.discord.panels.OverlappingPanelsLayout

class ServerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)

        findViewById<OverlappingPanelsLayout>(R.id.overlapping_panels)
            .setEndPanelLockState(OverlappingPanelsLayout.LockState.CLOSE)
    }
}
