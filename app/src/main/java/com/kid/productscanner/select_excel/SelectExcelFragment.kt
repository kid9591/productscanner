package com.kid.productscanner.select_excel

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.kid.productscanner.databinding.FragmentSelectExcelBinding
import com.kid.productscanner.utils.showToast
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.FileOutputStream


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class SelectExcelFragment : Fragment() {

    private lateinit var binding: FragmentSelectExcelBinding

    private var selectExcelResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    importExcelFrom(uri)
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonImportExcel.setOnClickListener {
//            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
            selectExcelResultLauncher.launch(
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/vnd.ms-excel"
                }
            )
        }
    }

    private fun importExcelFrom(uri: Uri) {
        requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
            WorkbookFactory.create(inputStream)?.use { workbook ->
                //Lấy sheet đầu tiên (file của a Dũng chỉ có 1 sheet)
                val sheet = workbook.getSheetAt(0)

                // Đọc giá trị từ ô A1
                val row: Row = sheet.getRow(0)
                val cell: Cell = row.getCell(0)
                val value = cell.stringCellValue

//// Ghi giá trị vào ô B1
//                row.createCell(1).setCellValue("New Value")
//// Lưu file
//                val outputStream = FileOutputStream("output.xlsx")
//                workbook.write(outputStream)
            }
        }
    }
}