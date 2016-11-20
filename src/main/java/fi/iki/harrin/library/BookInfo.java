package fi.iki.harrin.library;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains information of one book to be searched.
 *
 * @author $Author: $
 */
class BookInfo
{
    /** Author name */
    private String m_author;
    /** Title of the book */
    private String m_title;
    /** where the book is located */
    private String m_shelfNumber;
    /** Where the book is really located according to search */
    private String m_realLocation = "";
    /** web application shows books available even if they are reserved */
    private boolean m_isQueued = false;
    /** some books are in warehouse, but found book might still be on shelf */
    private boolean m_possiblyInStore = false;

    /** Library server does not understand ':' characters in titles.
     * Therefore save the part after colon and handle answers accordingly */
    private String m_colonTitlePart = null;

    /**
     * Constructor for BookInfo.
     *
     * @param author      Author name
     * @param title       Title of the book
     * @param shelfNumber where the book is located
     */
    private BookInfo(String author,
                     String title,
                     String shelfNumber)
    {
        m_author = author;
        m_title = title;
        m_shelfNumber = shelfNumber;

        int colonIndex = title.indexOf(":");
        if (colonIndex != -1)
        {
            m_title = title.substring(0,
                                      colonIndex);
            m_colonTitlePart = title.substring(colonIndex + 1);
        }
    }

    /**
     * Gets the books in config file
     *
     * @param filename config file
     * @return books in file
     * @throws IOException if reading fails
     */
    static BookInfo[] getBooks(String filename)
        throws IOException
    {
        List<BookInfo> listBooks = new ArrayList<BookInfo>();
        readFile(filename,
                 listBooks);

        BookInfo[] aBooks = new BookInfo[listBooks.size()];
        listBooks.toArray(aBooks);
        return aBooks;
    }

    /**
     * Gets the value.
     *
     * @return the value
     */
    String getAuthor()
    {
        return m_author;
    }

    /**
     * Gets the value.
     *
     * @return the value
     */
    String getShelfNumber()
    {
        return m_shelfNumber;
    }

    /**
     * Sets the real location number according to search.
     *
     * @param value the value
     */
    void setRealLocation(String value)
    {
        m_realLocation = value;
    }

    /**
     * Gets the value.
     *
     * @return the value
     */
    String getTitle()
    {
        return m_title;
    }

    /**
     * Gets the possible title part after ':', as library server does not
     * understand it in title name.
     *
     * @return title part after colon or null
     */
    String getColonTitlePart()
    {
        return m_colonTitlePart;
    }

    /**
     * Gets the isQueued.
     *
     * @return Returns the isQueued.
     */
    boolean isQueued() {
        return m_isQueued;
    }

    /**
     * Sets the isQueued.
     *
     * @param isQueued The isQueued to set.
     */
    void setQueued(boolean isQueued) {
        m_isQueued = isQueued;
    }

    /**
     * Gets the copy of the object.
     *
     * @return copy object
     */
    BookInfo getCopy()
    {
        BookInfo copy = new BookInfo(m_author,
                                     m_title,
                                     m_shelfNumber);
        copy.m_realLocation = m_realLocation;
        copy.m_colonTitlePart = m_colonTitlePart;
        return copy;
    }

    /**
     * Gets the string representation.
     *
     * @return object as string
     */
    @Override
    public String toString()
    {
        return
            (m_isQueued ? "VARAUSJONOSSA: " : "") +
            (m_possiblyInStore ? "VARASTOSSA??: " : "") +
            getAuthor() + ": " +
            getTitle() +
            (m_colonTitlePart != null ? ":" + m_colonTitlePart : "") +
            " (" + m_shelfNumber + ") " +
            m_realLocation;
    }

    /**
     * Reads the config file content
     *
     * @param filename  name of the file
     * @param listBooks output list containing BookInfo
     * @throws IOException if reading fails
     */
    private static void readFile(String         filename,
                                 List<BookInfo> listBooks)
        throws IOException
    {
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(filename));

            String inputLine = reader.readLine(); // HERVANTA
            reader.readLine(); // ---------
            while ((inputLine = reader.readLine()) != null)
            {
                if (inputLine.length() == 0)
                {
                    continue;
                }

                if (inputLine.startsWith("----------"))
                {
                    break;
                }

                String author = inputLine;
                String title = reader.readLine();
                String number = reader.readLine();
                if (number != null &&
                    number.equalsIgnoreCase("Hankinnassa"))
                {
                    number = reader.readLine();
                }

                reader.readLine(); // empty line or 'Hankinnassa'

                if (author.length() > 0 &&
                    title != null && title.length() > 0)
                {
                    listBooks.add(new BookInfo(author,
                                               title,
                                               number));
                }
            }
        }
        finally
        {
            if (reader != null)
            {
                reader.close();
            }
        }
    }

    void setPossiblyInStore(boolean inStore) {
        m_possiblyInStore = inStore;
    }

}
