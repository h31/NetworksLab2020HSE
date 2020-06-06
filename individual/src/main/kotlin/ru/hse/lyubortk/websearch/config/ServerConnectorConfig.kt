package ru.hse.lyubortk.websearch.config

import java.time.Duration

data class ServerConnectorConfig(
    val socketReadTimeout: Duration,
    val halfCloseTimeout: Duration
)