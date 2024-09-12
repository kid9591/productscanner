package com.kid.productscanner.presentation.select_excel

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kid.productscanner.R
import com.kid.productscanner.databinding.FragmentSelectExcelBinding
import com.kid.productscanner.presentation.application.ScannerApplication
import com.kid.productscanner.presentation.select_excel.viewmodel.SelectExcelViewModel
import com.kid.productscanner.presentation.select_excel.viewmodel.SelectExcelViewModelFactory
import com.kid.productscanner.repository.ScannerRepository
import com.kid.productscanner.repository.cache.room.entity.Excel
import com.kid.productscanner.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class SelectExcelFragment : Fragment() {

    private val TAG: String = "chi.trinh"
    private lateinit var binding: FragmentSelectExcelBinding

    private val viewModel: SelectExcelViewModel by viewModels {
        val repository =
            ScannerRepository((requireActivity().application as ScannerApplication).scannerDatabase)
        SelectExcelViewModelFactory(repository)
    }

    private var selectExcelResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->

                    lifecycleScope.launch(Dispatchers.Main) {
                        binding.relativeLoading.isVisible = true
                        withContext(Dispatchers.IO) {

                            //test de khong bi trung package booking no, sau nen xoa di
                            viewModel.deleteAllExcel()
                            deleteAppFiles()
                            //test de khong bi trung package booking no, sau nen xoa di

                            val shouldReload = viewModel.lastExcelLiveData.value == null

                            val excelName = queryFileName(requireContext().contentResolver, uri)

                            copyFileToAppData(uri, excelName)

                            val excelId =
                                viewModel.insertExcel(Excel(excelName, System.currentTimeMillis()))

                            openStreamToInsertPacks(uri, excelId.toInt())

                            if (shouldReload) {
                                viewModel.reloadExcel()
                            }
                        }
                        showToast("Nhập file excel thành công!")
                        binding.relativeLoading.isVisible = false
                    }
                } ?: showToast("Import excel file error - empty Uri")
            } else {
                if (result.resultCode != Activity.RESULT_CANCELED) {
                    // Handle the error
                    showToast("Import excel file error!")
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSelectExcelBinding.inflate(inflater, container, false)
        binding.viewModel = this@SelectExcelFragment.viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Trong Fragment hoặc Activity
        viewModel.percentLiveData.observe(viewLifecycleOwner) { percent ->
            Log.d("chi.trinh", "percent: ${percent}%")
        }

        binding.buttonUseThis.setOnClickListener {
            findNavController().navigate(R.id.action_SelectExcel_to_SelectTracking)
        }

        val selectExcelCodeBlock = {
            selectExcelResultLauncher.launch(
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                }
            )
        }
        binding.buttonImportExcel.setOnClickListener {
            selectExcelCodeBlock()
        }
        binding.buttonImportExcelNoFile.setOnClickListener {
            selectExcelCodeBlock()
        }
        binding.buttonDownloadExcel.setOnClickListener {
            binding.relativeLoading.isVisible = true
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val file = File(
                        requireContext().filesDir,
                        viewModel.lastExcelName ?: "Not existed file.xxx"
                    )
                    if (file.exists()) {
                        file.inputStream().use { inputStream ->
                            WorkbookFactory.create(inputStream)?.use { workbook ->

                                Log.d(TAG, "write excel file stream opened")
                                viewModel.updateChangedPacksTo(workbook)

                                file.outputStream().use { outputStream ->
                                    workbook.write(outputStream)
                                }

                                withContext(Dispatchers.Main) {
                                    binding.relativeLoading.isVisible = false
                                    shareFile(file)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        binding.relativeLoading.isVisible = false
                        e.printStackTrace()
                        showToast(e.localizedMessage ?: "Unknown error")
                    }
                }
            }
        }
    }

    private fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "com.kid.productscanner.fileprovider", // Thay thế bằng authority trong file provider của bạn
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        requireContext().startActivity(Intent.createChooser(intent, "Chia sẻ file"))
    }

    private fun queryFileName(resolver: ContentResolver, uri: Uri): String {
        val returnCursor: Cursor = checkNotNull(
            resolver.query(uri, null, null, null, null)
        )
        val nameIndex: Int = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor.moveToFirst()
        val name: String = returnCursor.getString(nameIndex)
        returnCursor.close()
        return name
    }

    private fun deleteAppFiles() {
        val filesDir = requireContext().filesDir
        if (filesDir.exists()) {
            filesDir.listFiles()?.forEach { file ->
                if (file.extension.contains("xls") || file.extension.contains("xlsx")) {
                    file.delete()
                }
            }
        }
    }

    private fun copyFileToAppData(sourceUri: Uri, destinationFileName: String) {
        try {
            // file sẽ được lưu tại đường dẫn /data/data/<application_id>/files/<destinationFileName>.
            requireContext().contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                requireContext().openFileOutput(destinationFileName, Context.MODE_PRIVATE)
                    .use { outputStream ->
                        val buffer = ByteArray(1024)
                        var length: Int
                        while (inputStream.read(buffer).also { length = it } > 0) {
                            outputStream.write(buffer, 0, length)
                        }
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Copy file vào ứng dụng lỗi: ${e.localizedMessage}")
        }
    }

    private fun openStreamToInsertPacks(uri: Uri, excelId: Int) {
        requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
            WorkbookFactory.create(inputStream)?.use { workbook ->
                viewModel.insertPackages(workbook, excelId)
            }
        }
    }

}