package com.kid.productscanner.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.kid.productscanner.BuildConfig
import com.kid.productscanner.common.ColumnName
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

fun Fragment.showToast(message: String, duration: Int = Toast.LENGTH_LONG) {
    Toast.makeText(requireContext(), message, duration).show()
}

fun Fragment.showToast(@StringRes resId: Int, duration: Int = Toast.LENGTH_LONG) {
    Toast.makeText(requireContext(), requireContext().getText(resId), duration).show()
}

fun Row.findIndexOfColumn(columnName: ColumnName): Int {
    cellIterator().forEach { cell ->
        if (cell.stringCellValue == columnName.name) {
            return cell.columnIndex
        }
    }
    return when (columnName) {
        ColumnName.BookingNo -> 0
        ColumnName.ProjectName -> 0
        ColumnName.PartNumber -> 0
        ColumnName.TrackingNumber -> 0
        ColumnName.QuantityReceived -> 0
        ColumnName.DateReceived -> 0
    }
}

fun Sheet.findLastNonEmptyRowIndex(): Int {
    for (i in this.lastRowNum downTo 0) {
        if (!this.getRow(i).getCell(0)?.stringCellValue.isNullOrEmpty()) {
            return i
        }
    }
    return 0
}

fun Context.getExcelFilesDir(): File =
    File(filesDir, "Excels").apply { if (!this.exists()) mkdirs() }

@SuppressLint("SimpleDateFormat")
fun Context.createFileToSaveImage(): Pair<String, Uri>? {
    return try {
        // Tạo tên file ảnh
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val file =
            File.createTempFile("Scan_${timeStamp}_", ".jpg", File(filesDir, "ScanImages").apply {
                if (!exists()) {
                    mkdirs()
                }
            })

        return Pair(
            file.absolutePath, FileProvider.getUriForFile(
                applicationContext, "${BuildConfig.APPLICATION_ID}.fileprovider", file
            )
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
