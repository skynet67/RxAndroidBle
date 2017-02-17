package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;

import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleSingleGattRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble.internal.util.ByteAssociation;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Observable;
import rx.functions.Func1;

public class RxBleRadioOperationDescriptorRead extends RxBleSingleGattRadioOperation<ByteAssociation<BluetoothGattDescriptor>> {

    private BluetoothGattDescriptor bluetoothGattDescriptor;

    @Inject
    RxBleRadioOperationDescriptorRead(RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt,
                                      @Named("operation") TimeoutConfiguration timeoutConfiguration) {
        super(bluetoothGatt, rxBleGattCallback, BleGattOperationType.DESCRIPTOR_READ, timeoutConfiguration);
    }

    public RxBleRadioOperationDescriptorRead setDescriptor(BluetoothGattDescriptor descriptor) {
        this.bluetoothGattDescriptor = descriptor;
        return this;
    }

    @Override
    protected Observable<ByteAssociation<BluetoothGattDescriptor>> getCallback(RxBleGattCallback rxBleGattCallback) {
        return rxBleGattCallback
                .getOnDescriptorRead()
                .filter(new Func1<ByteAssociation<BluetoothGattDescriptor>, Boolean>() {
                    @Override
                    public Boolean call(ByteAssociation<BluetoothGattDescriptor> uuidPair) {
                        return uuidPair.first.equals(bluetoothGattDescriptor);
                    }
                });
    }

    @Override
    protected boolean startOperation(BluetoothGatt bluetoothGatt) {
        return bluetoothGatt.readDescriptor(bluetoothGattDescriptor);
    }
}
