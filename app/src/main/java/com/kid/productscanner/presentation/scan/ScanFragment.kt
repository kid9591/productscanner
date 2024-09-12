package com.kid.productscanner.presentation.scan

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
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
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.kid.productscanner.R
import com.kid.productscanner.databinding.DialogInputQuantityBinding
import com.kid.productscanner.databinding.FragmentScanBinding
import com.kid.productscanner.presentation.application.ScannerApplication
import com.kid.productscanner.presentation.scan.viewmodel.ScanViewModel
import com.kid.productscanner.presentation.scan.viewmodel.ScanViewModelFactory
import com.kid.productscanner.repository.ScannerRepository
import com.kid.productscanner.repository.cache.room.entity.Pack
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

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

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
                val selectedPartNumber = parent.getItemAtPosition(position) as String
                findPackFrom(selectedPartNumber)?.let { selectedPack ->
                    showDialogPackFound(selectedPack)
                } ?: showToast("Không tìm thấy thông tin gói hàng này!")
            }
            setOnFocusChangeListener { _, focus ->
                if (focus) {
                    showDropDown()
                }
            }
        }
        viewModel.packsBelongsToTrackingNumberLiveData.observe(viewLifecycleOwner) { packs ->
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                packs.map { it.partNumber }
            )
            binding.atvSelectPartNumber.setAdapter(adapter)
        }
        viewModel.getPackBelongsTo(args.trackingNumber)

        binding.buttonClear.setOnClickListener {
            binding.atvSelectPartNumber.setText("")
        }

        binding.buttonTakePicture.setOnClickListener {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
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

                binding.relativeLoading.isVisible = true

                Log.d(TAG, "shortest: ${args.shortestPartNumber}")

                val imageUri = Uri.fromFile(File(currentPhotoPath))
                detectTexts(imageUri) { foundTexts ->

                    lifecycleScope.launch {
                        run breaking@{
                            foundTexts.forEach { foundText ->
                                val pack = if (foundText.length >= args.shortestPartNumber) {
                                    Log.d(TAG, "findingPack: $foundText")
                                    findPackFrom(foundText)
                                } else null
                                if (pack != null) {
                                    binding.relativeLoading.isVisible = false
                                    showDialogPackFound(pack)
                                    return@breaking
                                }
                            }
                            withContext(Dispatchers.Main) {
                                binding.relativeLoading.isVisible = false
                                showToast("Không tìm thấy gói hàng!")
                            }
                        }
                    }
                }
            }
        }

    private fun showDialogPackFound(pack: Pack) {
        val builder = AlertDialog.Builder(requireContext())
        val binding = DataBindingUtil.inflate<DialogInputQuantityBinding>(
            LayoutInflater.from(requireContext()),
            R.layout.dialog_input_quantity,
            null,
            false
        )
        binding.projectNumber = pack.projectName
        //sửa lại pack đã điền
        if (pack.quantityReceived.isNotEmpty()) {
            binding.editTextQuantity.setText(pack.quantityReceived)
        }

        builder.setCancelable(false)
        builder.setTitle("Tìm thấy!")
        builder.setView(binding.root)
        builder.setPositiveButton("OK") { dialog, _ ->
            if (binding.editTextQuantity.text.toString().isEmpty()) {
                showToast("Hãy nhập số lượng gói hàng")
            } else {
                lifecycleScope.launch(Dispatchers.IO) {
                    pack.apply {
                        quantityReceived = binding.editTextQuantity.text.toString()
                        dateReceivedMillis = System.currentTimeMillis()
                    }
                    if (viewModel.updatePack(pack)) {
                        withContext(Dispatchers.Main) {
                            showToast("Thành công!")
                            dialog.dismiss()
                        }
                    }
                }
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
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

            Log.d(TAG, "external: ${Environment.getExternalStorageDirectory().absolutePath}")

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

    private fun detectTexts(fileUri: Uri, foundTextsCallback: ((List<String>) -> Unit)) {
        val image = InputImage.fromFilePath(requireContext(), fileUri)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                val resultText = result.text
                Log.d(TAG, "resultText: $resultText")

                val texts = mutableListOf<String>()

                for (block in result.textBlocks) {
                    val blockText = block.text
//                    Log.d(TAG, "blockText: $blockText")
                    val blockCornerPoints = block.cornerPoints
                    val blockFrame = block.boundingBox
                    for (line in block.lines) {
                        val lineText = line.text
//                        Log.d(TAG, "lineText: $lineText")
                        val lineCornerPoints = line.cornerPoints
                        val lineFrame = line.boundingBox
                        for (element in line.elements) {
                            val elementText = element.text
//                            val elementCornerPoints = element.cornerPoints
//                            val elementFrame = element.boundingBox
                            texts.add(elementText)
                        }
                    }
                }

                foundTextsCallback(texts)
            }
            .addOnFailureListener { e ->
                showToast("Fail to recognize text: ${e.localizedMessage}")
            }
    }

    private fun findPackFrom(partNumber: String): Pack? {
        return viewModel.findPackWith(partNumber)
    }
}