# DAFAN 4K Android

تطبيق Android بسيط بواجهة عربية لتحويل الفيديو إلى 4K محليًا على الهاتف.

## الوظائف
- اختيار فيديو من مدير الملفات.
- تحويل الفيديو إلى 4K مع الحفاظ على نسبة الأبعاد.
- H.264 + AAC داخل MP4.
- استخدام Media3 Transformer.
- شريط تقدم.
- إلغاء التحويل.
- حفظ الناتج في:
  Movies/DAFAN_4K

## ملاحظة مهمة
هذا الإصدار يقوم بـ 4K Upscaling/Rescaling وليس AI Super Resolution.
أي أنه يرفع أبعاد الفيديو إلى 2160p على الضلع القصير، لكنه لا يستطيع خلق تفاصيل حقيقية غير موجودة في المصدر.

مثال:
1920x1080 -> 3840x2160
1080x1920 -> 2160x3840

## البناء
افتح المجلد في Android Studio ثم انتظر Gradle Sync وبعدها Build APK.

المكتبات الرئيسية:
- AndroidX Media3 Transformer 1.11.1
- AndroidX Media3 Effect 1.11.1
- Material Components

الحد الأدنى Android 6.0 (API 23).
