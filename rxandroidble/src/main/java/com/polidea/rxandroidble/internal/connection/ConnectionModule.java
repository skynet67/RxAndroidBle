package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothGatt;

import com.polidea.rxandroidble.RxBleConnection;

import java.util.concurrent.Callable;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

@Module
public class ConnectionModule {

    private final BluetoothGatt bluetoothGatt;

    public ConnectionModule(BluetoothGatt bluetoothGatt) {
        this.bluetoothGatt = bluetoothGatt;
    }

    @Provides
    @Named("current-mtu")
    Callable<Integer> provideCurrentMtuProvider(final RxBleConnectionImpl rxBleConnection) {
        return new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return rxBleConnection.currentMtu;
            }
        };
    }

    @Provides
    RxBleConnection.LongWriteOperationBuilder provideLongWriteOperationBuilder(LongWriteOperationBuilderImpl operationBuilder) {
        return operationBuilder;
    }

    @Provides
    @ConnectionScope
    RxBleConnection provideRxBleConnection(RxBleConnectionImpl rxBleConnection) {
        return rxBleConnection;
    }

    @Provides
    BluetoothGatt providesGatt() {
        return bluetoothGatt;
    }
}
