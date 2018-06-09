package fi.iki.harrin.library;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.xml.sax.SAXException;

/**
 * Application for checking if library database has available books
 * defined in list file.
 *
 * @author Harri Nissinen
 */
public class CheckLibrary
{
    /** Url of the start page */
    private static final String LIBRARY_URL = "https://piki.verkkokirjasto.fi/web/arena/tarkennettu_haku";

    private long m_startTime = System.currentTimeMillis();
    private long m_previousTime = System.currentTimeMillis();

    /**
     * Starts the application. Command line parameter specifies the
     * book list file.
     *
     * @param args [0] is the book list filename
     */
    public static void main(String[] args)
    {
        if (args == null
                || args.length != 1)
        {
            System.out.println("Missing book list filename!");
            System.exit(1);
        }

        try
        {
            BookInfo[] aBooks = BookInfo.getBooks(args[0]);
            CheckLibrary library = new CheckLibrary();
            library.check(aBooks);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void check(BookInfo[] aBooks)
        throws MalformedURLException, IOException, SAXException
    {
        List<BookInfo> listAvailable = new ArrayList<BookInfo>();
        List<BookInfo> listNotAvailable = new ArrayList<BookInfo>();
        List<BookInfo> listOrdered = new ArrayList<BookInfo>();
        List<BookInfo> listNotFound = new ArrayList<BookInfo>();

        System.setProperty("webdriver.chrome.driver",
                           "/usr/local/bin/chromedriver");
        WebDriver driver = new ChromeDriver();
        driver.get(LIBRARY_URL);
//        WebElement localeLink = driver.findElement(By.linkText("FI"));
//        localeLink.click();

//        driver.manage().window().setPosition(new Point(0, 0));
//        driver.manage().window().setSize(new Dimension(1000, 1000));

        for (int i = 0; i < aBooks.length; i++)
        {
            try
            {
                doQueryRequest(aBooks[i],
                               driver,
                               listAvailable,
                               listNotAvailable,
                               listOrdered,
                               listNotFound);
            }
            catch (Exception e)
            {
                System.out.println("ERROR: " + aBooks[i]);
                System.out.println(e.toString());
                listNotFound.add(aBooks[i]);
                e.printStackTrace();
//                File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
//                FileUtils.copyFile(scrFile, new File("~/Desktop/libScreenshot" + i + ".png"));
            }

            printStatistics(i + 1,
                            aBooks.length,
                            listAvailable.size(),
                            listNotAvailable.size(),
                            listOrdered.size(),
                            listNotFound.size());
        }

        System.out.println("");
        System.out.println(new Date());

        printResult("Available books",
                    listAvailable,
                    aBooks.length);

        printResult("Ordered books",
                    listOrdered,
                    aBooks.length);

        printResult("Books not available",
                    listNotAvailable,
                    aBooks.length);

        printResult("Books not found",
                    listNotFound,
                    aBooks.length);

        driver.quit();
    }

    private void printResult(String title,
                             List<BookInfo> listBooks,
                             int totalBookCount) {
        System.out.println("");
        System.out.println(title + " (" + listBooks.size() + "/" + totalBookCount + "):");

        for (BookInfo book : listBooks)
        {
            System.out.println("- " + book);
        }
    }

    private void printStatistics(int currentBook,
                                 int bookCount,
                                 int availableCount,
                                 int notAvailableCount,
                                 int orderedCount,
                                 int notFoundCount) {
        System.out.print("(" + currentBook + "/" + bookCount + ") ");

        System.out.print("(A=" + availableCount + ",NA=" + notAvailableCount + ",O=" + orderedCount + ",NF=" + notFoundCount + ") ");

        long currentTime = System.currentTimeMillis();
        System.out.print(" Book time: " + printTime(currentTime - m_previousTime));
        System.out.print(" Total time: " + printTime(currentTime - m_startTime));
        System.out.println(" Avg: " +
                printTime((currentTime - m_startTime) / currentBook));

        m_previousTime = currentTime;
    }

    private String printTime(long milliseconds) {
        int seconds = (int) (milliseconds / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;

        return minutes + " min " + seconds + " s ";// + millis + " ms.";
    }

    private void doQueryRequest(BookInfo         book,
                                WebDriver        driver,
                                List<BookInfo>   listAvailable,
                                List<BookInfo>   listNotAvailable,
                                List<BookInfo>   listOrdered,
                                List<BookInfo>   listNotFound)
        throws MalformedURLException, IOException, SAXException
    {
        search(book, driver);

        if (checkExistence(book,
                           listAvailable,
                           listNotAvailable,
                           listOrdered,
                           driver))
        {
            return;
        }

        System.out.println("NOT FOUND: " + book);
        listNotFound.add(book);
    }

    private void search(BookInfo book,
                        WebDriver driver) {
        WebElement searchLink = driver.findElement(By.linkText("Tarkennettu haku"));
        scrollTop(driver);
        searchLink.click();

        Select librarySelect = new Select(
            driver.findElement(By.name("organisationHierarchyPanel:organisationContainer:organisationChoice")));
        librarySelect.selectByVisibleText("Tampereen kaupunginkirjasto");
        waitForElement(driver,
                       By.xpath("//option[text()='Tampereen pääkirjasto']"));

        Select categorySelect = new Select(
            driver.findElement(By.name("materialPanel:mediaClassContainer:mediaClassChoice")));
        categorySelect.selectByVisibleText("Kirja");

        Select titleSelect = new Select(
            driver.findElement(By.name("freeTextFieldsContainer:freeTextView:0:freeTextPanel:freeTextTypeChoice")));
        titleSelect.selectByVisibleText("Teos");
        WebElement titleField =
             driver.findElement(By.name("freeTextFieldsContainer:freeTextView:0:freeTextPanel:freeTextField"));
        titleField.sendKeys(book.getTitle());

        if (book.getAuthor().length() > 1)
        {
            Select authorSelect = new Select(
                driver.findElement(By.name("freeTextFieldsContainer:freeTextView:1:freeTextPanel:freeTextTypeChoice")));
            authorSelect.selectByVisibleText("Tekijä");
waitSomeTime();
            WebElement authorField =
                driver.findElement(By.name("freeTextFieldsContainer:freeTextView:1:freeTextPanel:freeTextField"));
            authorField.sendKeys(book.getAuthor());
        }

        WebElement searchButton = driver.findElement(By.name("bottomButtonsContainer:bottomSearchButton"));
        scrollAndClick(searchButton,
                       driver);
    }

    private void scrollTop(WebDriver driver) {
        scrollToElement(findElement(driver,
                                    By.cssSelector(".custom-logo")),
                        driver);

    }

    private void scrollAndClick(WebElement element,
                                WebDriver driver) {
        scrollToElement(element,
                        driver);
        element.click();
    }

    private void scrollToElement(WebElement element,
                                 WebDriver driver) {
        Actions actions = new Actions(driver);
        actions.moveToElement(element);
        actions.perform();
    }

    private void waitSomeTime()
    {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean checkExistence(BookInfo book,
                                   List<BookInfo>   listAvailable,
                                   List<BookInfo>   listNotAvailable,
                                   List<BookInfo>   listOrdered,
                                   WebDriver driver)
        throws MalformedURLException, IOException, SAXException
    {
        waitForText(driver, "Hakutulos");
        waitSomeTime();

        List<WebElement> availabilityLinks = driver.findElements(By.linkText("Saatavilla"));
        if (availabilityLinks == null
                || availabilityLinks.isEmpty())
        {
            return false;
        }
        availabilityLinks.get(availabilityLinks.size() - 1).click();

        waitForText(driver, "Tampereen kaupunginkirjasto");
        WebElement tampereAvailabilityLink = findElement(driver,
                                                         By.linkText("Tampereen kaupunginkirjasto"));
        if (tampereAvailabilityLink == null)
        {
            return false;
        }
//        scrollAndClick(tampereAvailabilityLink,
//                       driver);
        scrollToReview(driver);
        tampereAvailabilityLink.click();

        waitForText(driver, "Tampereen pääkirjasto");
        WebElement paakirjastoAvailabilityLink = findElement(driver,
                                                             By.linkText("Tampereen pääkirjasto"));
        if (paakirjastoAvailabilityLink == null)
        {
            return false;
        }
        scrollToReview(driver);
        scrollAndClick(paakirjastoAvailabilityLink,
                       driver);

        waitForText(driver, "Osasto:");
        WebElement availableCountElement =
            findElement(driver,
                        By.xpath("//span[text()='Tampereen pääkirjasto']/../../following-sibling::div[@class='arena-holding-child-hyper-container']"
                        + "//td[@class='arena-holding-nof-available-for-loan']/span[@class='arena-value']"));
        if (availableCountElement == null)
        {
            WebElement loanedCountElement =
                findElement(driver,
                            By.xpath("//span[text()='Tampereen pääkirjasto']/../../following-sibling::div[@class='arena-holding-child-hyper-container']"
                                    + "//td[@class='arena-holding-nof-checked-out']/span[@class='arena-value']"));
            if (loanedCountElement != null)
            {
                listNotAvailable.add(book);
                System.out.println("NOT AVAILABLE: " + book);
                return true;
            }

            WebElement orderedCountElement =
                findElement(driver,
                            By.xpath("//span[text()='Tampereen pääkirjasto']/../../following-sibling::div[@class='arena-holding-child-hyper-container']"
                                   + "//td[@class='arena-holding-nof-ordered']/span[@class='arena-value']"));
            if (orderedCountElement != null)
            {
                listOrdered.add(book);
                System.out.println("ORDERED: " + book);
                return true;
            }

            return false;
        }

        int availableCount = Integer.parseInt(availableCountElement.getText());

        if (availableCount > 0)
        {
            WebElement shelfElement =
                findElement(driver,
                            By.xpath("//span[text()='Tampereen pääkirjasto']/../../following-sibling::div[@class='arena-holding-child-hyper-container']"
                            + "//td[@class='arena-holding-shelf-mark']/span[@class='arena-value']"));
            book.setRealLocation(shelfElement.getText());

            listAvailable.add(book);
            System.out.println("AVAILABLE: " + book);
        } else {
            listNotAvailable.add(book);
            System.out.println("NOT AVAILABLE: " + book);
        }

        return true;
    }

    private void scrollToReview(WebDriver driver) {
        scrollToElement(findElement(driver,
                                    By.cssSelector(".arena-review-subtitle")),
                        driver);
    }

    private WebElement findElement(WebDriver driver,
                                   By by)
    {
        try
        {
            return driver.findElement(by);
        }
        catch (NoSuchElementException e)
        {
            return null;
        }
    }

    private void waitForText(WebDriver driver,
                             final String text)
    {
        final By by = By.xpath("//span[contains(text(), '" + text + "')]");
        waitForElement(driver,
                       by);
    }

    private void waitForElement(WebDriver driver,
                                final By by) {
        (new WebDriverWait(driver, 30)).until(new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver d) {
                return d.findElement(by) != null;
            }
        });
    }
}