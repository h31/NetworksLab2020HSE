package ru.hse.lyubortk.websearch.core

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TermQuery
import org.apache.lucene.store.Directory
import org.apache.lucene.store.MMapDirectory
import ru.hse.lyubortk.websearch.crawler.Crawler
import java.net.URL
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

    fun search(text: String): List<SearchResult> {
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
            val query = booleanQueryBuilder.build()
            val topDocs = indexSearcher.search(query, 10)
            return topDocs.scoreDocs.map {
                val document = indexSearcher.doc(it.doc)
                SearchResult(URL(document.get(URL_FIELD_NAME)), document.get(TITLE_FIELD_NAME))
            }
        }
    }

    fun addToIndex(url: URL) {
        val pageInfo = Crawler.getPageInfo(url)
        indexWriter.addDocument(
            listOf(
                StringField(URL_FIELD_NAME, url.toString(), Field.Store.YES),
                TextField(TITLE_FIELD_NAME, pageInfo.name, Field.Store.YES),
                TextField(CONTENT_FIELD_NAME, pageInfo.content, Field.Store.NO)
            )
        )
        indexWriter.commit()
    }

    override fun close() {
        indexWriter.close()
    }

    companion object {
        const val URL_FIELD_NAME = "url"
        const val TITLE_FIELD_NAME = "title"
        const val CONTENT_FIELD_NAME = "content"
        const val LUCENE_PATH = "./lucene"

        data class SearchResult(val url: URL, val name: String)
    }
}