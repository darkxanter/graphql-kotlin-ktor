@file:Suppress("unused")

package example.graphql

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import com.expediagroup.graphql.server.extensions.getValueFromDataLoader
import graphql.schema.DataFetchingEnvironment
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass

interface GraphQLKotlinDataLoader<K, V> : KotlinDataLoader<K, V> {
    override val dataLoaderName: String
        get() = this.javaClass.name
}

/**
 * Helper method to get a value from a registered DataLoader.
 * The provided key should be the cache key object used to save the value for that particular data loader.
 */
fun <TDataloader : KotlinDataLoader<K, V>, K, V> DataFetchingEnvironment.getValueFromDataLoader(
    dataLoader: KClass<TDataloader>,
    key: K
): CompletableFuture<V> {
    return getValueFromDataLoader(dataLoader.java.name, key)
}

/**
 * Helper method to get values from a registered DataLoader.
 */
fun <TDataloader : KotlinDataLoader<K, V>, K, V> DataFetchingEnvironment.getValuesFromDataLoader(
    dataLoader: KClass<TDataloader>,
    keys: List<K>
): CompletableFuture<List<V>> {
    return getValueFromDataLoader(dataLoader.java.name, keys)
}
