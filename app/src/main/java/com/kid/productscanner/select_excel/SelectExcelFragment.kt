package com.kid.productscanner.select_excel

import android.app.Activity
import android.content.ContentResolver
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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.kid.productscanner.application.ScannerApplication
import com.kid.productscanner.common.ColumnName
import com.kid.productscanner.databinding.FragmentSelectExcelBinding
import com.kid.productscanner.repository.ScannerRepository
import com.kid.productscanner.repository.cache.room.entity.Excel
import com.kid.productscanner.repository.cache.room.entity.Pack
import com.kid.productscanner.select_excel.viewmodel.SelectExcelViewModel
import com.kid.productscanner.select_excel.viewmodel.SelectExcelViewModelFactory
import com.kid.productscanner.utils.findIndexOfColumn
import com.kid.productscanner.utils.findLastNonEmptyRowIndex
import com.kid.productscanner.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.WorkbookFactory


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class SelectExcelFragment : Fragment() {

    private val TAG: String = "chi.trinh"
    private lateinit var binding: FragmentSelectExcelBinding

    private val viewModel: SelectExcelViewModel by viewModels {
        val repository =
            ScannerRepository((requireActivity().application as ScannerApplication).scannerDatabase)
        Log.d(TAG, "vm: repository: $repository")
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
                            //test de khong bi trung package booking no, sau nen xoa di

                            val excelName = queryFileName(requireContext().contentResolver, uri)
                            val excelId =
                                viewModel.insertExcel(Excel(excelName, System.currentTimeMillis()))

                            insertPackages(uri, excelId.toInt())
                        }
                        binding.relativeLoading.isVisible = false
                    }
                } ?: showToast("Error in selecting Excel file - empty Uri")
            } else {
                // Handle the error
                showToast("Error in selecting Excel file")
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

//        viewModel.initialize()

        // Trong Fragment hoặc Activity
        viewModel.lastExcelDateLiveData.observe(viewLifecycleOwner) { date ->
            Log.d("Debug", "Excel date: $date")
        }
        viewModel.lastExcelNameLiveData.observe(viewLifecycleOwner) { excelName ->
            Log.d("Debug", "Excel name: $excelName")
        }
        viewModel.lastExcelLiveData.observe(viewLifecycleOwner) { excel ->
            Log.d("Debug", "Excel: $excel")
        }

        binding.buttonImportExcel.setOnClickListener {
//            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)

            selectExcelResultLauncher.launch(
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                }
            )
        }
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


    private fun insertPackages(uri: Uri, excelId: Int) {
        requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
            WorkbookFactory.create(inputStream)?.use { workbook ->

                //Lấy sheet đầu tiên (file của a Dũng chỉ có 1 sheet)
                val sheet = workbook.getSheetAt(0)
                val titleRow = sheet.getRow(0)

                val bookingNoIndex = titleRow.findIndexOfColumn(ColumnName.BookingNo)
                val projectNameIndex = titleRow.findIndexOfColumn(ColumnName.ProjectName)
                val partNumberIndex = titleRow.findIndexOfColumn(ColumnName.PartNumber)
                val trackingNumberIndex = titleRow.findIndexOfColumn(ColumnName.TrackingNumber)

                val lastRowIndex = sheet.findLastNonEmptyRowIndex()

                val packs = mutableListOf<Pack>()
                for (i in 0..lastRowIndex) {
                    val row = sheet.getRow(i)
                    val bookingNo = row.getCell(bookingNoIndex).stringCellValue.toLongOrNull()
                    val projectName = row.getCell(projectNameIndex).stringCellValue
                    val partNumber = row.getCell(partNumberIndex).stringCellValue
                    val trackingNumber = row.getCell(trackingNumberIndex).stringCellValue

                    if (bookingNo != null) {
                        packs.add(Pack(bookingNo, projectName, partNumber, trackingNumber, "", 0L, excelId))
                    }

                    Log.d(TAG, "insertPackages: percent: ${i.toDouble() / lastRowIndex * 100}%")
                }

                viewModel.insertPackages(packs)

//// Ghi giá trị vào ô B1
//                row.createCell(1).setCellValue("New Value")
//// Lưu file
//                val outputStream = FileOutputStream("output.xlsx")
//                workbook.write(outputStream)
            }
        }
    }

}