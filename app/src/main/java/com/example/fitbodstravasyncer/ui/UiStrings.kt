package com.example.fitbodstravasyncer.ui

object UiStrings {
    // Permissions & Onboarding
    const val WHY_THIS_PERMISSION = "Why this permission?"
    const val PERMISSION_DESC = "This app needs access to your Health Connect data to read workouts, calories, and heart rate.\n\nWe only use this data to sync your Fitbod sessions with Strava."
    const val ALLOW = "Allow"
    const val DENY = "Deny"
    const val WELCOME = "Welcome! Let’s get set up."
    const val STEP1_LABEL = "Step 1: Grant Health Connect Permission"
    const val STEP1_BTN = "Grant Health Connect"
    const val STEP2_LABEL = "Step 2: Connect to Strava"
    const val STEP2_BTN = "Connect Strava"
    const val SETUP_COMPLETE = "Setup complete! Entering app…"

    // ActionsSheet & Controls
    const val ACTIONS = "Actions"
    const val DATE_FROM = "Date From"
    const val DATE_TO = "Date To"
    const val PICK = "Pick"

    // Control help titles/descriptions
    const val FETCH_WORKOUTS_TITLE = "Fetch Workouts"
    const val FETCH_WORKOUTS_DESC = "Fetches Fitbod workouts between the selected dates."
    const val FETCH_FITBOD_BTN = "Fetch Fitbod Workouts"
    const val API_LIMIT_FETCH_HINT = "Can't Fetch Fitbod. Try again in %s"
    const val AUTO_SYNC_TITLE = "Auto-sync"
    const val AUTO_SYNC_DESC = "Automatically syncs workouts to or from Strava every 15 minutes for the past 24 hours."
    const val AUTO_SYNC_LABEL = "Auto-sync 24h every 15m"
    const val DAILY_SYNC_TITLE = "Daily Sync"
    const val DAILY_SYNC_DESC = "Performs a daily synchronization of your workouts."
    const val DAILY_SYNC_LABEL = "Daily Sync"
    const val CHECK_MATCHING_TITLE = "Check Matching Workouts"
    const val CHECK_MATCHING_DESC = "Checks for workouts that already exist in Strava to avoid duplicates."
    const val CHECK_MATCHING_BTN = "Read Strava's Activities"
    const val API_LIMIT_READ_HINT = "Can't read Strava. Try again in %s"
    const val SYNC_ALL_TITLE = "Sync All"
    const val SYNC_ALL_DESC = "Synchronizes all workouts to Strava."
    const val SYNC_ALL_BTN = "Sync All Strava Activities"
    const val API_LIMIT_SYNC_HINT = "Can't sync all. Try again in %s"
    const val DELETE_ALL_TITLE = "Delete All Sessions from App"
    const val DELETE_ALL_DESC = "Deletes all workout sessions from the list. Does not delete from Strava neither Fitbod just on this App"
    const val DELETE_ALL_BTN = "Delete All Activities"

    // Dialogs
    const val CONFIRM_DELETE_TITLE = "Confirm Delete"
    const val CONFIRM_DELETE_TEXT = "Delete %d sessions?"
    const val CONFIRM_DELETE_ALL_TITLE = "Confirm Delete All"
    const val CONFIRM_DELETE_ALL_TEXT = "Delete ALL sessions?"
    const val YES = "Yes"
    const val NO = "No"
    const val OK = "OK"
    const val CANCEL = "Cancel"
    const val HELP = "Help"

    // MainScreen/Filters/Themes
    const val FILTER_ACTIVITIES = "Filter Activities"
    const val APP_THEME = "App Theme"
    const val THEME_LIGHT = "Theme: Light"
    const val THEME_DARK = "Theme: Dark"
    const val THEME_SYSTEM = "Theme: System"
    const val USE_DYNAMIC_COLOR = "Use Dynamic Color"
    const val MENU = "Menu"
    const val MORE_ACTIONS = "More Actions"

    // FABs
    const val SYNC_SELECTED = "Sync Selected"
    const val DELETE_SELECTED = "Delete Selected"

    // Home Screen empty state
    const val NO_ACTIVITIES_ICON_DESC = "No activities"
    const val NO_ACTIVITIES = "No activities fetched"

    // SessionCard
    const val SYNCED = "Synced"
    const val NOT_SYNCED = "Not Synced"
    const val DURATION = "Duration"
    const val CALORIES = "Calories"
    const val AVG_HR = "Avg HR"
    const val MINUTES = "min"
    const val KCAL = "kcal"
    const val BPM = "bpm"

    // Chart
    const val NOT_ENOUGH_HR = "Not enough heart rate data for chart."

    // API & Error banners
    const val API_LIMIT_REACHED_BANNER = "API limit reached. Try again in %s"

    // Notifications (optional, for in-app toast/snackbars)
    const val DELETED_SESSIONS = "Deleted %d sessions"
    const val ALL_SESSIONS_DELETED = "All sessions deleted"
    const val FETCHED_ACTIVITIES = "Fetched activities"
    const val MATCHING_CHECKED = "Matching Strava workouts checked"
    const val WAIT_BETWEEN_CHECKS = "Please wait 15 minutes between checks."
    const val SYNC_ALL_REQUESTED = "Sync all requested"
    const val ALREADY_SYNCED = "Already synced"
    const val SYNCING_WORKOUTS = "Syncing %d workout(s) to Strava"
    const val CONNECT_STRAVA_FIRST = "Connect Strava first"
    const val INVALID_SESSION_SYNC = "Invalid session for sync."
}
