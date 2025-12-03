package com.example.qceqapp.uis.main

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.example.qceqapp.R
import com.example.qceqapp.data.model.session.UserSession
import com.example.qceqapp.data.network.Constants
import com.example.qceqapp.databinding.ActivityMainBinding
import com.example.qceqapp.uis.login.LoginActivity
import com.example.qceqapp.uis.toinspect.ToInspectFragment
import com.example.qceqapp.uis.viewhistory.ViewHistoryFragment
import com.google.android.material.navigation.NavigationView
import com.example.qceqapp.uis.torelease.ToReleaseFragment

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupNavigationDrawer()
        updateHeaderWithUserData()
        loadDefaultFragment()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "TO INSPECT"
    }

    private fun setupNavigationDrawer() {
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)

        updateVersionInMenu()
    }

    private fun updateHeaderWithUserData() {
        val headerView = binding.navView.getHeaderView(0)
        val tvName = headerView.findViewById<TextView>(R.id.headerUserName)
        val tvEmail = headerView.findViewById<TextView>(R.id.headerUserEmail)
        val tvRole = headerView.findViewById<TextView>(R.id.headerUserRole)
        if (UserSession.isLoggedIn()) {
            tvName.text = UserSession.getName().ifEmpty { UserSession.getUsername() }
            tvEmail.text = UserSession.getEmail()
            tvRole.text = UserSession.getRole()
        } else {
            goToLogin()
        }
    }

    private fun updateVersionInMenu() {
        val menu = binding.navView.menu
        val versionItem = menu.findItem(R.id.versionItem)
        versionItem?.title = "Version ${Constants.VERSION}"
    }

    private fun loadDefaultFragment() {
        loadFragment(ToInspectFragment())
        binding.navView.setCheckedItem(R.id.checkOrders)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.checkOrders -> {
                loadFragment(ToInspectFragment())
                supportActionBar?.title = "To Inspect"
            }
            R.id.viewHistory -> {
                loadFragment(ViewHistoryFragment())
                supportActionBar?.title = "View History"
            }
            R.id.toRelease -> {
                loadFragment(ToReleaseFragment())
                supportActionBar?.title = "To Release Boxes"
            }
            R.id.settingsItem -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.LogoutItem -> {
                showLogoutConfirmationDialog()
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirm Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _: DialogInterface, _: Int ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        UserSession.clearSession()
        Constants.token = ""

        val sessionManager = com.example.qceqapp.utils.SessionManager(this)
        sessionManager.clearCredentials()

        goToLogin()
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
