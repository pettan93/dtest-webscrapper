package cz.kalas.playground

import com.google.gson.Gson
import org.jsoup.Jsoup
import java.io.File
import java.nio.charset.Charset
import java.util.ArrayList
import java.util.concurrent.TimeUnit
import kotlin.streams.toList

/**
 * Gathering links params
 */
const val BASE_HREF = "https://www.dtest.cz"
const val ARTICLES_PAGINATOR_MAX = 2
const val DOWNLOAD_TASK_FILENAME = "download_list.json"

/**
 * Downloading articles params
 */
const val AUTH_COOKIE = ""
const val DOWNLOAD_FOLDER = "../dtestDownload"

fun main() {
    prepareDownloadTasks()
    executeDownloading()
}

/**
 * Prepares file with article links to download
 */
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

/**
 * Reads file with article links and executes downloading
 */
fun executeDownloading() {
    val downloadTasks =
        Gson().fromJson(File(DOWNLOAD_TASK_FILENAME).readText(Charset.defaultCharset()), Array<Article>::class.java)
            .toList() as ArrayList<Article>

    for (article in downloadTasks) {
        println("Downloading article : " + article.title + " " + article.mainHref)
        wget(article.mainHref, "${normalizeTitle(article.title)}/recenze/", AUTH_COOKIE)

        for (comparison in article.comparisons) {
            println(" - Downloading subarticle : " + comparison.title + " " + comparison.mainHref)
            wget(
                comparison.mainHref,
                "${normalizeTitle(article.title)}/srovnání/${normalizeTitle(comparison.title)}/",
                AUTH_COOKIE
            )
        }
    }
}

fun normalizeTitle(articleTitle: String): String {
    val articleNameInvalidChars = "\\s|\\(|\\)".toRegex()
    return articleTitle.replace(articleNameInvalidChars, "_")
}

fun wget(url: String, path: String, cookie: String) {
    try {
        val command =
            "wsl wget -E -H -k -K -nd -N -p -P $DOWNLOAD_FOLDER/$path --restrict-file-names=windows" +
                    " --header \"Cookie: $cookie\" $url"
//    println(command)
        val process = ProcessBuilder(command.split(" ")).start()
        process.inputStream.reader(Charsets.UTF_8).use {
            println(it.readText())
        }
        process.errorStream.reader(Charsets.UTF_8).use {
            println(it.readText())
        }
        process.waitFor(10, TimeUnit.SECONDS) // lets wait several seconds to finish wget download
        process.destroyForcibly() // kill the process after, to prevent main thread stuck
    } catch (e: Exception) {
        e.printStackTrace()
    }
}