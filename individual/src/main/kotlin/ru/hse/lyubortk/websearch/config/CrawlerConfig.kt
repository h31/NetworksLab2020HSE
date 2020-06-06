package ru.hse.lyubortk.websearch.config

import java.time.Duration

data class CrawlerConfig(val maxUriSetSize: Int, val requestTimeout: Duration)