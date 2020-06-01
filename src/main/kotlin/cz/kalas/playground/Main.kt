package cz.kalas.playground

import com.google.gson.Gson
import org.jsoup.Jsoup
import java.io.File
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.streams.toList

/**
 * Gathering links params
 */
const val BASE_HREF = "https://www.dtest.cz"
const val ARTICLES_PAGINATOR_MAX = 91
const val DOWNLOAD_TASK_FILENAME = "download_list.json"

/**
 * Downloading articles params
 */
const val AUTH_COOKIE = ""
const val DOWNLOAD_FOLDER = "dtest_offline"
const val MAIN_PAGE_FILENAME = "Offline dTest Recenze.html"

fun main() {
//    prepareDownloadTasks()
    executeDownloading(10)
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
 * Reads file with article links file and executes downloading
 */
fun executeDownloading(articleLimit: Int) {
    val downloadTasks =
        Gson().fromJson(File(DOWNLOAD_TASK_FILENAME).readText(Charset.defaultCharset()), Array<Article>::class.java)
            .toList() as ArrayList<Article>

    File(MAIN_PAGE_FILENAME).printWriter().use { out -> out.println(
            "<!DOCTYPE html><html><head><title>dTest offline</title><meta charset=\"utf-8\"/></head>" +
                    "<body><h1>dTest offline</h1><br/>"
        )}

    for ((i, article) in
    downloadTasks.subList(0, if (articleLimit > 0) articleLimit else downloadTasks.size).withIndex()) {
        println("Downloading $i/${downloadTasks.size} article : " + article.title + " " + article.mainHref)
        if (wget(article.mainHref, "${normalizeTitle(article.title)}/recenze/", AUTH_COOKIE)) {
            createArticleLink(article.title, "${normalizeTitle(article.title)}/recenze/")
        }

        for (comparison in article.comparisons) {
            println(" - Downloading subarticle : " + comparison.title + " " + comparison.mainHref)
            if (wget(
                    comparison.mainHref,
                    "${normalizeTitle(article.title)}/srovnání/${normalizeTitle(comparison.title)}/",
                    AUTH_COOKIE
                )
            ) {
                createArticleLink(
                    comparison.title,
                    "${normalizeTitle(article.title)}/srovnání/${normalizeTitle(comparison.title)}/"
                )
            }
        }
    }

    File(MAIN_PAGE_FILENAME).appendText("</body></html>")
}

fun createArticleLink(articleTitle: String, articlePath: String) {
    File("$DOWNLOAD_FOLDER/$articlePath")
        .walkTopDown()
        .filter { file ->
            file.isDirectory || file.name.endsWith(".html")
                    && !file.name.contains("ns.html")
        }
        .forEach { file ->
            run {
                if (file.name.endsWith(".html") && file.canonicalPath.contains("recenze")) {
                    File(MAIN_PAGE_FILENAME).appendText("<a href=\"${file.path}\">$articleTitle</a><br/>\n")
                }
                if (file.name.endsWith(".html") && !file.canonicalPath.contains("recenze")) {
                    File(MAIN_PAGE_FILENAME).appendText(" - <a href=\"${file.path}\">$articleTitle</a><br/>\n")
                }
            }
        }
}

fun wget(url: String, path: String, cookie: String): Boolean {
//    checkAuthCookie()
    try {
        val command =
            "wsl wget -E -H -k -K -nd -N -p -P  $DOWNLOAD_FOLDER/$path --restrict-file-names=windows" +
                    " --header \"Cookie: $cookie\" $url"
        println(command)
        val process = ProcessBuilder(command.split(" ")).start()
        process.inputStream.reader(Charsets.UTF_8).use {
            println(it.readText())
        }
        process.errorStream.reader(Charsets.UTF_8).use {
            println(it.readText())
        }
        process.waitFor(10, TimeUnit.SECONDS) // lets wait several seconds to finish wget download
        process.destroyForcibly() // kill the process after, to prevent main thread stuck
        return true
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}

fun normalizeTitle(articleTitle: String): String {
    val articleNameInvalidChars = "\\s|\\(|\\)".toRegex()
    return articleTitle.trim().replace(articleNameInvalidChars, "_")
        .substring(0, max(articleTitle.length,25))
}

fun checkAuthCookie() {
    TODO("Not yet implemented")
}