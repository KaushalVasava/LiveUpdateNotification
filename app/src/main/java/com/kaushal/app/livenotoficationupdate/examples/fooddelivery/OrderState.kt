package com.kaushal.app.livenotoficationupdate.examples.fooddelivery

/**
 * Represents different states in a food delivery order lifecycle
 * 
 * Status Text Guidelines:
 * - Used for status chips in Live Update notifications
 * - Keep under 7 characters for full display in status bar
 * - Shows critical info at a glance when notification is collapsed
 * - Source: https://developer.android.com/develop/ui/views/notifications/live-update#status-chips
 */
enum class OrderState(
    val progress: Int,
    val statusText: String,        // Status chip text (keep ≤7 chars for best display)
    val orderStatus: String,        // Full notification content text
    val delay: Long                 // Demo timing delay in milliseconds
) {
    INITIALIZING(0, "Placing", "Placing your order...", 3000),                      // 7 chars ✅
    ORDER_CONFIRMED(10, "Confirm", "Order confirmed - Restaurant preparing", 5000),  // 7 chars ✅ (shortened)
    FOOD_PREPARATION(25, "Cooking", "Your food is being prepared", 8000),           // 7 chars ✅ (shortened)
    READY_FOR_PICKUP(50, "Ready", "Food ready - Driver picking up", 10000),         // 5 chars ✅
    FOOD_ENROUTE(75, "On way", "Driver is on the way to you", 12000),               // 6 chars ✅
    FOOD_ARRIVING(90, "Near", "Driver arriving in 2 min", 8000),                    // 4 chars ✅ (shortened)
    ORDER_COMPLETE(100, "Arrived", "Order delivered - Enjoy your meal!", 5000)      // 7 chars ✅
}

