/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import akka.testkit.TestProbe;
import javax.annotation.Nonnull;
import org.junit.Assert;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.client.AbstractClientConnection;
import org.opendaylight.controller.cluster.access.client.AccessClientUtil;
import org.opendaylight.controller.cluster.access.commands.TransactionFailure;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.AbstractRequestFailureProxy;
import org.opendaylight.controller.cluster.access.concepts.FailureEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;
import org.opendaylight.controller.cluster.access.concepts.SuccessEnvelope;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Helper class. Allows checking messages received by backend and respond to them.
 */
class TranasactionTester {

    private final RemoteProxyTransaction transaction;
    private final AbstractClientConnection<ShardBackendInfo> connection;
    private final TestProbe backendProbe;
    private RequestEnvelope envelope;

    TranasactionTester(final RemoteProxyTransaction transaction,
                       final AbstractClientConnection<ShardBackendInfo> connection,
                       final TestProbe backendProbe) {
        this.transaction = transaction;
        this.connection = connection;
        this.backendProbe = backendProbe;
    }

    RemoteProxyTransaction getTransaction() {
        return transaction;
    }

    TransactionRequest getLastReceivedMessage() {
        return (TransactionRequest) envelope.getMessage();
    }

    <T extends TransactionRequest> T expectTransactionRequest(final Class<T> expected) {
        envelope = backendProbe.expectMsgClass(RequestEnvelope.class);
        Assert.assertTrue(expected.isAssignableFrom(envelope.getMessage().getClass()));
        return (T) envelope.getMessage();
    }

    void replySuccess(final RequestSuccess<?, ?> success) {
        final long sessionId = envelope.getSessionId();
        final long txSequence = envelope.getTxSequence();
        final long executionTime = 0L;
        final SuccessEnvelope responseEnvelope = new SuccessEnvelope(success, sessionId, txSequence, executionTime);
        AccessClientUtil.completeRequest(connection, responseEnvelope);
    }

    void replyFailure(final RequestException cause) {
        final long sessionId = envelope.getSessionId();
        final long txSequence = envelope.getTxSequence();
        final long executionTime = 0L;
        final RequestFailure<?, ?> fail =
                new MockFailure(transaction.getIdentifier(), envelope.getMessage().getSequence(), cause);
        final FailureEnvelope responseEnvelope = new FailureEnvelope(fail, sessionId, txSequence, executionTime);
        AccessClientUtil.completeRequest(connection, responseEnvelope);
    }

    private static class MockFailure extends RequestFailure<TransactionIdentifier, TransactionFailure> {
        private MockFailure(@Nonnull final TransactionIdentifier target, final long sequence,
                            @Nonnull final RequestException cause) {
            super(target, sequence, cause);
        }

        @Nonnull
        @Override
        protected TransactionFailure cloneAsVersion(@Nonnull final ABIVersion targetVersion) {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        protected AbstractRequestFailureProxy<TransactionIdentifier, TransactionFailure> externalizableProxy(
                @Nonnull final ABIVersion version) {
            throw new UnsupportedOperationException("Not implemented");
        }
    }
}