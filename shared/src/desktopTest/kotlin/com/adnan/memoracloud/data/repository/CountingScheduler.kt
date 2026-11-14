package com.adnan.memoracloud.data.repository

import com.adnan.memoracloud.core.publish.PublishScheduler

internal class CountingScheduler : PublishScheduler {
    var kicks = 0
    override fun kick() {
        kicks++
    }
}
