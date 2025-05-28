package com.example.fitbodstravasyncer.ui

object UiStrings {
    // --- App Title / Headings ---
    const val APP_BAR_TITLE = "Fitbod → Strava"

    // --- Generic Labels & UI Controls ---
    const val CHECK_MATCHING_BUTTON = "Check Matching"
    const val NOT_SET = "Not set"
    const val SELECTED = "Selected"

    // --- Permissions & Onboarding ---
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

    // --- ActionsSheet & Controls ---
    const val ACTIONS = "Actions"
    const val DATE_FROM = "Date From"
    const val DATE_TO = "Date To"
    const val PICK = "Pick"

    // --- Control help titles/descriptions ---
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

    // --- Dialogs ---
    const val CONFIRM_DELETE_TITLE = "Confirm Delete"
    const val CONFIRM_DELETE_TEXT = "Delete %d sessions?"
    const val CONFIRM_DELETE_ALL_TITLE = "Confirm Delete All"
    const val CONFIRM_DELETE_ALL_TEXT = "Delete ALL sessions?"
    const val YES = "Yes"
    const val NO = "No"
    const val OK = "OK"
    const val CANCEL = "Cancel"
    const val HELP = "Help"

    // --- MainScreen/Filters/Themes ---
    const val FILTER_ACTIVITIES = "Filter Activities"
    const val APP_THEME = "App Theme"
    const val THEME_LIGHT = "Theme: Light"
    const val THEME_DARK = "Theme: Dark"
    const val THEME_SYSTEM = "Theme: System"
    const val USE_DYNAMIC_COLOR = "Use Dynamic Color"
    const val MENU = "Menu"
    const val MORE_ACTIONS = "More Actions"

    // --- FABs ---
    const val SYNC_SELECTED = "Sync Selected"
    const val DELETE_SELECTED = "Delete Selected"

    // --- Home Screen empty state ---
    const val NO_ACTIVITIES_ICON_DESC = "No activities"
    const val NO_ACTIVITIES = "No activities fetched"

    // --- SessionCard ---
    const val SYNCED = "Synced"
    const val NOT_SYNCED = "Not Synced"
    const val DURATION = "Duration"
    const val CALORIES = "Calories"
    const val AVG_HR = "Avg HR"
    const val MINUTES = "min"
    const val KCAL = "kcal"
    const val BPM = "bpm"

    // --- Chart ---
    const val NOT_ENOUGH_HR = "Not enough heart rate data for chart."

    // --- API & Error banners ---
    const val API_LIMIT_REACHED_BANNER = "API limit reached. Try again in %s"

    // --- Notifications & Toasts ---
    const val STRAVA_API_LIMIT_NEARLY_REACHED = "Strava API limit nearly reached! The app will try again later."
    const val STRAVA_API_LIMIT_WARNING = "API Limit Warning"
    const val STRAVA_API_LIMIT_BODY = "The app is close to the Strava API request limit. It will try again later."
    const val STRAVA_API_LIMIT_REACHED = "API Limit Reached"
    const val STRAVA_API_LIMIT_REACHED_BODY = "No more uploads until %s"
    const val DAILY_SYNC_NOTIFICATION_TITLE = "Daily Strava Sync"
    const val DAILY_SYNC_NOTIFICATION_BODY = "Your Fitbod sessions have been checked against Strava."
    const val AUTO_SYNC_NOTIFICATION_TITLE = "Auto Strava Sync"
    const val AUTO_SYNC_NOTIFICATION_BODY = "%d new Fitbod session(s) uploaded to Strava."
    const val SYNCING_TO_STRAVA_TITLE = "Syncing to Strava"
    const val SYNCING_TO_STRAVA_BODY = "Uploading workout: %s…"
    const val STRAVA_SYNC_COMPLETE_TITLE = "Strava Sync Complete"
    const val WORKOUT_ALREADY_UPLOADED = "Workout already uploaded: %s"
    const val WORKOUT_ALREADY_ON_STRAVA = "Workout already on Strava: %s"
    const val WORKOUT_UPLOADED = "Workout uploaded: %s"
    const val STRAVA_SYNC_FAILED_TITLE = "Strava Sync Failed"
    const val WORKOUT_UPLOAD_FAILED = "Failed to upload workout: %s. Will retry."
    const val GENERIC_UPLOAD_FAILED = "Failed to upload workout. Will retry."
    const val API_RATE_LIMIT_HIT = "API rate limit hit. Try again in %sm."
    const val UPLOAD_FAILED = "Upload failed: %s"
    const val TOKEN_INVALID_EXPIRED = "Token invalid/expired. Please reconnect Strava."
    const val STRAVA_DISCONNECTED_TITLE = "Strava Disconnected"
    const val STRAVA_DISCONNECTED_BODY = "Please reconnect your Strava account to continue syncing."

    // --- Other Errors and Info ---
    const val INVALID_SESSION_FOR_SYNC = "Invalid session for sync."
    const val CONNECT_STRAVA_FIRST = "Connect Strava first"

    // --- Dynamic (format) examples ---
    // Use with String.format(UiStrings.WORKOUT_UPLOADED, session.title)
    // Use with String.format(UiStrings.WORKOUT_UPLOAD_FAILED, session.title)
    // Use with String.format(UiStrings.API_LIMIT_REACHED_BANNER, apiLimitResetHint)
}
