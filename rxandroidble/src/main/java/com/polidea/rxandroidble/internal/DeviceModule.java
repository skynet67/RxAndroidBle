package com.polidea.rxandroidble.internal;

import android.bluetooth.BluetoothDevice;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.internal.connection.ConnectionComponent;
import com.polidea.rxandroidble.internal.connection.RxBleConnectionConnectorImpl;
import com.polidea.rxandroidble.internal.operations.TimeoutConfiguration;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;

import java.util.concurrent.TimeUnit;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import rx.Scheduler;

@Module(subcomponents = ConnectionComponent.class)
public class DeviceModule {

    final String macAddress;

    DeviceModule(String macAddress) {
        this.macAddress = macAddress;
    }

    @Provides
    BluetoothDevice provideBluetoothDevice(RxBleAdapterWrapper adapterWrapper) {
        return adapterWrapper.getRemoteDevice(macAddress);
    }

    @Provides
    RxBleConnection.Connector provideRxBleConnectionConnector(RxBleConnectionConnectorImpl rxBleConnectionConnector) {
        return rxBleConnectionConnector;
    }

    @Provides
    RxBleDevice provideRxBleDevice(RxBleDeviceImpl rxBleDevice) {
        return rxBleDevice;
    }

    @Provides
    @Named("operation")
    TimeoutConfiguration providesOperationTimeoutConfiguration(@Named("timeout") Scheduler timeoutScheduler) {
        return new TimeoutConfiguration(30, TimeUnit.SECONDS, timeoutScheduler);
    }

    @Provides
    TimeoutConfiguration providesTimeoutConfiguration(@Named("timeout") Scheduler timeoutScheduler) {
        return new TimeoutConfiguration(10, TimeUnit.SECONDS, timeoutScheduler);
    }
}
