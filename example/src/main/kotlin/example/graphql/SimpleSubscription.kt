package example.graphql

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Subscription
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class SimpleSubscription : Subscription {

    private val logger: Logger = LoggerFactory.getLogger(SimpleSubscription::class.java)

    private fun flowTicker(interval: kotlin.time.Duration) = flow {
        var count = 1
        while (true) {
            logger.info("Returning $count from counter")
            emit(count)
            count++
            delay(interval.inWholeMilliseconds)
        }
    }

    @GraphQLDescription("Returns a single value")
    fun singleValueSubscription(): Flow<Int> = flowOf(1)

    @GraphQLDescription("Returns a random number every second")
    fun counter(limit: Int? = null): Flow<Int> {
        val flow = flowTicker(1.seconds)
        return if (limit != null) {
            flow.take(limit)
        } else {
            flow
        }
    }

    @GraphQLDescription("Returns a random number every second")
    fun counter2(limit: Int? = null): Flow<Int> {
        val flow = flowTicker(1.seconds)
        return if (limit != null) {
            flow.take(limit)
        } else {
            flow
        }
    }

    @GraphQLDescription("Returns a random number every second, errors if even")
    fun counterWithError(): Flow<Int> = flowTicker(1.seconds)
        .map {
            val value = Random.nextInt()
            if (value % 2 == 0) {
                throw Exception("Value is even $value")
            } else value
        }

    @GraphQLDescription("Returns one value then an error")
    fun singleValueThenError(): Flow<Int> = flowOf(1, 2)
        .map { if (it == 2) throw Exception("Second value") else it }

    @GraphQLDescription("Returns stream of errors")
    fun flowOfErrors(): Flow<DataFetcherResult<String?>> {
        val dfr: DataFetcherResult<String?> = DataFetcherResult.newResult<String?>()
            .data(null)
            .error(GraphqlErrorException.newErrorException().cause(Exception("error thrown")).build())
            .build()
        return flowOf(dfr, dfr)
    }
}
