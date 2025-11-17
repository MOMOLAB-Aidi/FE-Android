package com.example.momolabfe.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object RefreshSingleFlight {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var inFlight: Deferred<String?>? = null

    /**
     * 동시 재발급 요청을 단 1회로 합쳐주는 유틸
     * - 이미 재발급이 진행 중이면 그 결과를 기다렸다가 그대로 반환
     * - 새로운 재발급이 필요하면 직접 실행
     * - runBlocking 환경에서도 호출 가능
     */
    suspend fun refresh(block: suspend () -> String?): String? {
        mutex.withLock {
            inFlight?.let { return it.await() }

            val job = scope.async {
                try {
                    block()
                } finally {
                    mutex.withLock {
                        if (inFlight?.isCompleted == true) {
                            inFlight = null
                        }
                    }
                }
            }
            inFlight = job
            return job.await()
        }
    }
}