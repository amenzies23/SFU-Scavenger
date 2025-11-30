package com.aark.sfuscavenger.qrcode

import com.journeyapps.barcodescanner.CaptureActivity

class QRScanner : CaptureActivity()

//
//import androidx.annotation.OptIn
//import androidx.camera.core.CameraSelector
//import androidx.camera.core.ExperimentalGetImage
//import androidx.camera.core.ImageAnalysis
//import androidx.camera.core.ImageProxy
//import androidx.camera.core.Preview
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.camera.view.PreviewView
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.viewinterop.AndroidView
//import androidx.core.content.ContextCompat
//import androidx.lifecycle.compose.LocalLifecycleOwner
//import com.google.mlkit.vision.barcode.BarcodeScanner
//import com.google.mlkit.vision.barcode.BarcodeScannerOptions
//import com.google.mlkit.vision.barcode.BarcodeScanning
//import com.google.mlkit.vision.barcode.common.Barcode
//import com.google.mlkit.vision.common.InputImage
//
//@Composable
//fun QRScanner(
//    onScanned: (String) -> Unit,
//    onClose: () -> Unit
//) {
//    val context = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//
//    AndroidView(
//        modifier = Modifier.fillMaxSize(),
//        factory = { ctx ->
//            val previewView = PreviewView(ctx)
//
//            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
//            cameraProviderFuture.addListener({
//                val cameraProvider = cameraProviderFuture.get()
//
//                val scanner = BarcodeScanning.getClient(
//                    BarcodeScannerOptions.Builder()
//                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
//                        .build()
//                )
//
//                val preview = Preview.Builder().build().also {
//                    it.setSurfaceProvider(previewView.surfaceProvider)
//                }
//
//                val analysis = ImageAnalysis.Builder()
//                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                    .build()
//
//                analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
//                    processImageProxy(scanner, imageProxy, onScanned)
//                }
//
//                cameraProvider.unbindAll()
//                cameraProvider.bindToLifecycle(
//                    lifecycleOwner,
//                    CameraSelector.DEFAULT_BACK_CAMERA,
//                    preview,
//                    analysis
//                )
//
//            }, ContextCompat.getMainExecutor(ctx))
//
//            previewView
//        }
//    )
//}
//
//@OptIn(ExperimentalGetImage::class)
//fun processImageProxy(
//    scanner: BarcodeScanner,
//    imageProxy: ImageProxy,
//    onScanned: (String) -> Unit
//) {
//    val mediaImage = imageProxy.image ?: run {
//        imageProxy.close()
//        return
//    }
//
//    val image = InputImage.fromMediaImage(
//        mediaImage,
//        imageProxy.imageInfo.rotationDegrees
//    )
//
//    scanner.process(image)
//        .addOnSuccessListener { barcodes ->
//            for (barcode in barcodes) {
//                barcode.rawValue?.let { text ->
//                    onScanned(text)
//                }
//            }
//        }
//        .addOnCompleteListener {
//            imageProxy.close()
//        }
//}
