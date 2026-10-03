/**
 * Automated Digital Book Downloader & PDF Compiler
 * 
 * NOTE: This utility is specifically designed to work with embedded 
 * Adobe InDesign online readers (indd.adobe.com) hosted within web portals.
 * It uses pixel-based RGB thresholding to handle dynamic page positioning 
 * and two-page spreads specific to the Adobe web viewer platform.
 */

package Base;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Automated Digital Book Downloader & PDF Compiler
 * Captured pages from dynamic online readers, applies pixel-based content cropping,
 * and compiles the clean pages into a single PDF document.
 */
public class BookDownloader {

    // ==================== CONFIGURATION ====================
    // Placeholders for sensitive details
    private static final String LOGIN_URL = "https://your-portal-login-url.com/login/"; 
    private static final String BOOK_URL  = "https://your-book-reader-url.com/view/example-id";   
    
    private static final String USERNAME  = "YOUR_USERNAME_HERE"; 
    private static final String PASSWORD  = "YOUR_PASSWORD_HERE";
    
    private static final int TOTAL_PAGES  = 50; //Maximum pages that can be downloaded
    private static final String OUTPUT_PDF = "output/Downloaded_Book.pdf";

    // --- SELECTORS ---
    private static final By USERNAME_FIELD   = By.id("username");
    private static final By PASSWORD_FIELD   = By.id("password");
    private static final By LOGIN_BUTTON     = By.id("log-in-btn");
    // =======================================================

    public static void main(String[] args) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        List<File> imageFiles = new ArrayList<>();
        Actions actions = new Actions(driver);

        try {
            // 1. Navigate to portal login
            driver.get(LOGIN_URL);

            // 2. Perform Login
            wait.until(ExpectedConditions.visibilityOfElementLocated(USERNAME_FIELD)).sendKeys(USERNAME);
            wait.until(ExpectedConditions.visibilityOfElementLocated(PASSWORD_FIELD)).sendKeys(PASSWORD);
            WebElement loginBtn = wait.until(ExpectedConditions.elementToBeClickable(LOGIN_BUTTON));
            loginBtn.click();

            // 3. Wait for redirect and navigate to direct book page
            wait.until(ExpectedConditions.urlContains("/my/"));
            Thread.sleep(2000);
            
            if (!BOOK_URL.equalsIgnoreCase(driver.getCurrentUrl())) {
                driver.get(BOOK_URL);
            }

            // Allow the web reader and canvas elements to fully render
            Thread.sleep(6000); 

            // 4. Capture and crop pages dynamically
            for (int i = 1; i <= TOTAL_PAGES; i++) {
                System.out.println("Capturing page " + i + " of " + TOTAL_PAGES + "...");
                
                Thread.sleep(3000); 

                File tempImg = new File(String.format("temp_page_%03d.png", i));

                // Take full browser screenshot
                TakesScreenshot ts = (TakesScreenshot) driver;
                File screenshot = ts.getScreenshotAs(OutputType.FILE);
                BufferedImage fullImg = ImageIO.read(screenshot);

                // Dynamically crop out dark margins using color threshold detection
                BufferedImage croppedImg = cropToWhiteContent(fullImg);

                // Save temporary cropped PNG
                ImageIO.write(croppedImg, "png", tempImg);
                System.out.println("Page " + i + " cropped successfully!");

                imageFiles.add(tempImg);

                // Advance to next page via keyboard navigation
                if (i < TOTAL_PAGES) {
                    actions.sendKeys(Keys.ARROW_RIGHT).perform();
                }
            }

            // 5. Compile cropped page images into a single PDF
            System.out.println("Merging clean pages into PDF...");
            imagesToPdf(imageFiles, OUTPUT_PDF);
            System.out.println("SUCCESS! Clean PDF generated at: " + OUTPUT_PDF);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Clean up temporary image files
            for (File img : imageFiles) {
                if (img.exists()) {
                    img.delete();
                }
            }
            driver.quit();
        }
    }

    /**
     * Scans screenshot pixels dynamically to isolate central white content 
     * regardless of left, right, or center page alignment.
     */
    private static BufferedImage cropToWhiteContent(BufferedImage src) {
        int imgWidth = src.getWidth();
        int imgHeight = src.getHeight();

        int top = 0;
        int bottom = imgHeight - 1;
        int left = 0;
        int right = imgWidth - 1;

        // 1. Scan from left to right for content edge
        leftScan:
        for (int x = 0; x < imgWidth; x++) {
            for (int y = 50; y < imgHeight - 50; y++) {
                if (isWhitePixel(src.getRGB(x, y))) {
                    left = x;
                    break leftScan;
                }
            }
        }

        // 2. Scan from right to left for content edge
        rightScan:
        for (int x = imgWidth - 1; x > left; x--) {
            for (int y = 50; y < imgHeight - 50; y++) {
                if (isWhitePixel(src.getRGB(x, y))) {
                    right = x;
                    break rightScan;
                }
            }
        }

        // 3. Scan top edge within detected page boundary
        topScan:
        for (int y = 0; y < imgHeight; y++) {
            for (int x = left; x <= right; x++) {
                if (isWhitePixel(src.getRGB(x, y))) {
                    top = y;
                    break topScan;
                }
            }
        }

        // 4. Scan bottom edge (ignoring bottom toolbar margin)
        bottomScan:
        for (int y = imgHeight - 50; y > top; y--) {
            for (int x = left; x <= right; x++) {
                if (isWhitePixel(src.getRGB(x, y))) {
                    bottom = y;
                    break bottomScan;
                }
            }
        }

        int width = Math.max(1, right - left + 1);
        int height = Math.max(1, bottom - top + 1);

        return src.getSubimage(left, top, width, height);
    }

    /**
     * Helper method to determine white or near-white page background pixels.
     */
    private static boolean isWhitePixel(int rgb) {
        Color color = new Color(rgb);
        return color.getRed() > 235 && color.getGreen() > 235 && color.getBlue() > 235;
    }

    /**
     * Converts a list of image files into a compiled PDF document using Apache PDFBox.
     */
    private static void imagesToPdf(List<File> images, String outputPath) throws IOException {
        File outputFile = new File(outputPath);
        if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }

        try (PDDocument doc = new PDDocument()) {
            for (File imgFile : images) {
                PDImageXObject pdImage = PDImageXObject.createFromFileByExtension(imgFile, doc);
                
                PDRectangle pageSize = new PDRectangle(pdImage.getWidth(), pdImage.getHeight());
                PDPage page = new PDPage(pageSize);
                doc.addPage(page);

                try (PDPageContentStream contentStream = new PDPageContentStream(doc, page)) {
                    contentStream.drawImage(pdImage, 0, 0, pdImage.getWidth(), pdImage.getHeight());
                }
            }
            doc.save(outputFile);
        }
    }
}
