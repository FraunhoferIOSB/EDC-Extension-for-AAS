package de.fraunhofer.iosb.app.pipeline;

import org.eclipse.edc.spi.result.Failure;

import java.util.List;

public class PipelineFailure extends Failure {

    private Exception exception;

    public PipelineFailure(List<String> messages, Exception exception) {
        super(messages);
        this.exception = exception;
    }
}
