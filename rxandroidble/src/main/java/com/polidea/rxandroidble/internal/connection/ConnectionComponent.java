package com.polidea.rxandroidble.internal.connection;

import com.polidea.rxandroidble.RxBleConnection;

import dagger.Subcomponent;

@ConnectionScope
@Subcomponent(modules = ConnectionModule.class)
public interface ConnectionComponent {

    @Subcomponent.Builder
    interface Builder {

        ConnectionComponent build();

        Builder connectionModule(ConnectionModule module);
    }

    @ConnectionScope
    RxBleConnection provideConnection();
}
