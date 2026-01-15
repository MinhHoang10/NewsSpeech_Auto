package com.newsspeech.auto.util

/**
 * Format timestamp to readable format
 * Example: "2024-01-15T08:30:00" -> "15/01 08:30"
 */
fun formatTimestamp(timestamp: String): String {
    return try {
        when {
            timestamp.contains("T") -> {
                val parts = timestamp.split("T")
                val date = parts[0].split("-")
                val time = parts.getOrNull(1)?.split(":")

                if (date.size >= 3 && time != null && time.size >= 2) {
                    "${date[2]}/${date[1]} ${time[0]}:${time[1]}"
                } else {
                    timestamp
                }
            }
            else -> timestamp
        }
    } catch (e: Exception) {
        timestamp
    }
}