package com.kid.productscanner.utils

import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.kid.productscanner.common.ColumnName
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet

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
