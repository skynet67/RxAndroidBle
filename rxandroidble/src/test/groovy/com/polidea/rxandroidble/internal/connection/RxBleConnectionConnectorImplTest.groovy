package com.polidea.rxandroidble.internal.connection

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Context
import com.polidea.rxandroidble.RxBleAdapterStateObservable
import com.polidea.rxandroidble.RxBleConnection
import com.polidea.rxandroidble.exceptions.BleDisconnectedException
import com.polidea.rxandroidble.internal.RxBleRadio
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationConnect
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationDisconnect
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper
import rx.Observable
import rx.observers.TestSubscriber
import rx.subjects.PublishSubject
import spock.lang.Specification
import spock.lang.Unroll

import javax.inject.Provider

public class RxBleConnectionConnectorImplTest extends Specification {

    RxBleRadio mockRadio = Mock RxBleRadio
    BluetoothDevice mockDevice = Mock BluetoothDevice
    RxBleGattCallback mockCallback = Mock RxBleGattCallback
    RxBleRadioOperationConnect mockConnect = Mock RxBleRadioOperationConnect
    RxBleRadioOperationDisconnect mockDisconnect = Mock RxBleRadioOperationDisconnect
    RxBleConnectionConnectorImpl objectUnderTest
    RxBleAdapterWrapper mockAdapterWrapper = Mock RxBleAdapterWrapper
    PublishSubject<RxBleAdapterStateObservable.BleAdapterState> adapterStatePublishSubject = PublishSubject.create()
    TestSubscriber<RxBleConnection> testSubscriber = TestSubscriber.create()
    BluetoothGatt mockGatt = Mock BluetoothGatt

    def setup() {
        def rxBleConnection = Mock RxBleConnection
        mockRadio.queue(mockDisconnect) >> Observable.just(mockGatt)
        mockConnect.setAutoConnect(_) >> mockConnect
        mockCallback.observeDisconnect() >> Observable.never()
        Provider<ConnectionComponent.Builder> componentBuilder = { prepareConnectionComponent(rxBleConnection) }
        Provider<RxBleRadioOperationConnect> operationConnectProvider = { mockConnect }
        Provider<RxBleRadioOperationDisconnect> operationDisconnectProvider = { mockDisconnect }

        objectUnderTest = new RxBleConnectionConnectorImpl(
                mockDevice,
                mockCallback,
                mockRadio,
                mockAdapterWrapper,
                adapterStatePublishSubject,
                componentBuilder,
                operationConnectProvider,
                operationDisconnectProvider
        )
    }

    private static ConnectionComponent.Builder prepareConnectionComponent(RxBleConnection rxBleConnection) {
        return new MockConnectionComponentBuilder(rxBleConnection)
    }

    static class MockConnectionComponentBuilder implements ConnectionComponent.Builder {
        private final RxBleConnection rxBleConnection

        MockConnectionComponentBuilder(RxBleConnection rxBleConnection) {
            this.rxBleConnection = rxBleConnection
        }

        @Override
        ConnectionComponent build() {
            return new ConnectionComponent() {
                @Override
                RxBleConnection provideConnection() {
                    return rxBleConnection
                }
            }
        }

        @Override
        ConnectionComponent.Builder connectionModule(ConnectionModule module) {
            return this
        }
    }

    @Unroll
    def "prepareConnection() should pass arguments to RxBleConnectionConnectorOperationsProvider #id"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> true

        when:
        objectUnderTest.prepareConnection(autoConnectValue).subscribe(testSubscriber)

        then:
        1 * mockConnect.setAutoConnect(autoConnectValue) >> mockConnect

