package com.groqtap.service

import android.app.*
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.groqtap.MainActivity
import kotlin.math.abs

class FloatingWidgetService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private var initialX = 0; private var initialY = 0
    private var initialTouchX = 0f; private var initialTouchY = 0f
    private var isDragging = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createFloatingView()
        startForeground(NOTIF_ID, buildNotification())
    }

    // ── Build the floating circular button ──

    private fun createFloatingView() {
        val size = resources.displayMetrics.density.let { (52 * it).toInt() }

        // Draw an orange circle with "G" text on canvas
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Outer shadow ring
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF4A00")
            alpha = 60
            maskFilter = BlurMaskFilter(size * 0.15f, BlurMaskFilter.Blur.OUTER)
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, shadowPaint)

        // Main circle
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                size * 0.35f, size * 0.35f, size * 0.7f,
                Color.parseColor("#FF6840"), Color.parseColor("#CC3A00"),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 5f, bgPaint)

        // Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#FF7A5C")
            strokeWidth = 1.5f
            alpha = 180
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 6f, borderPaint)

        // "G" letter
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = size * 0.42f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val textBounds = Rect()
        textPaint.getTextBounds("G", 0, 1, textBounds)
        canvas.drawText("G", size / 2f, size / 2f + textBounds.height() / 2f, textPaint)

        floatingView = ImageView(this).apply {
            setImageBitmap(bmp)
        }

        val params = WindowManager.LayoutParams(
            size, size,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40; y = 200
        }

        floatingView.setOnTouchListener(DragTouchListener(params))
        windowManager.addView(floatingView, params)
    }

    // ── Drag + tap logic ──

    private inner class DragTouchListener(private val params: WindowManager.LayoutParams) : View.OnTouchListener {
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x; initialY = params.y
                    initialTouchX = event.rawX; initialTouchY = event.rawY
                    isDragging = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (abs(dx) > 8f || abs(dy) > 8f) isDragging = true
                    if (isDragging) {
                        params.x = (initialX + dx).toInt()
                        params.y = (initialY + dy).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) openMainApp()
                    return true
                }
            }
            return false
        }
    }

    private fun openMainApp() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                action = "com.groqtap.OPEN_CHAT"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )
    }

    // ── Notification (required for foreground service) ──

    private fun buildNotification(): Notification {
        val channelId = "groqtap_widget"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Floating Widget", NotificationManager.IMPORTANCE_MIN).apply {
                setShowBadge(false)
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }

        val openIntent = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = PendingIntent.getService(this, 1,
            Intent(this, FloatingWidgetService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("GroqTap")
            .setContentText("Floating widget is active")
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf() }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized && floatingView.isAttachedToWindow) {
            windowManager.removeView(floatingView)
        }
    }

    companion object {
        private const val NOTIF_ID = 1001
        const val ACTION_STOP = "com.groqtap.STOP_WIDGET"
    }
}
