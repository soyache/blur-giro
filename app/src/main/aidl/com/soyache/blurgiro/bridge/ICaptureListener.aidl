package com.soyache.blurgiro.bridge;
oneway interface ICaptureListener {
    void onFrame(long sequence, long timestampMs);
    void onError(String message);
}
