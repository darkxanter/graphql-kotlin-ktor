package example.feature.users

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory
import org.dataloader.DataLoaderOptions
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

class UserDataLoader(
    private val userRepository: UserRepository,
) : KotlinDataLoader<Long, User> {
    companion object {
        const val dataLoaderName = "UserBatchDataLoader"
    }

    private val logger = LoggerFactory.getLogger(UserDataLoader::class.java)

    override val dataLoaderName = UserDataLoader.dataLoaderName
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
