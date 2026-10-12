package com.guardpay.shared.ai

/**
 * Where the on-device model stands. The Pay screen shows it next to the message
 * field; it never changes a payment, a lane or the form.
 */
sealed interface ModelStatus {
    /** This platform has no model (iOS, tests). Readings come back unavailable. */
    data object Unsupported : ModelStatus

    /** The model can run here but is not on the phone yet. */
    data class NotDownloaded(val totalBytes: Long) : ModelStatus

    data class Downloading(val bytes: Long, val totalBytes: Long) : ModelStatus

    /** The download ended without a complete, matching file. It can be retried. */
    data object DownloadFailed : ModelStatus

    data object OnDevice : ModelStatus
}
