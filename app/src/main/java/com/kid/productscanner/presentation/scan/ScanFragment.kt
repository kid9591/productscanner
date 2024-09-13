package com.kid.productscanner.presentation.scan

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.kid.productscanner.BuildConfig
import com.kid.productscanner.R
import com.kid.productscanner.databinding.DialogInputQuantitiesBinding
import com.kid.productscanner.databinding.FragmentScanBinding
import com.kid.productscanner.presentation.application.ScannerApplication
import com.kid.productscanner.presentation.input_quantity.adapter.PacksFoundAdapter
import com.kid.productscanner.presentation.scan.viewmodel.ScanViewModel
import com.kid.productscanner.presentation.scan.viewmodel.ScanViewModelFactory
import com.kid.productscanner.repository.ScannerRepository
import com.kid.productscanner.repository.cache.room.entity.Pack
import com.kid.productscanner.utils.createFileToSaveImage
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

    private var takingPicture: Boolean = false

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
                val packs = viewModel.findInTrackingPacksWith(selectedPartNumber)
                if (packs.isNotEmpty()) {
                    showDialogPacksFound(packs)
                } else {
                    showToast("Không tìm thấy thông tin gói hàng này!")
                }
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
                packs.map { it.partNumber }.distinct()
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

        binding.buttonDone.setOnClickListener {
            findNavController().navigate(R.id.action_scanFragment_to_SelectExcelFragment)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                requireContext().createFileToSaveImage()?.let { pairs ->
                    currentPhotoPath = pairs.first
                    takePictureResultLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                        putExtra(MediaStore.EXTRA_OUTPUT, pairs.second)
                    })
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

                val imageUri = Uri.fromFile(File(currentPhotoPath))
                detectTexts(imageUri) { foundTexts ->

                    lifecycleScope.launch {
                        run breaking@{
                            foundTexts.forEach { foundText ->
                                val packs =
                                    if (foundText.length >= viewModel.shortestPartNumber.length) {
                                        Log.d(TAG, "findingPack: $foundText")
                                        viewModel.findInTrackingPacksWith(foundText)
                                    } else null
                                if (!packs.isNullOrEmpty()) {
                                    takingPicture = true
                                    binding.relativeLoading.isVisible = false
                                    showDialogPacksFound(packs)
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
            } else {
                takingPicture = false
            }
        }

    private fun showDialogPacksFound(packs: List<Pack>) {
        val builder = AlertDialog.Builder(requireContext())
        val binding = DialogInputQuantitiesBinding.inflate(
            LayoutInflater.from(requireContext()), null, false
        )
        binding.apply {
            viewModel = this@ScanFragment.viewModel
            lifecycleOwner = this@ScanFragment.viewLifecycleOwner

            if (packs.size > 1) {
                this@ScanFragment.viewModel.showNextPackButtonLiveData.value = true
            }

            recyclerPacks.apply {
                layoutManager =
                    GridLayoutManager(requireContext(), 1, LinearLayoutManager.HORIZONTAL, false)
                val snapHelper = PagerSnapHelper()
                snapHelper.attachToRecyclerView(this)
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            snapHelper.findSnapView(recyclerView.layoutManager)?.let { snapView ->
                                val snapPosition = recyclerView.layoutManager?.getPosition(snapView) ?: -1
                                this@ScanFragment.viewModel.showNextPackButtonLiveData.value = snapPosition < packs.size - 1
                                this@ScanFragment.viewModel.showBackPackButtonLiveData.value = snapPosition > 0
                            }
                        }
                    }
                })
                adapter = PacksFoundAdapter(packs)
            }
        }

        builder.setCancelable(false)
        builder.setView(binding.root)
        builder.setPositiveButton("OK") { dialog, _ ->
            lifecycleScope.launch(Dispatchers.IO) {
                packs.forEach { it.dateReceivedMillis = System.currentTimeMillis() }
                try {
                    viewModel.updatePacks(packs)
                    withContext(Dispatchers.Main) {
                        showToast("Thành công!")
                        if (takingPicture) {
                            this@ScanFragment.binding.buttonTakePicture.performClick()
                        }
                        dialog.dismiss()
                    }
                } catch (e: SQLiteConstraintException) {
                    withContext(Dispatchers.Main) {
                        showToast("Lỗi khi lưu dữ liệu!")
                    }
                    e.printStackTrace()
                }
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            takingPicture = false
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        }
        dialog.show()
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
}