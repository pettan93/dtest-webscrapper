package cz.kalas.playground

import com.google.gson.Gson
import org.jsoup.Jsoup
import java.io.File
import kotlin.streams.toList

/**
 * Gathering links constants
 */
const val BASE_HREF = "https://www.dtest.cz"
const val ARTICLES_PAGINATOR_MAX = 91
const val DOWNLOAD_TASK_FILENAME = "download_list.json"

fun main() {
    prepareDownloadTasks()
}

fun prepareDownloadTasks() {
    val mainArticleLinks = mutableListOf<String>()
    val downloadTaskList = mutableListOf<Article>()

    println("Gathering main articles links...")
    for (x in 1 until ARTICLES_PAGINATOR_MAX) {
        println("Page $x of $ARTICLES_PAGINATOR_MAX")
        Jsoup.connect("$BASE_HREF/clanky?page=$x&filter%5Bt%5D%5B0%5D=1&filter%5Btab%5D=1&do=page")
            .get()
            .select("a")
            .parallelStream()
            .filter { it.attr("href").contains(Regex("clanek.*/test-")) }
            .map { it.attr("href") }
            .map { a -> BASE_HREF + a }
            .forEach { mainArticleLinks.add(it) }
    }

    println("Gathering subarticles links from main articles...")
    for (mainArticleLink in mainArticleLinks.distinct()) {
        val doc = Jsoup.connect(mainArticleLink)
            .get()

        val mainArticle = doc.select("h1").first().text()
        println("$mainArticle $mainArticleLink")

        val comparisionArticles = doc.select("h3")
            .parallelStream()
            .filter { it.html().contains(Regex("/compare/")) }
            .map {
                Article(
                    it.text().capitalize().replace("porovnat", "Porovnání"),
                    BASE_HREF + it.select("a").attr("href"),
                    emptyList()
                )
            }
            .toList()

        comparisionArticles.forEach { println(" Srovnání výrobků -  ${it.mainHref}") }
        downloadTaskList.add(Article(mainArticle, mainArticleLink, comparisionArticles))
    }

    println("Saving download tasks to $DOWNLOAD_TASK_FILENAME...")

    File(DOWNLOAD_TASK_FILENAME).printWriter().use { out -> out.println(Gson().toJson(downloadTaskList)) }

    println("Done! $DOWNLOAD_TASK_FILENAME created")
}