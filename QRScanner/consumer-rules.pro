# Add rules to prevent ProGuard from obfuscating classes that are used by consumers
# of this library.

# Keep public API classes
-keep class com.shareconnect.qrscanner.QRScannerManager { *; }
-keep class com.shareconnect.qrscanner.QRScannerActivity { *; }

# Keep ML Kit classes that might be obfuscated
-keep class com.google.mlkit.vision.barcode.** { *; }
-keep class com.google.mlkit.vision.common.** { *; }