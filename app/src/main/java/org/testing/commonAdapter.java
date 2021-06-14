package org.testing;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class commonAdapter extends RecyclerView.Adapter<commonAdapter.ViewHolder> {

    public void setOnRootItemClick(commonAdapter.onRootItemClick e) {
    }

    public commonAdapter() {
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private onRootItemClick onRootItemClick;

        public void setOnRootItemClick(onRootItemClick onRootItemClick) {
            this.onRootItemClick = onRootItemClick;
        }

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.setOnClickListener(view -> {
                if (onRootItemClick != null)
                    onRootItemClick.position(getAdapterPosition(), view);
            });
        }
    }

    public interface onRootItemClick {
        void position(Integer pos, View view);
    }
}