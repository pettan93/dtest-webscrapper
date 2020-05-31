#### Problem to solve
As a subscriber of magazine called [dTest](https://www.dtest.cz/), I have access to online archive of all published articles/reviews.
I want to make it accessible for offline usage for my parents. 
As there is no provided option to download articles on dTest website and there are too many articles to download it manually I decided to download it programmatically.
For each article/test I want full copy of page with images and everything visible in form of html page. I do not want it as PDF as I am not satisfied with broken layout.

Disclaimer: There isn't any intend to illegally share downloaded content or any other bad intent.

#### Way to solve
1) Iterate over paginator on page with list of articles for store all links to article.
2) Iterate over all articles page bodies and find links to comparision pages (article can have one or more links to sub-article with products comparision) and store them.
3) Persist parsed tree hierarchy of links (and titles). Structure is going to be used as list of tasks for downloading part of program.
4) Copy cookies from a browser after authentication on a website to use it later.
5) Download all the links using! Cookies provided to access content behind paywall.
6) Create simple html page in root directory for navigation across downloaded content. 


I choose 
- The easiest way I found to webscrap webpage using `wget` utility, inspired by [this gist](https://gist.github.com/dannguyen/03a10e850656577cfb57) (thanks Dannguyen).
- Kotlin/Gradle for the rest, because as a Java/Maven guy and wanna taste a different kind of coffee.

##### Notes
 - I am running on Windows so there is usage of [WSL](https://docs.microsoft.com/en-us/windows/wsl/about) to easily reach `wget`


   
