package ru.hse.lyubortk.websearch.config

import java.time.Duration

data class ClientConnectorConfig(val halfCloseTimeout: Duration, val connectTimeout: Duration)