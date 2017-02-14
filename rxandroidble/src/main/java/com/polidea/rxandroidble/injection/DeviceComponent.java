package com.polidea.rxandroidble.injection;

import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.internal.DeviceModule;

import dagger.Subcomponent;

@Subcomponent(modules = DeviceModule.class)
public interface DeviceComponent {

    @Subcomponent.Builder
    interface Builder {
        DeviceComponent build();
        Builder deviceModule(DeviceModule module);
    }

    @DeviceScope
    RxBleDevice provideDevice();
}
