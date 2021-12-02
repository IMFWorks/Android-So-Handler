package com.hzy.lib7z;

/**
 * Created by huzongyao on 17-11-24.
 */

public interface ErrorCode {

    int SZ_OK = 0;

    int SZ_ERROR_DATA = 1;
    int SZ_ERROR_MEM = 2;
    int SZ_ERROR_CRC = 3;
    int SZ_ERROR_UNSUPPORTED = 4;
    int SZ_ERROR_PARAM = 5;
    int SZ_ERROR_INPUT_EOF = 6;
    int SZ_ERROR_OUTPUT_EOF = 7;
    int SZ_ERROR_READ = 8;
    int SZ_ERROR_WRITE = 9;
    int SZ_ERROR_PROGRESS = 10;
    int SZ_ERROR_FAIL = 11;
    int SZ_ERROR_THREAD = 12;

    int SZ_ERROR_ARCHIVE = 16;
    int SZ_ERROR_NO_ARCHIVE = 17;

    int ERROR_CODE_PATH_ERROR = 999;
}
