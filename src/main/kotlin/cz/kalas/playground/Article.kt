package cz.kalas.playground

class Article(
    var title: String,
    var mainHref: String,
    var comparisons: List<Article>
) {
    override fun toString(): String {
        return "Article(title='$title', mainHref='$mainHref', comparisons=$comparisons)"
    }
}