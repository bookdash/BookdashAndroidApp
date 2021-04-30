package org.bookdash.android.data.book;


import android.net.Uri;
import android.util.Log;

import androidx.annotation.WorkerThread;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.gson.Gson;

import org.bookdash.android.BookDashApplication;
import org.bookdash.android.data.utils.FileManager;
import org.bookdash.android.data.utils.ZipManager;
import org.bookdash.android.domain.model.DownloadProgressItem;
import org.bookdash.android.domain.model.firebase.FireBookDetails;
import org.bookdash.android.domain.model.gson.BookPages;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;


public class DownloadServiceImpl implements DownloadService {
    public static final String TAG = "DownloadService";
    private final FirebaseStorage storageRef;

    public DownloadServiceImpl(FirebaseStorage firebaseStorageRef) {
        this.storageRef = firebaseStorageRef;
    }

    @Override
    public Observable<DownloadProgressItem> downloadFile(final FireBookDetails book) {
        if (book.isDownloadedAlready()) {
            return getBookPagesFromDownloadedBook(book);
        }
        try {
            final File localFile;
            Uri uri = Uri.parse(book.getBookUrl());
            String tempFileName = uri.getLastPathSegment();
            String tempFileParsed[] = tempFileName.split("/");
            tempFileName = tempFileParsed[tempFileParsed.length - 1];
            localFile = File.createTempFile(tempFileName, "");

            return RxFirebaseStorage.getFile(book.getBookUrlStorageRef(), localFile)
                    .flatMap(new Func1<FileDownloadTask.TaskSnapshot, Observable<DownloadProgressItem>>() {
                        @Override
                        public Observable<DownloadProgressItem> call(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            DownloadProgressItem downloadProgressItem = new DownloadProgressItem(
                                    taskSnapshot.getBytesTransferred(), taskSnapshot.getTotalByteCount());
                            if (downloadProgressItem.isComplete()) {
                                return Observable
                                        .defer(new TransformFileIntoBookPages(downloadProgressItem, localFile, book));
                            }
                            return Observable.just(downloadProgressItem);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "IOException downloading file", e);
            return Observable.error(e);
        }
    }

    private Observable<DownloadProgressItem> getBookPagesFromDownloadedBook(final FireBookDetails bookDetails) {

        return Observable.defer(new Func0<Observable<DownloadProgressItem>>() {
            @Override
            public Observable<DownloadProgressItem> call() {
                BookPages bookPages = getBookPages(
                        bookDetails.getFolderLocation() + File.separator + FireBookDetails.BOOK_FORMAT_JSON_FILE);
                DownloadProgressItem downloadProgressItem = new DownloadProgressItem(100, 100);
                downloadProgressItem.setBookPages(bookPages);

                return Observable.just(downloadProgressItem);
            }
        });
    }

    private BookPages getBookPages(String fileName) {
        Gson gson = new Gson();
        BufferedReader br = null;
        BookPages bookPages = null;
        try {
            br = new BufferedReader(new FileReader(fileName));
            bookPages = gson.fromJson(br, BookPages.class);
        } catch (FileNotFoundException e) {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e1) {
                Log.e(TAG, "EX: ", e);
            }

            FirebaseCrashlytics.getInstance().log("File path: " + fileName); // TODO Remove in next release if book downloading bug is fixed.
            FirebaseCrashlytics.getInstance().recordException(e);
            Log.e(TAG, "Ex:" + e.getMessage(), e);
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().log("File path: " + fileName); // TODO Remove in next release if book downloading bug is fixed.
            FirebaseCrashlytics.getInstance().recordException(e);
            Log.e(TAG, "error parsing book: " + fileName, e);
        }
        return bookPages;
    }

    public Observable<Boolean> deleteDownload(final FireBookDetails bookToDelete) {
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                return Observable.just(deleteLocalBook(bookToDelete));
            }
        });
    }

    @WorkerThread
    private boolean deleteLocalBook(FireBookDetails book) {
        FileManager.deleteFolder(book.getFolderLocation());
        FileManager.deleteFolder(BookDashApplication.FILES_DIR + File.separator + book.getId());
        return true;
    }

    private class TransformFileIntoBookPages implements Func0<Observable<DownloadProgressItem>> {
        private final File file;
        private final FireBookDetails book;
        DownloadProgressItem downloadProgressItem;

        TransformFileIntoBookPages(DownloadProgressItem downloadProgressItem, File localFile, FireBookDetails book) {
            this.downloadProgressItem = downloadProgressItem;
            this.book = book;
            this.file = localFile;
        }

        @Override
        public Observable<DownloadProgressItem> call() {
            String targetLocation = BookDashApplication.FILES_DIR + File.separator + book.getId();
            ZipManager zipManager = new ZipManager();
            zipManager.unzip(file.getAbsolutePath(), targetLocation);

            BookPages bookPages = getBookPages(
                    book.getFolderLocation() + File.separator + FireBookDetails.BOOK_FORMAT_JSON_FILE);
            downloadProgressItem.setBookPages(bookPages);
            return Observable.just(downloadProgressItem);

        }
    }
}
