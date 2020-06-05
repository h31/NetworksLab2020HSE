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
import ru.hse.lyubortk.websearch.crawler.Crawler
import ru.hse.lyubortk.websearch.crawler.Crawler.PageFetchResult.*
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class Searcher : AutoCloseable {
    private val log = LoggerFactory.getLogger(Searcher::class.java)

    private val index: Directory = MMapDirectory(Path.of(LUCENE_PATH))
    private val analyzer = StandardAnalyzer()
    private val indexWriterConfig = IndexWriterConfig(analyzer)
    private val indexWriter = IndexWriter(index, indexWriterConfig).also { it.commit() }

    private val indexingInProgress = AtomicInteger(0)
    private val indexingThreadPool = Executors.newFixedThreadPool(INDEXING_THREADS)

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
            val luceneSearchResult = indexSearcher.search(booleanQueryBuilder.build(), MAX_RESULTS)

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
        if (newIndexingNumber > MAX_INDEXING_PROCESSES) {
            indexingInProgress.decrementAndGet()
            return StartIndexingResult.Refused(TO_MANY_INDEXING_PROCESSES_REASON)
        }

        val pageStream = Crawler.getPageStream(uri)
        return when (val firstPageResult = pageStream.next()) {
            is TextPage -> {
                addToIndex(firstPageResult)
                startIndexingProcess(pageStream)
                StartIndexingResult.Started
            }
            is NotTextPage -> {
                indexingInProgress.decrementAndGet()
                StartIndexingResult.Refused("Not a text page")
            }
            is RequestError -> {
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
            while (visitedPages < MAX_VISITED_PAGES_PER_INDEXING && pageStream.hasNext()) {
                val pageResult = pageStream.next()
                visitedPages++
                when (pageResult) {
                    is TextPage -> addToIndex(pageResult)
                    else -> Unit
                }
            }
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
    }

    override fun close() {
        indexWriter.close()
    }

    companion object {
        const val URI_FIELD_NAME = "uri"
        const val TITLE_FIELD_NAME = "title"
        const val CONTENT_FIELD_NAME = "content"
        const val LUCENE_PATH = "./lucene"
        const val MAX_RESULTS = 10
        const val INDEXING_THREADS = 5
        const val MAX_INDEXING_PROCESSES = 5
        const val TO_MANY_INDEXING_PROCESSES_REASON = "Too many indexing processes"
        const val MAX_VISITED_PAGES_PER_INDEXING = 5000

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