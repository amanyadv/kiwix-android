package org.kiwix.kiwixmobile.readinglists;


import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter_extensions.ActionModeHelper;

import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.database.KiwixDatabase;
import org.kiwix.kiwixmobile.database.ReadingListFolderDao;
import org.kiwix.kiwixmobile.readinglists.entities.BookmarkArticle;
import org.kiwix.kiwixmobile.readinglists.entities.ReadinglistFolder;
import org.kiwix.kiwixmobile.readinglists.lists.ReadingListArticleItem;
import org.kiwix.kiwixmobile.readinglists.lists.ReadingListItem;

import java.util.ArrayList;
import java.util.Set;

/**
 * A simple {@link Fragment} subclass.
 */
public class ReadingListFragment extends Fragment implements FastAdapter.OnClickListener<ReadingListArticleItem> {


    private FastAdapter<ReadingListArticleItem> fastAdapter;
    private ItemAdapter<ReadingListArticleItem> itemAdapter;
    private final String FRAGMENT_ARGS_FOLDER_TITLE = "requested_folder_title";
    private ReadingListFolderDao readinglistFoldersDao;
    private ArrayList<BookmarkArticle> articles;
    private ActionModeHelper mActionModeHelper;
    private RecyclerView readinglistRecyclerview;
    private String folderTitle = null;


    public ReadingListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_readinglist, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        folderTitle = this.getArguments().getString(FRAGMENT_ARGS_FOLDER_TITLE);
        setUpToolbar();
    }

    private void setUpToolbar() {
        ActionBar toolbar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (folderTitle != null && toolbar != null) {
            toolbar.setTitle(folderTitle);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_reading_list, container, false);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        readinglistRecyclerview = (RecyclerView) view.findViewById(R.id.readinglist_articles_list);

        setupFastAdapter();

        // shoud be injected in presenter when moving to mvp
        readinglistFoldersDao = new ReadingListFolderDao(KiwixDatabase.getInstance(getActivity()));
        loadArticlesOfFolder();

    }


    private void setupFastAdapter() {

        mActionModeHelper = new ActionModeHelper(fastAdapter, R.menu.actionmenu_readinglist, new ActionBarCallBack());

        fastAdapter = new FastAdapter<>();
        itemAdapter = new ItemAdapter<>();

        fastAdapter.withOnClickListener(this);
        fastAdapter.withSelectOnLongClick(false);
        fastAdapter.withSelectable(false);
        fastAdapter.withMultiSelect(true);
        readinglistRecyclerview.setLayoutManager(new LinearLayoutManager(getActivity()));

        readinglistRecyclerview.setAdapter(itemAdapter.wrap(fastAdapter));


        fastAdapter.withOnPreClickListener((v, adapter, item, position) -> {
            //we handle the default onClick behavior for the actionMode. This will return null if it didn't do anything and you can handle a normal onClick
            Boolean res = mActionModeHelper.onClick(item);
            return res != null ? res : false;
        });

        fastAdapter.withOnClickListener((v, adapter, item, position) -> {
//                Toast.makeText(v.getContext(), "SelectedCount: " + fastAdapter.getSelections().size() + " ItemsCount: " + fastAdapter.getSelectedItems().size(), Toast.LENGTH_SHORT).show();
            return false;
        });

        fastAdapter.withOnPreLongClickListener((v, adapter, item, position) -> {
            ActionMode actionMode = mActionModeHelper.onLongClick((AppCompatActivity)getActivity(),position);

            if (actionMode != null) {
                //we want color our CAB
                getActivity().findViewById(R.id.toolbar).setBackgroundColor(getResources().getColor(R.color.blue_grey));
            }

            //if we have no actionMode we do not consume the event
            return actionMode != null;
        });
    }


    void loadArticlesOfFolder() {
        articles = readinglistFoldersDao.getArticlesOfFolder(new ReadinglistFolder(folderTitle));
        for (BookmarkArticle article: articles) {
            itemAdapter.add(new ReadingListArticleItem(article.getBookmarkTitle()));
        }
    }


    private void deleteSelectedItems() {
        Set<ReadingListArticleItem> selectedItems = fastAdapter.getSelectedItems();
        readinglistFoldersDao.deleteArticles(selectedItems);
        loadArticlesOfFolder();
    }

    @Override
    public boolean onClick(View v, IAdapter<ReadingListArticleItem> adapter, ReadingListArticleItem item, int position) {

        Intent intent = new Intent(getActivity(), KiwixMobileActivity.class);
        if (!item.getArticle_url().equals("null")) {
            intent.putExtra("choseXURL", item.getArticle_url());
        } else {
            intent.putExtra("choseXTitle", item.getTitle());
        }
        intent.putExtra("bookmarkClicked", true);
        getActivity().finish();
        return true;
    }


    /**
     * Our ActionBarCallBack to showcase the CAB
     */
    class ActionBarCallBack implements ActionMode.Callback {

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

            switch (item.getItemId()) {
                case R.id.menu_bookmarks_delete:
                    deleteSelectedItems();
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
    }







}