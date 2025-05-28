package app.secondclass.healthsyncer.data.db

import kotlinx.coroutines.flow.Flow

class SessionRepository(
    private val sessionDao: SessionDao
) {
    fun allSessions(): Flow<List<SessionEntity>> = sessionDao.getAll()

    suspend fun saveSession(session: SessionEntity) {
        sessionDao.insert(session)
    }

    suspend fun deleteSessions(ids: List<String>) {
        sessionDao.deleteByIds(ids)
    }

    suspend fun deleteAllSessions() {
        sessionDao.deleteAll()
    }
}
