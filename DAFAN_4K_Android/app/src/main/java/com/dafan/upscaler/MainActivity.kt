package com.dafan.upscaler

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.Effects
import androidx.media3.effect.Presentation
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.dafan.upscaler.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedUri: Uri? = null
    private var transformer: Transformer? = null
    private var inputTemp: File? = null
    private var outputTemp: File? = null

    private val handler = Handler(Looper.getMainLooper())
    private val progressHolder = ProgressHolder()

    private val pickVideo =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                selectedUri = uri
                binding.tvSelected.text = "تم اختيار الفيديو ✓"
                binding.btnConvert.isEnabled = true
                binding.tvProgress.text = "جاهز للتحويل"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPick.setOnClickListener {
            pickVideo.launch(arrayOf("video/*"))
        }

        binding.btnConvert.setOnClickListener {
            start4K()
        }

        binding.btnCancel.setOnClickListener {
            cancelConversion()
        }
    }

    private fun start4K() {
        val uri = selectedUri ?: run {
            toast("اختر فيديو أولاً")
            return
        }

        setBusy(true)
        binding.progressBar.progress = 0
        binding.tvProgress.text = "جاري تجهيز الفيديو..."

        try {
            inputTemp = File(cacheDir, "dafan_input_${System.currentTimeMillis()}.mp4")
            contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "تعذر فتح الفيديو" }
                FileOutputStream(inputTemp!!).use { output ->
                    input.copyTo(output)
                }
            }

            outputTemp = File(cacheDir, "dafan_4k_${System.currentTimeMillis()}.mp4")

            val mediaItem = MediaItem.fromUri(Uri.fromFile(inputTemp))

            // 2160p على الضلع القصير يحافظ على نسبة العرض/الارتفاع.
            // فيديو 16:9 يصبح 3840x2160، والعمودي يصبح 2160x3840.
            val presentation = Presentation.createForShortSide(2160)

            val edited = EditedMediaItem.Builder(mediaItem)
                .setEffects(
                    Effects(
                        emptyList(),
                        listOf(presentation)
                    )
                )
                .build()

            transformer = Transformer.Builder(this)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(object : Transformer.Listener {

                    override fun onCompleted(
                        composition: androidx.media3.transformer.Composition,
                        exportResult: ExportResult
                    ) {
                        saveToGallery(outputTemp!!)
                    }

                    override fun onError(
                        composition: androidx.media3.transformer.Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        runOnUiThread {
                            setBusy(false)
                            binding.tvProgress.text = "فشل التحويل: ${exportException.message ?: "خطأ غير معروف"}"
                            toast("تعذر تحويل الفيديو")
                        }
                        cleanupTemp()
                    }
                })
                .build()

            transformer!!.start(edited, outputTemp!!.absolutePath)
            updateProgress()

        } catch (e: Exception) {
            setBusy(false)
            binding.tvProgress.text = "خطأ: ${e.message}"
            toast("حدث خطأ أثناء تجهيز الفيديو")
            cleanupTemp()
        }
    }

    private fun updateProgress() {
        val current = transformer ?: return
        val state = current.getProgress(progressHolder)

        if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
            val value = progressHolder.progress.coerceIn(0, 100)
            binding.progressBar.progress = value
            binding.tvProgress.text = "جاري التحويل إلى 4K: $value%"
        } else if (state == Transformer.PROGRESS_STATE_WAITING_FOR_AVAILABILITY) {
            binding.tvProgress.text = "جاري تجهيز الترميز..."
        }

        if (state != Transformer.PROGRESS_STATE_NOT_STARTED) {
            handler.postDelayed({ updateProgress() }, 500)
        }
    }

    private fun saveToGallery(file: File) {
        try {
            val name = "DAFAN_4K_${System.currentTimeMillis()}.mp4"

            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_MOVIES + "/DAFAN_4K"
                )
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }

            val collection = MediaStore.Video.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )

            val resultUri = contentResolver.insert(collection, values)
                ?: throw IllegalStateException("تعذر إنشاء ملف الإخراج")

            contentResolver.openOutputStream(resultUri).use { output ->
                requireNotNull(output)
                file.inputStream().use { input ->
                    input.copyTo(output)
                }
            }

            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            contentResolver.update(resultUri, values, null, null)

            runOnUiThread {
                setBusy(false)
                binding.progressBar.progress = 100
                binding.tvProgress.text = "اكتمل التحويل ✓\nتم الحفظ في Movies/DAFAN_4K"
                toast("تم حفظ فيديو 4K بنجاح")
            }

        } catch (e: Exception) {
            runOnUiThread {
                setBusy(false)
                binding.tvProgress.text = "تم التحويل لكن تعذر الحفظ: ${e.message}"
                toast("تعذر حفظ الفيديو")
            }
        } finally {
            cleanupTemp()
        }
    }

    private fun cancelConversion() {
        transformer?.cancel()
        setBusy(false)
        binding.tvProgress.text = "تم إلغاء التحويل"
        cleanupTemp()
    }

    private fun setBusy(busy: Boolean) {
        binding.btnPick.isEnabled = !busy
        binding.btnConvert.isEnabled = !busy && selectedUri != null
        binding.btnCancel.isEnabled = busy
    }

    private fun cleanupTemp() {
        try { inputTemp?.delete() } catch (_: Exception) {}
        try { outputTemp?.delete() } catch (_: Exception) {}
        inputTemp = null
        outputTemp = null
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        transformer?.cancel()
        cleanupTemp()
        super.onDestroy()
    }
}
