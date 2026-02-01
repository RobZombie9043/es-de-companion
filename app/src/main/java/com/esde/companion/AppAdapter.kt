package com.esde.companion

import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private var apps: List<ResolveInfo>,
    private val packageManager: PackageManager,
    private val onAppClick: (ResolveInfo) -> Unit,
    private val onAppLongClick: (ResolveInfo, View) -> Unit,
    private val appLaunchPrefs: AppLaunchPreferences,
    private val hiddenApps: Set<String> = setOf()  // ADD THIS PARAMETER
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appIcon: ImageView = view.findViewById(R.id.appIcon)
        val appName: TextView = view.findViewById(R.id.appName)
        val launchIndicator: View = view.findViewById(R.id.launchIndicator)
        val hiddenBadge: TextView = view.findViewById(R.id.hiddenBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        val packageName = app.activityInfo?.packageName ?: ""

        holder.appName.text = app.loadLabel(packageManager)
        holder.appIcon.setImageDrawable(app.loadIcon(packageManager))

        // Show indicator if app launches on other screen
        val launchesOnOtherScreen = appLaunchPrefs.shouldLaunchOnBottom(packageName)
        holder.launchIndicator.visibility = if (launchesOnOtherScreen) View.VISIBLE else View.GONE

        // Show hidden badge if app is hidden
        val isHidden = hiddenApps.contains(packageName)
        holder.hiddenBadge.visibility = if (isHidden) View.VISIBLE else View.GONE

        // Regular click to launch app
        holder.itemView.setOnClickListener {
            onAppClick(app)
        }

        // Long click to show options menu
        holder.itemView.setOnLongClickListener {
            onAppLongClick(app, holder.itemView)
            true
        }
    }

    override fun getItemCount() = apps.size

    // Add method to refresh indicators when preferences change
    fun refreshIndicators() {
        notifyDataSetChanged()
    }

    // Add method to update data without losing scroll position
    fun updateApps(newApps: List<ResolveInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }
}