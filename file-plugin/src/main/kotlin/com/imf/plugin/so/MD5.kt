package com.imf.plugin.so

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.DigestInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

private val HEX_DIGITS_UPPER = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
private val HEX_DIGITS_LOWER = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

/**
 * Bytes to hex string.
 *
 * e.g. bytes2HexString(new byte[] { 0, (byte) 0xa8 }, true) returns "00A8"
 *
 * @param bytes       The bytes.
 * @param isUpperCase True to use upper case, false otherwise.
 * @return hex string
 */
private fun bytes2HexString(bytes: ByteArray?, isUpperCase: Boolean): String {
    if (bytes == null) return ""
    val hexDigits = if (isUpperCase) HEX_DIGITS_UPPER else HEX_DIGITS_LOWER
    val len = bytes.size
    if (len <= 0) return ""
    val ret = CharArray(len shl 1)
    var i = 0
    var j = 0
    while (i < len) {
        val byte2Int: Int = bytes[i].toInt()
        ret[j++] = hexDigits[byte2Int shr 4 and 0x0f]
        ret[j++] = hexDigits[byte2Int and 0x0f]
        i++
    }
    return String(ret)
}

fun getFileMD5ToString(file: File?, isUpperCase: Boolean): String {
    return bytes2HexString(getFileMD5(file), isUpperCase)
}

fun getFileMD5ToString(file: File?): String {
    return getFileMD5ToString(file, true)
}

fun getFileMD5(file: File?): ByteArray? {
    if (file == null) return null
    var dis: DigestInputStream? = null
    try {
        val fis = FileInputStream(file)
        var md = MessageDigest.getInstance("MD5")
        dis = DigestInputStream(fis, md)
        val buffer = ByteArray(1024 * 256)
        while (true) {
            if (dis.read(buffer) <= 0) break
        }
        md = dis.messageDigest
        return md.digest()
    } catch (e: NoSuchAlgorithmException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        try {
            dis?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    return null
}