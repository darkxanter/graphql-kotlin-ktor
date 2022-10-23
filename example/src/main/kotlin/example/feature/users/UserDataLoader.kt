package example.feature.users

import example.graphql.GraphQLKotlinDataLoader
import mu.KotlinLogging
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory
import org.dataloader.DataLoaderOptions
import java.util.concurrent.CompletableFuture

class UserDataLoader(
    private val userRepository: UserRepository,
) : GraphQLKotlinDataLoader<Long, User> {
    private val logger = KotlinLogging.logger {  }

    override fun getDataLoader(): DataLoader<Long, User> = DataLoaderFactory.newDataLoader({ ids ->
        logger.info("get users for ids $ids")
        CompletableFuture.supplyAsync {
            val users = userRepository.listByIds(ids).associateBy { it.id }
            ids.mapNotNull {
                users[it]
            }
        }
    }, DataLoaderOptions.newOptions().setCachingEnabled(true))
}
