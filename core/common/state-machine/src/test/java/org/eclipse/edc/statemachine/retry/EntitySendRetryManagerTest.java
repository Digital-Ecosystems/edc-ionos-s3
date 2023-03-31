/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.statemachine.retry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.retry.WaitStrategy;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EntitySendRetryManagerTest {

    private static final Random RANDOM = new Random();
    private final Monitor monitor = mock(Monitor.class);
    private final WaitStrategy delayStrategy = mock(WaitStrategy.class);
    private final int sendRetryLimit = 5 + RANDOM.nextInt(11);

    private final Clock clock = mock(Clock.class);
    private final Supplier<WaitStrategy> waitStrategy = () -> delayStrategy;
    private final EntitySendRetryManager sendRetryManager = new EntitySendRetryManager(monitor, waitStrategy, clock, sendRetryLimit);

    @ParameterizedTest
    @ArgumentsSource(DelayArgs.class)
    void shouldDelay(long stateTimestamp, long currentTime, long retryDelay, boolean shouldDelay) {
        var stateCount = sendRetryLimit - 2;
        var entity = TestEntity.Builder.newInstance()
                .id("any")
                .stateCount(stateCount)
                .stateTimestamp(stateTimestamp)
                .clock(clock)
                .build();

        when(delayStrategy.retryInMillis())
                .thenAnswer(i -> {
                    verify(delayStrategy).failures(stateCount - 1);
                    return retryDelay;
                })
                .thenThrow(new RuntimeException("should call only once"));

        when(clock.millis()).thenReturn(currentTime);

        assertThat(sendRetryManager.shouldDelay(entity)).isEqualTo(shouldDelay);
    }

    @ParameterizedTest
    @ValueSource(ints = { -2, -1, 0, 1, 2 })
    void retriesExhausted(int retriesLeft) {
        var stateCount = sendRetryLimit - retriesLeft;
        var stateTimestamp = RANDOM.nextInt();
        var process = TestEntity.Builder.newInstance()
                .id("any")
                .stateCount(stateCount)
                .stateTimestamp(stateTimestamp)
                .clock(clock)
                .build();

        var expected = retriesLeft < 0;
        assertThat(sendRetryManager.retriesExhausted(process)).isEqualTo(expected);
    }

    private static class DelayArgs implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    arguments(0, 0, 0, false),
                    arguments(0, 10, 9, false),
                    arguments(0, 9, 10, true),
                    arguments(2, 10, 9, true),
                    arguments(2, 12, 9, false)
            );
        }
    }

    private static class TestEntity extends StatefulEntity<TestEntity> {
        @Override
        public TestEntity copy() {
            return this;
        }

        @JsonPOJOBuilder(withPrefix = "")
        public static class Builder extends StatefulEntity.Builder<TestEntity, Builder> {

            private Builder(TestEntity entity) {
                super(entity);
            }

            @JsonCreator
            public static Builder newInstance() {
                return new Builder(new TestEntity());
            }

            @Override
            public Builder self() {
                return this;
            }

            @Override
            protected TestEntity build() {
                return super.build();
            }
        }
    }
}