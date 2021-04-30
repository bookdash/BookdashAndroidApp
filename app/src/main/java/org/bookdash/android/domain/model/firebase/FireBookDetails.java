package org.bookdash.android.domain.model.firebase;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.bookdash.android.BookDashApplication;
import org.bookdash.android.Injection;
import org.threeten.bp.Instant;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.FormatStyle;
import org.threeten.bp.zone.ZoneRulesException;

import java.io.File;
import java.util.Comparator;
import java.util.List;

public class FireBookDetails implements Parcelable {
    public static final String TABLE_NAME = "bd_books";
    public static final String CONTRIBUTORS_ITEM_NAME = "contributors";

    public static final String BOOK_LANGUAGE_FIELD = "bookLanguage";
    public static final String BOOK_FORMAT_JSON_FILE = "bookdetails.json";
    public static final String BOOK_COLUMN_CREATED_DATE = "createdDate";
    public static final Parcelable.Creator<FireBookDetails> CREATOR = new Parcelable.Creator<FireBookDetails>() {
        @Override
        public FireBookDetails createFromParcel(Parcel source) {
            return new FireBookDetails(source);
        }

        @Override
        public FireBookDetails[] newArray(int size) {
            return new FireBookDetails[size];
        }
    };
    public static final Comparator<? super FireBookDetails> COMPARATOR = new Comparator<FireBookDetails>() {
        @Override
        public int compare(final FireBookDetails bookDetails, final FireBookDetails bookDetails2) {
            Long long1 = Long.valueOf(bookDetails.createdDate);
            Long long2 = Long.valueOf(bookDetails2.createdDate);
            return long2.compareTo(long1);
        }
    };

    private String bookTitle;
    private String bookCoverPageUrl;
    private boolean bookEnabled;
    private String bookLanguage;
    private String bookLanguageAbbreviation = "en";
    private String bookId;
    private String bookDescription;
    private List<String> contributorsIndexedKeys;
    private boolean isDownloading;
    private String bookUrl;
    private long createdDate;

    public FireBookDetails(final String title, final String url, final String coverUrl, final boolean enabled,
                           final String description, FireLanguage fireLanguage, long createdDate) {
        this.bookTitle = title;
        this.bookCoverPageUrl = coverUrl;
        this.bookUrl = url;
        this.bookEnabled = enabled;
        this.bookDescription = description;
        this.bookLanguage = fireLanguage.getId();
        this.bookLanguageAbbreviation = fireLanguage.getLanguageAbbreviation();
        this.createdDate = createdDate;
    }

    public FireBookDetails() {

    }

    protected FireBookDetails(Parcel in) {
        this.bookTitle = in.readString();
        this.bookUrl = in.readString();
        this.bookCoverPageUrl = in.readString();
        this.bookEnabled = in.readByte() != 0;
        this.bookLanguage = in.readString();
        this.bookId = in.readString();
        this.bookDescription = in.readString();
        this.contributorsIndexedKeys = in.createStringArrayList();
        this.isDownloading = in.readByte() != 0;
        this.createdDate = in.readLong();
        this.bookLanguageAbbreviation = in.readString();
    }

    public boolean isBookEnabled() {
        return bookEnabled;
    }

    public void setBookEnabled(boolean bookEnabled) {
        this.bookEnabled = bookEnabled;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getBookCoverPageUrl() {
        return Injection.STORAGE_PREFIX + bookCoverPageUrl;
    }

    public void setBookCoverPageUrl(String bookCoverPageUrl) {
        this.bookCoverPageUrl = bookCoverPageUrl;
    }

    public StorageReference getFirebaseBookCoverUrl() {
        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(Injection.STORAGE_PREFIX);
        return storageRef.child(bookCoverPageUrl);
    }

    public boolean isDownloadedAlready() {
        String folderLocation = getFolderLocation();
        if (folderLocation == null || folderLocation.isEmpty()) {
            return false;
        }
        File f = new File("", folderLocation);
        return f.exists();
    }

    public String getFolderLocation() {
        return getFolderLocation(new File(BookDashApplication.FILES_DIR, getId() + File.separator));
    }

    private String getFolderLocation(File file) {
        if (file != null && file.isDirectory() && (file.canRead())) {
            File[] files = file.listFiles();
            if (files == null || files.length == 0 || files[0] == null) {
                return null;
            }
            for (File aFile : files) {
                if (!aFile.getAbsoluteFile().toString().contains("MACOSX")) {
                    // This is a workaround bugfix (no other solution possible). Previously the app
                    // assumed that a book's root folder only contains one folder (containing the
                    // book images and .json file). However, in the CMS content conversion process
                    // where PDF books are converted into .json and images so that the app can
                    // consume it, mac inserted a "__MACOSX" folder in certain cases. This causes
                    // this method to return the MACOSX folder as the book folder on certain devices
                    // as file.listFiles() does not guarantee any specific ordering.
                    return aFile.getAbsoluteFile().toString();
                }
            }
        }
        return null;
    }

    public String getId() {
        return bookId;
    }

    public boolean isDownloading() {
        return isDownloading;
    }

    public void setDownloading(boolean downloading) {
        isDownloading = downloading;
    }

    public void setIsDownloading(boolean isDownloading) {
        this.isDownloading = isDownloading;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }


    public StorageReference getBookUrlStorageRef() {
        if (bookUrl == null || bookUrl.isEmpty()) {
            return null;
        }
        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(Injection.STORAGE_PREFIX);
        return storageRef.child(bookUrl);
    }

    public String getBookUrl() {
        return bookUrl;
    }

    public void setBookUrl(String bookUrl) {
        this.bookUrl = bookUrl;
    }

    public String getBookDescription() {
        return bookDescription;
    }

    public void setBookDescription(String bookDescription) {
        this.bookDescription = bookDescription;
    }

    public List<String> getContributorsIndexList() {
        return contributorsIndexedKeys;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.bookTitle);
        dest.writeString(this.bookUrl);
        dest.writeString(this.bookCoverPageUrl);
        dest.writeByte(this.bookEnabled ? (byte) 1 : (byte) 0);
        dest.writeString(this.bookLanguage);
        dest.writeString(this.bookId);
        dest.writeString(this.bookDescription);
        dest.writeStringList(this.contributorsIndexedKeys);
        dest.writeByte(this.isDownloading ? (byte) 1 : (byte) 0);
        dest.writeLong(this.createdDate);
        dest.writeString(this.bookLanguageAbbreviation);
    }

    public String getBookLanguage() {
        return bookLanguage;
    }

    public void setBookLanguage(String bookLanguage) {
        this.bookLanguage = bookLanguage;
    }

    public void setContributorsIndexedKeys(List<String> contributorsIndexedKeys) {
        this.contributorsIndexedKeys = contributorsIndexedKeys;
    }

    public String getCreatedDateFormatted() {
        Log.d("bookdateials", "getCreatedDateFormatted() called:" + createdDate);
        Instant i = Instant.ofEpochMilli(createdDate);
        ZoneId zoneId;
        try {
            zoneId = ZoneId.systemDefault();
        } catch (ZoneRulesException zre) {
            // Fallback. Just show the date to the user as if they're in South Africa.
            zoneId = ZoneId.of("UTC+02:00");
        }
        ZonedDateTime z = ZonedDateTime.ofInstant(i, zoneId);
        return z.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG));
    }

    public long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(final long createdDate) {
        this.createdDate = createdDate;
    }
}
