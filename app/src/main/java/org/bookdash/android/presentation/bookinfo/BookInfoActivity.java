package org.bookdash.android.presentation.bookinfo;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.transition.Transition;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.databinding.DataBindingUtil;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.storage.StorageReference;

import org.bookdash.android.Injection;
import org.bookdash.android.R;
import org.bookdash.android.config.GlideApp;
import org.bookdash.android.databinding.ActivityBookInformationBinding;
import org.bookdash.android.domain.model.firebase.FireBookDetails;
import org.bookdash.android.domain.model.firebase.FireContributor;
import org.bookdash.android.domain.model.gson.BookPages;
import org.bookdash.android.presentation.activity.BaseAppCompatActivity;
import org.bookdash.android.presentation.readbook.BookDetailActivity;

import java.util.List;

import mbanje.kurt.fabbutton.FabButton;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public class BookInfoActivity extends BaseAppCompatActivity implements BookInfoContract.View {
    public static final String BOOK_PARCEL = "book_parcel";
    private static final String TAG = "BookInfoActivity";
    /**
     * Presenter object
     */
    private BookInfoContract.Presenter bookInfoPresenter;

    /**
     * Views
     */
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private FabButton floatingActionButton;
    private View gradientBackground;
    private RecyclerView contributorRecyclerView;
    private View scrollView;
    private ImageView imageViewBook;
    private AppBarLayout appBarLayout;
    private CoordinatorLayout coordinatorLayout;
    private View errorLayout;
    private TextView errorText;
    private ActivityBookInformationBinding binding;
    private ProgressBar loadingProgressBar;
    private CardView contributorCard, mainBookCard;
    private Toolbar toolbar;
    private ActionBar actionBar;
    private FireBookDetails bookInfo;
    private Button errorRetryButton;
    private int progress = 0;

    public static void startBookInfo(Activity activity, FireBookDetails bookDetails) {
        Intent intent = new Intent(activity, BookInfoActivity.class);
        intent.putExtra(BookInfoActivity.BOOK_PARCEL, bookDetails);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_book_information);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getSharedElementReturnTransition().addListener(new Transition.TransitionListener() {
                @Override
                public void onTransitionStart(Transition transition) {
                    floatingActionButton.animate().scaleY(0).scaleX(0).setInterpolator(new AccelerateInterpolator())
                            .setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime)).start();

                }

                @Override
                public void onTransitionEnd(Transition transition) {
                    enterAnimation();
                }

                @Override
                public void onTransitionCancel(Transition transition) {

                }

                @Override
                public void onTransitionPause(Transition transition) {

                }

                @Override
                public void onTransitionResume(Transition transition) {

                }
            });
            postponeEnterTransition();
        }
        contributorRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_contributors);
        contributorCard = (CardView) findViewById(R.id.contributor_card);
        imageViewBook = (ImageView) findViewById(R.id.image_view_book_cover);
        appBarLayout = (AppBarLayout) findViewById(R.id.app_bar_layout);
        mainBookCard = (CardView) findViewById(R.id.card_view_main_book_info);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout_content);
        errorLayout = findViewById(R.id.linear_layout_error);
        errorText = (TextView) findViewById(R.id.text_view_error_screen);

        errorRetryButton = (Button) findViewById(R.id.button_retry);
        loadingProgressBar = (ProgressBar) findViewById(R.id.activity_loading_book_info);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        scrollView = findViewById(R.id.scrollViewBookInfo);
        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);

        gradientBackground = findViewById(R.id.toolbar_background_gradient);
        floatingActionButton = (FabButton) findViewById(R.id.fab_download);
        floatingActionButton.setScaleX(0);
        floatingActionButton.setScaleY(0);
        binding.setDownloadClick(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bookInfo == null) {
                    showSnackBarMessage(R.string.book_not_available);
                    return;
                }
                if (!bookInfo.isDownloadedAlready()) {
                    floatingActionButton.resetIcon();
                    floatingActionButton.showProgress(true);
                    floatingActionButton.setProgress(0);
                }
                bookInfoPresenter.downloadBook(bookInfo);
            }
        });


        bookInfoPresenter = new BookInfoPresenter(Injection.provideBookService(), Injection.provideDownloadService(),
                Injection.provideAnalytics(), Schedulers.io(), AndroidSchedulers.mainThread());
        bookInfoPresenter.attachView(this);
        calculateLayoutHeight();
        imageViewBook.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                imageViewBook.getViewTreeObserver().removeOnPreDrawListener(this);

                enterAnimation();
                return true;
            }
        });
        final FireBookDetails bookDetailParcelable = getIntent().getParcelableExtra(BOOK_PARCEL);

        if (bookDetailParcelable != null) {
            setBookInfoBinding(bookDetailParcelable);
            startLoadingBook(bookDetailParcelable);
            showBookDetailView();
        } else {
            onNewIntent(getIntent());
        }

    }

    private void enterAnimation() {

        floatingActionButton.setScaleX(0);
        floatingActionButton.setScaleY(0);
        floatingActionButton.animate().setStartDelay(500).scaleY(1).scaleX(1)
                .setInterpolator(new OvershootInterpolator())
                .setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime)).start();
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void calculateLayoutHeight() {
        if (getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            Log.d(TAG, "Setting image height");
            DisplayMetrics dMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dMetrics);
            CollapsingToolbarLayout.LayoutParams lp = (CollapsingToolbarLayout.LayoutParams) imageViewBook
                    .getLayoutParams();
            lp.height = dMetrics.widthPixels;
            imageViewBook.setLayoutParams(lp);

            CoordinatorLayout.LayoutParams lp2 = (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
            lp2.height = dMetrics.widthPixels;
            appBarLayout.setLayoutParams(lp2);
        }
    }

    private void startLoadingBook(final FireBookDetails bookDetailId) {
        errorRetryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bookInfoPresenter.loadContributorInformation(bookDetailId);
            }
        });

        bookInfoPresenter.loadContributorInformation(bookDetailId);
        showBookDetailView();
    }

    @Override
    public void showBookDetailView() {
        mainBookCard.setVisibility(View.VISIBLE);
        loadingProgressBar.setVisibility(View.GONE);
        errorLayout.setVisibility(View.GONE);
        coordinatorLayout.setVisibility(View.VISIBLE);
        scrollView.setVisibility(View.VISIBLE);
        floatingActionButton.setVisibility(View.VISIBLE);
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();
        String data = intent.getDataString();
        Log.d(TAG, "onNewIntent() called: action" + action);

        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            Uri uri = Uri.parse(data);
            String bookId = uri.getLastPathSegment();
            String invitationId = uri.getQueryParameter("invitation_id");
            Log.d(TAG, "Action View: book id:" + bookId + ". Full URL:" + uri
                    .toString() + ". InvitationId:" + invitationId);
            //  startLoadingBook(bookId);
        }
    }

    private void loadImage(StorageReference url) {
        GlideApp.with(this).load(url)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable com.bumptech.glide.request.transition.Transition<? super Drawable> transition) {

                        Bitmap bitmap = drawableToBitmap(resource);
                        onImageLoaded(bitmap);
                        extractPaletteColors(bitmap);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Clear the view.
                        onImageLoaded(null);
                    }
                });
    }
    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
    private void onImageLoaded(@Nullable Bitmap bitmap) {
        imageViewBook.setImageBitmap(bitmap);

    }

    private void extractPaletteColors(Bitmap resource) {
        Palette.from(resource).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(Palette palette) {
                if (null == palette) {
                    return;
                }
                Log.d("onGenerated", palette.toString());
                Palette.Swatch toolbarSwatch = palette.getMutedSwatch();

                int toolbarColor = toolbarSwatch != null ? toolbarSwatch.getRgb() : ContextCompat
                        .getColor(getApplicationContext(), R.color.colorPrimary);
                int accentColor = palette
                        .getVibrantColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));

                setToolbarColor(toolbarColor);
                setAccentColor(accentColor);

                if (toolbarSwatch != null) {
                    float[] darkerShade = toolbarSwatch.getHsl();
                    darkerShade[2] = darkerShade[2] * 0.8f; //Make it a darker shade for the status bar
                    setStatusBarColor(ColorUtils.HSLToColor(darkerShade));
                } else {
                    setStatusBarColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                }
            }
        });
    }

    private void setToolbarColor(int color) {
        if (collapsingToolbarLayout != null) {
            collapsingToolbarLayout.setStatusBarScrimColor(color);
            collapsingToolbarLayout.setContentScrimColor(color);
        } else {
            actionBar.setBackgroundDrawable(new ColorDrawable(color));
        }
        floatingActionButton.setRingProgressColor(color);

        if (gradientBackground != null) {
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setColor(color);
            gradientDrawable.setAlpha(140);
            gradientBackground.setBackground(gradientDrawable);
        }
    }

    private void setAccentColor(int accentColor) {
        floatingActionButton.setColor(accentColor);

    }

    private void setStatusBarColor(int color) {
        if (isFinishing()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(color);
        }
    }

    @Override
    public void showError(String errorMessage) {
        loadingProgressBar.setVisibility(View.GONE);
        errorLayout.setVisibility(View.VISIBLE);
        errorText.setText(errorMessage);
    }

    @Override
    public void showError(@StringRes int stringRes) {
        showError(getString(stringRes));
    }

    @Override
    public void showSnackBarMessage(final int message, final String errorDetail) {
        Snackbar.make(scrollView, getString(message, errorDetail), Snackbar.LENGTH_LONG).show();

    }

    @Override
    public void showSnackBarMessage(int message) {
        Snackbar.make(scrollView, message, Snackbar.LENGTH_LONG).show();

    }

    @Override
    public void showDownloadProgress(final int downloadProgress) {
        if (progress == downloadProgress || downloadProgress == 0) {
            return;
        }
        progress = downloadProgress;
        Log.d(TAG, "Download progress:" + downloadProgress);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                floatingActionButton.setProgress(downloadProgress);
            }
        });

    }

    @Override
    public void showDownloadFinished() {
        Log.d(TAG, "Download finished");
        floatingActionButton.setProgress(100.0f);
    }

    @Override
    public void setToolbarTitle(String title) {
        toolbar.setTitle(title);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
        if (collapsingToolbarLayout != null) {
            collapsingToolbarLayout.setTitle(title);
        }
    }

    @Override
    public void setBookInfoBinding(FireBookDetails bookInfo) {
        this.bookInfo = bookInfo;
        binding.setBookInfo(bookInfo);
        if (bookInfo.isDownloadedAlready()) {
            showDownloadFinished();
        }
        loadImage(bookInfo.getFirebaseBookCoverUrl());
    }

    @Override
    public void openBook(FireBookDetails bookDetail, BookPages bookPages, String location) {
        if (isFinishing()) {
            return;
        }
        Intent intent = new Intent(BookInfoActivity.this, BookDetailActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(BookDetailActivity.Companion.getBOOK_PAGES(), bookPages);
        intent.putExtra(BookDetailActivity.Companion.getBOOK_ARG(), bookDetail.getBookTitle());
        intent.putExtra(BookDetailActivity.Companion.getLOCATION_BOOK(), location);
        startActivity(intent);
    }

    @Override
    public void showContributors(List<FireContributor> list) {
        if (list == null || list.size() == 0) {
            contributorCard.setVisibility(View.GONE);
            return;
        }
        contributorCard.setVisibility(View.VISIBLE);
        ContributorAdapter contributorAdapter = new ContributorAdapter(list, this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        contributorRecyclerView.setLayoutManager(linearLayoutManager);
        contributorRecyclerView.setAdapter(contributorAdapter);
    }

    @Override
    public void sendShareEvent(String bookTitle) {
        try {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.sharing_book_title, bookTitle));
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        } catch (ActivityNotFoundException anfe) {
            showSnackBarMessage(R.string.share_book_error_no_apps_found);
        }
    }

    @Override
    protected String getScreenName() {
        return "BookInfoActivity";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_book_info, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_share_book) {
            bookInfoPresenter.shareBookClicked(bookInfo);
            return true;
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bookInfoPresenter.detachView();
    }
}
