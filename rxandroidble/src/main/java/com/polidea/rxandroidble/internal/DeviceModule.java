package com.polidea.rxandroidble.internal;

import android.bluetooth.BluetoothDevice;

import com.polidea.rxandroidble.RxBleAdapterStateObservable;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.internal.connection.RxBleConnectionConnectorImpl;
import com.polidea.rxandroidble.internal.connection.RxBleConnectionConnectorOperationsProvider;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble.internal.util.BleConnectionCompat;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;

import javax.inject.Provider;

import dagger.Module;
import dagger.Provides;

@Module
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
    RxBleConnection.Connector provideRxBleConnectionConnector(
            BluetoothDevice bluetoothDevice,
            RxBleGattCallback.Provider gattCallbackProvider,
            RxBleConnectionConnectorOperationsProvider operationsProvider,
            RxBleRadio rxBleRadio,
            BleConnectionCompat bleConnectionCompat,
            RxBleAdapterWrapper rxBleAdapterWrapper,
            RxBleAdapterStateObservable adapterStateObservable) {

        return new RxBleConnectionConnectorImpl(bluetoothDevice,
                gattCallbackProvider,
                operationsProvider,
                rxBleRadio,
                bleConnectionCompat,
                rxBleAdapterWrapper,
                adapterStateObservable);
    }

    @Provides
    RxBleDevice provideRxBleDevice(RxBleDeviceImpl rxBleDevice) {
        return rxBleDevice;
    }

    @Provides
    RxBleGattCallback.Provider provideRxBleGattCallback(final Provider<RxBleGattCallback> gattCallback) {
        return new RxBleGattCallback.Provider() {
            @Override
            public RxBleGattCallback provide() {
                return gattCallback.get();
            }
        };
    }
}
