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
    private val index: Directory = MMapDirectory(Path.of("./lucene"))
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
                booleanQueryBuilder.add(TermQuery(Term("content", it)), BooleanClause.Occur.SHOULD)
                booleanQueryBuilder.add(TermQuery(Term("title", it)), BooleanClause.Occur.SHOULD)
            }
            val query = booleanQueryBuilder.build()
            val topDocs = indexSearcher.search(query, 10)
            return topDocs.scoreDocs.map {
                val document = indexSearcher.doc(it.doc)
                SearchResult(URL(document.get("url")), document.get("title"))
            }
        }
    }

    fun addToIndex(url: URL) {
        val pageInfo = Crawler.getPageInfo(url)
        indexWriter.addDocument(
            listOf(
                StringField("url", url.toString(), Field.Store.YES),
                TextField("title", pageInfo.name, Field.Store.YES),
                TextField("content", pageInfo.content, Field.Store.NO)
            )
        )
        indexWriter.commit()
    }

    override fun close() {
        indexWriter.close()
    }

    companion object Searcher {
        data class SearchResult(val url: URL, val name: String)
    }
}