package app.secondclass.healthsyncer.util

import android.content.Context
import android.health.connect.HealthPermissions
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord

@RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 13)
val HEALTH_PERMISSIONS = setOf(
    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
    HealthPermission.getReadPermission(HeartRateRecord::class),
    HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY,
    HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND
)

@RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 13)
suspend fun hasRequiredHealthPermissions(context: Context): Boolean {
    val healthClient = HealthConnectClient.getOrCreate(context)
    val granted = healthClient.permissionController.getGrantedPermissions()
    return HEALTH_PERMISSIONS.all { granted.contains(it) }
}