        where:
        contextObject | autoConnectValue
        null          | true
        null          | false
        Mock(Context) | true
        Mock(Context) | false
    }

    def "subscribing prepareConnection() should schedule provided RxBleRadioOperationConnect on RxBleRadio"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> true

        when:
        objectUnderTest.prepareConnection(true).subscribe(testSubscriber)

        then:
        1 * mockRadio.queue(mockConnect)
    }

    def "prepareConnection() should schedule provided RxBleRadioOperationDisconnect on RxBleRadio if RxBleRadio.queue(RxBleRadioOperation) emits error"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> true
        mockRadio.queue(mockConnect) >> Observable.error(new Throwable("test"))

        when:
        objectUnderTest.prepareConnection(true).subscribe(testSubscriber)

        then:
        1 * mockRadio.queue(mockDisconnect) >> Observable.just(null)
    }

    def "prepareConnection() should schedule provided RxBleRadioOperationDisconnect on RxBleRadio only once if RxBleRadio.queue(RxBleRadioOperation) emits error and subscriber will unsubscribe"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> true
        mockRadio.queue(mockConnect) >> Observable.error(new Throwable("test"))

        when:
        objectUnderTest.prepareConnection(true).subscribe(testSubscriber)

        then:
        1 * mockRadio.queue(mockDisconnect) >> Observable.just(null)
    }

    def "prepareConnection() should schedule provided RxBleRadioOperationDisconnect on RxBleRadio when subscriber will unsubscribe"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> true
        mockRadio.queue(mockConnect) >> Observable.empty()

        when:
        objectUnderTest.prepareConnection(true).subscribe(testSubscriber)
        testSubscriber.unsubscribe()

        then:
        1 * mockRadio.queue(mockDisconnect) >> Observable.just(null)
    }

    def "prepareConnection() should emit RxBleConnection and not complete"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> true
        mockRadio.queue(mockConnect) >> Observable.just(mockGatt)

        when:
        objectUnderTest.prepareConnection(true).subscribe(testSubscriber)

        then:
        testSubscriber.assertValueCount(1)
        testSubscriber.assertNotCompleted()
    }

    def "prepareConnection() should emit error from RxBleGattCallback.disconnectedErrorObservable()"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> true
        def testError = new Throwable("test")
        mockRadio.queue(_) >> Observable.just(mockGatt)

        when:
        objectUnderTest.prepareConnection(true).subscribe(testSubscriber)

        then:
        testSubscriber.assertError(testError)
        mockCallback.observeDisconnect() >> Observable.error(testError) // Overwriting default behaviour
    }


    def "prepareConnection() should emit error from BleDisconnectedException when RxBleAdapterWrapper.isEnabled() returns false"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> false

        when:
        objectUnderTest.prepareConnection(autoConnect).subscribe(testSubscriber)

        then:
        testSubscriber.assertError(BleDisconnectedException)

        where:
        autoConnect << [
                true,
                false
        ]
    }

    @Unroll
    def "prepareConnection() should emit BleDisconnectedException when RxBleAdapterStateObservable emits not usable RxBleAdapterStateObservable.BleAdapterState"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> true
        mockRadio.queue(_) >> Observable.never()
        objectUnderTest.prepareConnection(autoConnect).subscribe(testSubscriber)

        when:
        adapterStatePublishSubject.onNext(state)

        then:
        testSubscriber.assertError BleDisconnectedException

        where:
        autoConnect | state
        true        | RxBleAdapterStateObservable.BleAdapterState.STATE_OFF
        true        | RxBleAdapterStateObservable.BleAdapterState.STATE_TURNING_OFF
        true        | RxBleAdapterStateObservable.BleAdapterState.STATE_TURNING_ON
        false       | RxBleAdapterStateObservable.BleAdapterState.STATE_OFF
        false       | RxBleAdapterStateObservable.BleAdapterState.STATE_TURNING_OFF
        false       | RxBleAdapterStateObservable.BleAdapterState.STATE_TURNING_ON
    }

    @Unroll
    def "prepareConnection() should not emit BleDisconnectedException when RxBleAdapterStateObservable emits usable RxBleAdapterStateObservable.BleAdapterState"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> true
        mockRadio.queue(_) >> Observable.never()
        objectUnderTest.prepareConnection(autoConnect).subscribe(testSubscriber)

        when:
        adapterStatePublishSubject.onNext(RxBleAdapterStateObservable.BleAdapterState.STATE_ON)

        then:
        testSubscriber.assertNoErrors()

        where:
        autoConnect << [
                true,
                false
        ]
    }
}