package helium314.keyboard.keyboard.ai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import helium314.keyboard.latin.R;

public class AiResultAdapter extends RecyclerView.Adapter<AiResultAdapter.ViewHolder> {

    private final List<String> results = new ArrayList<>();
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String result);
    }

    public AiResultAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setResults(List<String> newResults) {
        results.clear();
        results.addAll(newResults);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.ai_result_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String result = results.get(position);
        holder.resultText.setText(result);
        holder.itemView.setOnClickListener(v -> listener.onItemClick(result));
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView resultText;

        ViewHolder(View itemView) {
            super(itemView);
            resultText = itemView.findViewById(R.id.result_text);
        }
    }
}
