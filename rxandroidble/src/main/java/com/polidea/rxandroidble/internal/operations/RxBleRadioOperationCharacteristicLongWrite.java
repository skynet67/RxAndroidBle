package com.polidea.rxandroidble.internal.operations;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.DeadObjectException;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.RxBleConnection.WriteOperationAckStrategy;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.exceptions.BleException;
import com.polidea.rxandroidble.exceptions.BleGattCallbackTimeoutException;
import com.polidea.rxandroidble.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble.internal.util.ByteAssociation;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

public class RxBleRadioOperationCharacteristicLongWrite extends RxBleRadioOperation<byte[]> {

    private final BluetoothGatt bluetoothGatt;
    private final RxBleGattCallback rxBleGattCallback;
    private final Scheduler mainThreadScheduler;
    private final Scheduler callbackScheduler;
    private final TimeoutConfiguration timeoutConfiguration;

    private BluetoothGattCharacteristic bluetoothGattCharacteristic;
    private Callable<Integer> batchSizeProvider;
    private WriteOperationAckStrategy writeOperationAckStrategy;
    private byte[] bytesToWrite;

    @Inject
    RxBleRadioOperationCharacteristicLongWrite(
            BluetoothGatt bluetoothGatt,
            RxBleGattCallback rxBleGattCallback,
            @Named("main-thread") Scheduler mainThreadScheduler,
            @Named("computation") Scheduler callbackScheduler,
            @Named("operation") TimeoutConfiguration timeoutConfiguration) {
        this.bluetoothGatt = bluetoothGatt;
        this.rxBleGattCallback = rxBleGattCallback;
        this.mainThreadScheduler = mainThreadScheduler;
        this.callbackScheduler = callbackScheduler;
        this.timeoutConfiguration = timeoutConfiguration;
    }

    public RxBleRadioOperationCharacteristicLongWrite setCharacteristic(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        this.bluetoothGattCharacteristic = bluetoothGattCharacteristic;
        return this;
    }

    public RxBleRadioOperationCharacteristicLongWrite setData(byte[] bytes) {
        this.bytesToWrite = bytes;
        return this;
    }

    public RxBleRadioOperationCharacteristicLongWrite setMaxBatchSize(Callable<Integer> maxBatchSizeCallable) {
        this.batchSizeProvider = maxBatchSizeCallable;
        return this;
    }

    public RxBleRadioOperationCharacteristicLongWrite setWriteAckStrategy(WriteOperationAckStrategy writeOperationAckStrategy) {
        this.writeOperationAckStrategy = writeOperationAckStrategy;
        return this;
    }

    @Override
    protected void protectedRun() throws Throwable {
        int batchSize = getBatchSize();

        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSizeProvider value must be greater than zero (now: " + batchSize + ")");
        }
        final Observable<ByteAssociation<UUID>> timeoutObservable = Observable.error(
                new BleGattCallbackTimeoutException(bluetoothGatt, BleGattOperationType.CHARACTERISTIC_LONG_WRITE)
        );
        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytesToWrite);
        rxBleGattCallback.getOnCharacteristicWrite()
                .doOnSubscribe(writeNextBatch(batchSize, byteBuffer))
                .subscribeOn(mainThreadScheduler)
                .observeOn(callbackScheduler)
                .takeFirst(writeResponseForMatchingCharacteristic())
                .timeout(
                        timeoutConfiguration.timeout,
                        timeoutConfiguration.timeoutTimeUnit,
                        timeoutObservable,
                        timeoutConfiguration.timeoutScheduler
                )
                .repeatWhen(bufferIsNotEmptyAndOperationHasBeenAcknowledged(byteBuffer))
                .toCompletable()
                .subscribe(
                        new Action0() {
                            @Override
                            public void call() {
                                onNext(bytesToWrite);
                                onCompleted();
                                releaseRadio();
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                onError(throwable);
                            }
                        }
                );
    }

    @Override
    protected BleException provideException(DeadObjectException deadObjectException) {
        return new BleDisconnectedException(deadObjectException, bluetoothGatt.getDevice().getAddress());
    }

    private int getBatchSize() {
        try {
            return batchSizeProvider.call();
        } catch (Exception e) {
            RxBleLog.w(e, "Failed to get batch size.");
            throw new RuntimeException("Failed to get batch size from the batchSizeProvider.", e);
        }
    }

    private Action0 writeNextBatch(final int batchSize, final ByteBuffer byteBuffer) {
        return new Action0() {
            @Override
            public void call() {
                final byte[] bytesBatch = getNextBatch(byteBuffer, batchSize);
                writeData(bytesBatch);
            }
        };
    }

    private byte[] getNextBatch(ByteBuffer byteBuffer, int batchSize) {
        final int remainingBytes = byteBuffer.remaining();
        final int nextBatchSize = Math.min(remainingBytes, batchSize);
        final byte[] bytesBatch = new byte[nextBatchSize];
        byteBuffer.get(bytesBatch);
        return bytesBatch;
    }

    private void writeData(byte[] bytesBatch) {
        bluetoothGattCharacteristic.setValue(bytesBatch);
        final boolean success = bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
        if (!success) {
            throw new BleGattCannotStartException(bluetoothGatt, BleGattOperationType.CHARACTERISTIC_LONG_WRITE);
        }
    }

    private Func1<ByteAssociation<UUID>, Boolean> writeResponseForMatchingCharacteristic() {
        return new Func1<ByteAssociation<UUID>, Boolean>() {
            @Override
            public Boolean call(ByteAssociation<UUID> uuidByteAssociation) {
                return uuidByteAssociation.first.equals(bluetoothGattCharacteristic.getUuid());
            }
        };
    }

    private Func1<Observable<? extends Void>, Observable<?>> bufferIsNotEmptyAndOperationHasBeenAcknowledged(final ByteBuffer byteBuffer) {
        return new Func1<Observable<? extends Void>, Observable<?>>() {
            @Override
            public Observable<?> call(Observable<? extends Void> emittingOnBatchWriteFinished) {
                return writeOperationAckStrategy.call(emittingOnBatchWriteFinished.map(bufferIsNotEmpty(byteBuffer)))
                        .takeWhile(bufferIsNotEmpty(byteBuffer));
            }

            @NonNull
            private Func1<Object, Boolean> bufferIsNotEmpty(final ByteBuffer byteBuffer) {
                return new Func1<Object, Boolean>() {
                    @Override
                    public Boolean call(Object emittedFromActStrategy) {
                        return byteBuffer.hasRemaining();
                    }
                };
            }
        };
    }
}
