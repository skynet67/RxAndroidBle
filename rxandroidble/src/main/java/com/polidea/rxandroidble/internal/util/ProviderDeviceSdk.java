package com.polidea.rxandroidble.internal.util;


import android.os.Build;

import javax.inject.Inject;

public class ProviderDeviceSdk {

    @Inject
    public ProviderDeviceSdk() {
    }

    public int provide() {
        return Build.VERSION.SDK_INT;
    }
}
