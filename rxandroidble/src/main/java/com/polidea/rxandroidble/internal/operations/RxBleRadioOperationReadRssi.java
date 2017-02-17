package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;

import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleSingleGattRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Observable;

public class RxBleRadioOperationReadRssi extends RxBleSingleGattRadioOperation<Integer> {

    @Inject
    RxBleRadioOperationReadRssi(RxBleGattCallback bleGattCallback, BluetoothGatt bluetoothGatt,
                                       @Named("operation") TimeoutConfiguration timeoutConfiguration) {
        super(bluetoothGatt, bleGattCallback, BleGattOperationType.READ_RSSI, timeoutConfiguration);
    }

    @Override
    protected Observable<Integer> getCallback(RxBleGattCallback rxBleGattCallback) {
        return rxBleGattCallback.getOnRssiRead();
    }

    @Override
    protected boolean startOperation(BluetoothGatt bluetoothGatt) {
        return bluetoothGatt.readRemoteRssi();
    }
}
