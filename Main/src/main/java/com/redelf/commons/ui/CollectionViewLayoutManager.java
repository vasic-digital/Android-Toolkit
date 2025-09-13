package com.redelf.commons.ui;

import android.content.Context;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.redelf.commons.logging.Console;

public class CollectionViewLayoutManager extends LinearLayoutManager {

    public CollectionViewLayoutManager(Context context) {
        super(context);

        // Disable prefetching to prevent GapWorker crashes with complex adapters
        setItemPrefetchEnabled(false);
        setInitialPrefetchItemCount(0);
    }

    @Override
    public boolean supportsPredictiveItemAnimations() {

        return true;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {

        try {

            super.onLayoutChildren(recycler, state);

        } catch (IndexOutOfBoundsException e) {

            Console.warning("LayoutManager recovered from exception: " + e.getMessage());

            detachAndScrapAttachedViews(recycler);
            requestLayout();
        }
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        try {
            // Validate adapter state before scrolling
            if (getChildCount() == 0 || state.getItemCount() == 0) {
                return 0;
            }
            
            return super.scrollVerticallyBy(dy, recycler, state);
            
        } catch (IndexOutOfBoundsException e) {
            Console.error("ScrollVerticallyBy IndexOutOfBounds: " + e.getMessage());
            // Try to recover by stopping scroll
            return 0;
        } catch (IllegalStateException e) {
            Console.error("ScrollVerticallyBy IllegalState: " + e.getMessage());
            return 0;
        } catch (Exception e) {
            Console.error("ScrollVerticallyBy unexpected error: " + e.getMessage());
            return 0;
        }
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        try {
            // Validate adapter state before scrolling
            if (getChildCount() == 0 || state.getItemCount() == 0) {
                return 0;
            }
            
            return super.scrollHorizontallyBy(dx, recycler, state);
            
        } catch (IndexOutOfBoundsException e) {
            Console.error("ScrollHorizontallyBy IndexOutOfBounds: " + e.getMessage());
            return 0;
        } catch (IllegalStateException e) {
            Console.error("ScrollHorizontallyBy IllegalState: " + e.getMessage());
            return 0;
        } catch (Exception e) {
            Console.error("ScrollHorizontallyBy unexpected error: " + e.getMessage());
            return 0;
        }
    }
}
