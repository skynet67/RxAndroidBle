package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import com.polidea.rxandroidble.RxBleAdapterStateObservable.BleAdapterState;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationConnect;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationDisconnect;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;

import javax.inject.Inject;
import javax.inject.Provider;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Actions;
import rx.functions.Func0;
import rx.functions.Func1;

import static com.polidea.rxandroidble.internal.util.ObservableUtil.justOnNext;

public class RxBleConnectionConnectorImpl implements RxBleConnection.Connector {

    private final BluetoothDevice bluetoothDevice;
    private final RxBleGattCallback gattCallback;
    private final RxBleRadio rxBleRadio;
    private final RxBleAdapterWrapper rxBleAdapterWrapper;
    private final Observable<BleAdapterState> adapterStateObservable;
    private final Provider<ConnectionComponent.Builder> connectionComponentBuilder;
    private final Provider<RxBleRadioOperationConnect> connectOperationProvider;
    private final Provider<RxBleRadioOperationDisconnect> disconnectOperationProvider;

    @Inject
    public RxBleConnectionConnectorImpl(
            BluetoothDevice bluetoothDevice,
            RxBleGattCallback gattCallback,
            RxBleRadio rxBleRadio,
            RxBleAdapterWrapper rxBleAdapterWrapper,
            Observable<BleAdapterState> adapterStateObservable,
            Provider<ConnectionComponent.Builder> connectionComponentBuilder,
            Provider<RxBleRadioOperationConnect> connectOperationProvider,
            Provider<RxBleRadioOperationDisconnect> disconnectOperationProvider) {
        this.bluetoothDevice = bluetoothDevice;
        this.gattCallback = gattCallback;
        this.rxBleRadio = rxBleRadio;
        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
        this.adapterStateObservable = adapterStateObservable;
        this.connectionComponentBuilder = connectionComponentBuilder;
        this.connectOperationProvider = connectOperationProvider;
        this.disconnectOperationProvider = disconnectOperationProvider;
    }

    @Override
    public Observable<RxBleConnection> prepareConnection(final boolean autoConnect) {
        return Observable.defer(new Func0<Observable<RxBleConnection>>() {
            @Override
            public Observable<RxBleConnection> call() {
                if (!rxBleAdapterWrapper.isBluetoothEnabled()) {
                    return Observable.error(new BleDisconnectedException(bluetoothDevice.getAddress()));
                }

                RxBleRadioOperationConnect operationConnect = connectOperationProvider.get().setAutoConnect(autoConnect);
                final RxBleRadioOperationDisconnect operationDisconnect = disconnectOperationProvider.get();

                return Observable.merge(
                        rxBleRadio.queue(operationConnect),
                        adapterNotUsableObservable()
                                .flatMap(new Func1<BleAdapterState, Observable<BluetoothGatt>>() {
                                    @Override
                                    public Observable<BluetoothGatt> call(BleAdapterState bleAdapterState) {
                                        return Observable.error(new BleDisconnectedException(bluetoothDevice.getAddress()));
                                    }
                                })
                )
                        .first()
                        .flatMap(new Func1<BluetoothGatt, Observable<RxBleConnection>>() {
                            @Override
                            public Observable<RxBleConnection> call(BluetoothGatt bluetoothGatt) {
                                return emitConnectionWithoutCompleting(bluetoothGatt);
                            }
                        })
                        .mergeWith(gattCallback.<RxBleConnection>observeDisconnect())
                        .doOnUnsubscribe(new Action0() {
                            @Override
                            public void call() {
                                enqueueDisconnectOperation(operationDisconnect);
                            }
                        });
            }
        });
    }

    private Observable<BleAdapterState> adapterNotUsableObservable() {
        return adapterStateObservable
                .filter(new Func1<BleAdapterState, Boolean>() {
                    @Override
                    public Boolean call(BleAdapterState bleAdapterState) {
                        return !bleAdapterState.isUsable();
                    }
                });
    }

    private Observable<RxBleConnection> emitConnectionWithoutCompleting(BluetoothGatt bluetoothGatt) {
        return justOnNext(connectionComponentBuilder
                .get()
                .connectionModule(new ConnectionModule(bluetoothGatt))
                .build()
                .provideConnection()
        );
    }

    private Subscription enqueueDisconnectOperation(RxBleRadioOperationDisconnect operationDisconnect) {
        return rxBleRadio
                .queue(operationDisconnect)
                .subscribe(
                        Actions.empty(),
                        Actions.<Throwable>toAction1(Actions.empty())
                );
    }

}
