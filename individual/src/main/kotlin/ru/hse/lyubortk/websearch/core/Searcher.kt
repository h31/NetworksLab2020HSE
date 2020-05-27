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
import ru.hse.lyubortk.websearch.crawler.Crawler
import java.net.URI
import java.nio.file.Path

//TODO: move field names to constants
class Searcher : AutoCloseable {
    private val index: Directory = MMapDirectory(Path.of(LUCENE_PATH))
    private val analyzer = StandardAnalyzer()
    private val indexWriterConfig = IndexWriterConfig(analyzer)
    private val indexWriter = run {
        val writer = IndexWriter(index, indexWriterConfig)
        writer.commit() // commit creation
        writer
    }

    fun search(text: String): SearchResult {
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

    fun addToIndex(url: URI) {
        val pageInfo = Crawler.getPageInfo(url)
        indexWriter.updateDocument(
            Term(URI_FIELD_NAME, pageInfo.uri.toString()),
            listOf(
                StringField(URI_FIELD_NAME, pageInfo.uri.toString(), Field.Store.YES),
                TextField(TITLE_FIELD_NAME, pageInfo.name, Field.Store.YES),
                TextField(CONTENT_FIELD_NAME, pageInfo.content, Field.Store.NO)
            )
        )
        indexWriter.commit()
    }

    fun getStats(): SearcherStats {
        DirectoryReader.open(index).use{
            return SearcherStats(it.maxDoc()) // probably faster than numdocs
        }
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

        data class SearchResult(
            val searchQuery: String,
            val totalResults: TotalResults,
            val topPages: List<Page>
        )

        data class Page(val uri: URI, val title: String)
        data class TotalResults(val number: Long, val isExact: Boolean)

        data class SearcherStats(val indexedPagesNum: Int)
    }
}