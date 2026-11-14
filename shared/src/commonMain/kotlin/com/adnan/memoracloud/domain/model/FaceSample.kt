package com.adnan.memoracloud.domain.model

/**
 * A face found in one photo. [embedding] is the 192-number signature the on-device model produced;
 * two signatures are compared with cosine similarity to decide "same person".
 */
class FaceSample(
    val embedding: FloatArray,
    val cropPath: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val score: Float
) {
    /** Bigger faces make better group covers, so the largest crop wins. */
    val area: Int get() = (right - left) * (bottom - top)
}
