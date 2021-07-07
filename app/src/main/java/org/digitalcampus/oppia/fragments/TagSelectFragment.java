package org.digitalcampus.oppia.fragments;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.digitalcampus.mobile.learning.R;
import org.digitalcampus.mobile.learning.databinding.FragmentTagSelectBinding;
import org.digitalcampus.oppia.activity.DownloadCoursesActivity;
import org.digitalcampus.oppia.adapter.TagsAdapter;
import org.digitalcampus.oppia.analytics.Analytics;
import org.digitalcampus.oppia.api.ApiEndpoint;
import org.digitalcampus.oppia.listener.APIRequestListener;
import org.digitalcampus.oppia.model.Tag;
import org.digitalcampus.oppia.model.TagRepository;
import org.digitalcampus.oppia.task.result.BasicResult;
import org.digitalcampus.oppia.utils.UIUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import javax.inject.Inject;

public class TagSelectFragment extends AppFragment implements APIRequestListener {

    private static final String KEY_JSON = "json";
    private static final String KEY_TAGS = "tags";

    private JSONObject json;
    private ArrayList<Tag> tags;

    @Inject
    TagRepository tagRepository;
    @Inject
    ApiEndpoint apiEndpoint;
    private TagsAdapter adapterTags;
    private FragmentTagSelectBinding binding;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        binding = FragmentTagSelectBinding.inflate(LayoutInflater.from(getActivity()), container, false);

        getAppComponent().inject(this);

        tags = new ArrayList<>();
        adapterTags = new TagsAdapter(getActivity(), tags);

        binding.recyclerTags.setAdapter(adapterTags);
        adapterTags.setOnItemClickListener((view1, position) -> {
            Tag selectedTag = tags.get(position);
            getDownloadCoursesActivity().onTagSelected(selectedTag);

            if (isTabletLandscape()) {
                adapterTags.setSelectedTagPosition(position);
            }
        });

        return binding.getRoot();
    }

    private DownloadCoursesActivity getDownloadCoursesActivity() {
        return (DownloadCoursesActivity) getActivity();
    }

    @Override
    public void onResume(){
        super.onResume();
        // Get tags list
        if(this.json == null){
            getTagList();
        } else if ((tags != null) && !tags.isEmpty()) {
            //We already have loaded JSON and tags (coming from orientationchange)
            adapterTags.notifyDataSetChanged();
        }
        else{
            //The JSON is downloaded but tag list is not
            refreshTagList();
        }
    }

    @Override
    public void onPause(){
        hideProgressDialog();
        super.onPause();
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        onRestoreInstanceState(savedInstanceState);
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        try {
            if (savedInstanceState.containsKey(KEY_TAGS)) {
                Serializable savedTags = savedInstanceState.getSerializable(KEY_TAGS);
                if (savedTags != null){
                    ArrayList<Tag> savedTagsList = (ArrayList<Tag>) savedTags;
                    this.tags.addAll(savedTagsList);
                }
            }

            if (savedInstanceState.containsKey(KEY_JSON)) {
                this.json = new JSONObject(savedInstanceState.getString(KEY_JSON));
            }
        } catch (Exception e) {
            Analytics.logException(e);
            Log.d(TAG, "Error restoring saved state: ", e);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if (json != null){
            //Only save the instance if the request has been proccessed already
            savedInstanceState.putString(KEY_JSON, json.toString());
            savedInstanceState.putSerializable(KEY_TAGS, tags);
        }
    }

    private void getTagList() {
        showProgressDialog(getString(R.string.loading));

        tagRepository.getTagList(getActivity(), this, apiEndpoint);
    }

    public void refreshTagList() {
        tags.clear();
        try {
            tagRepository.refreshTagList(tags, json);

            adapterTags.notifyDataSetChanged();
            binding.emptyState.setVisibility((tags.isEmpty()) ? View.VISIBLE : View.GONE);

        } catch (JSONException e) {
            Analytics.logException(e);
            Log.d(TAG, "Error refreshing tag list: ", e);
        }

    }

    public void apiRequestComplete(BasicResult result) {
        hideProgressDialog();

        Callable<Boolean> finishActivity = () -> {
            getActivity().finish();
            return true;
        };

        if(result.isSuccess()){
            try {
                json = new JSONObject(result.getResultMessage());
                refreshTagList();
            } catch (JSONException e) {
                Analytics.logException(e);
                Log.d(TAG, "Error conencting to server: ", e);
                UIUtils.showAlert(getActivity(), R.string.loading, R.string.error_connection, finishActivity);
            }
        } else {
            String errorMsg = result.getResultMessage();
            UIUtils.showAlert(getActivity(), R.string.error, errorMsg, finishActivity);
        }

    }

}
