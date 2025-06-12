package org.hank.botm.data.repository

import org.hank.botm.data.database.dao.ResultDao
import org.hank.botm.data.database.model.asDomain
import org.hank.botm.domain.model.Result
import org.hank.botm.domain.model.asEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface ResultRepository {
    fun getAllResults(): Flow<List<Result>>
    fun getRoundResults(roundId: Int): Flow<List<Result>>
    suspend fun insertResults(results: List<Result>, roundId: Int)
}

class ResultRepositoryImpl (
    private val resultDao: ResultDao,
    private val ioDispatcher: CoroutineDispatcher,
) : ResultRepository {
    override fun getAllResults(): Flow<List<Result>> {
        return resultDao.getAllResults().map { entityList ->
            entityList.map {
                it.asDomain()
            }
        }
    }

    override fun getRoundResults(roundId: Int): Flow<List<Result>> =
        resultDao.getRoundResults(roundId).map {
            it.map { it.asDomain() }
        }


    override suspend fun insertResults(results: List<Result>, roundId: Int) = withContext(ioDispatcher) {
        resultDao.insertResults(results.map { it.asEntity(roundId) })
        Unit
    }
}