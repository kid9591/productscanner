package com.kid.productscanner.presentation.scan

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.datatransport.backend.cct.BuildConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.kid.productscanner.databinding.FragmentScanBinding
import com.kid.productscanner.presentation.application.ScannerApplication
import com.kid.productscanner.presentation.scan.viewmodel.ScanViewModel
import com.kid.productscanner.presentation.scan.viewmodel.ScanViewModelFactory
import com.kid.productscanner.repository.ScannerRepository
import com.kid.productscanner.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ScanFragment : Fragment() {

    private lateinit var binding: FragmentScanBinding

    private val TAG: String = "chi.trinh"

    private val args: ScanFragmentArgs by navArgs()

    private val viewModel: ScanViewModel by viewModels {
        val repository =
            ScannerRepository((requireActivity().application as ScannerApplication).scannerDatabase)
        ScanViewModelFactory(repository)
    }

    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private lateinit var currentPhotoPath: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentScanBinding.inflate(inflater, container, false)
        binding.viewModel = this@ScanFragment.viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.setSelectedTrackingNumber(args.trackingNumber)

        binding.atvSelectPartNumber.apply {
            setOnItemClickListener { parent, view, position, id ->
                val selectedItem = parent.getItemAtPosition(position) as String

                //TODO: show dialog input quantity

//                binding.textShortestPn.text = "Đang tìm kiếm Part Number ngắn nhất..."
//                viewModel.findShortestPartNumber(selectedItem)
            }
            setOnFocusChangeListener { _, focus ->
                if (focus) {
                    showDropDown()
                }
            }
        }
        fillPartNumbersToDropdown()

        binding.buttonClear.setOnClickListener {
            binding.atvSelectPartNumber.setText("")
        }

        binding.buttonTakePicture.setOnClickListener {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

    }

    private fun fillPartNumbersToDropdown() {
        lifecycleScope.launch {
            val partNumbers = viewModel.getPartNumbersOf(args.trackingNumber)
            withContext(Dispatchers.Main) {
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    partNumbers
                )
                binding.atvSelectPartNumber.setAdapter(adapter)
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                createFileToSaveImage()?.let { photoUri ->
                    takePictureResultLauncher.launch(
                        Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
//                            flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                    )
                } ?: run {
                    showToast("Khong the tao file de luu anh")
                }
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied
                showToast("Permission denied, please allow camera permission!")
            }
        }

    private val takePictureResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri = Uri.fromFile(File(currentPhotoPath))
                detectTexts(imageUri) { foundText ->
                    Log.d(TAG, "foundText: $foundText")
//                    findProjectNumberFrom(elementText)
                }
            }
        }

    @SuppressLint("SimpleDateFormat")
    private fun createFileToSaveImage(): Uri? {
        return try {
            // Tạo tên file ảnh
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val storageDir: File? =
                requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File.createTempFile(
                "JPEG_${timeStamp}_", /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
            ).apply {
                // Lưu đường dẫn file để sử dụng sau
                currentPhotoPath = absolutePath
            }

            return FileProvider.getUriForFile(
                requireContext().applicationContext,
                "com.kid.productscanner.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun detectTexts(fileUri: Uri, foundTextCallback: ((String) -> Unit)) {
        val image = InputImage.fromFilePath(requireContext(), fileUri)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                val resultText = result.text
                Log.d(TAG, "resultText: $resultText")
                for (block in result.textBlocks) {
                    val blockText = block.text
                    Log.d(TAG, "blockText: $blockText")
                    val blockCornerPoints = block.cornerPoints
                    val blockFrame = block.boundingBox
                    for (line in block.lines) {
                        val lineText = line.text
                        Log.d(TAG, "lineText: $lineText")
                        val lineCornerPoints = line.cornerPoints
                        val lineFrame = line.boundingBox
                        for (element in line.elements) {
                            val elementText = element.text
                            foundTextCallback(elementText)
                            val elementCornerPoints = element.cornerPoints
                            val elementFrame = element.boundingBox
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                showToast("Fail to recognize text: ${e.localizedMessage}")
            }
    }

    private fun findProjectNumberFrom(partNumber: String): String {
        return ""
    }
}