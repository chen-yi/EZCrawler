Known Bugs & Issues: None.


How To Run:

The crawler will read all need information in the configuration file and store crawled material in independent files on your hard disk. The parameters:

• NumberOfTriesOnURLBeforeGiveUp: Try so many times on parsing each URL.

• NumberOfTriesBeforeGivingUpOnQueue: Try so many times on dequeue the URL with highest points.

• MaxDepth: The original URL in the seed file is at depth 0. The crawler will only crawl  URLs that are no more than MaxDepth deeper.• MaxBreadth: On any given downloaded page, just crawl the first MaxBreadth linked URLs and ignores the rest.

• MaxURLsApproximately: Download no more than so many URLs in one single run. When the number is reached, no HTML or text will be stored.• Number of threads: Use so many independent threads to crawl.• PolitenessInMS: Each thread sleeps so many MS’s between crawls.• Download directory: The directory you want to store the downloaded material in .

• OutputFormat: Options: HTMLONLY, TEXTONLY, BOTHHTMLANDTEXT
 
• PathToOutputDictionary: The directory to store one line per downloaded URL: <URL LOCATION>.

• StemForDictionary: Each thread has a dictionary file named by adding id to it.

• PreferredURLRegexes: One regex per line. URLs that match no regex will get 0 point. The ones that match any of the regexes will get one point.

• AvoidURLRegexes: One regex per line. The URLs that match these regexes will not be crawled at all.  

• PreferedURLText: One regex per line. The format is: String\tWeight. If the downloaded text matches a regex, the supplied amount will be added to all its children URLs.


Features Implemented:

All requirements were implemented. And some specifications were adjusted during the implementation. For the convenience of running the jar file, libraries are packed in the jar.


Features Not Implemented: None.


Libraries Used: 
    dom4j-1.6.1.jar
    jaxen-1.1.6.jar
    jsoup-1.7.3.jar


How To Build: 
    
(1) The Eclipse project
    Untar the tar ball: tar -zxvf MyNewCrawler.tar.gz;
    Import it into Eclipse;
    Change the configuration file config.xml and crawl seed file CrawlTHeseURLs.txt;
    Run the main class CleanerMyCrawler.

(2) The Jar file
    Untar the tar ball: tar -zxvf EZCrawler.tar.gz;
    Change the configuration file config.xml and crawl seed file CrawlTHeseURLs.txt;
    Use command: java -jar EZCrawler.jar.


Reference: CrawlerSpecifications.docx.
