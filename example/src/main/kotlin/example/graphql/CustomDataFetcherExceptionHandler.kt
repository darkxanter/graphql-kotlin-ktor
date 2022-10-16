package example.graphql

import example.feature.auth.exceptions.AuthException
import graphql.ErrorClassification
import graphql.ErrorType
import graphql.GraphQLError
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.DataFetcherExceptionHandlerResult
import graphql.execution.ResultPath
import graphql.language.SourceLocation
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

class CustomDataFetcherExceptionHandler : DataFetcherExceptionHandler {
    private val logger = LoggerFactory.getLogger("DataFetcherExceptionHandler")

    override fun handleException(handlerParameters: DataFetcherExceptionHandlerParameters): CompletableFuture<DataFetcherExceptionHandlerResult> {
        val exception = unwrap(handlerParameters.exception)
        val path = handlerParameters.path
        val error = DataFetcherError(path, exception)
        if (exception !is AuthException) {
            logger.error(error.message, exception)
        }
        val result = DataFetcherExceptionHandlerResult.newResult().error(error).build()
        return CompletableFuture.completedFuture(result)
    }

    private fun unwrap(exception: Throwable): Throwable {
        if (exception.cause != null) {
            if (exception is CompletionException) {
                return exception.cause!!
            }
        }
        return exception
    }
}

class DataFetcherError(
    private val path: ResultPath,
    private val exception: Throwable,
) : GraphQLError {
    override fun getMessage(): String = when (exception) {
        is AuthException -> "$path : ${exception.message}"
        else -> "Exception while fetching data ($path) : $exception"
    }

    override fun getPath(): MutableList<Any> = path.toList()

    override fun getLocations(): MutableList<SourceLocation> = mutableListOf()

    override fun getErrorType(): ErrorClassification = ErrorType.DataFetchingException
}
