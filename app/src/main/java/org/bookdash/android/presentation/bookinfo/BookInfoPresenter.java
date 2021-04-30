package org.bookdash.android.presentation.bookinfo;

import androidx.annotation.NonNull;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.bookdash.android.R;
import org.bookdash.android.data.book.BookService;
import org.bookdash.android.data.book.DownloadService;
import org.bookdash.android.data.tracking.Analytics;
import org.bookdash.android.domain.model.DownloadProgressItem;
import org.bookdash.android.domain.model.firebase.FireBookDetails;
import org.bookdash.android.domain.model.firebase.FireContributor;
import org.bookdash.android.domain.model.gson.BookPages;
import org.bookdash.android.presentation.base.BasePresenter;

import java.util.List;

import rx.Scheduler;
import rx.Subscriber;

/**
 * @author rebeccafranks
 * @since 15/11/03.
 */
class BookInfoPresenter extends BasePresenter<BookInfoContract.View> implements BookInfoContract.Presenter {

    private static final String TAG = "BookInfoPresenter";
    private final BookService bookService;
    private final DownloadService downloadService;
    private final Scheduler ioScheduler, mainScheduler;
    private final Analytics analytics;

    BookInfoPresenter(@NonNull BookService bookService, @NonNull DownloadService downloadService,
                      @NonNull Analytics analytics, Scheduler ioScheduler, Scheduler mainScheduler) {
        this.bookService = bookService;
        this.downloadService = downloadService;
        this.ioScheduler = ioScheduler;
        this.mainScheduler = mainScheduler;
        this.analytics = analytics;
    }

    @Override
    public void loadContributorInformation(FireBookDetails bookDetailId) {
        addSubscription(
                bookService.getContributorsForBook(bookDetailId).subscribeOn(ioScheduler).observeOn(mainScheduler)
                        .subscribe(new Subscriber<List<FireContributor>>() {
                            @Override
                            public void onCompleted() {

                            }

                            @Override
                            public void onError(final Throwable e) {
                                getView().showSnackBarMessage(R.string.error_getting_contributors);
                            }

                            @Override
                            public void onNext(final List<FireContributor> fireContributors) {
                                getView().showContributors(fireContributors);
                            }
                        }));
    }

    @Override
    public void downloadBook(final FireBookDetails bookInfo) {
        if (bookInfo == null || bookInfo.getBookUrl() == null) {
            analytics.trackDownloadBookFailed(bookInfo, "book_not_available");
            getView().showSnackBarMessage(R.string.book_not_available);
            return;
        }
        if (bookInfo.isDownloading()) {
            getView().showSnackBarMessage(R.string.book_is_downloading);
            return;
        }
        bookInfo.setIsDownloading(true);
        analytics.trackDownloadBookStarted(bookInfo);

        addSubscription(downloadService.downloadFile(bookInfo).subscribeOn(ioScheduler).observeOn(mainScheduler)
                .subscribe(new Subscriber<DownloadProgressItem>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        bookInfo.setIsDownloading(false);
                        String error = null;
                        if (e != null) {
                            getView().showSnackBarMessage(R.string.failed_to_download_book, e.getMessage());
                            error = e.getMessage();
                        }
                        analytics.trackDownloadBookFailed(bookInfo, error);

                    }

                    @Override
                    public void onNext(DownloadProgressItem downloadProgressItem) {
                        getView().showDownloadProgress(downloadProgressItem.getDownloadProgress());
                        if (downloadProgressItem.isComplete()) {
                            bookInfo.setIsDownloading(false);
                            analytics.trackViewBook(bookInfo);
                            BookPages bookPages = downloadProgressItem.getBookPages();
                            if (bookPages == null) {
                                getView().showSnackBarMessage(R.string.failed_to_open_book);
                                FirebaseCrashlytics.getInstance().recordException(new Exception("Book pages null after completed download.")); // TODO Remove in next release if book downloading bug is fixed.
                                analytics.trackDownloadBookFailed(bookInfo, "failed_to_open_book");
                                return;
                            }
                            getView().showDownloadFinished();
                            getView().openBook(bookInfo, bookPages,
                                    bookInfo.getFolderLocation());
                        }
                    }
                }));
    }

    @Override
    public void shareBookClicked(FireBookDetails bookInfo) {
        if (bookInfo == null) {
            getView().showSnackBarMessage(R.string.book_info_still_loading);
            return;
        }
        analytics.trackShareBook(bookInfo);
        getView().sendShareEvent(bookInfo.getBookTitle());
    }
}


