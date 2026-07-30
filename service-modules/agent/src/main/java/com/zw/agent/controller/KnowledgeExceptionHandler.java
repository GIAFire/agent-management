package com.zw.agent.controller;

import com.zw.agent.knowledge.KnowledgeOperationException;
import com.zw.common.entity.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice(assignableTypes = {
        AiKnowledgeBaseController.class,
        AiKnowledgeDocumentController.class,
        AiKnowledgeChunkController.class,
        AiKnowledgeTaskController.class,
        AiKnowledgeAgentBindingController.class
})
public class KnowledgeExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(KnowledgeExceptionHandler.class);

    @ExceptionHandler(KnowledgeOperationException.class)
    public Result<Void> handleKnowledgeOperation(
            KnowledgeOperationException error
    ) {
        return Result.fail(error.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleBadRequest(IllegalArgumentException error) {
        return Result.fail(error.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleUploadTooLarge(
            MaxUploadSizeExceededException ignored
    ) {
        return Result.fail("文件大小不能超过 50 MB");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknown(Exception error) {
        log.error(
                "Knowledge management request failed, errorType={}",
                error.getClass().getSimpleName()
        );
        return Result.fail("知识管理操作失败，请查看服务日志");
    }
}
