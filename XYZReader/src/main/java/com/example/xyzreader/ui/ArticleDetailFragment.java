package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, Toolbar.OnMenuItemClickListener, View.OnClickListener {
    private static final String TAG = "ArticleDetailFragment";
    public static final String ARG_ITEM_ID = "item_id";
    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private WebView bodyView;
    private ImageView mPhotoView;
    private Toolbar mtoolbar;
    private FloatingActionButton mFab;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);
        mtoolbar = (Toolbar) mRootView.findViewById(R.id.toolbar);
        mtoolbar.inflateMenu(R.menu.activity_article_detail);
        mFab = (FloatingActionButton) mRootView.findViewById(R.id.fab);
        bodyView = (WebView) mRootView.findViewById(R.id.article_body);
        bodyView.getSettings().setStandardFontFamily("Roboto-Light");
        //Configuração do background do WebView bodyView para android.R.color.transparent
        bodyView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.setBackgroundColor(ContextCompat.getColor(view.getContext(), android.R.color.transparent));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                } else {
                    view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                }
                doAnimationFab();
            }
        });
        bodyView.setBackgroundResource(android.R.color.transparent);
        mFab.setOnClickListener(this);

        bindViews();
        return mRootView;
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }
        TextView articleTitle = (TextView) mRootView.findViewById(R.id.article_title);
        TextView articlePublished = (TextView) mRootView.findViewById(R.id.article_published);
        TextView articleBy = (TextView) mRootView.findViewById(R.id.article_by);


        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);

            mtoolbar.setOnMenuItemClickListener(this);
            mtoolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            mtoolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getActivityCast().onSupportNavigateUp();
                }
            });

            articleTitle.setText(mCursor.getString(ArticleLoader.Query.TITLE));;
            mtoolbar.setTitle(articleTitle.getText());
            articlePublished.setText( DateUtils.getRelativeTimeSpanString(
                    mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                    System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_ALL).toString()
            );
            articleBy.setText(mCursor.getString(ArticleLoader.Query.AUTHOR));
            String justifyText = String.format("<p style='text-align:justify'>%s</>", mCursor.getString(ArticleLoader.Query.BODY));
            bodyView.loadData(justifyText, "text/html; charset=utf-8", "UTF-8");
            bodyView.getSettings().setTextZoom(getActivityCast().getBodyViewTextZoom());

            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                mPhotoView.setImageBitmap(imageContainer.getBitmap());
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {

                        }
                    });

        } else {
            mRootView.setVisibility(View.GONE);
            articleTitle.setText("N/A");
            articlePublished.setText("N/A" );
            articleBy.setText("N/A" );
            bodyView.loadData("", "text/html; charset=utf-8", "UTF-8");
        }
    }


    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_share){
            shareArticle();
        } else if (item.getItemId() == R.id.action_zoom_in){
            getActivityCast().setBodyViewTextZoomIn();
            bodyView.getSettings().setTextZoom(getActivityCast().getBodyViewTextZoom());
        } else if (item.getItemId() == R.id.action_zoom_out){
            getActivityCast().setBodyViewTextZoomOut();
            bodyView.getSettings().setTextZoom(getActivityCast().getBodyViewTextZoom());
        }



        return true;
    }

    private void shareArticle() {
        startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                .setType("text/plain")
                .setText("Some sample text")
                .getIntent(), getString(R.string.action_share)));
    }

    @Override
    public void onClick(View view) {
        if ( view.equals(mFab)){
            shareArticle();
        }
    }

    public void doAnimationFab(){
        if ( getActivityCast() != null) {
            Animation animation = AnimationUtils.loadAnimation(getActivityCast(), R.anim.fab_button);
            this.mFab.startAnimation(animation);
        }
    }
}
