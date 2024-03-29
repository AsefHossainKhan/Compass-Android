package com.asef.compass

import android.graphics.Bitmap
import android.graphics.Matrix

fun multiplyMatrices(matrix1: Array<Array<Float>>, matrix2: Array<Array<Float>>): Array<Array<Float>> {
    val row1 = matrix1.size
    val col1 = matrix1[0].size
    val col2 = matrix2[0].size
    val product = Array(row1) { Array(col2) { 0f } }

    for (i in 0 until row1) {
        for (j in 0 until col2) {
            for (k in 0 until col1) {
                product[i][j] += matrix1[i][k] * matrix2[k][j]
            }
        }
    }

    return product
}

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}