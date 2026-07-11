// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.launcher.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kurostream.launcher.R
import com.kurostream.launcher.databinding.ActivityLauncherBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LauncherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLauncherBinding
    private val viewModel: LauncherViewModel by viewModels()
    private lateinit var tileAdapter: LauncherTileAdapter

    companion object {
        private const val REQUEST_ENABLE_LAUNCHER = 1001
        private const val SPAN_COUNT = 5
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupObservers()
        setupFocusHandling()

        viewModel.loadTiles()
    }

    private fun setupRecyclerView() {
        tileAdapter = LauncherTileAdapter(
            onTileClicked = { tile -> handleTileClick(tile) },
            onTileFocused = { tile, view -> handleTileFocus(tile, view) }
        )

        binding.recyclerTiles.apply {
            layoutManager = GridLayoutManager(this@LauncherActivity, SPAN_COUNT)
            adapter = tileAdapter
            setHasFixedSize(true)
            isFocusable = true
            isFocusableInTouchMode = true
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.tiles.collectLatest { tiles ->
                    tileAdapter.submitList(tiles)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isDefaultLauncher.collectLatest { isDefault ->
                    binding.btnSetDefault.visibility = if (isDefault) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun setupFocusHandling() {
        binding.btnSetDefault.setOnClickListener {
            requestSetDefaultLauncher()
        }

        binding.btnSetDefault.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.btnSetDefault.animate().scaleX(1.05f).scaleY(1.05f).setDuration(150).start()
            } else {
                binding.btnSetDefault.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
            }
        }
    }

    private fun handleTileClick(tile: LauncherTile) {
        when (tile.type) {
            TileType.APP -> launchApp(tile.packageName, tile.activityName)
            TileType.SHORTCUT -> launchShortcut(tile.intentAction, tile.extras)
            TileType.SETTINGS -> openSettings()
            TileType.STREAMBOX -> launchStreamBox()
        }
    }

    private fun handleTileFocus(tile: LauncherTile, view: View) {
        view.animate().scaleX(1.08f).scaleY(1.08f).setDuration(200).start()
        binding.tvTileTitle.text = tile.title
        binding.tvTileDescription.text = tile.description
    }

    private fun launchApp(packageName: String?, activityName: String?) {
        if (packageName == null) return
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "App not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchShortcut(action: String?, extras: Bundle?) {
        val intent = Intent(action).apply {
            extras?.let { putExtras(it) }
        }
        startActivity(intent)
    }

    private fun openSettings() {
        startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
    }

    private fun launchStreamBox() {
        val intent = Intent(this, Class.forName("com.kurostream.launcher.MainActivity"))
        startActivity(intent)
    }

    private fun requestSetDefaultLauncher() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as android.app.role.RoleManager
            if (roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME)) {
                startActivityForResult(
                    roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME),
                    REQUEST_ENABLE_LAUNCHER
                )
            }
        } else {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            val componentName = ComponentName(this, LauncherActivity::class.java)
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_LAUNCHER) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "StreamBox is now your home screen", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                // Prevent exiting launcher with back button
                true
            }
            KeyEvent.KEYCODE_HOME -> {
                // Already home
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkDefaultLauncherStatus()
    }
}
