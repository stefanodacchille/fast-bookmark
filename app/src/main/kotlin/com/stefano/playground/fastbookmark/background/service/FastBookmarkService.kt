package com.stefano.playground.fastbookmark.background.service

import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
import android.os.IBinder
import com.stefano.playground.fastbookmark.FastBookmarkApplication
import com.stefano.playground.fastbookmark.R
import com.stefano.playground.fastbookmark.utils.AppPreferences
import java.net.MalformedURLException
import java.net.URL
import javax.inject.Inject

class FastBookmarkService : Service(), ClipboardManager.OnPrimaryClipChangedListener {

  @Inject lateinit var preferences: AppPreferences

  private val CHOOSER_ITEM_FLAGS: Int =
      (Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
          or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
          or(Intent.FLAG_ACTIVITY_CLEAR_TOP)
          or(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
          or(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
          or(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
          or(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
          or(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
          or(Intent.FLAG_ACTIVITY_NEW_TASK)
          or(Intent.FLAG_ACTIVITY_NO_ANIMATION)
          or(Intent.FLAG_ACTIVITY_NO_HISTORY)
          or(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
          or(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP)
          or(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
          or(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
          or(Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS)
          or(Intent.FLAG_ACTIVITY_SINGLE_TOP)
          //          or(Intent.FLAG_ACTIVITY_TASK_ON_HOME)
          //          or(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
          //          or(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
          //          or(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
          //          or(Intent.FLAG_RECEIVER_FOREGROUND)
          //          or(Intent.FLAG_RECEIVER_NO_ABORT)
          //          or(Intent.FLAG_RECEIVER_REGISTERED_ONLY)
          //          or(Intent.FLAG_RECEIVER_REPLACE_PENDING)
          )

  override fun onPrimaryClipChanged() {
    if (!clipboardManager.hasPrimaryClip()) return

    val item = clipboardManager.primaryClip.getItemAt(0)
    if (isUrl(item.text.toString())) {
      val baseIntent = Intent(Intent.ACTION_SEND)
          .setType("text/plain")
          .putExtra(Intent.EXTRA_TEXT, item.text)
      val intent = favouriteBookmarksAppOrChooserIntent(baseIntent)
      if (intent != null) {
        applicationContext.startActivity(intent)
      }
    }
  }

  private fun favouriteBookmarksAppOrChooserIntent(intent: Intent): Intent? {
    val shareActivityInfo = retrieveFavouriteBookmarksAppInfo(intent)
    if (shareActivityInfo != null) {
      return Intent(intent)
          .setClassName(shareActivityInfo.first, shareActivityInfo.second)
          .setFlags(CHOOSER_ITEM_FLAGS)
    } else {
      val text = applicationContext.getString(R.string.save_link)
      val chooserIntent =
          Intent.createChooser(intent, text).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      return if (chooserIntent.resolveActivity(packageManager) != null) {
        chooserIntent
      } else {
        null
      }
    }
  }

  private fun retrieveFavouriteBookmarksAppInfo(intent: Intent): Pair<String, String>? {
    val info = preferences.getFavouriteBookmarksAppData()
    return if (favouriteBookmarksAppInstalled(intent, info)) {
      info
    } else {
      null
    }
  }

  private fun favouriteBookmarksAppInstalled(intent: Intent,
                                             shareActivityInfo: Pair<String, String>?)
      : Boolean {
    val activities =
        applicationContext.packageManager.queryIntentActivities(intent, MATCH_DEFAULT_ONLY)
    val res = activities.filter { a ->
      a.activityInfo.packageName == shareActivityInfo?.first
          && a.activityInfo.name == shareActivityInfo?.second
    }
    return res.isNotEmpty()
  }

  // TODO: is there a cleaner way to do this that doesn't involve crazy regexes?
  private fun isUrl(text: String?): Boolean {
    try {
      URL(text)
    } catch (e: MalformedURLException) {
      return false
    }
    return true
  }

  override fun onCreate() {
    super.onCreate()
    DaggerServiceComponent
        .builder()
        .applicationComponent(FastBookmarkApplication.component)
        .build()
        .inject(this)
    clipboardManager.addPrimaryClipChangedListener(this)
  }

  override fun onDestroy() {
    super.onDestroy()
    clipboardManager.removePrimaryClipChangedListener(this)
  }

  override fun onBind(intent: Intent?): IBinder? {
    throw UnsupportedOperationException()
  }

  private val clipboardManager: ClipboardManager
    get() {
      return applicationContext.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    }

  companion object {
    fun start(context: Context) {
      context.startService(Intent(context, FastBookmarkService::class.java))
    }

    fun stop(context: Context) {
      context.stopService(Intent(context, FastBookmarkService::class.java))
    }
  }
}