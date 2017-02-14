package com.polidea.rxandroidble;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.location.LocationManager;

import com.polidea.rxandroidble.injection.ClientScope;
import com.polidea.rxandroidble.injection.DeviceComponent;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.radio.RxBleRadioImpl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Named;

import dagger.Component;
import dagger.Module;
import dagger.Provides;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

@ClientScope
@Component(modules = {ClientComponent.ClientModule.class})
interface ClientComponent {

    @Module(subcomponents = DeviceComponent.class)
    class ClientModule {

        private final Context context;

        public ClientModule(Context context) {
            this.context = context;
        }

        @Provides
        Observable<RxBleAdapterStateObservable.BleAdapterState> provideBleAdapterState(RxBleAdapterStateObservable stateObservable) {
            return stateObservable;
        }

        @Provides
        @Named("callback")
        @ClientScope
        ExecutorService provideGattCallbackExecutorService() {
            return Executors.newSingleThreadExecutor();
        }

        @Provides
        @Named("callback")
        @ClientScope
        Scheduler provideGattCallbackScheduler(@Named("callback") ExecutorService executorService) {
            return Schedulers.from(executorService);
        }

        @Provides
        @ClientScope
        RxBleClient provideRxBleClient(RxBleClientImpl rxBleClient) {
            return rxBleClient;
        }

        @Provides
        @ClientScope
        RxBleRadio provideRxBleRadio(RxBleRadioImpl rxBleRadio) {
            return rxBleRadio;
        }

        @Provides
        Context provideApplicationContext() {
            return context;
        }

        @Provides
        BluetoothAdapter provideBlutetoothAdapter() {
            return BluetoothAdapter.getDefaultAdapter();
        }

        @Provides
        LocationManager provideLocationManager() {
            return (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }
    }

    RxBleClient rxBleClient();
}
