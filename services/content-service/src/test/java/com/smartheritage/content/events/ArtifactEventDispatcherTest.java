package com.smartheritage.content.events;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class ArtifactEventDispatcherTest {
    @Test
    @SuppressWarnings("unchecked")
    void outageWarnsOnceWithoutStackTraceAndStopsBatchThenReportsRecovery(CapturedOutput output) throws Exception {
        var outbox = mock(ArtifactEventOutboxRepository.class);
        var delivery = mock(ArtifactEventDelivery.class);
        ObjectProvider<ArtifactEventTransport> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mock(ArtifactEventTransport.class));
        var first = new ArtifactEventOutbox("one", java.time.Instant.now(), "{}");
        var second = new ArtifactEventOutbox("two", java.time.Instant.now(), "{}");
        when(outbox.findTop50ByPublishedAtIsNullOrderByOccurredAtAsc()).thenReturn(List.of(first, second));
        var dispatcher = new ArtifactEventDispatcher(outbox, delivery, provider);
        doThrow(new IllegalStateException("broker down\nconnection refused")).when(delivery).deliver("one");
        dispatcher.retryPending(); dispatcher.retryPending();
        verify(delivery, times(2)).deliver("one");
        verify(delivery, never()).deliver("two");
        assertThat(output.getOut().lines().filter(line -> line.contains("delivery unavailable")).count()).isEqualTo(1);
        assertThat(output.getOut()).doesNotContain("IllegalStateException", "\tat ");
        doNothing().when(delivery).deliver("one");
        dispatcher.retryPending();
        verify(delivery).deliver("two");
        assertThat(output.getOut()).contains("delivery recovered");
    }
}
