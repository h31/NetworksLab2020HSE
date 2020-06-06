package ru.hse.lyubortk.websearch.core

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.search.*
import org.apache.lucene.store.Directory
import org.apache.lucene.store.MMapDirectory
import org.slf4j.LoggerFactory
import ru.hse.lyubortk.websearch.config.SearcherConfig
import ru.hse.lyubortk.websearch.crawler.Crawler
import ru.hse.lyubortk.websearch.crawler.Crawler.PageFetchResult.*
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class Searcher(private val crawler: Crawler, private val config: SearcherConfig) : AutoCloseable {
    private val log = LoggerFactory.getLogger(Searcher::class.java)

    private val index: Directory = MMapDirectory(Path.of(config.lucenePath))
    private val analyzer = StandardAnalyzer()
    private val indexWriterConfig = IndexWriterConfig(analyzer)
    private val indexWriter = IndexWriter(index, indexWriterConfig).also { it.commit() }

    private val indexingInProgress = AtomicInteger(0)
    private val indexingThreadPool = Executors.newFixedThreadPool(config.indexingThreads)

    fun search(text: String): SearchResult {
        log.info("Searching for \"$text\"")
        DirectoryReader.open(index).use { indexReader ->
            val indexSearcher = IndexSearcher(indexReader)
            val booleanQueryBuilder = BooleanQuery.Builder()
            text.split(Regex("\\s+")).filter {
                it.isNotEmpty()
            }.map {
                it.toLowerCase()
            }.forEach {
                booleanQueryBuilder.add(TermQuery(Term(CONTENT_FIELD_NAME, it)), BooleanClause.Occur.SHOULD)
                booleanQueryBuilder.add(TermQuery(Term(TITLE_FIELD_NAME, it)), BooleanClause.Occur.SHOULD)
            }
            val luceneSearchResult = indexSearcher.search(booleanQueryBuilder.build(), config.maxSearchResults)

            val totalResults = TotalResults(
                luceneSearchResult.totalHits.value,
                luceneSearchResult.totalHits.relation == TotalHits.Relation.EQUAL_TO
            )
            val topPages = luceneSearchResult.scoreDocs.map {
                val document = indexSearcher.doc(it.doc)
                Page(URI(document.get(URI_FIELD_NAME)), document.get(TITLE_FIELD_NAME))
            }
            return SearchResult(text, totalResults, topPages)
        }
    }

    fun startIndexing(uri: URI): StartIndexingResult {
        log.info("Starting indexing from $uri")
        val newIndexingNumber = indexingInProgress.incrementAndGet()
        if (newIndexingNumber > config.maxIndexingProcesses) {
            indexingInProgress.decrementAndGet()
            return StartIndexingResult.Refused(TO_MANY_INDEXING_PROCESSES_REASON)
        }

        val pageStream = crawler.getPageStream(uri)
        return when (val firstPageResult = pageStream.next()) {
            is TextPage -> {
                addToIndex(firstPageResult)
                startIndexingProcess(pageStream)
                StartIndexingResult.Started
            }
            is NotTextPage -> {
                log.info("Indexing stopped")
                indexingInProgress.decrementAndGet()
                StartIndexingResult.Refused("Not a text page")
            }
            is RequestError -> {
                log.info("Indexing stopped")
                indexingInProgress.decrementAndGet()
                StartIndexingResult.Refused(firstPageResult.exception.message)
            }
        }
    }

    fun getStats(): SearcherStats {
        log.info("Getting stats")
        DirectoryReader.open(index).use {
            return SearcherStats(it.maxDoc(), indexingInProgress.get()) // probably faster than numdocs
        }
    }

    private fun startIndexingProcess(pageStream: Crawler.PageStream) {
        indexingThreadPool.submit {
            var visitedPages = 0
            while (visitedPages < config.maxVisitedPagesPerProcess && pageStream.hasNext()) {
                val pageResult = pageStream.next()
                visitedPages++
                when (pageResult) {
                    is TextPage -> addToIndex(pageResult)
                    else -> Unit
                }
            }
            log.info("Indexing stopped")
            indexingInProgress.decrementAndGet()
        }
    }

    private fun addToIndex(textPage: TextPage) {
        indexWriter.updateDocument(
            Term(URI_FIELD_NAME, textPage.uri.toString()),
            listOf(
                StringField(URI_FIELD_NAME, textPage.uri.toString(), Field.Store.YES),
                TextField(TITLE_FIELD_NAME, textPage.name, Field.Store.YES),
                TextField(CONTENT_FIELD_NAME, textPage.content, Field.Store.NO)
            )
        )
        indexWriter.commit()
        log.info("added to index: ${textPage.uri}")
    }

    override fun close() {
        indexWriter.close()
    }

    companion object {
        const val URI_FIELD_NAME = "uri"
        const val TITLE_FIELD_NAME = "title"
        const val CONTENT_FIELD_NAME = "content"
        const val TO_MANY_INDEXING_PROCESSES_REASON = "Too many indexing processes"

        data class SearchResult(
            val searchQuery: String,
            val totalResults: TotalResults,
            val topPages: List<Page>
        )

        data class Page(val uri: URI, val title: String)
        data class TotalResults(val number: Long, val isExact: Boolean)

        data class SearcherStats(val indexedPagesNum: Int, val runningProcesses: Int)

        sealed class StartIndexingResult {
            object Started : StartIndexingResult()
            data class Refused(val reason: String?) : StartIndexingResult()
        }
    }
}