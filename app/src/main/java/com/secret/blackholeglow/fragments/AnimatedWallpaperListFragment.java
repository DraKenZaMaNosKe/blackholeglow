package com.secret.blackholeglow.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.adapters.WallpaperAdapter;
import com.secret.blackholeglow.models.WallpaperItem;
import com.secret.blackholeglow.systems.SubscriptionManager;
import com.secret.blackholeglow.systems.WallpaperCatalog;

import java.util.List;

public class AnimatedWallpaperListFragment extends Fragment {

    private List<WallpaperItem> wallpaperItems;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_animated_wallpapers,
                container,
                false
        );

        RecyclerView recyclerView = view.findViewById(R.id.wallpaper_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setInitialPrefetchItemCount(3);
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(3);

        androidx.recyclerview.widget.RecyclerView.RecycledViewPool viewPool =
            new androidx.recyclerview.widget.RecyclerView.RecycledViewPool();
        viewPool.setMaxRecycledViews(0, 5);
        recyclerView.setRecycledViewPool(viewPool);
        recyclerView.setNestedScrollingEnabled(true);

        SubscriptionManager.init(requireContext());
        wallpaperItems = WallpaperCatalog.get().getAll();

        WallpaperAdapter adapter = new WallpaperAdapter(
                getContext(),
                wallpaperItems,
                item -> { }
        );
        recyclerView.setAdapter(adapter);

        return view;
    }
}
