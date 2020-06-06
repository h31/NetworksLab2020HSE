package ru.hse.lyubortk.websearch.config

data class SearcherConfig(
    val lucenePath: String,
    val maxSearchResults: Int,
    val indexingThreads: Int,
    val maxIndexingProcesses: Int,
    val maxVisitedPagesPerProcess: Int
)