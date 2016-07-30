package org.bookdash.android.data.books;

import android.support.annotation.NonNull;

import org.bookdash.android.domain.pojo.Book;
import org.bookdash.android.domain.pojo.BookContributor;
import org.bookdash.android.domain.pojo.BookDetail;
import org.bookdash.android.domain.pojo.Language;
import org.bookdash.android.domain.pojo.gson.BookPages;

import java.util.List;

/**
 * Can add caching here or fetching from db instead of from service
 *
 * @author rebeccafranks
 * @since 15/11/03.
 */
public class BookDetailRepositoryImpl implements BookDetailRepository {

    private final BookDetailApi bookDetailApi;

    public BookDetailRepositoryImpl(@NonNull BookDetailApi bookDetailApi) {
        this.bookDetailApi = bookDetailApi;
    }

    @Override
    public void getBooksForLanguage(@NonNull String language, @NonNull final GetBooksForLanguageCallback booksForLanguageCallback) {
        bookDetailApi.getBooksForLanguages(language,
                new BookServiceCallbackImpl(booksForLanguageCallback));
    }

    @Override
    public void searchBooksForLanguage(String searchString, @NonNull String language, @NonNull final GetBooksForLanguageCallback booksForLanguageCallback) {
        bookDetailApi.searchBooksForLanguages(searchString, language,
                new BookServiceCallbackImpl(booksForLanguageCallback));
    }

    @Override
    public void getDownloadedBooks(final GetBooksForLanguageCallback getBooksForLanguageCallback) {
        bookDetailApi.getDownloadedBooks(new BookDetailApi.BookServiceCallback<List<BookDetail>>() {

            @Override
            public void onLoaded(List<BookDetail> result) {
                getBooksForLanguageCallback.onBooksLoaded(result);
            }

            @Override
            public void onError(Exception error) {
                getBooksForLanguageCallback.onBooksLoadError(error);
            }
        });

    }


    @Override
    public void getBookDetail(String bookDetailId, @NonNull final GetBookDetailCallback bookDetailCallback) {
        bookDetailApi.getBookDetail(bookDetailId, new BookDetailApi.BookServiceCallback<BookDetail>() {
            @Override
            public void onLoaded(BookDetail result) {
                bookDetailCallback.onBookDetailLoaded(result);
            }

            @Override
            public void onError(Exception error) {
                bookDetailCallback.onBookDetailLoadError(error);
            }
        });
    }

    @Override
    public void getContributorsForBook(Book bookId, @NonNull final GetContributorsCallback contributorsCallback) {
        bookDetailApi.getContributorsForBook(bookId, new BookDetailApi.BookServiceCallback<List<BookContributor>>() {
            @Override
            public void onLoaded(List<BookContributor> result) {
                contributorsCallback.onContributorsLoaded(result);
            }

            @Override
            public void onError(Exception error) {
                contributorsCallback.onContributorsLoadError(error);
            }
        });
    }


    @Override
    public void getLanguages(@NonNull final GetLanguagesCallback languagesCallback) {
        bookDetailApi.getLanguages(new BookDetailApi.BookServiceCallback<List<Language>>() {

            @Override
            public void onLoaded(List<Language> result) {
                languagesCallback.onLanguagesLoaded(result);
            }

            @Override
            public void onError(Exception error) {
                languagesCallback.onLanguagesLoadError(error);
            }
        });
    }

    @Override
    public void downloadBook(BookDetail bookDetail, @NonNull final GetBookPagesCallback bookPagesCallback) {
        bookDetailApi.downloadBook(bookDetail, new BookDetailApi.BookServiceCallback<BookPages>() {
            @Override
            public void onLoaded(BookPages result) {
                bookPagesCallback.onBookPagesLoaded(result);
            }

            @Override
            public void onError(Exception error) {
                bookPagesCallback.onBookPagesLoadError(error);
            }
        }, new BookDetailApi.BookServiceProgressCallback() {
            @Override
            public void onProgressChanged(int progress) {
                bookPagesCallback.onBookPagesDownloadProgressUpdate(progress);
            }
        });
    }

    @Override
    public void deleteBook(final BookDetail bookDetail, @NonNull final DeleteBookCallBack deleteBookCallBack) {
        bookDetailApi.deleteBook(bookDetail, new BookDetailApi.BookServiceCallback<Boolean>() {
            @Override
            public void onLoaded(Boolean result) {
                deleteBookCallBack.onBookDeleted(bookDetail);
            }

            @Override
            public void onError(Exception error) {
                deleteBookCallBack.onBookDeleteFailed(error);
            }
        });
    }


    private class BookServiceCallbackImpl implements BookDetailApi.BookServiceCallback<List<BookDetail>> {

        private GetBooksForLanguageCallback booksForLanguageCallback;

        public BookServiceCallbackImpl(@NonNull final GetBooksForLanguageCallback booksForLanguageCallback) {
            this.booksForLanguageCallback = booksForLanguageCallback;
        }

        @Override
        public void onLoaded(List<BookDetail> result) {
            booksForLanguageCallback.onBooksLoaded(result);
        }

        @Override
        public void onError(Exception error) {
            booksForLanguageCallback.onBooksLoadError(error);
        }
    }
}
